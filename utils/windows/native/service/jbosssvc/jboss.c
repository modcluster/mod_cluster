/*
 *  JBOSSSVC - JBoss Service helper
 *
 *  Copyright(c) 2007 Red Hat Middleware, LLC,
 *  and individual contributors as indicated by the @authors tag.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library in the file COPYING.LIB;
 *  if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 * @author Mladen Turk
 */

/*
 * Ignore most warnings (back down to /W3) for poorly constructed headers
 */
#if defined(_MSC_VER) && _MSC_VER >= 1200
#pragma warning(push, 3)
#endif

/* disable or reduce the frequency of...
 *   C4057: indirection to slightly different base types
 *   C4075: slight indirection changes (unsigned short* vs short[])
 *   C4100: unreferenced formal parameter
 *   C4127: conditional expression is constant
 *   C4163: '_rotl64' : not available as an intrinsic function
 *   C4201: nonstandard extension nameless struct/unions
 *   C4244: int to char/short - precision loss
 *   C4514: unreferenced inline function removed
 */
#pragma warning(disable: 4100 4127 4163 4201 4514; once: 4057 4075 4244)

/* Ignore Microsoft's interpretation of secure development
 * and the POSIX string handling API
 */
#if defined(_MSC_VER) && _MSC_VER >= 1400
#ifndef _CRT_SECURE_NO_DEPRECATE
#define _CRT_SECURE_NO_DEPRECATE
#endif
#pragma warning(disable: 4996)
#endif

#ifndef _WINDOWS_
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#ifndef _WIN32_WINNT
/* Restrict the server to a subset of Windows 2000 header files by default
 */
#define _WIN32_WINNT 0x0500
#endif
#endif

#include <windows.h>
#include <tlhelp32.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <process.h>
#include <time.h>

/* Custom return error values */
#define ERR_RET_USAGE           1
#define ERR_RET_VERSION         2
#define ERR_RET_INSTALL         3
#define ERR_RET_REMOVE          4
#define ERR_RET_PARAMS          5
#define ERR_RET_MODE            6

#define MSG_ERROR               0xC0000001L
#define MSG_INFO                0x40000002L



#define MAX_CMDLINE             8192
/* Extensions On, Old quote style */
static LPCSTR CMD_DEFAULT     = "/E:ON /S /C \"SET JSERVICE_PPID=%d&&SET JSERVICE_NAME=%s&&CALL %s %s";
static LPCSTR REGSERVICE_ROOT = "SYSTEM\\CurrentControlSet\\Services\\";
static LPCSTR REGSERVICE_LOG  = "SYSTEM\\CurrentControlSet\\Services\\EventLog\\Application\\";

static LPCSTR REGPARAMS       = "\\Parameters";
static LPCSTR REGDESCRIPTION  = "Description";
static LPCSTR REG_SERVFILE    = "ServiceFile";
static LPCSTR REG_LOGFILE     = "LogFile";
static LPCSTR REG_WPATH       = "WorkingPath";


/* Main servic table entry
 * filled at run-time
 */
static SERVICE_TABLE_ENTRY  _service_table[] = {
        {NULL, NULL},
        {NULL, NULL}
};

static SERVICE_STATUS        _service_status;
static SERVICE_STATUS_HANDLE _service_status_handle = NULL;
static char                  _service_name[MAX_PATH + 1];
static char                  _service_disp[MAX_PATH + 1];
static char                  _service_desc[MAX_PATH * 2 + 1];
static char                  _working_path[MAX_PATH + 1];
static char                  _service_image[MAX_PATH + 1];
static char                  _service_bat[MAX_PATH + 1];
static char                  _cmd_exe[MAX_PATH + 1];
static BOOL                  _service_log  = FALSE;
static DWORD                 _service_auto = SERVICE_AUTO_START;

enum _service_mode_e {
    mode_none,
    mode_install,
    mode_uninstall,
    mode_debug,
    mode_run,
    mode_signal
};

static enum   _service_mode_e   _service_mode = mode_none;
static HANDLE _service_run_event = NULL;
static HANDLE _service_run_hproc = NULL;
static DWORD  _service_run_pid   = 0;

static BOOL IsWindowsNT()
{
    BOOL rv = FALSE;
    OSVERSIONINFO osvi;

    ZeroMemory(&osvi, sizeof(OSVERSIONINFO));
    osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFO);

    if (!GetVersionEx(&osvi))
        return FALSE;

    switch (osvi.dwPlatformId) {
        case VER_PLATFORM_WIN32_NT:
            rv = TRUE;
        break;
        default:
            rv = FALSE;
        break;
    }
    return rv;
}

