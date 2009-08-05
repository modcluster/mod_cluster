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

/**
 * Process windows implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"

/*
 * Process
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Process"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "ParentId",
    "I"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "BaseName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "Arguments",
    "[Ljava/lang/String;"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "Environment",
    "[Ljava/lang/String;"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "ThreadCount",
    "I"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "ReadOperationCount",
    "J"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "WriteOperationCount",
    "J"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "OtherOperationCount",
    "J"
};

J_DECLARE_F_ID(0009) = {
    NULL,
    "ReadTransferCount",
    "J"
};

J_DECLARE_F_ID(0010) = {
    NULL,
    "WriteTransferCount",
    "J"
};

J_DECLARE_F_ID(0011) = {
    NULL,
    "OtherTransferCount",
    "J"
};

J_DECLARE_F_ID(0012) = {
    NULL,
    "CreateTime",
    "J"
};

J_DECLARE_F_ID(0013) = {
    NULL,
    "ExitTime",
    "J"
};

J_DECLARE_F_ID(0014) = {
    NULL,
    "InKernelTime",
    "J"
};

J_DECLARE_F_ID(0015) = {
    NULL,
    "InUserTime",
    "J"
};

J_DECLARE_F_ID(0016) = {
    NULL,
    "UserId",
    "J"
};

J_DECLARE_F_ID(0017) = {
    NULL,
    "GroupId",
    "J"
};

J_DECLARE_F_ID(0018) = {
    NULL,
    "CurrentWorkingDirectory",
    "Ljava/lang/String;"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "setState",
    "(I)V"
};

SIGHT_CLASS_LDEF(Process)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);
    J_LOAD_IFIELD(0003);
    J_LOAD_IFIELD(0004);
    J_LOAD_IFIELD(0005);
    J_LOAD_IFIELD(0006);
    J_LOAD_IFIELD(0007);
    J_LOAD_IFIELD(0008);
    J_LOAD_IFIELD(0009);
    J_LOAD_IFIELD(0010);
    J_LOAD_IFIELD(0011);
    J_LOAD_IFIELD(0012);
    J_LOAD_IFIELD(0013);
    J_LOAD_IFIELD(0014);
    J_LOAD_IFIELD(0015);
    J_LOAD_IFIELD(0016);
    J_LOAD_IFIELD(0017);
    J_LOAD_IFIELD(0018);
    J_LOAD_METHOD(0000);

    return 0;
}

SIGHT_CLASS_UDEF(Process)
{
    sight_unload_class(_E, &_clazzn);
}

SIGHT_EXPORT_DECLARE(jint, Process, getpid0)(SIGHT_STDARGS)
{
    UNREFERENCED_STDARGS;
    return (jint)GetCurrentProcessId();
}

SIGHT_EXPORT_DECLARE(jintArray, Process, getpids0)(SIGHT_STDARGS)
{
    jintArray rv = NULL;
    jint *parr;
    DWORD siz = SIGHT_MAX_PROCESSES * sizeof(jint);
    DWORD rsz;
    jsize pnum;

    UNREFERENCED_O;

    if (!(parr = (jint *)sight_malloc(_E, siz, THROW_FMARK))) {
        return NULL;
    }
    if (!EnumProcesses(parr, siz, &rsz)) {
        throwAprException(_E, apr_get_os_error());
        goto cleanup;
    }
    pnum = rsz / sizeof(DWORD);

    if (!(rv = (*_E)->NewIntArray(_E, pnum))) {

        goto cleanup;
    }
    (*_E)->SetIntArrayRegion(_E, rv, 0, pnum, parr);

cleanup:
    free(parr);

    return rv;
}

#define MAX_ENV_CHARS  2048

static DWORD read_proc_peb(HANDLE hProcess, WCHAR **lpCwd,
                           WCHAR **lpCmdLine, WCHAR **lpEnv,
                           apr_pool_t *pool)
{
    DWORD status;
    PROCESS_BASIC_INFORMATION pbi;
    SIGHT_PEB peb;
    RTL_USER_PROCESS_PARAMETERS rtl;
    DWORD dwSize;

    memset(&pbi, 0, sizeof(PROCESS_BASIC_INFORMATION));
    memset(&peb, 0, sizeof(SIGHT_PEB));
    memset(&rtl, 0, sizeof(RTL_USER_PROCESS_PARAMETERS));

    if ((status = NtQueryInformationProcess(hProcess,
                                            ProcessBasicInformation,
                                            &pbi,
                                            sizeof(PROCESS_BASIC_INFORMATION),
                                            &dwSize)) != ERROR_SUCCESS) {
        return status;
    }

    if (!pbi.PebBaseAddress)
        return EINVAL;
    if (!ReadProcessMemory(hProcess,
                           pbi.PebBaseAddress,
                           &peb,
                           sizeof(SIGHT_PEB),
                           &dwSize)) {
        return GetLastError();
    }

    if (!ReadProcessMemory(hProcess,
                           peb.ProcessParameters,
                           &rtl,
                           sizeof(RTL_USER_PROCESS_PARAMETERS),
                           &dwSize)) {
        return GetLastError();
    }
    /* Read actual data */
    if (rtl.CurrentDirectory.DosPath.Buffer &&
        rtl.CurrentDirectory.DosPath.Length) {
        *lpCwd = apr_pcalloc(pool,
                  rtl.CurrentDirectory.DosPath.Length + sizeof(WCHAR));
        if (*lpCwd) {
            if (!ReadProcessMemory(hProcess,
                                   rtl.CurrentDirectory.DosPath.Buffer,
                                   *lpCwd,
                                   rtl.CurrentDirectory.DosPath.Length,
                                   NULL))
                *lpCwd = NULL;
        }
    }
    if (rtl.CommandLine.Buffer &&
        rtl.CommandLine.Length) {
        *lpCmdLine = apr_pcalloc(pool,
                        rtl.CommandLine.Length + sizeof(WCHAR));
        if (*lpCmdLine) {
            if (!ReadProcessMemory(hProcess,
                                   rtl.CommandLine.Buffer,
                                   *lpCmdLine,
                                   rtl.CommandLine.Length,
                                   NULL))
            *lpCmdLine = NULL;
        }
    }
    if (rtl.Environment) {
        MEMORY_BASIC_INFORMATION mbi;
        if (VirtualQueryEx(hProcess, rtl.Environment, &mbi, sizeof(mbi)))
            *lpEnv = apr_pcalloc(pool, mbi.RegionSize);
        else
            *lpEnv = NULL;
        if (*lpEnv) {
            if (!ReadProcessMemory(hProcess,
                                   rtl.Environment,
                                   *lpEnv,
                                   mbi.RegionSize - 4,
                                   NULL))
                *lpEnv = NULL;
        }
    }

    return ERROR_SUCCESS;
}

