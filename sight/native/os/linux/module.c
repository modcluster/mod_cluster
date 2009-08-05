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
 * Process windows implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"

/*
 * Module
 */

J_DECLARE_CLAZZ = {
    NULL,
    NULL,
    SIGHT_CLASS_PATH "Module"
};

J_DECLARE_M_ID(0000) = {
    NULL,
    "<init>",
    "(II)V"
};


J_DECLARE_F_ID(0000) = {
    NULL,
    "Name",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0001) = {
    NULL,
    "BaseName",
    "Ljava/lang/String;"
};

J_DECLARE_F_ID(0002) = {
    NULL,
    "BaseAddress",
    "J"
};

J_DECLARE_F_ID(0003) = {
    NULL,
    "Size",
    "J"
};


SIGHT_CLASS_LDEF(Module)
{
    if (sight_load_class(_E, &_clazzn))
        return 1;
    J_LOAD_METHOD(0000);
    J_LOAD_IFIELD(0000);
    J_LOAD_IFIELD(0001);
    J_LOAD_IFIELD(0002);
    J_LOAD_IFIELD(0003);

    return 0;
}

SIGHT_CLASS_UDEF(Module)
{
    sight_unload_class(_E, &_clazzn);
}

typedef struct module_addr_t module_addr_t;
struct module_addr_t {
    const char        *path;
    unsigned long long base;
    unsigned long long size;
};

static jobject new_module_class(SIGHT_STDARGS, jint pid, jint id)
{
    if (_clazzn.i && _m0000n.i)
        return (*_E)->NewObject(_E, _clazzn.i, _m0000n.i, pid, id, NULL);
    else
        return NULL;
}


SIGHT_EXPORT_DECLARE(jobjectArray, Module, enum0)(SIGHT_STDARGS,
                                                  jint pid)
{
    char spath[SIGHT_SBUFFER_SIZ];
    jsize j, i = 0, nmods = 0;
    jobjectArray mods = NULL;
    sight_arr_t   *amods = NULL;
    cache_table_t *cmods = NULL;

    UNREFERENCED_O;
    if (pid < 0)
        strcpy(spath, "/proc/self/maps");
    else
        sprintf(spath, "/proc/%d/maps", pid);
    if (!(amods = sight_arr_rload(spath))) {
        throwAprIOException(_E, apr_get_os_error());
        goto cleanup;
    }
    if (!(cmods = cache_new(4))) {
        throwAprMemoryException(_E, THROW_FMARK,
                                apr_get_os_error());
        goto cleanup;
    }

    for (j = 0; j < amods->siz; j++) {
        char *bp;
        if ((bp = strchr(amods->arr[j], '/'))) {
            module_addr_t *ma;
            cache_entry_t *me;
            char *bn = NULL;
            char *p;
            unsigned long long b, o;

            if ((bn = strrchr(bp, '/')))
                bn++;
            else
                continue;

            me = cache_add(cmods, bn);
            if (!me->data) {
                if (!(ma = (module_addr_t *)sight_calloc(_E,
                                               sizeof(module_addr_t),
                                               THROW_FMARK))) {
                    goto cleanup;
                }
                ma->path = bp;
                me->data = ma;
                nmods++;
            }
            else
                ma = (module_addr_t *)me->data;
            b = strtoull(amods->arr[j], &p, 16);
            if (p && *p == '-')
                o = strtoull(p + 1, NULL, 16) - b;
            else
                o = 0;
            if (!ma->base)
                ma->base = b;
            ma->size += o;
        }
    }

    mods = (*_E)->NewObjectArray(_E, nmods, _clazzn.a, NULL);
    if (!mods || (*_E)->ExceptionCheck(_E)) {
        goto cleanup;
    }
    for (j = 0; j < cmods->siz; j++) {
        module_addr_t *ma = (module_addr_t *)cmods->list[j]->data;
        jobject m = new_module_class(_E, _O, pid, i);
        if (!m || (*_E)->ExceptionCheck(_E)) {
            mods = NULL;
            goto cleanup;
        }
        SET_IFIELD_S(0000, m, cmods->list[j]->key);
        SET_IFIELD_S(0001, m, ma->path);
        SET_IFIELD_J(0002, m, ma->base);
        SET_IFIELD_J(0003, m, ma->size);

        (*_E)->SetObjectArrayElement(_E, mods, i++, m);
        (*_E)->DeleteLocalRef(_E, m);
        if (i > nmods)
            break;
    }

cleanup:
    if (amods)
        sight_arr_free(amods);
    if (cmods)
        cache_free(cmods, NULL);

    return mods;
}