static DWORD WaitToKillServiceTimeout()
{
    static DWORD dwTimeout = 0;

    if (dwTimeout) {
        return dwTimeout;
    }
    else {
        DWORD   err;
        HKEY    hKey;
        CHAR    wsBuf[MAX_PATH];
        DWORD   dwLen, dwType = REG_SZ;

        dwTimeout = 20000;
        if ((err = RegOpenKeyExA(HKEY_LOCAL_MACHINE,
                                 "SYSTEM\\CurrentControlSet\\Control",
                                 0,
                                 KEY_READ,
                                 &hKey)) != ERROR_SUCCESS) {
            return dwTimeout;
        }

        dwLen = sizeof(wsBuf);
        err = RegQueryValueExA(hKey,
                               "WaitToKillServiceTimeout",
                               NULL,
                               &dwType,
                               (LPBYTE)wsBuf,
                               &dwLen);
        RegCloseKey(hKey);
        if (err == ERROR_SUCCESS) {
            dwTimeout = (DWORD)atoi(wsBuf);
            if (!dwTimeout)
                dwTimeout = 20000;
        }

        return dwTimeout;
    }
}

/* To share the semaphores with other processes, we need a NULL ACL
 * Code from MS KB Q106387
 */
static PSECURITY_ATTRIBUTES GetNullACL()
{
    PSECURITY_DESCRIPTOR pSD;
    PSECURITY_ATTRIBUTES sa;

    sa  = (PSECURITY_ATTRIBUTES)LocalAlloc(LPTR, sizeof(SECURITY_ATTRIBUTES));
    sa->nLength = sizeof(sizeof(SECURITY_ATTRIBUTES));

    pSD = (PSECURITY_DESCRIPTOR)LocalAlloc(LPTR, SECURITY_DESCRIPTOR_MIN_LENGTH);
    sa->lpSecurityDescriptor = pSD;

    if (pSD == NULL || sa == NULL) {
        return NULL;
    }
    SetLastError(0);
    if (!InitializeSecurityDescriptor(pSD, SECURITY_DESCRIPTOR_REVISION)
        || GetLastError()) {
        LocalFree(pSD);
        LocalFree(sa);
        return NULL;
    }
    if (!SetSecurityDescriptorDacl(pSD, TRUE, (PACL) NULL, FALSE)
        || GetLastError()) {
        LocalFree(pSD );
        LocalFree(sa);
        return NULL;
    }

    sa->bInheritHandle = FALSE;
    return sa;
}


static void CleanNullACL(PSECURITY_ATTRIBUTES sa)
{
    if (sa) {
        LocalFree(sa->lpSecurityDescriptor);
        LocalFree(sa);
    }
}

static void AddToMessageLog(BOOL isError, LPSTR szFormat, ...)
{
    char szMsg [MAX_PATH];
    LPSTR   lpszStrings[2];
    LPVOID  lpMsgBuf = NULL;
    HANDLE  hEventSource;
    DWORD dwErr = GetLastError();
    WORD  wErrType;
    DWORD dwErrId;
    WORD  nStr;
    va_list args;

    if (!_service_log && !isError) {
        /* Nothing to log */
        return;
    }

    va_start(args, szFormat);
    vsprintf(szMsg, szFormat, args);
    va_end(args);

    if (isError) {
        nStr = 2;
        wErrType = EVENTLOG_ERROR_TYPE;
        dwErrId = MSG_ERROR;
        FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER |
                        FORMAT_MESSAGE_FROM_SYSTEM |
                        FORMAT_MESSAGE_IGNORE_INSERTS,
                        NULL, GetLastError(),
                        MAKELANGID(LANG_NEUTRAL, LANG_NEUTRAL),
                        (LPSTR) &lpMsgBuf, 0, NULL);
        lpszStrings[0] = lpMsgBuf;
        lpszStrings[1] = szMsg;
    }
    else {
        wErrType = EVENTLOG_INFORMATION_TYPE;
        dwErrId = MSG_INFO;
        nStr = 1;
        lpszStrings[0] = szMsg;
    }
    /* Use event logging to log the error.
    */
    hEventSource = RegisterEventSource(NULL, _service_name);

    if (hEventSource != NULL) {
        ReportEvent(hEventSource, // handle of event source
            wErrType,             // event type
            0,                    // event category
            dwErrId,              // event ID
            NULL,                 // current user's SID
            nStr,                 // strings in lpszStrings
            0,                    // no bytes of raw data
            lpszStrings,          // array of error strings
            NULL);                // no raw data
        DeregisterEventSource(hEventSource);
    }
    if (lpMsgBuf)
        LocalFree(lpMsgBuf);
}

