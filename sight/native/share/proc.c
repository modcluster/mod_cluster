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

#define MAX_ARGS_SIZE 1024
#define MAX_ENV_SIZE  1024

typedef struct {
    apr_proc_t      p;
    apr_cmdtype_e   cmdtype;
    apr_procattr_t  *attr;
    int             exitval;
    apr_exit_why_e  exitwhy;
    int             killhow;
} sight_runproc_t;

static void proc_cleanup(int mode, sight_object_t *no)
{
    if (no) {
        /* Just an empty place holder for now */
#ifdef SIGHT_DO_STATS
        sight_cnt_native_clrcall++;
#endif
    }
}

SIGHT_EXPORT_DECLARE(jint, Process, create0)(SIGHT_STDARGS,
                                             jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p = NULL;
    apr_status_t rv;

    UNREFERENCED_O;

    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        rv = APR_ENOPOOL;
        goto cleanup;
    }
    SIGHT_LOCAL_TRY(no) {
        p = (sight_runproc_t *)sight_pcalloc(_E, no->pool, sizeof(sight_runproc_t),
                                             THROW_FMARK);
        if (p == NULL) {
            rv = APR_ENOMEM;
            SIGHT_LOCAL_BRK(no);
            goto cleanup;
        }
        if ((rv = apr_procattr_create(&p->attr, no->pool)) != APR_SUCCESS) {
            SIGHT_LOCAL_BRK(no);
            goto cleanup;
        }
        p->exitval = -1;
#ifdef SIGHT_DO_STATS
        no->clean  = proc_cleanup;
#endif
        no->native = p;
        rv = APR_SUCCESS;
    } SIGHT_LOCAL_END(no);
cleanup:
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, Process, ioset0)(SIGHT_STDARGS,
                                            jlong instance,
                                            jint in,
                                            jint out,
                                            jint err)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;

    UNREFERENCED_STDARGS;
    if (!no || !no->native)
        return APR_EINVAL;
    p = (sight_runproc_t *)no->native;
    return (jint)apr_procattr_io_set(p->attr, (apr_int32_t)in,
                             (apr_int32_t)out, (apr_int32_t)err);
}

SIGHT_EXPORT_DECLARE(jint, Process, cmdset0)(SIGHT_STDARGS,
                                             jlong instance,
                                             jint cmd)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;

    UNREFERENCED_STDARGS;
    if (!no || !no->native)
        return APR_EINVAL;
    p = (sight_runproc_t *)no->native;
    return (jint)apr_procattr_cmdtype_set(p->attr, (apr_int32_t)cmd);
}

SIGHT_EXPORT_DECLARE(jint, Process, sdetach0)(SIGHT_STDARGS,
                                              jlong instance,
                                              jboolean on)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;
    apr_int32_t set = on ? 1 : 0;

    UNREFERENCED_STDARGS;
    if (!no || !no->native)
        return APR_EINVAL;
    p = (sight_runproc_t *)no->native;
    return (jint)apr_procattr_detach_set(p->attr, set);
}

SIGHT_EXPORT_DECLARE(jint, Process, detach)(SIGHT_STDARGS,
                                            jint deamonize)
{
    UNREFERENCED_STDARGS;
    return (jint)apr_proc_detach(deamonize);
}

SIGHT_EXPORT_DECLARE(jint, Process, dirset0)(SIGHT_STDARGS,
                                             jlong instance,
                                             jstring dir)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;
    apr_status_t rv;
    SIGHT_ALLOC_CSTRING(dir);

    UNREFERENCED_O;
    if (!no || !no->native) {
        rv = APR_EINVAL;
        goto cleanup;
    }
    p = (sight_runproc_t *)no->native;
    rv = apr_procattr_dir_set(p->attr, J2S(dir));

cleanup:
    SIGHT_FREE_CSTRING(dir);
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, Process, uset0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jstring username,
                                           jstring password)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;
    apr_status_t rv;
    SIGHT_ALLOC_CSTRING(username);
    SIGHT_ALLOC_CSTRING(password);

    UNREFERENCED_O;
    if (!no || !no->native) {
        rv = APR_EINVAL;
        goto cleanup;
    }
    p = (sight_runproc_t *)no->native;
    rv = apr_procattr_user_set(p->attr, J2S(username), J2S(password));

