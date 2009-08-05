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

static void shm_cleanup(int mode, sight_object_t *no)
{
    if (mode != POOL_CALLBACK && no && no->native) {
        if (no->opaque)
            apr_shm_detach((apr_shm_t *)no->native);
        else
            apr_shm_destroy((apr_shm_t *)no->native);
    }
}


SIGHT_EXPORT_DECLARE(jint, Shm, create0)(SIGHT_STDARGS,
                                         jlong instance,
                                         jstring name,
                                         jlong size)
{
    sight_object_t *no   = J2P(instance, sight_object_t *);
    apr_shm_t *shm = NULL;
    apr_status_t rv;
    SIGHT_ALLOC_CSTRING(name);

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        rv = APR_ENOPOOL;
        goto cleanup;
    }
    SIGHT_LOCAL_TRY(no) {
        rv = apr_shm_create(&shm, (apr_size_t)size, J2S(name), no->pool);

        if (rv == APR_SUCCESS) {
            no->clean  = shm_cleanup;
            no->native = shm;
            no->opaque = NULL;
        }
    } SIGHT_LOCAL_END(no);
cleanup:
    SIGHT_FREE_CSTRING(name);
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, Shm, attach0)(SIGHT_STDARGS,
                                         jlong instance,
                                         jstring name)
{
    sight_object_t *no   = J2P(instance, sight_object_t *);
    apr_shm_t *shm = NULL;
    apr_status_t rv;
    SIGHT_ALLOC_CSTRING(name);

    UNREFERENCED_O;
    if (!no || !no->pool) {
        throwNullPointerException(_E, THROW_FMARK,
                                  sight_strerror(SIGHT_ENOPOOL));
        rv = APR_ENOPOOL;
        goto cleanup;
    }

    SIGHT_LOCAL_TRY(no) {
        rv = apr_shm_attach(&shm, J2S(name), no->pool);

        if (rv == APR_SUCCESS) {
            no->clean  = shm_cleanup;
            no->native = shm;
            no->opaque = shm;
        }
    } SIGHT_LOCAL_END(no);
cleanup:
    SIGHT_FREE_CSTRING(name);
    return rv;
}

SIGHT_EXPORT_DECLARE(jlong, Shm, size0)(SIGHT_STDARGS,
                                        jlong instance)
{
    sight_object_t *no   = J2P(instance, sight_object_t *);
    UNREFERENCED_STDARGS;
    if (no && no->native)
        return apr_shm_size_get(no->native);
    else
        return -1;
}

SIGHT_EXPORT_DECLARE(jobject, Shm, addr0)(SIGHT_STDARGS,
                                          jlong instance,
                                          jlong asize)
{
    sight_object_t *no   = J2P(instance, sight_object_t *);

    UNREFERENCED_O;
    if (no && no->native) {
        jobject rv = NULL;
        SIGHT_LOCAL_TRY(no) {
            jbyte *base = (jbyte *)apr_shm_baseaddr_get(no->native);
            apr_size_t  siz;
            if (asize < 0)
                siz = apr_shm_size_get(no->native);
            else
                siz = (apr_size_t)asize;
            if (siz > 0 && base) {
                rv = (*_E)->NewDirectByteBuffer(_E, base, siz);
            }
        } SIGHT_LOCAL_END(no);
        return rv;
    }
    return NULL;
}

SIGHT_EXPORT_DECLARE(jint, Shm, remove0)(SIGHT_STDARGS,
                                         jstring name,
                                         jlong pool)
{
    apr_status_t rv = APR_ENOPOOL;
    apr_pool_t *p   = J2P(pool, apr_pool_t *);
    SIGHT_ALLOC_CSTRING(name);

    UNREFERENCED_O;
    if (p) {
        SIGHT_GLOBAL_TRY {
            rv = apr_shm_remove(J2S(name), p);
        } SIGHT_GLOBAL_END();
    }
    SIGHT_FREE_CSTRING(name);
    return rv;
}