static void ErrorUsage()
{
    if (_service_mode == mode_run)
        return;

    fprintf(stderr, "jbosssvc -- program for running batch files as services.\n\n");
    fprintf(stderr, "Usage: jbosssvc -i        service service.bat\n");
    fprintf(stderr, "                -i[wdcl]  service workingpath description comment\n"
                    "                          service.bat\n");
    fprintf(stderr, "                -u        service\n");
    fprintf(stderr, "                -t        service\n");
    fprintf(stderr, "                -s\n");
    fprintf(stderr, "Options:\n");
    fprintf(stderr, "   -d   Service display name\n");
    fprintf(stderr, "   -c   Service description\n");
    fprintf(stderr, "   -w   Service working path\n");
    fprintf(stderr, "   -l   Turn info logging On\n");
    fprintf(stderr, "   -s   Sleep 1 second and exit\n");
    fprintf(stderr, "   -m   Start service manually\n");

    ExitProcess(ERR_RET_USAGE);
}

static void DumpParams()
{
    if (_service_mode == mode_run)
        return;

    fprintf(stdout, "Name           %s\n", _service_name);
    fprintf(stdout, "Display        %s\n", _service_disp);
    fprintf(stdout, "Description    %s\n", _service_desc);
    fprintf(stdout, "ImagePath      %s\n", _service_image);
    fprintf(stdout, "Shell          %s\n", _cmd_exe);
    fprintf(stdout, "WorkingPath    %s\n", _working_path);
    fprintf(stdout, "Service Script %s\n", _service_bat);

}

static BOOL IsServiceRunning(LPCSTR szServiceName)
{
    DWORD rc = 0;
    SC_HANDLE schService;
    SC_HANDLE schSCManager;
    SERVICE_STATUS schSStatus;

    schSCManager = OpenSCManager(NULL, NULL,
                                 SC_MANAGER_CONNECT);
    if (!schSCManager) {
        AddToMessageLog(TRUE, "Unable to OpenSCManager %s", szServiceName);
        return FALSE;
    }
    schService = OpenService(schSCManager, szServiceName,
                             SERVICE_QUERY_STATUS);
    if (schService != NULL) {
        if (QueryServiceStatus(schService, &schSStatus))
            rc = schSStatus.dwCurrentState;
        else
            AddToMessageLog(TRUE, "Unable to QueryServiceStatus for %s",
                            szServiceName);
        CloseServiceHandle(schService);
        CloseServiceHandle(schSCManager);
        return rc == SERVICE_RUNNING ? TRUE : FALSE;
    }
    else
        AddToMessageLog(TRUE, "Unable to OpenService for %s",
                        szServiceName);

    CloseServiceHandle(schSCManager);
    return FALSE;

}

static void BuildCommandLine(char *buf, const char *action)
{

    ZeroMemory(buf, MAX_CMDLINE);
    lstrcpy(buf, _cmd_exe);
    lstrcat(buf, " ");
    buf += lstrlen(buf);

    sprintf(buf, CMD_DEFAULT, _getpid(), _service_name, _service_bat, action);
    lstrcat(buf, "\"");
    fprintf(stdout, "Service Cmd    %s\n", buf);
}

/* We could use the ChangeServiceConfig2 on WIN2K+
 * For now use the registry.
 */
static BOOL SetServiceDescription(LPCSTR szServiceName,
                                  LPCSTR szDescription)
{
    HKEY  hKey;
    CHAR  szName[MAX_PATH + 1];
    DWORD rc;

    if (lstrlen(szServiceName) > MAX_PATH)
        return FALSE;
    lstrcpy(szName, REGSERVICE_ROOT);
    lstrcat(szName, szServiceName);

    rc = RegOpenKeyEx(HKEY_LOCAL_MACHINE, szName, 0, KEY_WRITE, &hKey);
    if (rc != ERROR_SUCCESS) {
        return FALSE;
    }

    rc = RegSetValueEx(hKey, REGDESCRIPTION, 0, REG_SZ,
                       (CONST BYTE *)szDescription,
                       lstrlen(szDescription) + 1);
    CloseHandle(hKey);

    return rc == ERROR_SUCCESS;
}

/* We could use the ChangeServiceConfig2 on WIN2K+
 * For now use the registry.
 */
