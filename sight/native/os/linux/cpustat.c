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
#include "sight_private.h"
#include <sys/sysinfo.h>
#include <unistd.h>

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

SIGHT_CLASS_UDEF(CpuStatistic)
{
    sight_unload_class(_E, &_clazzn);
}

static const char stat_cpu[] = "/proc/stat";
static const char *stat_cpu_fmt[] = {
    "cpu",
    "cpu0",
    "intr",
    "ctxt",
    "btime",
    "processes",
    "proc_running",
    "proc_blocked",
    NULL
};

#define CPUX_FMT  "%" APR_INT64_T_FMT " %" APR_INT64_T_FMT " %" \
                  APR_INT64_T_FMT " %" APR_INT64_T_FMT " %" \
                  APR_INT64_T_FMT " %" APR_INT64_T_FMT " %" \
                  APR_INT64_T_FMT

SIGHT_EXPORT_DECLARE(jint, Cpu, stats0)(SIGHT_STDARGS,
                                        jobject thiz,
                                        jlong instance,
                                        jint cpuid)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_table_t *tcpu;
    char cpuname[32];
    char *val;
    apr_int64_t jt[8];

    if (!no || !no->pool)
        return APR_EINVAL;
    if (cpuid > 0)
        sprintf(cpuname, "cpu%d", cpuid - 1);
    else
        strcpy(cpuname, "cpu");
    if (!(tcpu = sight_ftable(stat_cpu, ' ', no->pool)))
        return apr_get_os_error();

    if (!(val = sight_table_get_s(tcpu, cpuname)))
        return APR_EINVAL;
    sscanf(val, CPUX_FMT,
           &jt[0],
           &jt[1],
           &jt[2],
           &jt[3],
           &jt[4],
           &jt[5],
           &jt[6]);

    if (!cpuid) {
        /* Fill common values only for the aggregate cpu */
        SET_IFIELD_J(0007, thiz, sight_table_get_j(tcpu, stat_cpu_fmt[3]));
        SET_IFIELD_J(0008, thiz, sight_table_get_j(tcpu, stat_cpu_fmt[4]) * 1000);
        SET_IFIELD_I(0009, thiz, sight_table_get_i(tcpu, stat_cpu_fmt[5]));
        SET_IFIELD_I(0009, thiz, sight_table_get_i(tcpu, stat_cpu_fmt[6]));
        SET_IFIELD_I(0009, thiz, sight_table_get_i(tcpu, stat_cpu_fmt[7]));
    }
    /* Adjust times to miliseconds */
    SET_IFIELD_J(0000, thiz, TCK2MS(jt[0]));
    SET_IFIELD_J(0001, thiz, TCK2MS(jt[1]));
    SET_IFIELD_J(0002, thiz, TCK2MS(jt[2]));
    SET_IFIELD_J(0003, thiz, TCK2MS(jt[3]));
    SET_IFIELD_J(0004, thiz, TCK2MS(jt[4]));
    SET_IFIELD_J(0005, thiz, TCK2MS(jt[5]));
    SET_IFIELD_J(0006, thiz, TCK2MS(jt[6]));


    return APR_SUCCESS;
}
