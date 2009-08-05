/*
 *  SIGHT - System information gathering hybrid tool
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
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_private.h"

static HINSTANCE        dll_instance = NULL;
static WCHAR            dll_file_name[MAX_PATH+1];

static SYSTEM_INFO      osinf;
static OSVERSIONINFOEXA osver;

LPSYSTEM_INFO           sight_osinf = &osinf;
LPOSVERSIONINFOEXA      sight_osver = &osver;
UINT64                  sight_vmem  = 2147483647L;

BOOL WINAPI DllMain(HINSTANCE instance, DWORD reason, LPVOID reserved)
{

    switch (reason) {
        /** The DLL is loading due to process
         *  initialization or a call to LoadLibrary.
         */
        case DLL_PROCESS_ATTACH:
            dll_instance = instance;
            GetModuleFileNameW(instance, dll_file_name, MAX_PATH);
            break;
        /** The attached process creates a new thread.
         */
        case DLL_THREAD_ATTACH:
            break;
        /** The thread of the attached process terminates.
         */
        case DLL_THREAD_DETACH:
            break;
        /** DLL unload due to process termination
         *  or FreeLibrary.
         */
        case DLL_PROCESS_DETACH:
            dll_instance = NULL;
            break;
        default:
            break;
    }

    return TRUE;
    UNREFERENCED_PARAMETER(reserved);
}

/* This is the helper code to resolve late bound entry points
 * missing from one or more releases of the Win32 API
 */

static const char* const late_dll_mames[SYSDLL_defined] = {
    "kernel32",
    "ntdll.dll",
    "user32.dll",
    "iphlpapi.dll",
    "jvm.dll"
};

static HMODULE late_dll_handles[SYSDLL_defined] = { NULL, NULL, NULL,
                                                    NULL, NULL };

FARPROC sight_load_dll_func(sight_dlltoken_e fnLib, const char* fnName, int ordinal)
{
    if (!late_dll_handles[fnLib]) {
        /* First see if the .dll is already loaded in the process */
        late_dll_handles[fnLib] = GetModuleHandleA(late_dll_mames[fnLib]);
        if (!late_dll_handles[fnLib]) {
            /* Do not display error messages when loading library */
            UINT em = SetErrorMode(SEM_FAILCRITICALERRORS);
            late_dll_handles[fnLib] = LoadLibraryA(late_dll_mames[fnLib]);
            SetErrorMode(em);
        }
        if (!late_dll_handles[fnLib])
            return NULL;
    }
    if (ordinal)
        return GetProcAddress(late_dll_handles[fnLib],
                               V2P(const char *, ordinal));
    else
        return GetProcAddress(late_dll_handles[fnLib], fnName);
}

static DWORD sight_preload_late_dlls()
{
    sight_dlltoken_e fnLib = SYSDLL_KERNEL32;
    for (fnLib = SYSDLL_KERNEL32; fnLib < SYSDLL_defined; fnLib++) {
        if (!late_dll_handles[fnLib]) {
            DWORD rc = ERROR_SUCCESS;
                /* First see if the .dll is already loaded in the process */
            late_dll_handles[fnLib] = GetModuleHandleA(late_dll_mames[fnLib]);
            if (!late_dll_handles[fnLib]) {
                UINT em;
                rc = GetLastError();
                /* Do not display error messages when loading library */
                em = SetErrorMode(SEM_FAILCRITICALERRORS);
                late_dll_handles[fnLib] = LoadLibraryA(late_dll_mames[fnLib]);
                rc = GetLastError();
                SetErrorMode(em);
            }
            if (!late_dll_handles[fnLib])
                return rc;
        }
    }
    return ERROR_SUCCESS;
}