static BOOL SetServiceEventLog(LPCSTR szServiceName)
{
    HKEY  hKey;
    CHAR  szName[MAX_PATH + 1];
    DWORD dwData;

    if (lstrlen(szServiceName) > MAX_PATH)
        return FALSE;
    lstrcpy(szName, REGSERVICE_LOG);
    lstrcat(szName, szServiceName);

    RegCreateKey(HKEY_LOCAL_MACHINE, szName, &hKey);

    if (!GetModuleFileName(NULL, szName, MAX_PATH)) {
        RegCloseKey(hKey);
        return FALSE;
    }

    RegSetValueEx(hKey, "EventMessageFile", 0, REG_SZ, (LPBYTE)szName,
                  lstrlen(szName) + 1);
    dwData = EVENTLOG_ERROR_TYPE | EVENTLOG_WARNING_TYPE |
             EVENTLOG_INFORMATION_TYPE;

    RegSetValueEx(hKey, "TypesSupported", 0, REG_DWORD, (LPBYTE)&dwData,
                  sizeof(DWORD));

    RegFlushKey(hKey);
    RegCloseKey(hKey);

    return TRUE;
}

BOOL SetServiceParameters(LPCSTR szServiceName)
{
    HKEY  hKey;
    CHAR  szName[MAX_PATH + 1];
    DWORD rc;
    BOOL  rv = TRUE;

    if (lstrlen(szServiceName) > MAX_PATH)
        return FALSE;

    lstrcpy(szName, REGSERVICE_ROOT);
    lstrcat(szName, szServiceName);
    lstrcat(szName, REGPARAMS);

    rc = RegCreateKeyEx(HKEY_LOCAL_MACHINE,
                        szName,
                        0,
                        NULL,
                        0,
                        KEY_WRITE,
                        NULL,
                        &hKey,
                        NULL);
    if (rc != ERROR_SUCCESS) {
        rv = FALSE;
        goto cleanup;
    }
    rc = RegSetValueEx(hKey, REG_SERVFILE, 0, REG_SZ,
                       (CONST BYTE *)_service_bat,
                       lstrlen(_service_bat) + 1);
    if (rc != ERROR_SUCCESS) {
        rv = FALSE;
        goto cleanup;
    }
    if (_service_log) {
        rc = RegSetValueEx(hKey, REG_LOGFILE, 0, REG_DWORD,
            (CONST BYTE *)&_service_log,
            sizeof(DWORD));
        if (rc != ERROR_SUCCESS) {
            rv = FALSE;
            goto cleanup;
        }
    }
    if (lstrlen(_working_path)) {
        rc = RegSetValueEx(hKey, REG_WPATH, 0, REG_SZ,
            (CONST BYTE *)_working_path,
            lstrlen(_working_path) + 1);
        if (rc != ERROR_SUCCESS) {
            rv = FALSE;
            goto cleanup;
        }
    }

cleanup:
    CloseHandle(hKey);
    return rv;
}

BOOL GetServiceParameters(LPCSTR szServiceName)
{
    HKEY  hKey;
    CHAR  szName[MAX_PATH + 1];
    DWORD rc;
    BOOL  rv = TRUE;
    DWORD dwType;
    DWORD dwSize;

    if (lstrlen(szServiceName) > MAX_PATH)
        return FALSE;
    lstrcpy(szName, REGSERVICE_ROOT);
    lstrcat(szName, szServiceName);
    lstrcat(szName, REGPARAMS);
    rc = RegOpenKeyEx(HKEY_LOCAL_MACHINE, szName, 0, KEY_READ, &hKey);
    if (rc != ERROR_SUCCESS) {
        return FALSE;
    }
    dwSize = MAX_PATH + 1;
    rc = RegQueryValueEx(hKey, REG_SERVFILE, NULL, &dwType,
                         (LPBYTE)_service_bat, &dwSize);
    if (rc != ERROR_SUCCESS || dwType != REG_SZ) {
        rv = FALSE;
        goto cleanup;
    }
    dwSize = sizeof(DWORD);
    rc = RegQueryValueEx(hKey, REG_LOGFILE, NULL, &dwType,
                         (LPBYTE)&_service_log, &dwSize);
    if (rc != ERROR_SUCCESS || dwType != REG_DWORD) {
        _service_log = FALSE;
    }
    dwSize = MAX_PATH + 1;
    rc = RegQueryValueEx(hKey, REG_WPATH, NULL, &dwType,
                         (LPBYTE)_working_path, &dwSize);
    if (rc != ERROR_SUCCESS || dwType != REG_SZ) {
        _working_path[0] = '\0';
    }

    if (GetSystemDirectory(szName, MAX_PATH + 1)) {
        lstrcat(szName, "\\cmd.exe");
        if (strchr(_cmd_exe, ' ')) {
            _cmd_exe[0] = '"';
            lstrcpy(&_cmd_exe[1], szName);
            lstrcat(_cmd_exe, "\"");
        }
        else
            lstrcpy(_cmd_exe, szName);
    }

cleanup:
    CloseHandle(hKey);
    return rv;
}

