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
 * Process Linux implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"

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


extern apr_pool_t *sight_global_pool;
extern apr_pool_t *sight_temp_pool;

SIGHT_EXPORT_DECLARE(jint, Process, getpid0)(SIGHT_STDARGS)
{
    UNREFERENCED_STDARGS;
    return (jint)getpid();
}

static apr_status_t enum_pids(apr_pool_t *pool, jint *parr, jint len, jint *num)
{
    apr_status_t rv = APR_SUCCESS;
    jint cnt = 0;
    apr_dir_t  *dir;
    apr_finfo_t fi;

    if ((rv = apr_dir_open(&dir, "/proc", pool)) != APR_SUCCESS)
        return rv;

    while (apr_dir_read(&fi, APR_FINFO_DIRENT, dir) == APR_SUCCESS) {
        /* Detect if /proc/xxx is numeric */
        if (apr_isdigit(*fi.name)) {
            int pid = atoi(fi.name);
            if (pid > 0) {
                /* TODO: Check if the /proc/# is a thread
                 *       or a regular process
                 */
                parr[cnt++] = pid;
            }
            if (cnt >= len) {
                rv = APR_ENOMEM;
                break;
            }
        }
    }
    apr_dir_close(dir);
    *num = cnt;
    return rv;
}

SIGHT_EXPORT_DECLARE(jintArray, Process, getpids0)(SIGHT_STDARGS)
{
    apr_status_t rc;
    jintArray rv = NULL;
    jint *parr;
    jint siz = SIGHT_MAX_PROCESSES * sizeof(jint);
    jsize pnum;
    apr_pool_t *pool;

    UNREFERENCED_O;

    SIGHT_GLOBAL_TRY {
        if ((rc = sight_create_pool(&pool, sight_temp_pool)) != APR_SUCCESS) {
            throwAprMemoryException(_E, THROW_FMARK, rc);
            SIGHT_GLOBAL_BRK();
            return NULL;

        }
        if (!(parr = (jint *)apr_palloc(pool, siz))) {
            throwAprMemoryException(_E, THROW_FMARK,
                                    apr_get_os_error());
            SIGHT_GLOBAL_BRK();
            return NULL;
        }
        if ((rc = enum_pids(pool, parr, SIGHT_MAX_PROCESSES, &pnum)) == APR_SUCCESS) {
            if ((rv = (*_E)->NewIntArray(_E, pnum)))
                (*_E)->SetIntArrayRegion(_E, rv, 0, pnum, parr);
        }
        else
            throwAprException(_E, rc);
        apr_pool_destroy(pool);
    } SIGHT_GLOBAL_END();
    return rv;
}

