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

#include <unistd.h>
#include <procfs.h>

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

    if ((rc = sight_pool_create(&pool, NULL, sight_temp_pool, 0)) != APR_SUCCESS) {
        throwAprMemoryException(_E, THROW_FMARK, rc);
        return NULL;

    }
    if (!(parr = (jint *)apr_palloc(pool, siz))) {
        throwAprMemoryException(_E, THROW_FMARK,
                                apr_get_os_error());
        return NULL;
    }
    if ((rc = enum_pids(pool, parr, SIGHT_MAX_PROCESSES, &pnum)) != APR_SUCCESS) {
        throwAprException(_E, rc);
        goto cleanup;
    }

    if (!(rv = (*_E)->NewIntArray(_E, pnum)))
        goto cleanup;
    (*_E)->SetIntArrayRegion(_E, rv, 0, pnum, parr);
cleanup:
    apr_pool_destroy(pool);

    return rv;
}

SIGHT_EXPORT_DECLARE(jint, Process, alloc0)(SIGHT_STDARGS,
                                            jobject thiz,
                                            jlong instance,
                                            jint pid)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    jobjectArray arr;

    char pname[SIGHT_STYPE_SIZ];
    char psname[SIGHT_HBUFFER_SIZ];
    apr_size_t slen;
    apr_status_t rc;
    jsize jlen;
    char *s, *p;
    char **sa;
    apr_finfo_t i;
    psinfo_t psinfo;
    int fd, n, count;
    sight_procstate_e pstate;

    if (pid < 0)
        return APR_SUCCESS;
    if (!no || !no->pool)
        return APR_EINVAL;

    /* read the psinfo structure */
    rc = sight_solaris_get_psinfo(no->pool, pid, &psinfo);
    if (rc != APR_SUCCESS) {
        return rc;
    }
    SET_IFIELD_S(0001, thiz, psinfo.pr_fname);

    SET_IFIELD_I(0000, thiz, psinfo.pr_ppid);
    SET_IFIELD_S(0002, thiz, psinfo.pr_fname); /* XXX: trucate ? */

    /* The state of the process corresponds to the state of lwp */
    switch(psinfo.pr_lwp.pr_sname) {
        case 'R': case 'I': case 'O':
            pstate = SIGHT_PROC_R;
            break;
        case 'S':
            pstate = SIGHT_PROC_S;
            break;
        case 'Z':
            pstate = SIGHT_PROC_Z;
            break;
        case 'T':
            pstate = SIGHT_PROC_T;
            break;
        default:
            pstate = SIGHT_PROC_U;
            break;
    }
    CALL_METHOD1(0000, thiz, pstate);

    SET_IFIELD_I(0005, thiz, psinfo.pr_nlwp);

    /* user + sys ... */
    SET_IFIELD_J(0014, thiz, psinfo.pr_time.tv_sec * 1000 + psinfo.pr_time.tv_nsec/1000);
    /* XXX: SET_IFIELD_J(0015, thiz, TCK2MS(utime)); */

    /* to read the arguments and the environment we must read /proc/pid/as */
    sprintf(pname, "/proc/%d/as", pid);
    if ((fd = open(pname, O_RDONLY))<0){
        rc = apr_get_os_error();
        if (rc != APR_EACCES)
            return rc;
    }
    if ( rc == APR_SUCCESS) {
        if (psinfo.pr_argc !=0) {
            sa = (char **) malloc(sizeof(char *) * (psinfo.pr_argc + 1));
            if (lseek(fd, psinfo.pr_argv, SEEK_SET) == -1) {
                close(fd);
                free(sa);
                return apr_get_os_error();
            }
            if (read(fd, sa, sizeof(char *) * (psinfo.pr_argc + 1)) < 0) {
                close(fd);
                free(sa);
                return apr_get_os_error();
            }

            arr = sight_new_cc_array(_E, SIGHT_CC_STRING, psinfo.pr_argc);
            pname[SIGHT_STYPE_LEN] = '\0';
            for (n = 0; n < psinfo.pr_argc; n++) {
                jstring s;
                if (lseek(fd, (off_t) sa[n], SEEK_SET) != -1) {
                    if (read(fd, pname, SIGHT_STYPE_LEN) > 0) {
                        s = CSTR_TO_JSTRING(pname);
                        if (s) {
                            (*_E)->SetObjectArrayElement(_E, arr, n, s);
                            (*_E)->DeleteLocalRef(_E, s);
                        }
                        continue;
                    }
                }
                s = CSTR_TO_JSTRING("");
                if (s) {
                    (*_E)->SetObjectArrayElement(_E, arr, n, s);
                    (*_E)->DeleteLocalRef(_E, s);
                }
            }
    
            SET_IFIELD_O(0003, thiz, arr);
            free(sa);
        }

        /* process environment like arguments */
        if (psinfo.pr_envp !=0) {
            if (lseek(fd, psinfo.pr_envp, SEEK_SET) == -1) {
                close(fd);
                return apr_get_os_error();
            }
            /* count the environment variables */
            p = (char *)1;
            count = 0;
            while (p) {
                if (read(fd, &p, sizeof(char *)) < 0) {
                    close(fd);
                    return apr_get_os_error();
                }
                count++;
            }
            sa = (char **) malloc(sizeof(char *) * (count));
            if (lseek(fd, psinfo.pr_envp, SEEK_SET) == -1) {
                close(fd);
                free(sa);
                return apr_get_os_error();
            }
            if (read(fd, sa, sizeof(char *) * (count)) < 0) {
                close(fd);
                free(sa);
                return apr_get_os_error();
            }
        } else 
           count = 1; /* no argumentts */

        /* The last argument is NULL */
        if (count-1 != 0) {
            arr = sight_new_cc_array(_E, SIGHT_CC_STRING, count-1);
            pname[SIGHT_STYPE_LEN] = '\0';
            for (n = 0; n < count-1; n++) {
                jstring s;
                if (lseek(fd, (off_t) sa[n], SEEK_SET) != -1) {
                    if (read(fd, pname, SIGHT_STYPE_LEN) > 0) {
                        s = CSTR_TO_JSTRING(pname);
                        if (s) {
                            (*_E)->SetObjectArrayElement(_E, arr, n, s);
                            (*_E)->DeleteLocalRef(_E, s);
                        }
                        continue;
                    }
                }
                s = CSTR_TO_JSTRING("");
                if (s) {
                    (*_E)->SetObjectArrayElement(_E, arr, n, s);
                    (*_E)->DeleteLocalRef(_E, s);
                }
            }
    
            SET_IFIELD_O(0004, thiz, arr);
        } 
        free(sa);
        close(fd);
    } else {
        /* Use the psinfo */
        char pr_name[PRARGSZ];
        int k = 0;
        int j = 0;
        count = 0;
        int l = 0;
        while (k<PRARGSZ && psinfo.pr_psargs[k] != '\0') {
            if (psinfo.pr_psargs[k] == ' ')
                count++;
            k++;
        }
          
        arr = sight_new_cc_array(_E, SIGHT_CC_STRING, count + 1);
        for (k=0; k<count+1;k++) {
            jstring s;
            while (j<PRARGSZ && psinfo.pr_psargs[j] != '\0') {
                if (psinfo.pr_psargs[j] == ' ')
                    break;
                j++;
            }
            memset(pr_name, '\0', sizeof(pr_name));
            memcpy(pr_name, &psinfo.pr_psargs[l], j-l);
            j++;
            l = j;
            s = CSTR_TO_JSTRING(pr_name);
            if (s) {
                (*_E)->SetObjectArrayElement(_E, arr, k, s);
                (*_E)->DeleteLocalRef(_E, s);
            }
 
        }
        SET_IFIELD_O(0003, thiz, arr);
    }
    
    /* get user, group and creation time (pr_start?) */
    sprintf(pname, "/proc/%d", pid);
    if ((rc = apr_stat(&i, pname,
                      APR_FINFO_MIN | APR_FINFO_OWNER,
                      no->pool)) != APR_SUCCESS) {
        return rc;
    }
    SET_IFIELD_J(0012, thiz, apr_time_as_msec(i.ctime));
    SET_IFIELD_J(0016, thiz, P2J(i.user));
    SET_IFIELD_J(0017, thiz, P2J(i.group));

    sprintf(pname, "/proc/%d/path/cwd", pid);
    if ((rc = readlink(pname, psname, SIGHT_HBUFFER_LEN)) > 0 ) {
        psname[rc] = '\0';
        SET_IFIELD_S(0018, thiz, psname);
    } else {
        /* that is more tricky here */
        int current_fd;

        sprintf(pname, "/proc/%d/cwd", pid);
        current_fd = open(".", O_RDONLY);
        if (chdir(pname) == -1) {
            close(current_fd);
            return APR_SUCCESS; /* ignore error */
        }
        if (getcwd(psname, SIGHT_HBUFFER_LEN) != NULL)
            SET_IFIELD_S(0018, thiz, psname);
        fchdir(current_fd);
        close(current_fd);
    }

    return APR_SUCCESS;
}

SIGHT_EXPORT_DECLARE(jint, Process, term0)(SIGHT_STDARGS, jint pid,
                                           jint signum)
{
    jint rv = APR_SUCCESS;

    if (kill(pid, signum) == -1) {
        return apr_get_os_error();
    }
    else
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