static BOOL find_pe32(DWORD pid, PROCESSENTRY32W *ppe32)
{
    HANDLE hProcessSnap;
    PROCESSENTRY32W pe32;
    BOOL rv = FALSE;

    hProcessSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (!IS_INVALID_HANDLE(hProcessSnap)) {
        pe32.dwSize = sizeof(PROCESSENTRY32W);
        if (Process32FirstW(hProcessSnap, &pe32)) {
            do {
                if (pe32.th32ProcessID == pid) {
                    *ppe32 = pe32;
                    rv = TRUE;
                    break;
                }
            } while(Process32NextW(hProcessSnap, &pe32));
        }
        CloseHandle(hProcessSnap);
    }
    return rv;
}

static void proc_cleanup(int mode, sight_object_t *no)
{
    if (no) {
        /* Just an empty place holder for now */
#ifdef SIGHT_DO_STATS
        sight_cnt_native_clrcall++;
#endif
    }
}

SIGHT_EXPORT_DECLARE(jint, Process, alloc0)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong instance,
                                            jint pid)
{
    HANDLE hProcess;
    sight_object_t *no = J2P(instance, sight_object_t *);
    jchar buf[SIGHT_STYPE_SIZ];
    IO_COUNTERS ioc;
    FILETIME ft[4];
    int ppid = -1, tcnt = 0;
    PROCESSENTRY32W pe32;
    jlong et = 0;
    apr_uid_t uid;
    apr_gid_t gid;
    LPWSTR *pargs;
    int     pargc = 0;

    if (pid < 0)
        return APR_SUCCESS;
    if (!no || !no->pool)
        return APR_EINVAL;

#ifdef SIGHT_DO_STATS
    no->clean = proc_cleanup;
#endif

    SIGHT_LOCAL_TRY(no) {
        if (!find_pe32(pid, &pe32)) {
            /* System pids have low values */
            if (pid > 10) {
                SIGHT_LOCAL_BRK(no);
                return APR_ENOENT;
            }
            // Check if this is needed at all.
            lstrcpyW(pe32.szExeFile, L"Unknown");
        }
        else {
            ppid = pe32.th32ParentProcessID;
            tcnt = pe32.cntThreads;
        }
        if (pid == (jint)GetCurrentProcessId())
            hProcess = GetCurrentProcess();
        else {
            if (!(hProcess = OpenProcess(READ_CONTROL | PROCESS_VM_READ |
                                         PROCESS_QUERY_INFORMATION,
                                         FALSE, pid))) {
                SET_IFIELD_W(0002, thiz, pe32.szExeFile);
                SET_IFIELD_J(0000, thiz, ppid);
                SET_IFIELD_J(0005, thiz, tcnt);
                CALL_METHOD1(0000, thiz, SIGHT_PROC_R);
                SIGHT_LOCAL_BRK(no);
                return APR_SUCCESS;
            }
        }
        if (GetModuleFileNameExW(hProcess, NULL, buf, SIGHT_STYPE_LEN)) {
            SET_IFIELD_W(0001, thiz, buf);
        }
        if (GetModuleBaseNameW(hProcess, NULL, buf, SIGHT_STYPE_LEN)) {
            SET_IFIELD_W(0002, thiz, buf);
        }
        else {
            SET_IFIELD_W(0002, thiz, pe32.szExeFile);
        }
        if (GetProcessIoCounters(hProcess, &ioc)) {
            SET_IFIELD_J(0006, thiz, ioc.ReadOperationCount);
            SET_IFIELD_J(0007, thiz, ioc.WriteOperationCount);
            SET_IFIELD_J(0008, thiz, ioc.OtherOperationCount);
            SET_IFIELD_J(0009, thiz, ioc.ReadTransferCount);
            SET_IFIELD_J(0010, thiz, ioc.WriteTransferCount);
            SET_IFIELD_J(0011, thiz, ioc.OtherTransferCount);
        }
        if (GetProcessTimes(hProcess, &ft[0], &ft[1], &ft[2], &ft[3])) {
            et = winftime_to_ms(&ft[1]);
            SET_IFIELD_J(0012, thiz, winftime_to_ms(&ft[0]));
            SET_IFIELD_J(0013, thiz, et);
            SET_IFIELD_J(0014, thiz, filetime_to_ms(&ft[2]));
            SET_IFIELD_J(0015, thiz, filetime_to_ms(&ft[3]));
        }
        if (et) {
            CALL_METHOD1(0000, thiz, SIGHT_PROC_T);
        }
        else {
            CALL_METHOD1(0000, thiz, SIGHT_PROC_R);
        }

        /* XXX: Reading process memory is a hack.
         */
        if (pid != (jint)GetCurrentProcessId()) {
            WCHAR *lpCwd = NULL, *lpCmdLine = NULL, *lpEnv = NULL;
            if (read_proc_peb(hProcess, &lpCwd, &lpCmdLine,
                              &lpEnv, no->pool) == ERROR_SUCCESS) {
                SET_IFIELD_O(0004, thiz, sight_mw_to_sa(_E, lpEnv));
                SET_IFIELD_W(0018, thiz, lpCwd);
                if (lpCmdLine) {
                    pargs = (LPWSTR *)CommandLineToArgvW(lpCmdLine,
                                                         &pargc);
                    if (pargs) {
                        SET_IFIELD_O(0003, thiz, sight_aw_to_sa(_E, pargs, pargc));
                        LocalFree(pargs);
                    }
                }
            }
        }
        else {
            LPVOID  env;
            LPWSTR  cmdl;

            if ((env = GetEnvironmentStringsW())) {
                SET_IFIELD_O(0004, thiz, sight_mw_to_sa(_E, env));
                FreeEnvironmentStrings(env);
            }

            if ((cmdl = GetCommandLineW())) {
                pargs = (LPWSTR *)CommandLineToArgvW(cmdl, &pargc);
                if (pargs) {
                    SET_IFIELD_O(0003, thiz, sight_aw_to_sa(_E, pargs, pargc));
                    LocalFree(pargs);
                }
            }
            buf[0] = L'\0';
            if (GetCurrentDirectoryW(SIGHT_STYPE_LEN, buf)) {
                SET_IFIELD_W(0018, thiz, buf);
            }
        }
        if (sight_uid_get(hProcess, &uid, &gid, no->pool) == APR_SUCCESS) {
            SET_IFIELD_J(0016, thiz, P2J(uid));
            SET_IFIELD_J(0017, thiz, P2J(gid));
        }

        CloseHandle(hProcess);
        SET_IFIELD_J(0000, thiz, ppid);
        SET_IFIELD_J(0005, thiz, tcnt);
    } SIGHT_LOCAL_END(no);

    return APR_SUCCESS;
}

SIGHT_EXPORT_DECLARE(jint, Process, term0)(SIGHT_STDARGS, jint pid,
                                           jint signum)
{
    HANDLE hProcess;
    jint rv = APR_SUCCESS;

    UNREFERENCED_STDARGS;
    if (!(hProcess = OpenProcess(PROCESS_TERMINATE, FALSE, pid)))
        return apr_get_os_error();
    /*
     * TODO
     * Inject ExitProcess from procrun
     * If that fails then use the hard 'TerminateProcess'
     */
    if (TerminateProcess(hProcess, signum)) {
        DWORD exitCode;
        if (GetExitCodeProcess(hProcess, &exitCode)) {
            if (exitCode != (DWORD)signum)
                rv = APR_CHILD_DONE;
        }
        else
            rv = APR_CHILD_NOTDONE;
    }
    else
        rv = apr_get_os_error();
    CloseHandle(hProcess);
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, Process, signal0)(SIGHT_STDARGS, jint pid,
                                             jint signal)
{

    UNREFERENCED_STDARGS;
    UNREFERENCED(pid);
    UNREFERENCED(signal);
    return APR_ENOTIMPL;
}
