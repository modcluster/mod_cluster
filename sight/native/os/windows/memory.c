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


/*
 * Memory implementation
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Memory"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "Physical",
    "J"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "AvailPhysical",
    "J"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "Swap",
    "J"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "AvailSwap",
    "J"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "Kernel",
    "J"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "Cached",
    "J"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "Load",
    "I"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "Pagesize",
    "I"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "RSS",
    "J"
};

J_DECLARE_F_ID(0009) = {
    NULL,
    "Shared",
    "J"
};

J_DECLARE_F_ID(0010) = {
    NULL,
    "PageFaults",
    "J"
};

J_DECLARE_F_ID(0011) = {
    NULL,
    "MaxVirtual",
    "J"
};


SIGHT_CLASS_LDEF(Memory)
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

SIGHT_CLASS_UDEF(Memory)
{
    sight_unload_class(_E, &_clazzn);
}

SIGHT_EXPORT_DECLARE(jint, Memory, alloc0)(SIGHT_STDARGS,
                                           jobject thiz,
                                           jlong instance,
                                           jint pid)
{

    sight_object_t *no = J2P(instance, sight_object_t *);
    MEMORYSTATUSEX gms;
    PERFORMANCE_INFORMATION gpi;

    if (!no || !no->pool)
        return APR_EINVAL;

    gms.dwLength = sizeof(MEMORYSTATUSEX);
    gpi.cb       = sizeof(PERFORMANCE_INFORMATION);
    if (!GlobalMemoryStatusEx(&gms))
        return apr_get_os_error();
    if (!GetPerformanceInfo(&gpi, sizeof(gpi)))
        gpi.PageSize = 0;

    if (pid < 0) {
        SET_IFIELD_J(0000, thiz, gms.ullTotalPhys - gms.ullAvailPhys);
        SET_IFIELD_J(0001, thiz, gms.ullAvailPhys);
        SET_IFIELD_J(0002, thiz, gms.ullTotalPageFile - gms.ullAvailPageFile);
        SET_IFIELD_J(0003, thiz, gms.ullAvailPageFile - gms.ullTotalPhys);
        SET_IFIELD_I(0006, thiz, gms.dwMemoryLoad);
        if (!gpi.PageSize) {
            SET_IFIELD_I(0007, thiz, sight_osinf->dwPageSize);
        }
        else {
            SET_IFIELD_J(0004, thiz, gpi.PageSize * gpi.KernelTotal);
            SET_IFIELD_J(0005, thiz, gpi.PageSize * gpi.SystemCache);
            SET_IFIELD_I(0007, thiz, gpi.PageSize);
        }
        SET_IFIELD_J(0011, thiz, sight_vmem);
    }
    else {
        PROCESS_MEMORY_COUNTERS pmc;
        HANDLE proc = OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ,
                                  FALSE, pid);
        if (proc) {
            if (GetProcessMemoryInfo(proc, &pmc, sizeof(pmc))) {
                UINT64 as = gms.ullAvailPageFile - gms.ullTotalPhys;
                UINT64 am = gms.ullAvailPhys;
                UINT64 ap = sight_vmem - pmc.WorkingSetSize;
                if (am > ap)
                    am = ap;
                if (as > ap)
                    as = ap;
                SET_IFIELD_J(0000, thiz, pmc.WorkingSetSize);
                SET_IFIELD_J(0001, thiz, am);
                SET_IFIELD_J(0002, thiz, pmc.PagefileUsage);
                SET_IFIELD_J(0003, thiz, as);
                SET_IFIELD_I(0006, thiz, ((pmc.WorkingSetSize * 100) / gms.ullTotalPhys));
                if (!gpi.PageSize) {
                    SET_IFIELD_I(0007, thiz, sight_osinf->dwPageSize);
                }
                else {
                    SET_IFIELD_I(0007, thiz, sight_osinf->dwPageSize);
                }
                SET_IFIELD_J(0010, thiz, pmc.PageFaultCount);
                SET_IFIELD_J(0011, thiz, ap);
            }
            CloseHandle(proc);
        }
    }
    return APR_SUCCESS;
}
