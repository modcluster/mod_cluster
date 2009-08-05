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

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mkdev.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <libelf.h>
#include <procfs.h>

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

extern apr_pool_t *sight_temp_pool;

typedef struct module_enum_t {
  int nummodules;
  apr_pool_t *pool;
  struct module *first_module;
  struct module *current;
} module_enum_t;

struct module {
  char *name;
  long base;
  long size;
  struct module *next;
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

static jobject new_module_class(SIGHT_STDARGS, jint pid, jint id)
{
    if (_clazzn.i && _m0000n.i)
        return (*_E)->NewObject(_E, _clazzn.i, _m0000n.i, pid, id, NULL);
    else
        return NULL;
}

static int proc_map_f(void *ptr, const prmap_t *map, const char *car)
{
    module_enum_t *module = ptr;
    struct module *current;
    // fill first, use current and put next in current...
    if (module->first_module == NULL) {
        module->first_module = apr_palloc(module->pool, sizeof(struct module));
        current = module->first_module;
    } else {
        current = module->current;
        current->next = apr_palloc(module->pool, sizeof(struct module));
        current = current->next;
    }
    module->current = current;
    current->next = NULL;
    current->name = apr_pstrdup(module->pool, car);
    current->base = map->pr_vaddr;
    current->size = map->pr_size;
    module->nummodules++;
    return 0;
}
/* Use a child process to read maps */
static int proc_map_child(void *ptr, const prmap_t *map, const char *car)
{
    FILE *file = (FILE *)ptr;
    fprintf(file, "%s\n", car);
    fprintf(file, "%u\n", map->pr_vaddr);
    fprintf(file, "%u\n", map->pr_size);
    return 0;
}
static void proc_child(int fd, int pid)
{
    struct ps_prochandle *ph;
    int ret;
    FILE *file = fdopen(fd, "w");
    ph = Pgrab(pid, 0, &ret);
    if (ph == NULL)
        return;
    Pobject_iter(ph, proc_map_child, file);
    Prelease(ph, 0);
    return;
}
/* read information for the son process */
static void  Read_map_f(int fd, module_enum_t *module)
{
    FILE *file = fdopen(fd, "r");
    char car[128];
    char buf[128];
    int addr, pr_size; 
    prmap_t map;

    for (;;) {
        if (fscanf(file, "%s", car)<0)
            break;
        fscanf(file, "%s", buf);
        map.pr_vaddr = atoi(buf);
        fscanf(file, "%s", buf);
        map.pr_size = atoi(buf);
        proc_map_f((void *)module, &map, car);
    }
}
#define G_SELF          9

SIGHT_EXPORT_DECLARE(jobjectArray, Module, enum0)(SIGHT_STDARGS,
                                                  jint pid)
{
    int mpid = getpid();
    apr_pool_t *pool;
    int j;
    int ret;
    apr_status_t rc;
    struct ps_prochandle *ph;
    module_enum_t *module;
    struct module *current;
    jobjectArray mods = NULL;
    int filedes[2];

    UNREFERENCED_O;
    if (pid >= 0)
        mpid = pid;

    if ((rc = sight_pool_create(&pool, NULL, sight_temp_pool, 0)) != APR_SUCCESS) {
        throwAprMemoryException(_E, THROW_FMARK, rc);
        return NULL;
    }
    ph = Pgrab(mpid, 0, &ret);
    if (ph == NULL) {
        if (ret != G_SELF) {
            throwOSException(_E, Pgrab_error(ret));
            apr_pool_destroy(pool);
            return NULL;
        } else {
            /* fork and use a pipe to read self info */
            pipe(filedes);
            if (fork() == 0) {
                close(filedes[0]);
                proc_child(filedes[1], pid);
                exit(0);
            } else {
                close(filedes[1]);
            }
        }
    } else {
        filedes[0] = -1;
    }

    /* Process all entries */
    module = apr_palloc(pool, sizeof(module_enum_t));
    if (module == NULL) {
        throwAprMemoryException(_E, THROW_FMARK, apr_get_os_error());
        if (filedes[0] == -1)
            Prelease(ph, 0);
        apr_pool_destroy(pool);
        return NULL;
    }
    module->pool = pool;
    module->nummodules = 0;
    module->first_module = NULL;
    module->current = NULL;
    if (filedes[0] == -1) {
        Pobject_iter(ph, proc_map_f, module);
        if (ret<0) {
            throwOSException(_E, Pgrab_error(ret));
            Prelease(ph, 0);
            apr_pool_destroy(pool);
            return NULL;
        }
        /* Release the proc file system */
        Prelease(ph, 0);
    } else {
        /* use pipe to read the information */
        Read_map_f(filedes[0], module);
        close(filedes[0]);
    }

    /* Fill the java objects */
    mods = (*_E)->NewObjectArray(_E, module->nummodules, _clazzn.a, NULL);
    if (!mods || (*_E)->ExceptionCheck(_E)) {
        apr_pool_destroy(pool);
        return NULL;
    }
    current = module->first_module;
    for (j = 0; j < module->nummodules; j++) {
        jobject m = new_module_class(_E, _O, pid, j);
        if (!m || (*_E)->ExceptionCheck(_E)) {
            apr_pool_destroy(pool);
            return NULL;
        }
        SET_IFIELD_S(0000, m, current->name);
        SET_IFIELD_S(0001, m, current->name); /* should be basename */
        SET_IFIELD_J(0002, m, current->base);
        SET_IFIELD_J(0003, m, current->size);
        (*_E)->SetObjectArrayElement(_E, mods, j, m);
        (*_E)->DeleteLocalRef(_E, m);
        current = current->next;
    }
    apr_pool_destroy(pool);

    return mods;
}