BOOL InstallService(LPCSTR szServiceName)
{
    char szImage[MAX_PATH + 1];
    char szPath[MAX_PATH + 1];
    char *p;
    BOOL rv = TRUE;
    SC_HANDLE hManager;
    SC_HANDLE hService;

    if (!(hManager = OpenSCManager(NULL, NULL, SC_MANAGER_CREATE_SERVICE))) {
        return FALSE;
    }

    p = &szImage[1];
    if (!GetModuleFileName(NULL, p, MAX_PATH)) {
        rv = FALSE;
        goto cleanup;
    }
    lstrcpy(szPath, p);

    if ((p = strrchr(szPath, '\\'))) {
        *p = '\0';
    }
    szImage[0] = '"';
    lstrcat(szImage, "\" -r ");
    lstrcat(szImage, szServiceName);
    if (!_working_path[0])
        lstrcpy(_working_path, szPath);

    hService = CreateService(hManager,                  // SCManager database
                             _service_name,             // name of service
                             _service_disp,             // name to display
                             SERVICE_ALL_ACCESS,        // access required
                             SERVICE_WIN32_OWN_PROCESS, // service type
                             _service_auto,             // start type
                             SERVICE_ERROR_NORMAL,      // error control type
                             szImage,                   // service's binary
                             NULL,                      // no load svc group
                             NULL,                      // no tag identifier
                             "Tcpip\0Afd\0",            // dependencies
                             NULL,                      // use SYSTEM account
                             NULL);                     // no password
    if (!hService) {
        rv = FALSE;
        goto cleanup;
    }
cleanup:
    if (hService)
        CloseServiceHandle(hService);
    CloseServiceHandle(hManager);

    return rv;
}

BOOL RemoveService(LPCSTR szServiceName)
{
    BOOL rv = TRUE;
    SC_HANDLE hManager;
    SC_HANDLE hService;

    if (!(hManager = OpenSCManager(NULL, NULL, SC_MANAGER_CONNECT))) {
        return FALSE;
    }
    hService = OpenService(hManager, _service_name, DELETE);
    if (!hService) {
        rv = FALSE;
        goto cleanup;
    }

    rv = DeleteService(hService);

cleanup:
    if (hService)
        CloseServiceHandle(hService);
    CloseServiceHandle(hManager);
    return rv;

}

/* Report the service status to the SCM
 */
BOOL ReportServiceStatus(DWORD dwCurrentState,
                         DWORD dwWin32ExitCode,
                         DWORD dwWaitHint)
{
   static DWORD dwCheckPoint = 1;
   BOOL fResult = TRUE;

   if (_service_mode == mode_run && _service_status_handle) {
       if (dwCurrentState == SERVICE_START_PENDING)
            _service_status.dwControlsAccepted = 0;
        else
            _service_status.dwControlsAccepted = SERVICE_ACCEPT_STOP;

       _service_status.dwCurrentState  = dwCurrentState;
       _service_status.dwWin32ExitCode = dwWin32ExitCode;
       _service_status.dwWaitHint      = dwWaitHint;

       if ((dwCurrentState == SERVICE_RUNNING) ||
           (dwCurrentState == SERVICE_STOPPED))
           _service_status.dwCheckPoint = 0;
       else
           _service_status.dwCheckPoint = dwCheckPoint++;
       fResult = SetServiceStatus(_service_status_handle, &_service_status);
       if (!fResult) {
           /* TODO: Deal with error */
       }
   }
   return fResult;
}

static BOOL RunChildProcess(LPCSTR szApplication, LPSTR szCmdLine,
                            LPPROCESS_INFORMATION lpprInfo)
{
    STARTUPINFO stInfo;
    BOOL bResult;
    PSECURITY_ATTRIBUTES sa;

    ZeroMemory(&stInfo, sizeof(stInfo));
    stInfo.cb = sizeof(stInfo);
    stInfo.dwFlags = STARTF_USESHOWWINDOW;
    stInfo.wShowWindow = SW_HIDE;
    sa = GetNullACL();
    bResult = CreateProcess(szApplication,
                            szCmdLine,
                            sa,
                            sa,
                            TRUE,
                            CREATE_NEW_PROCESS_GROUP,
                            NULL,
                            _working_path,
                            &stInfo,
                            lpprInfo);

    CleanNullACL(sa);
    return bResult;
}