cleanup:
    SIGHT_FREE_CSTRING(username);
    SIGHT_FREE_CSTRING(password);
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, Process, gset0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jstring group)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;
    apr_status_t rv;
    SIGHT_ALLOC_CSTRING(group);

    UNREFERENCED_O;
    if (!no || !no->native) {
        rv = APR_EINVAL;
        goto cleanup;
    }
    p = (sight_runproc_t *)no->native;
    rv = apr_procattr_group_set(p->attr, J2S(group));

cleanup:
    SIGHT_FREE_CSTRING(group);
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, Process, ciset0)(SIGHT_STDARGS,
                                            jlong instance,
                                            jlong in,
                                            jlong parent)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_object_t *nf = J2P(in, sight_object_t *);
    sight_object_t *np = J2P(parent, sight_object_t *);
    sight_runproc_t *p;

    UNREFERENCED_STDARGS;
    if (!no || !no->native) {
        return APR_EINVAL;
    }
    p = (sight_runproc_t *)no->native;
    return (jint)apr_procattr_child_in_set(p->attr,
                                           (apr_file_t *)nf->native,
                                           (apr_file_t *)np->native);
}

SIGHT_EXPORT_DECLARE(jint, Process, coset0)(SIGHT_STDARGS,
                                            jlong instance,
                                            jlong out,
                                            jlong parent)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_object_t *nf = J2P(out, sight_object_t *);
    sight_object_t *np = J2P(parent, sight_object_t *);
    sight_runproc_t *p;

    UNREFERENCED_STDARGS;
    if (!no || !no->native) {
        return APR_EINVAL;
    }
    p = (sight_runproc_t *)no->native;
    return (jint)apr_procattr_child_out_set(p->attr,
                                            (apr_file_t *)nf->native,
                                            (apr_file_t *)np->native);
}

SIGHT_EXPORT_DECLARE(jint, Process, ceset0)(SIGHT_STDARGS,
                                            jlong instance,
                                            jlong err,
                                            jlong parent)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_object_t *nf = J2P(err, sight_object_t *);
    sight_object_t *np = J2P(parent, sight_object_t *);
    sight_runproc_t *p;

    UNREFERENCED_STDARGS;
    if (!no || !no->native) {
        return APR_EINVAL;
    }
    p = (sight_runproc_t *)no->native;
    return (jint)apr_procattr_child_err_set(p->attr,
                                            (apr_file_t *)nf->native,
                                            (apr_file_t *)np->native);
}

SIGHT_EXPORT_DECLARE(jint, Process, exitval0)(SIGHT_STDARGS,
                                              jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;

    UNREFERENCED_STDARGS;
    if (!no || !no->native)
        return 0;
    p = (sight_runproc_t *)no->native;
    return (jint)p->exitval;
}

SIGHT_EXPORT_DECLARE(jint, Process, exitwhy0)(SIGHT_STDARGS,
                                              jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;

    UNREFERENCED_STDARGS;
    if (!no || !no->native)
        return 0;
    p = (sight_runproc_t *)no->native;
    return (jint)p->exitwhy;
}

SIGHT_EXPORT_DECLARE(jint, Process, wait0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jint waithow)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;
    apr_status_t rv;

    UNREFERENCED_STDARGS;
    if (!no || !no->native)
        return APR_EINVAL;
    SIGHT_LOCAL_TRY(no) {
        p = (sight_runproc_t *)no->native;
        rv = apr_proc_wait(&p->p, &p->exitval, &p->exitwhy,
                           (apr_wait_how_e)waithow);
    } SIGHT_LOCAL_END(no);
    return rv;
}


