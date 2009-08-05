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
#include <procfs.h>
#include <sys/sysinfo.h>
#include <sys/stat.h>
#include <sys/swap.h>
#include <unistd.h>

/*
 * Memory
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

    return 0;
}

SIGHT_CLASS_UDEF(Memory)
{
    sight_unload_class(_E, &_clazzn);
}

#define MEMX_FMT  "%lu %lu %lu"


SIGHT_EXPORT_DECLARE(jint, Memory, alloc0)(SIGHT_STDARGS,
                                           jobject thiz,
                                           jlong instance,
                                           jint pid)
{

    sight_object_t *no = J2P(instance, sight_object_t *);
    unsigned int pgsz;

    if (!no || !no->pool)
        return APR_EINVAL;

    pgsz = getpagesize();

    if (pid < 0) {
        /* Read the system information */
        kstat_ctl_t *kc = sight_solaris_get();
        kstat_t *sys_pagesp;
        kstat_named_t *kn;
        size_t size; 
        struct anoninfo anon;

        sys_pagesp = kstat_lookup(kc, "unix", 0, "system_pages");
        if (sys_pagesp == NULL)
            return APR_EINVAL;
        if(kstat_read(kc,sys_pagesp,0) == -1)
            return APR_EINVAL;
        kn = kstat_data_lookup(sys_pagesp, "lotsfree");
        size = kn->value.ul;
        kn = kstat_data_lookup(sys_pagesp, "freemem");
        size = kn->value.ul -size;
        

        SET_IFIELD_J(0000, thiz, sysconf(_SC_PHYS_PAGES) * pgsz);
        SET_IFIELD_J(0001, thiz, size * pgsz);

        /* the vm could also be read using:
           sys_pagesp = kstat_lookup(kc, "unix", 0, "vminfo");
         */

        if (swapctl(SC_AINFO, &anon) == -1)
            return apr_get_os_error();

        SET_IFIELD_J(0002, thiz, anon.ani_max);
        SET_IFIELD_J(0003, thiz, anon.ani_max - anon.ani_resv);

        /* XXX shared memory SET_IFIELD_J(0009, thiz, info.sharedram * info.mem_unit); */
        /* XXX data from sar SET_IFIELD_I(0006, thiz, info.loads[0]); */

        SET_IFIELD_I(0007, thiz, pgsz);
    }
    else {
        /* read a process information /proc/pid/psinfo */
        psinfo_t psinfo;
        apr_status_t rc;
        rc = sight_solaris_get_psinfo(no->pool, pid, &psinfo);
        if (rc != APR_SUCCESS) {
            return rc;
        }

        /* Those are in Kbytes... */
        SET_IFIELD_J(0000, thiz, psinfo.pr_size * 1024);
        SET_IFIELD_J(0008, thiz, psinfo.pr_rssize * 1024);
        /* XXX: shared mem size ???    SET_IFIELD_J(0009, thiz, vals[2] * pgsz); */
        SET_IFIELD_I(0007, thiz, pgsz);
    }

    return APR_SUCCESS;
}