static BOOL TerminateProcessGroup(HANDLE hProcess, DWORD dwProcessId, UINT retCode)
{
    HANDLE hProcessSnap;
    PROCESSENTRY32 pe32;

    hProcessSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (hProcessSnap == INVALID_HANDLE_VALUE) {
        return FALSE;
    }

    pe32.dwSize = sizeof(PROCESSENTRY32);

    if (!Process32First(hProcessSnap, &pe32)) {
        CloseHandle(hProcessSnap);
        return FALSE;
    }

    do {
        if (pe32.th32ParentProcessID == dwProcessId) {
            HANDLE hP = OpenProcess(PROCESS_ALL_ACCESS, FALSE, pe32.th32ProcessID);
            if (hP) {
                TerminateProcess(hP, retCode);
                CloseHandle(hP);
            }
        }
    } while (Process32Next(hProcessSnap, &pe32));

    CloseHandle(hProcessSnap);
    Sleep(500);
    return TerminateProcess(hProcess, retCode);

}

/* Executed when the service receives stop event */
static DWORD ServiceStop()
{
    int step = 14000;
    PROCESS_INFORMATION prInfo;
    char cmd[MAX_CMDLINE + 1];
    time_t s;
    double t;

    if (!IsServiceRunning(_service_name)) {
        AddToMessageLog(FALSE, "Service %s is already stopped", _service_name);
        return 0;
    }

    t = WaitToKillServiceTimeout() / 1000 - 2;
    time(&s);
    BuildCommandLine(cmd, "stop");
    AddToMessageLog(FALSE, "Stopping service %s", _service_name);

    ReportServiceStatus(SERVICE_STOP_PENDING, NO_ERROR,
                        WaitToKillServiceTimeout());

    while (RunChildProcess(_cmd_exe, cmd, &prInfo)) {
        WaitForSingleObject(prInfo.hProcess, INFINITE);
        AddToMessageLog(FALSE, "Stopped service %s", _service_name);
        CloseHandle(prInfo.hProcess);
        CloseHandle(prInfo.hThread);

        ReportServiceStatus(SERVICE_STOP_PENDING, NO_ERROR, 0);
        if (WaitForSingleObject(_service_run_event, step) == WAIT_OBJECT_0)
            break;

        if (step > 2000)
            step = 2000;
        if (difftime(time(NULL), s) > t) {
            ReportServiceStatus(SERVICE_STOP_PENDING, NO_ERROR, 0);
            TerminateProcessGroup(_service_run_hproc, _service_run_pid, 1);
            AddToMessageLog(FALSE, "Service terminated %s", _service_name);
        }
    }
    ReportServiceStatus(SERVICE_STOPPED, NO_ERROR, 0);
    AddToMessageLog(FALSE, "Stopping service %s", _service_name);
    return 0;
}

/* Executed when the service receives restart event */
static DWORD ServiceRestart()
{
    DWORD rv;
    PROCESS_INFORMATION prInfo;
    char cmd[MAX_CMDLINE + 1];

    BuildCommandLine(cmd, "restart");
    ReportServiceStatus(SERVICE_STOP_PENDING, NO_ERROR, 0);
    if (RunChildProcess(_cmd_exe, cmd, &prInfo)) {
        ReportServiceStatus(SERVICE_RUNNING, NO_ERROR, 0);
        rv = WaitForSingleObject(prInfo.hProcess, INFINITE);
        printf("Restart Wait %d %d\n", rv, WAIT_OBJECT_0);
        CloseHandle(prInfo.hProcess);
        CloseHandle(prInfo.hThread);
        rv = 0;
    }
    else {
        AddToMessageLog(TRUE, "Restarting service %s", _service_name);
        rv = 1;
    }
    ReportServiceStatus(SERVICE_STOP_PENDING, NO_ERROR, 0);
    ReportServiceStatus(SERVICE_STOPPED, NO_ERROR, 0);

    return rv;
}

/* Service controll handler
 */
void WINAPI ServiceCtrlHandler(DWORD dwCtrlCode)
{

    switch (dwCtrlCode) {
        case SERVICE_CONTROL_STOP:
            /* Call the stop handler that will actualy stop the service */
            ServiceStop();
            return;
        case SERVICE_CONTROL_INTERROGATE:
        break;
        default:
        break;
   }
   ReportServiceStatus(_service_status.dwCurrentState, NO_ERROR, 0);
}

/* Console control handler
 *
 */