SIGHT_EXPORT_DECLARE(jint, Process, wait1)(SIGHT_STDARGS,
                                           jlong instance,
                                           jint waithow,
                                           jlong timeout)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;
    apr_status_t rc = APR_ENOPOOL;
    apr_interval_time_t timeout_value, timeout_interval;
    apr_wait_how_e how = (apr_wait_how_e)waithow;

    UNREFERENCED_STDARGS;
    if (!no || !no->native)
        return APR_EINVAL;

    SIGHT_LOCAL_TRY(no) {
        p = (sight_runproc_t *)no->native;
        if (timeout < 0)
            how = APR_WAIT;
        else
            how = APR_NOWAIT;
        if ((rc = apr_proc_wait(&p->p, &p->exitval, &p->exitwhy,
                                how)) != APR_CHILD_NOTDONE) {
            SIGHT_LOCAL_BRK(no);
            return rc;
        }
        if (timeout < 1) {
            SIGHT_LOCAL_BRK(no);
            return APR_EINVAL;
        }
        if (SIGHT_LOCAL_IRQ(no)) {
            SIGHT_LOCAL_BRK(no);
            return APR_EINTR;
        }
        timeout_value = timeout * 1000L;
        timeout_interval = timeout_value / 64;
        do {
            if (SIGHT_LOCAL_IRQ(no)) {
                rc = APR_EINTR;
                break;
            }
            apr_sleep(timeout_interval);
            if (SIGHT_LOCAL_IRQ(no)) {
                rc = APR_EINTR;
                break;
            }
            rc = apr_proc_wait(&p->p, &p->exitval, &p->exitwhy, APR_NOWAIT);
            if (SIGHT_LOCAL_IRQ(no)) {
                rc = APR_EINTR;
                break;
            }
            if (timeout_interval >= timeout_value)
                break;
            timeout_interval *= 2;
        } while (rc == APR_CHILD_NOTDONE);
    } SIGHT_LOCAL_END(no);
    return rc;
}

SIGHT_EXPORT_DECLARE(jint, Process, wait2)(SIGHT_STDARGS,
                                           jlong instance,
                                           jobject progress,
                                           jlong timeout)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;
    apr_status_t rc;
    apr_interval_time_t timeout_value, timeout_interval;
    jclass c;
    jint   cres;
    jint   tick = 0;
    sight_callback_t cb;

    UNREFERENCED_O;

    if (!no || !no->native || !progress)
        return APR_EINVAL;
    SIGHT_LOCAL_TRY(no) {
        p = (sight_runproc_t *)no->native;
        if ((rc = apr_proc_wait(&p->p, &p->exitval, &p->exitwhy,
                                APR_NOWAIT)) != APR_CHILD_NOTDONE) {
            SIGHT_LOCAL_BRK(no);
            return rc;
        }
        if (SIGHT_LOCAL_IRQ(no)) {
            SIGHT_LOCAL_BRK(no);
            return APR_EINTR;
        }
        c = (*_E)->GetObjectClass(_E, progress);
        cb.name   = "progress";
        cb.msig   = "(I)I";
        cb.object = progress;
        cb.method = (*_E)->GetMethodID(_E, c, cb.name, cb.msig);
        if (!cb.method || (*_E)->ExceptionCheck(_E)) {
            SIGHT_LOCAL_BRK(no);
            return APR_EINVAL;
        }
        cres = (*_E)->CallIntMethod(_E, cb.object, cb.method, tick, NULL);
        if ((*_E)->ExceptionCheck(_E)) {
            SIGHT_LOCAL_BRK(no);
            return APR_FROM_OS_ERROR(EINTR);
        }

        if (timeout > 0) {
            timeout_value = timeout * 1000L;
            timeout_interval = timeout_value / 100;
        }
        else {
            /* Defaults to 100 ms */
            timeout_interval = 100000L;
        }
        do {
            if (cres < 0) {
                /* Broken by the callback */
                break;
            }
            else if (cres > 0) {
                timeout_interval = cres * 1000L;
            }
            if (SIGHT_LOCAL_IRQ(no)) {
                rc = APR_EINTR;
                break;
            }
            apr_sleep(timeout_interval);
            if (SIGHT_LOCAL_IRQ(no)) {
                rc = APR_EINTR;
                break;
            }
            rc = apr_proc_wait(&p->p, &p->exitval, &p->exitwhy, APR_NOWAIT);
            if (SIGHT_LOCAL_IRQ(no)) {
                rc = APR_EINTR;
                break;
            }
            if (timeout > 0) {
                if (timeout_interval >= timeout_value)
                    break;
                timeout_value -= timeout_interval;
            }
            cres = (*_E)->CallIntMethod(_E, cb.object, cb.method, tick++, NULL);
            if ((*_E)->ExceptionCheck(_E)) {
                SIGHT_LOCAL_BRK(no);
                return APR_FROM_OS_ERROR(EINTR);
            }

        } while (rc == APR_CHILD_NOTDONE);
    } SIGHT_LOCAL_END(no);
    return rc;
}