/*

/proc/#/stat format

pid %d The process id.

comm %s
    The filename of the executable,  in  parentheses.
    This  is visible whether or not the executable is
    swapped out.

state %c
    One character from the string "RSDZTW" where R is
    running,  S is sleeping in an interruptible wait,
    D is waiting in uninterruptible disk sleep, Z  is
    zombie, T is traced or stopped (on a signal), and
    W is paging.

ppid %d
    The PID of the parent.

pgrp %d
    The process group ID of the process.

session %d
    The session ID of the process.

tty_nr %d
    The tty the process uses.

tpgid %d
    The process group ID of the  process  which  cur-
    rently owns the tty that the process is connected
    to.

flags %lu
    The flags of the process.  The math bit is  deci-
    mal 4, and the traced bit is decimal 10.

minflt %lu
    The  number  of minor faults the process has made
    which have not required  loading  a  memory  page
    from disk.

cminflt %lu
    The  number  of minor faults that the process and
    its children have made.

majflt %lu
    The number of major faults the process  has  made
    which  have  required  loading a memory page from
    disk.

cmajflt %lu
    The number of major faults that the  process  and
    its children have made.

utime %lu
    The  number of jiffies that this process has been
    scheduled in user mode.

stime %lu
    The number of jiffies that this process has  been
    scheduled in kernel mode.

cutime %ld
    The  number  of jiffies that this process and its
    children have been scheduled in user mode.

cstime %ld
    The number of jiffies that this process  and  its
    children have been scheduled in kernel mode.

priority %ld
    The standard nice value, plus fifteen.  The value
    is never negative in the kernel.

nice %ld
    The nice value ranges from  19  (nicest)  to  -19
    (not nice to others).

 0 %ld  This  value  is  hard coded to 0 as a placeholder
    for a removed field.

itrealvalue %ld
    The time in jiffies before the  next  SIGALRM  is
    sent to the process due to an interval timer.

starttime %lu
    The  time  in  jiffies  the process started after
    system boot.

vsize %lu
    Virtual memory size in bytes.

rss %ld
    Resident Set Size: number of  pages  the  process
    has  in  real  memory, minus 3 for administrative
    purposes. This is  just  the  pages  which  count
    towards  text,  data,  or stack space.  This does
    not include pages which  have  not  been  demand-
    loaded in, or which are swapped out.

rlim %lu
    Current  limit in bytes on the rss of the process
    (usually 4294967295 on i386).

startcode %lu
    The address above which program text can run.

endcode %lu
    The address below which program text can run.

startstack %lu
    The address of the start of the stack.

kstkesp %lu
    The current value  of  esp  (stack  pointer),  as
    found in the kernel stack page for the process.

kstkeip %lu
    The current EIP (instruction pointer).

signal %lu
    The bitmap of pending signals (usually 0).

blocked %lu
    The  bitmap  of blocked signals (usually 0, 2 for
    shells).

sigignore %lu
    The bitmap of ignored signals.

sigcatch %lu
    The bitmap of catched signals.

wchan %lu
    This is the "channel" in  which  the  process  is
    waiting.  It is the address of a system call, and
    can be looked up in a namelist if you need a tex-
    tual   name.    (If   you   have   an  up-to-date
    /etc/psdatabase, then try ps -l to see the  WCHAN
    field in action.)

nswap %lu
    Number of pages swapped - not maintained.

cnswap %lu
    Cumulative nswap for child processes.

exit_signal %d
    Signal to be sent to parent when we die.

processor %d
    CPU number last executed on.

*/