static DWORD set_current_privilege(LPCWSTR szPrivilege,
                                   BOOL bEnablePrivilege)
{
    DWORD dwError;
    HANDLE hToken;
    TOKEN_PRIVILEGES tp;
    LUID luid;
    TOKEN_PRIVILEGES tpPrevious;
    DWORD cbPrevious = sizeof(TOKEN_PRIVILEGES);
    BOOL bSuccess=FALSE;

    if (!LookupPrivilegeValueW(NULL, szPrivilege, &luid))
        return ERROR_NO_SUCH_PRIVILEGE;

    if (!OpenProcessToken(GetCurrentProcess(),
                          TOKEN_QUERY | TOKEN_ADJUST_PRIVILEGES,
                          &hToken))
        return GetLastError();

    tp.PrivilegeCount           = 1;
    tp.Privileges[0].Luid       = luid;
    tp.Privileges[0].Attributes = 0;

    AdjustTokenPrivileges(hToken,
                          FALSE,
                          &tp,
                          sizeof(TOKEN_PRIVILEGES),
                          &tpPrevious,
                          &cbPrevious);

    if ((dwError = GetLastError()) == ERROR_SUCCESS) {
        tpPrevious.PrivilegeCount     = 1;
        tpPrevious.Privileges[0].Luid = luid;

        if(bEnablePrivilege)
            tpPrevious.Privileges[0].Attributes |= (SE_PRIVILEGE_ENABLED);
        else
            tpPrevious.Privileges[0].Attributes &= ~(SE_PRIVILEGE_ENABLED);

        AdjustTokenPrivileges(hToken,
                              FALSE,
                              &tpPrevious,
                              cbPrevious,
                              NULL,
                              NULL);

        dwError = GetLastError();
    }
    CloseHandle(hToken);
    return dwError;
}

static DWORD enable_privilege(LPCWSTR szPrivilege)
{
    DWORD dwError;
    HANDLE hToken;
    TOKEN_PRIVILEGES tp;
    LUID luid;

    if (!LookupPrivilegeValueW(NULL, szPrivilege, &luid))
        return ERROR_NO_SUCH_PRIVILEGE;

    if (!OpenProcessToken(GetCurrentProcess(),
                          TOKEN_QUERY | TOKEN_ADJUST_PRIVILEGES,
                          &hToken))
        return GetLastError();

    tp.PrivilegeCount           = 1;
    tp.Privileges[0].Luid       = luid;
    tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;

    AdjustTokenPrivileges(hToken,
                          FALSE,
                          &tp,
                          sizeof(TOKEN_PRIVILEGES),
                          NULL,
                          NULL);

    dwError = GetLastError();
    CloseHandle(hToken);
    return dwError;
}

#define LOG_MSG_EMERG           0xC0000001L
#define LOG_MSG_ERROR           0xC0000002L
#define LOG_MSG_NOTICE          0x80000003L
#define LOG_MSG_WARN            0x80000004L
#define LOG_MSG_INFO            0x40000005L
#define LOG_MSG_DEBUG           0x00000006L
#define LOG_MSG_DOMAIN          "Sight"

static char log_domain[MAX_PATH] = "Sight";

static void init_log_source(const char *domain)
{
    HKEY  key;
    DWORD ts;
    char event_key[MAX_PATH];

    lstrcpyA(event_key,
             "SYSTEM\\CurrentControlSet\\Services\\EventLog\\Application\\");
    lstrcatA(event_key, domain);
    if (!RegCreateKeyA(HKEY_LOCAL_MACHINE, event_key, &key)) {
        RegSetValueExW(key, L"EventMessageFile", 0, REG_SZ,
                       (LPBYTE)&dll_file_name[0],
                       lstrlenW(dll_file_name) + 1);
        ts = EVENTLOG_ERROR_TYPE | EVENTLOG_WARNING_TYPE |
             EVENTLOG_INFORMATION_TYPE;

        RegSetValueExW(key, L"TypesSupported", 0, REG_DWORD,
                       (LPBYTE) &ts, sizeof(DWORD));
        RegCloseKey(key);
    }
    lstrcpyA(log_domain, domain);
}