SIGHT_EXPORT_DECLARE(jint, Process, kill0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jint sig)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;

    UNREFERENCED_STDARGS;
    if (!no || !no->native)
        return APR_EINVAL;
    p = (sight_runproc_t *)no->native;
    return (jint)apr_proc_kill(&p->p, sig);
}

SIGHT_EXPORT_DECLARE(jint, Process, pid0)(SIGHT_STDARGS,
                                          jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;

    UNREFERENCED_STDARGS;
    if (!no || !no->native)
        return -1;
    p = (sight_runproc_t *)no->native;
    return (jint)p->p.pid;
}

SIGHT_EXPORT_DECLARE(jint, Process, exec0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jstring progname,
                                           jobjectArray args,
                                           jobjectArray env)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;
    apr_status_t rv;
    char *sargs[MAX_ARGS_SIZE + 1];
    char *senv[MAX_ENV_SIZE + 1];
    const char * const *pargs = NULL;
    const char * const *penv  = NULL;
    jsize largs = 0;
    jsize lenv = 0;
    jsize i;
    SIGHT_ALLOC_CSTRING(progname);

    UNREFERENCED_O;

    if (!no || !no->native) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPROC));
        rv = APR_ENOPOOL;
        goto cleanup;
    }
    SIGHT_LOCAL_TRY(no) {
        if (args)
            largs = (*_E)->GetArrayLength(_E, args);
        if (env)
            lenv = (*_E)->GetArrayLength(_E, env);
        if (largs > MAX_ARGS_SIZE || lenv > MAX_ENV_SIZE) {
            rv = APR_EINVAL;
            SIGHT_LOCAL_BRK(no);
            goto cleanup;
        }
        if (largs) {
            for (i = 0; i < largs; i++) {
                jstring str = (*_E)->GetObjectArrayElement(_E, args, i);
    	        if (!str || (*_E)->ExceptionCheck(_E)) {
    	            rv = APR_EINVAL;
                    SIGHT_LOCAL_BRK(no);
    	            goto cleanup;
    	        }
                sargs[i] = sight_pstrdupj(no->pool, _E, str);
                (*_E)->DeleteLocalRef(_E, str);
            }
            sargs[i] = NULL;
            pargs = (const char * const *)&sargs[0];
        }
        if (lenv) {
            for (i = 0; i < lenv; i++) {
                jstring str = (*_E)->GetObjectArrayElement(_E, env, i);
    	        if (!str || (*_E)->ExceptionCheck(_E)) {
    	            rv = APR_EINVAL;
                    SIGHT_LOCAL_BRK(no);
    	            goto cleanup;
    	        }
                senv[i] = sight_pstrdupj(no->pool, _E, str);
                (*_E)->DeleteLocalRef(_E, str);
            }
            senv[i] = NULL;
            penv = (const char * const *)&senv[0];
        }
        p = (sight_runproc_t *)no->native;
        rv = apr_proc_create(&p->p, J2S(progname), pargs,
                             penv, p->attr, no->pool);
        if (rv != APR_SUCCESS) {
            throwAprException(_E, rv);
        }
    } SIGHT_LOCAL_END(no);
cleanup:
    SIGHT_FREE_CSTRING(progname);
    return rv;
}