SIGHT_EXPORT_DECLARE(jint, Process, alloc0)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong instance,
                                            jint pid)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    char pname[SIGHT_STYPE_SIZ];
    char psname[SIGHT_HBUFFER_SIZ];
    apr_size_t slen;
    apr_status_t rv;
    jsize jlen;
    char *s, *p;
    char **sa;
    apr_finfo_t i;
    int rc;

    if (pid < 0)
        return APR_SUCCESS;
    if (!no || !no->pool)
        return APR_EINVAL;

    sprintf(pname, "/proc/%d", pid);
    SIGHT_LOCAL_TRY(no) {
        if ((rv = apr_stat(&i, pname,
                          APR_FINFO_MIN | APR_FINFO_OWNER,
                          no->pool)) != APR_SUCCESS) {
            SIGHT_LOCAL_BRK(no);
            return rv;
        }
        sprintf(pname, "/proc/%d/cmdline", pid);
        if ((s = sight_fread(pname))) {
            SET_IFIELD_S(0001, thiz, s);
            SET_IFIELD_O(0003, thiz, sight_mc_to_sa(_E, s));
            free(s);
        }

        sprintf(pname, "/proc/%d/environ", pid);
        if ((s = sight_fread(pname))) {
            SET_IFIELD_O(0004, thiz, sight_mc_to_sa(_E, s));
            free(s);
        }
        sprintf(pname, "/proc/%d/cwd", pid);
        if ((rc = readlink(pname, psname, SIGHT_HBUFFER_LEN)) > 0 ) {
            psname[rc] = '\0';
            SET_IFIELD_S(0018, thiz, psname);
        }
        sprintf(pname, "/proc/%d/stat", pid);
        if ((s = sight_fread(pname))) {
            int pspid, psppid, pspgid, nthreads;
            char psstate;
            unsigned long utime, stime;
            unsigned long long starttime;
            sight_procstate_e pstate;

            /* fs/proc/array.c for 2.6 kernel (42 fields)
            rc = sscanf(s,
                        "%d (%[^)]) %c %d %d %d %d %d %lu %lu "
                        "%lu %lu %lu %lu %lu %ld %ld %ld %ld %d 0 %llu %lu %ld %lu %lu %lu %lu %lu "
                        "%lu %lu %lu %lu %lu %lu %lu %lu %d %d %lu %lu %llu",
               fs/proc/array.c for 2.4 kernel (39 fields)
            rc = sscanf(s,
                        "%d (%[^)]) %c %d %d %d %d %d %lu %lu "
                        "%lu %lu %lu %lu %lu %ld %ld %ld %ld %ld %ld %lu %lu %ld %lu %lu %lu %lu %lu "
                        "%lu %lu %lu %lu %lu %lu %lu %lu %d %d",
            */
            rc = sscanf(s,
                        "%d (%[^)]) %c %d %d %*d %*d %*d %*u %*u "
                        "%*u %*u %*u %lu %lu %*d %*d %*d %*d %d "
                        "%*d %llu",
                        &pspid,
                        psname,
                        &psstate,
                        &psppid,
                        &pspgid,
                        &utime,
                        &stime,
                        &nthreads,
                        &starttime
                    );
            SET_IFIELD_I(0000, thiz, psppid);
            SET_IFIELD_S(0002, thiz, psname);
            switch(psstate) {
                case 'R':
                    pstate = SIGHT_PROC_R;
                break;
                case 'S':
                    pstate = SIGHT_PROC_S;
                break;
                case 'D':
                    pstate = SIGHT_PROC_D;
                break;
                case 'Z':
                    pstate = SIGHT_PROC_Z;
                break;
                case 'T':
                    pstate = SIGHT_PROC_T;
                break;
                case 'W':
                    pstate = SIGHT_PROC_W;
                break;
                case 'X':
                    pstate = SIGHT_PROC_X;
                break;
                default:
                    pstate = SIGHT_PROC_U;
                break;
            }
            SET_IFIELD_I(0005, thiz, nthreads);
            SET_IFIELD_J(0012, thiz, apr_time_as_msec(i.ctime));
            SET_IFIELD_J(0014, thiz, TCK2MS(stime));
            SET_IFIELD_J(0015, thiz, TCK2MS(utime));
            SET_IFIELD_J(0016, thiz, P2J(i.user));
            SET_IFIELD_J(0017, thiz, P2J(i.group));
            CALL_METHOD1(0000, thiz, pstate);
            free(s);
        }
    } SIGHT_LOCAL_END(no);
    return APR_SUCCESS;
}

SIGHT_EXPORT_DECLARE(jint, Process, term0)(SIGHT_STDARGS, jint pid,
                                           jint signum)
{
#ifndef HAVE_GETPGID
    apr_proc_t proc;
    apr_status_t rv;
    apr_exit_why_e why;
    int status;
#else
    pid_t pg;
#endif

    /* Ensure pid sanity */
    if (pid < 1)
        return APR_EINVAL;

#ifndef HAVE_GETPGID
    proc.pid = pid;
    rv = apr_proc_wait(&proc, &status, &why, APR_NOWAIT);
    if (rv == APR_CHILD_DONE) {
        /* Process already dead... */
        return APR_EINVAL;
    }
    else if (rv != APR_CHILD_NOTDONE) {
        return rv;
    }
#else
    pg = getpgid(pid);
    if (pg == -1) {
        /* Process already dead... */
        return apr_get_os_error();
    }
#endif

    if (kill(pid, signum)) {
        return apr_get_os_error();
    }
    else
        return APR_SUCCESS;
}

SIGHT_EXPORT_DECLARE(jint, Process, signal0)(SIGHT_STDARGS, jint pid,
                                             jint signal)
{

    UNREFERENCED_STDARGS;
    UNREFERENCED(pid);
    UNREFERENCED(signal);
    return APR_ENOTIMPL;
}
