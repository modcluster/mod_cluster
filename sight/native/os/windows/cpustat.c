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
#include "sight_types.h"

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Cpu"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "UserTime",
    "J"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "NicedTime",
    "J"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "SystemTime",
    "J"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "IdleTime",
    "J"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "WaitTime",
    "J"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "IrqTime",
    "J"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "SoftirqTime",
    "J"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "ContextSwitched",
    "J"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "BootTime",
    "J"
};

J_DECLARE_F_ID(0009) = {
    NULL,
    "Processes",
    "I"
};

J_DECLARE_F_ID(0010) = {
    NULL,
    "RunningProcesses",
    "I"
};

J_DECLARE_F_ID(0011) = {
    NULL,
    "BlockedProcesses",
    "I"
};

SIGHT_CLASS_LDEF(CpuStatistic)
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

    return 0;
}

static jlong cached_boot_time = 0L;

SIGHT_CLASS_UDEF(CpuStatistic)
{
    sight_unload_class(_E, &_clazzn);
}

SIGHT_EXPORT_DECLARE(jint, Cpu, stats0)(SIGHT_STDARGS,
                                        jobject thiz,
                                        jlong instance,
                                        jint cpuid)
{

    sight_object_t *no = J2P(instance, sight_object_t *);
    BYTE b[SIGHT_HBUFFER_SIZ];
    FILETIME ft[4];
    jlong jt[4] = { 0, 0, 0, 0 };
    int has_aggregate = 0;

    if (!no || !no->pool)
        return APR_EINVAL;
    if ((DWORD)cpuid > sight_osinf->dwNumberOfProcessors)
        return APR_EINVAL;
    SIGHT_LOCAL_TRY(no) {
        if (GetSystemTimes(&ft[0], &ft[1], &ft[2])) {
            jt[0] = filetime_to_ms(&ft[2]);
            jt[1] = filetime_to_ms(&ft[1]);
            jt[2] = filetime_to_ms(&ft[0]);
            has_aggregate = 1;
        }
        if (!cpuid && !has_aggregate) {
            DWORD i;
            for (i = 0; i < sight_osinf->dwNumberOfProcessors; i++) {
                if (NtQuerySystemInformation(SystemProcessorPerformanceInformation,
                        (LPVOID)&b[0], SIGHT_HBUFFER_SIZ, NULL) == 0) {
                    PSYSTEM_PROCESSOR_PERFORMANCE_INFORMATION pspi =
                        (PSYSTEM_PROCESSOR_PERFORMANCE_INFORMATION)&b[0];

                    jt[0] += largeint_to_ms(&(pspi[i].UserTime));
                    jt[1] += largeint_to_ms(&(pspi[i].KernelTime));
                    jt[2] += largeint_to_ms(&(pspi[i].IdleTime));
                }
            }
        }
        if (cpuid) {
            if (NtQuerySystemInformation(SystemProcessorPerformanceInformation,
                                (LPVOID)&b[0], SIGHT_HBUFFER_SIZ, NULL) == 0) {
                PSYSTEM_PROCESSOR_PERFORMANCE_INFORMATION pspi =
                    (PSYSTEM_PROCESSOR_PERFORMANCE_INFORMATION)&b[0];

                jt[0] = largeint_to_ms(&(pspi[cpuid - 1].UserTime));
                jt[1] = largeint_to_ms(&(pspi[cpuid - 1].KernelTime));
                jt[2] = largeint_to_ms(&(pspi[cpuid - 1].IdleTime));
            }
        }
        else {
            HANDLE hProcessSnap;
            PROCESSENTRY32 pe32;
            hProcessSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
            if (!IS_INVALID_HANDLE(hProcessSnap)) {
                DWORD pc = 0, tc = 0;
                pe32.dwSize = sizeof(PROCESSENTRY32);
                if (Process32First(hProcessSnap, &pe32)) {
                    do {
                        if (SIGHT_LOCAL_IRQ(no)) {
                            SIGHT_LOCAL_BRK(no);
                            return APR_EINTR;
                        }
                        pc += 1;
                        tc += pe32.cntThreads;
                        if (!cached_boot_time && (apr_strnatcasecmp(pe32.szExeFile, "smss.exe") == 0)) {
                            HANDLE wp = OpenProcess(PROCESS_QUERY_INFORMATION,
                                                    FALSE,
                                                    pe32.th32ProcessID);
                            if (!IS_INVALID_HANDLE(wp)) {
                                if (GetProcessTimes(wp, &ft[0], &ft[1],
                                                    &ft[2], &ft[3])) {
                                    cached_boot_time = winftime_to_ms(&ft[0]);
                                }
                                CloseHandle(wp);
                            }
                        }
                    } while(Process32Next(hProcessSnap, &pe32));
                }
                CloseHandle(hProcessSnap);
                SET_IFIELD_I(0009, thiz, pc + tc);
                SET_IFIELD_I(0010, thiz, pc);
            }
            if (!cached_boot_time) {
                GetSystemTimeAsFileTime(&ft[0]);
                /* XXX: Using GetTickCount will give 49.7 days resolution */
                cached_boot_time = winftime_to_ms(&ft[0]) - GetTickCount();
            }
            SET_IFIELD_J(0008, thiz, cached_boot_time);

        }
        SET_IFIELD_J(0000, thiz, jt[0]);
        SET_IFIELD_J(0002, thiz, jt[1]);
        SET_IFIELD_J(0003, thiz, jt[2]);
    } SIGHT_LOCAL_END(no);
    return APR_SUCCESS;
}
