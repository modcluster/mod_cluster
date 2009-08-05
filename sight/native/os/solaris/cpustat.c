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

#include <kstat.h>
#include <sys/sysinfo.h>

typedef struct {
  uint_t idle;
  uint_t user;
  uint_t kernel;
  uint_t wait;
  uint_t intr;
  uint_t syscall;
} solaris_cpu_info_t;

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
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
    kstat_ctl_t *kc;
    kstat_t *ksp;
    cpu_stat_t cs;
    solaris_cpu_info_t *cpuinfo;
    solaris_cpu_info_t *cpu;
    unsigned ncpu = sysconf(_SC_NPROCESSORS_CONF);
    int i;

    if (!no || !no->pool)
        return APR_EINVAL;
    if (cpuid > ncpu) {
       return APR_ENOTIMPL;
    }

    /* get the kstat handler */
    kc = sight_solaris_get();
    if (!kc)
       return APR_ENOTIMPL;

    /* loop in the chain until we get the "cpu_stat" module */
    cpuinfo = (solaris_cpu_info_t *) apr_pcalloc(no->pool, ncpu * sizeof(solaris_cpu_info_t));
    i = 0;
    for (ksp = kc->kc_chain; ksp; ksp = ksp->ks_next) {
        if (strcmp(ksp->ks_module, "cpu_stat"))
            continue;
        if (kstat_read(kc,ksp,&cs) < 0) {
            return APR_ENOTIMPL;
        }
        cpuinfo[i].idle = cs.cpu_sysinfo.cpu[CPU_IDLE];
        cpuinfo[i].user = cs.cpu_sysinfo.cpu[CPU_USER];
        cpuinfo[i].kernel = cs.cpu_sysinfo.cpu[CPU_KERNEL];
        cpuinfo[i].wait = cs.cpu_sysinfo.cpu[CPU_WAIT];
        cpuinfo[i].intr = cs.cpu_sysinfo.intr;
        cpuinfo[i].syscall = cs.cpu_sysinfo.syscall;
        i++;
    }

    cpu = (solaris_cpu_info_t*)apr_pcalloc(no->pool, sizeof(solaris_cpu_info_t));
    if (cpuid > 0) {
        cpu->user = cpuinfo[cpuid-1].user;
        /* cpu->nice.t XXX: where to get it. */
        cpu->kernel = cpuinfo[cpuid-1].kernel;
        cpu->idle = cpuinfo[cpuid-1].idle;
        cpu->wait = cpuinfo[cpuid-1].wait;
        cpu->intr = cpuinfo[cpuid-1].intr;
        cpu->syscall = cpuinfo[cpuid-1].syscall;
    } else {
        for (i=0;i<ncpu;i++) {
            cpu->user = cpu->user + cpuinfo[i].user;
            /* cpu->nice.t XXX: where to get it. */
            cpu->kernel = cpu->kernel + cpuinfo[i].kernel;
            cpu->idle = cpu->idle + cpuinfo[i].idle;
            cpu->wait = cpu->wait + cpuinfo[i].wait;
            cpu->intr = cpu->intr + cpuinfo[i].intr;
            cpu->syscall = cpu->syscall + cpuinfo[i].syscall;
        }
        /* XXX: sysinfo_t stuff here ? */
    }
    /* XXX: SIGHT_HZ ajustements ? */

    SET_IFIELD_J(0000, thiz, cpu->user);
    SET_IFIELD_J(0002, thiz, cpu->kernel);
    SET_IFIELD_J(0003, thiz, cpu->wait);
    SET_IFIELD_J(0005, thiz, cpu->intr);
    SET_IFIELD_J(0006, thiz, cpu->syscall);
    return APR_SUCCESS;
}