BOOL WINAPI ConsoleHandler(DWORD dwCtrlType)
{

    switch (dwCtrlType) {
        case CTRL_BREAK_EVENT:
            if (_service_mode == mode_run) {
                ServiceStop();
                return TRUE;
            }
            else
                return FALSE;
        break;
        case CTRL_C_EVENT:
        case CTRL_CLOSE_EVENT:
        case CTRL_SHUTDOWN_EVENT:
            ServiceStop();
            return TRUE;
        break;
        case CTRL_LOGOFF_EVENT:
            if (_service_mode == mode_debug) {
                ServiceStop();
            }
            return TRUE;
        break;
    }

    return FALSE;
}

/* Executed when the service receives start event */
static DWORD ServiceStart()
{
    DWORD rv;
    PROCESS_INFORMATION prInfo;
    char cmd[MAX_CMDLINE + 1];

    BuildCommandLine(cmd, "start");

    DumpParams();

    AddToMessageLog(FALSE, "Starting service %s", _service_name);
    ResetEvent(_service_run_event);
    if (RunChildProcess(_cmd_exe, cmd, &prInfo)) {
        AddToMessageLog(FALSE, "Started service %s", _service_name);
        ReportServiceStatus(SERVICE_RUNNING, NO_ERROR, 0);
        SetConsoleCtrlHandler((PHANDLER_ROUTINE)ConsoleHandler, TRUE);

        _service_run_hproc = prInfo.hProcess;
        _service_run_pid   = prInfo.dwProcessId;
        rv = WaitForSingleObject(prInfo.hProcess, INFINITE);
        _service_run_hproc = NULL;
        _service_run_pid   = 0;

        AddToMessageLog(FALSE, "Finished service %s", _service_name);
        ReportServiceStatus(SERVICE_STOP_PENDING, NO_ERROR, 0);
        SetEvent(_service_run_event);
        ReportServiceStatus(SERVICE_STOPPED, NO_ERROR, 0);

        CloseHandle(prInfo.hProcess);
        CloseHandle(prInfo.hThread);
        return 0;
    }
    else {
        AddToMessageLog(TRUE, "Starting service %s", _service_name);
        return 1;
    }
}

/* Main service execution loop */
void WINAPI ServiceMain(DWORD argc, LPTSTR *argv)
{
    DWORD rc;
    _service_status.dwServiceType      = SERVICE_WIN32_OWN_PROCESS;
    _service_status.dwCurrentState     = SERVICE_START_PENDING;
    _service_status.dwControlsAccepted = SERVICE_ACCEPT_STOP;
    _service_status.dwWin32ExitCode    = 0;
    _service_status.dwCheckPoint       = 0;
    _service_status.dwWaitHint         = 0;
    _service_status.dwServiceSpecificExitCode = 0;

    if (_service_mode == mode_run) {
        /* Register Service Control handler */
        _service_status_handle = RegisterServiceCtrlHandler(_service_name,
                                                            ServiceCtrlHandler);
        if (!_service_status_handle) {
            AddToMessageLog(TRUE, "RegisterServiceCtrlHandler failed for %s",
                            _service_name);
            goto cleanup;
        }
    }
    ReportServiceStatus(SERVICE_START_PENDING, NO_ERROR, 3000);
    if ((rc = ServiceStart()) == 0) {
        AddToMessageLog(FALSE, "Service %s Main finished", _service_name);
    }

    return;
cleanup:
    /* Cleanup */
    ReportServiceStatus(SERVICE_STOPPED, ERROR_SERVICE_SPECIFIC_ERROR, 0);
    return;
}

static void PrintInfo(int mode, const char *msg)
{
    int padd   = 0;
    struct tm   *t;
    time_t       c;
    char buff[128];

    if (msg) {
        fputs(msg, stdout);
        padd++;
    }
    time(&c);
    if ((mode & 0x03) == 1) {
        if (padd++)
            fputc(' ', stdout);
        t = localtime(&c);
        strftime(buff, 128, "[%Y-%m-%d %H:%M:%S]", t);
        fputs(buff, stdout);
    }
    if ((mode & 0x03) == 2) {
        if (padd++)
            fputc(' ', stdout);
        fputc('[', stdout);
        t = gmtime(&c);
        strftime(buff, 128, "[%Y-%m-%d %H:%M:%S GMT]", t);
        fputs(buff, stdout);
    }
    fputc('\n', stdout);
}

