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

static const char proc_cpu[] = "/proc/cpuinfo";
static const char *proc_cpu_fmt[] = {
    "processor",
    "vendor_id",
    "cpu family",
    "model",
    "model name",
    "stepping",
    "cpu MHz",
    "cache size",
    "fpu",
    "fpu_exception",
    "cpuid level",
    "wp",
    "flags",
    "bogomips",
    "TLB size",
    "clflush size",
    "cache_alignment",
    "address sizes",
    "power management",
    NULL
};

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Cpu"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "IsBigEndian",
    "Z"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "NumberOfProcessors",
    "I"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "Family",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "Model",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0004) = {
    NULL,
    "Stepping",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0005) = {
    NULL,
    "Vendor",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0006) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0007) = {
    NULL,
    "MHz",
    "D"
};

J_DECLARE_F_ID(0008) = {
    NULL,
    "Bogomips",
    "D"
};

SIGHT_CLASS_LDEF(Cpu)
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

    return 0;
}

SIGHT_CLASS_UDEF(Cpu)
{
    sight_unload_class(_E, &_clazzn);
}

SIGHT_EXPORT_DECLARE(jint, Cpu, alloc0)(SIGHT_STDARGS,
                                        jobject thiz,
                                        jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_table_t *tcpu;

    if (!no || !no->pool)
        return APR_EINVAL;
    if (!(tcpu = sight_ftable(proc_cpu, ':', no->pool)))
        return apr_get_os_error();

    SET_IFIELD_Z(0000, thiz, sight_byteorder());
    SET_IFIELD_I(0001, thiz, sysconf(_SC_NPROCESSORS_ONLN));

    SET_IFIELD_S(0005, thiz, sight_table_get_s(tcpu, proc_cpu_fmt[1]));
    SET_IFIELD_S(0002, thiz, sight_table_get_s(tcpu, proc_cpu_fmt[2]));
    SET_IFIELD_S(0003, thiz, sight_table_get_s(tcpu, proc_cpu_fmt[3]));
    SET_IFIELD_S(0006, thiz, sight_table_get_s(tcpu, proc_cpu_fmt[4]));
    SET_IFIELD_S(0004, thiz, sight_table_get_s(tcpu, proc_cpu_fmt[5]));
    SET_IFIELD_D(0007, thiz, sight_table_get_d(tcpu, proc_cpu_fmt[6]));
    SET_IFIELD_D(0008, thiz, sight_table_get_d(tcpu, proc_cpu_fmt[13]));

    return APR_SUCCESS;
}
