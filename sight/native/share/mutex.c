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


/*
 * Mutex
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Mutex"
};

J_DECLARE_F_ID(0000) = {
    NULL,
    "PROC_MUTEX_IS_GLOBAL",
    "Z"
};

SIGHT_CLASS_LDEF(Mutex)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_SFIELD(0000);
    SET_SFIELD_Z(0000, APR_PROC_MUTEX_IS_GLOBAL);
    return 0;
}

SIGHT_CLASS_UDEF(Mutex)
{
    sight_unload_class(_E, &_clazzn);
}

static void mutex_cleanup(int mode, sight_object_t *no)
{
    if (mode != POOL_CALLBACK && no && no->native) {
        if (no->opaque)
            apr_global_mutex_destroy((apr_global_mutex_t *)no->native);
        else
            apr_proc_mutex_destroy((apr_proc_mutex_t *)no->native);
    }
}

SIGHT_EXPORT_DECLARE(jstring, Mutex, getDefaultName)(SIGHT_STDARGS)
{
    UNREFERENCED_O;
    RETURN_JCSTR(apr_proc_mutex_defname());
}

SIGHT_EXPORT_DECLARE(jint, Mutex, create0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jstring name,
                                           jint mech,
                                           jboolean global)
{
    sight_object_t *no   = J2P(instance, sight_object_t *);
    apr_proc_mutex_t   *pmutex = NULL;
    apr_global_mutex_t *gmutex = NULL;
    apr_status_t rv;
    SIGHT_ALLOC_CSTRING(name);

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        rv = APR_ENOPOOL;
        goto cleanup;
    }
#if APR_PROC_MUTEX_IS_GLOBAL
    if ((rv = apr_proc_mutex_create(&pmutex, J2S(name),
                                    (apr_lockmech_e)mech,
                                    no->pool)) != APR_SUCCESS) {
        throwAprException(_E, rv);
        goto cleanup;
    }
    no->native = pmutex;
    no->opaque = NULL;
#else
    if (global) {
        if ((rv = apr_global_mutex_create(&gmutex, J2S(name),
                                          (apr_lockmech_e)mech,
                                          no->pool)) != APR_SUCCESS) {
            throwAprException(_E, rv);
            goto cleanup;
        }
        no->native = gmutex;
        no->opaque = gmutex;
    } else {
        if ((rv = apr_proc_mutex_create(&pmutex, J2S(name),
                                        (apr_lockmech_e)mech,
                                        no->pool)) != APR_SUCCESS) {
            throwAprException(_E, rv);
            goto cleanup;
        }
        no->native = pmutex;
        no->opaque = NULL;
    }

#endif

    no->clean  = mutex_cleanup;
cleanup:
    SIGHT_FREE_CSTRING(name);
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, Mutex, child0)(SIGHT_STDARGS,
                                          jlong instance,
                                          jstring name,
                                          jboolean global)
{
    sight_object_t *no   = J2P(instance, sight_object_t *);
    apr_proc_mutex_t   *pmutex = NULL;
    apr_global_mutex_t *gmutex = NULL;
    apr_status_t rv;
    SIGHT_ALLOC_CSTRING(name);

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        rv = APR_ENOPOOL;
        goto cleanup;
    }
#if APR_PROC_MUTEX_IS_GLOBAL
    if ((rv = apr_proc_mutex_child_init(&pmutex, J2S(name),
                                        no->pool)) != APR_SUCCESS) {
        throwAprException(_E, rv);
        goto cleanup;
    }
    no->native = pmutex;
#else
    if (global) {
        if ((rv = apr_global_mutex_child_init(&gmutex, J2S(name),
                                              no->pool)) != APR_SUCCESS) {
            throwAprException(_E, rv);
            goto cleanup;
        }
        no->native = gmutex;
        no->opaque = gmutex;
    }
    else {
        if ((rv = apr_proc_mutex_child_init(&pmutex, J2S(name),
                                            no->pool)) != APR_SUCCESS) {
            throwAprException(_E, rv);
            goto cleanup;
        }
        no->native = pmutex;
        no->opaque = NULL;
    }
#endif

    no->clean  = mutex_cleanup;
cleanup:
    SIGHT_FREE_CSTRING(name);
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, Mutex, lock0)(SIGHT_STDARGS,
                                         jlong instance)
{
    sight_object_t *no   = J2P(instance, sight_object_t *);

    UNREFERENCED_STDARGS;
    if (!no || !no->pool)
        return APR_ENOPOOL;
    if (!no->native)
        return APR_EINVAL;
    if (no->opaque)
        return apr_global_mutex_lock((apr_global_mutex_t *)no->native);
    else
        return apr_proc_mutex_lock((apr_proc_mutex_t *)no->native);

}

SIGHT_EXPORT_DECLARE(jint, Mutex, trylock0)(SIGHT_STDARGS,
                                            jlong instance)
{
    sight_object_t *no   = J2P(instance, sight_object_t *);

    UNREFERENCED_STDARGS;
    if (!no || !no->pool)
        return APR_ENOPOOL;
    if (!no->native)
        return APR_EINVAL;
    if (no->opaque)
        return apr_global_mutex_trylock((apr_global_mutex_t *)no->native);
    else
        return apr_proc_mutex_trylock((apr_proc_mutex_t *)no->native);

}

SIGHT_EXPORT_DECLARE(jint, Mutex, unlock0)(SIGHT_STDARGS,
                                           jlong instance)
{
    sight_object_t *no   = J2P(instance, sight_object_t *);

    UNREFERENCED_STDARGS;
    if (!no || !no->pool)
        return APR_ENOPOOL;
    if (!no->native)
        return APR_EINVAL;
    if (no->opaque)
        return apr_global_mutex_unlock((apr_global_mutex_t *)no->native);
    else
        return apr_proc_mutex_unlock((apr_proc_mutex_t *)no->native);
}

SIGHT_EXPORT_DECLARE(jstring, Mutex, fname0)(SIGHT_STDARGS,
                                             jlong instance)
{
    sight_object_t *no   = J2P(instance, sight_object_t *);

    UNREFERENCED_O;
    if (no && no->native) {
#if APR_PROC_MUTEX_IS_GLOBAL
        RETURN_JCSTR(apr_proc_mutex_lockfile((apr_proc_mutex_t *)no->native));
#else
        if (no->opaque) {
            apr_os_global_mutex_t osm;
            apr_os_global_mutex_get(&osm, (apr_global_mutex_t *)no->native);
            RETURN_JCSTR(apr_proc_mutex_lockfile(osm.proc_mutex));
        }
        else {
            RETURN_JCSTR(apr_proc_mutex_lockfile((apr_proc_mutex_t *)no->native));
        }
#endif
    }
    else
        return NULL;
}

SIGHT_EXPORT_DECLARE(jstring, Mutex, name0)(SIGHT_STDARGS,
                                            jlong instance)
{
    sight_object_t *no   = J2P(instance, sight_object_t *);

    UNREFERENCED_O;
    if (no && no->native) {
#if APR_PROC_MUTEX_IS_GLOBAL
        RETURN_JCSTR(apr_proc_mutex_name((apr_proc_mutex_t *)no->native));
#else
        if (no->opaque) {
            apr_os_global_mutex_t osm;
            apr_os_global_mutex_get(&osm, (apr_global_mutex_t *)no->native);
            RETURN_JCSTR(apr_proc_mutex_name(osm.proc_mutex));
        }
        else {
            RETURN_JCSTR(apr_proc_mutex_name((apr_proc_mutex_t *)no->native));
        }
#endif
    }
    else
        return NULL;
}