void __cdecl main(int argc, char **argv)
{
    UINT rv = 0;
    char *arg;
    int i, l;
    int args_left = 1;
    int need_desc = 0;
    int need_disp = 0;
    int need_path = 0;
    int kill_code = 0;

    if (argc < 3) {
        ErrorUsage();
    }

    if (!IsWindowsNT()) {
        fprintf(stderr, "This program will run only on Windows NT or higher\n");
        ExitProcess(ERR_RET_VERSION);
    }

    for (i = 1; i < argc; i++) {
        arg = argv[i];
        if (*arg != '-')
            break;

        while (*++arg != '\0') {
            switch (*arg) {
                case 's':
                    l = atoi(argv[i+1]);
                    Sleep(l * 1000);
                    ExitProcess(0);
                    return;
                break;
                case 'p':
                    l = atoi(argv[i+1]);
                    PrintInfo(l, argv[i+2]);
                    ExitProcess(0);
                    return;
                break;
                case 'i':
                    _service_mode = mode_install;
                    args_left++;
                break;
                case 'u':
                    _service_mode = mode_uninstall;
                break;
                case 't':
                    _service_mode = mode_debug;
                break;
                case 'r':
                    _service_mode = mode_run;
                break;
                case 'k':
                    _service_mode = mode_signal;
                    if (*++arg != '\0') {
                        kill_code = *arg - '0';
                    }
                    else
                        ErrorUsage();
                break;
                case 'd':
                    need_disp = 1;
                    args_left++;
                break;
                case 'c':
                    need_desc = 1;
                    args_left++;
                break;
                case 'w':
                    need_path = 1;
                    args_left++;
                break;
                case 'l':
                    _service_log  = TRUE;
                break;
                case 'm':
                    _service_auto = SERVICE_DEMAND_START;
                break;
                default:
                    ErrorUsage();
                break;
            }
        }
    }
    if ((argc - i) < args_left)
        ErrorUsage();
    lstrcpy(_service_name, argv[i++]);
    for (l = 0; l < lstrlen(_service_name); l++) {
        /* Make service name uppercase */
        _service_name[l] = toupper((unsigned char)_service_name[l]);
    }
    if (need_path) {
        l = lstrlen(argv[i]);
        lstrcpy(_working_path, argv[i++]);
        /* Remove trailing backslash */
        if (l > 0 && _working_path[l - 1] == '\\')
            _working_path[l - 1] = '\0';
    }
    if (need_disp)
        lstrcpy(_service_disp, argv[i++]);
    if (need_desc)
        lstrcpy(_service_desc, argv[i++]);

    if (_service_mode != mode_signal) {
        _service_run_event = CreateEvent(NULL, TRUE, FALSE, NULL);
        SetServiceEventLog(_service_name);
    }
    if (_service_mode == mode_install) {
        lstrcpy(_service_bat, argv[i++]);
        if (!InstallService(_service_name)) {
            rv = ERR_RET_INSTALL;
            AddToMessageLog(TRUE, "Failed installing %s", _service_name);
            goto cleanup;
        }
        SetServiceParameters(_service_name);
        if (need_desc)
            SetServiceDescription(_service_name, _service_desc);
        AddToMessageLog(FALSE, "Installed %s", _service_name);
    }
    else if (_service_mode == mode_uninstall) {
        GetServiceParameters(_service_name);
        AddToMessageLog(FALSE, "Uninstalling %s", _service_name);
        ServiceStop();
        if (!RemoveService(_service_name)) {
            AddToMessageLog(TRUE, "Failed removing %s", _service_name);
            rv = ERR_RET_REMOVE;
        }
    }
    else if (_service_mode == mode_run) {
        GetServiceParameters(_service_name);
        AddToMessageLog(FALSE, "Initialized %s", _service_name);
        _service_table[0].lpServiceName = _service_name;
        _service_table[0].lpServiceProc = (LPSERVICE_MAIN_FUNCTION)ServiceMain;
        StartServiceCtrlDispatcher(_service_table);
    }
    else if (_service_mode == mode_debug) {
        GetServiceParameters(_service_name);
        AddToMessageLog(FALSE, "Debugging %s", _service_name);
        ServiceMain(argc, argv);
    }
    else if (_service_mode == mode_signal) {
        char kill_name[MAX_PATH + 1];
        HANDLE kill_event = NULL;
        lstrcpy(kill_name, _service_name);
        sprintf(kill_name, "Global\\PSIGNUM_%s_%d", _service_name, kill_code);
        kill_event = OpenEvent(EVENT_MODIFY_STATE, FALSE, kill_name);
        if (kill_event) {
            SetEvent(kill_event);
            CloseHandle(kill_event);
        }
        else
            fprintf(stderr, "Error opening event %d for %s\n",
                    kill_code, _service_name);
    }
    else {
        AddToMessageLog(TRUE, "Unknown service mode for %s", _service_name);
        rv = ERR_RET_MODE;
    }


cleanup:
    if (_service_run_event != NULL)
        CloseHandle(_service_run_event);
    AddToMessageLog(FALSE, "JBosssvc finished");
    ExitProcess(rv);
}