SIGHT_EXPORT_DECLARE(jlong, Process, getios0)(SIGHT_STDARGS,
                                              jlong instance,
                                              jint which)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;

    UNREFERENCED_O;
    if (!no || !no->native) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPROC));
        return 0;
    }
    p = (sight_runproc_t *)no->native;
    switch (which) {
        case 0:
            return P2J(p->p.in);
        break;
        case 1:
            return P2J(p->p.out);
        break;
        case 2:
            return P2J(p->p.err);
        break;
        default:
            throwAprException(_E, APR_EINVAL);
        break;
    }
    return 0;
}

SIGHT_EXPORT_DECLARE(jint, Process, setiostm0)(SIGHT_STDARGS,
                                               jlong instance,
                                               jlong timeout,
                                               jint which)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;
    apr_status_t rv = APR_EINVAL;

    UNREFERENCED_O;
    if (!no || !no->native) {
        return APR_EINVAL;
    }
    SIGHT_LOCAL_TRY(no) {
        p = (sight_runproc_t *)no->native;
        switch (which) {
            case 0:
                rv = apr_file_pipe_timeout_set(p->p.in, timeout * 1000L);
            break;
            case 1:
                rv = apr_file_pipe_timeout_set(p->p.out, timeout * 1000L);
            break;
            case 2:
                rv = apr_file_pipe_timeout_set(p->p.err, timeout * 1000L);
            break;
        }
    } SIGHT_LOCAL_END(no);
    return rv;
}

SIGHT_EXPORT_DECLARE(void, Process, notes0)(SIGHT_STDARGS,
                                            jlong instance,
                                            jint how)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    sight_runproc_t *p;

    UNREFERENCED_STDARGS;
    if (!no || !no->native || !no->pool) {
        return;
    }
    p = (sight_runproc_t *)no->native;
    if (p->killhow)
        return;
    apr_pool_note_subprocess(no->pool, &p->p, (apr_kill_conditions_e)how);
    p->killhow = 1;
}

#if APR_HAVE_STRUCT_RLIMIT
SIGHT_EXPORT_DECLARE(int, Process, rlimit0)(SIGHT_STDARGS,
                                            jlong instance,
                                            jint what,
                                            jint soft,
                                            jint hard)
{
    sight_object_t *no = J2P(instance,  sight_object_t *);
    sight_runproc_t *p;
    struct rlimit *limit;

    UNREFERENCED_O;
    if (!no || !no->native || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPROC));
        return APR_ENOPOOL;
    }
    p = (sight_runproc_t *)no->native;
    limit = sight_palloc(_E, no->pool, sizeof(struct rlimit), THROW_FMARK);
    if (!limit)
        return APR_ENOMEM;
    limit->rlim_cur = soft;
    limit->rlim_max = hard;
    return apr_procattr_limit_set(p->attr, what, limit);
}
#else
SIGHT_EXPORT_DECLARE(int, Process, rlimit0)(SIGHT_STDARGS,
                                            jlong instance,
                                            jint what,
                                            jint soft,
                                            jint hard)
{
    sight_object_t *no = J2P(instance,  sight_object_t *);

    UNREFERENCED_STDARGS;
    if (!no || !no->native || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPROC));
        return APR_ENOPOOL;
    }
    return APR_ENOTIMPL;
}
#endif

SIGHT_EXPORT_DECLARE(void, Pool, notes0)(SIGHT_STDARGS,
                                         jlong instance,
                                         jlong process,
                                         jint how)
{
    sight_object_t *np = J2P(instance, sight_object_t *);
    sight_object_t *no = J2P(process,  sight_object_t *);
    sight_runproc_t *p;

    UNREFERENCED_STDARGS;
    if (!np || !np->pool) {
        return;
    }
    if (!no || !no->native || !no->pool) {
        return;
    }
    p = (sight_runproc_t *)no->native;
    if (p->killhow)
        return;
    apr_pool_note_subprocess(np->pool, &p->p, (apr_kill_conditions_e)how);
    p->killhow = 1;
}