SIGHT_EXPORT_DECLARE(void, Syslog, init0)(SIGHT_STDARGS,
                                          jstring domain)
{
    const char *d;
    SIGHT_ALLOC_CSTRING(domain);

    UNREFERENCED_O;

    if ((d = J2S(domain)) == NULL)
        d = LOG_MSG_DOMAIN;
    init_log_source(d);

    SIGHT_FREE_CSTRING(domain);
}

SIGHT_EXPORT_DECLARE(void, Syslog, close0)(SIGHT_STDARGS)
{

    UNREFERENCED_STDARGS;
}

static void do_syslog(jint level, LPWSTR msg, DWORD err)
{
    DWORD id = LOG_MSG_DEBUG;
    WORD  il = EVENTLOG_SUCCESS;
    HANDLE  source;
    WCHAR *messages[2];
    WCHAR  buffer[1024];
    WORD   nStrings = 1;
    switch (level) {
        case SIGHT_LOG_EMERG:
            id = LOG_MSG_EMERG;
            il = EVENTLOG_ERROR_TYPE;
        break;
        case SIGHT_LOG_ERROR:
            id = LOG_MSG_ERROR;
            il = EVENTLOG_ERROR_TYPE;
        break;
        case SIGHT_LOG_NOTICE:
            id = LOG_MSG_NOTICE;
            il = EVENTLOG_WARNING_TYPE;
        break;
        case SIGHT_LOG_WARN:
            id = LOG_MSG_WARN;
            il = EVENTLOG_WARNING_TYPE;
        break;
        case SIGHT_LOG_INFO:
            id = LOG_MSG_INFO;
            il = EVENTLOG_INFORMATION_TYPE;
        break;
    }

    messages[0] = msg;
    if (err) {
        FormatMessageW(FORMAT_MESSAGE_ALLOCATE_BUFFER |
                       FORMAT_MESSAGE_FROM_SYSTEM |
                       FORMAT_MESSAGE_IGNORE_INSERTS,
                       NULL,
                       err,
                       MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                       buffer,
                       1024, NULL);
        messages[1] = &buffer[0];
        nStrings = 2;
    }
    source = RegisterEventSourceA(NULL, log_domain);

    if (source != NULL) {
        ReportEventW(source, il,
                     0,
                     id,
                     NULL,
                     nStrings, 0,
                     messages, NULL);
        DeregisterEventSource(source);
    }
}

SIGHT_EXPORT_DECLARE(void, Syslog, log0)(SIGHT_STDARGS,
                                         jint level,
                                         jstring msg)
{
    SIGHT_ALLOC_WSTRING(msg);

    UNREFERENCED_O;
    SIGHT_INIT_WSTRING(msg);
    do_syslog(level, J2C(msg), 0);
    SIGHT_FREE_WSTRING(msg);
}

static int main_called = 0;

apr_status_t sight_main(apr_pool_t *pool)
{
    DWORD rc;

    /* sight_main should be called only once per process */
    if (main_called++)
        return APR_SUCCESS;

    GetSystemInfo(sight_osinf);
    sight_osver->dwOSVersionInfoSize = sizeof(OSVERSIONINFOEXA);
    GetVersionExA((LPOSVERSIONINFOA)sight_osver);
    sight_vmem = (char *)sight_osinf->lpMaximumApplicationAddress  -
                 (char *)sight_osinf->lpMinimumApplicationAddress;
    if ((rc = sight_preload_late_dlls()) != ERROR_SUCCESS)
        return APR_FROM_OS_ERROR(rc);
    if ((rc = enable_privilege(L"SeDebugPrivilege")) != ERROR_SUCCESS) {
        /* Log that we couldn't set privilege */
        do_syslog(SIGHT_LOG_ERROR,
                  L"Failed setting SeDebugPrivilege",
                  GetLastError());
    }

    return APR_SUCCESS;
}
