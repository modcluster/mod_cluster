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
 * Service Control Manager implementation
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"

static const struct {
    const char *flavor; /* Linux flavor */
    const char *idpath; /* Path of the init.d scripts */
    const char *rlpath; /* Format or the rc0.d ... rc6.d scripts */
    int         rlevel; /* Default run level */
} scm_paths[] = {
    { "redhat",
      "/etc/rc.d/init.d/",
      "/etc/rc.d/rc%d.d/",
      5
    },
    { "debian",
      "/etc/init.d/",
      "/etc/rc%d.d/",
      5
    },
    { NULL,
      NULL,
      NULL,
      0
    }
};

static void scm_cleanup(int mode, sight_object_t *no)
{
    if (no && no->native) {
        scm_instance_t *scm = (scm_instance_t *)no->native;
        if (scm->services)
            sight_arr_free(scm->services);
        free(scm);
        no->native = NULL;
    }
}


SIGHT_EXPORT_DECLARE(jint, ServiceControlManager, open0)(SIGHT_STDARGS,
                                                         jlong instance,
                                                         jstring database,
                                                         jint mode)
{
    int i = 0;
    DIR *sd = NULL;
    scm_instance_t *si;
    sight_object_t *no = J2P(instance, sight_object_t *);
    apr_status_t rv;

    UNREFERENCED_O;
    UNREFERENCED(database);
    UNREFERENCED(mode);

    if (!no || !no->pool) {
        return APR_EINVAL;
    }
    SIGHT_LOCAL_TRY(no) {
        if (no->native)
            scm_cleanup(0, no);
        if (!(si = (scm_instance_t *)sight_calloc(_E,
                                        sizeof(scm_instance_t),
                                        THROW_FMARK))) {
            SIGHT_LOCAL_BRK(no);
            return apr_get_os_error();
        }
        no->native = si;
        while (scm_paths[i].flavor) {
            char sname[PATH_MAX];
            struct stat sb;
            if ((sd = opendir(scm_paths[i].idpath))) {
                sprintf(sname, scm_paths[i].rlpath, scm_paths[i].rlevel);
                /* Check default run level path */
                if (stat(sname, &sb) < 0) {
                    closedir(sd);
                    sd = NULL;
                    continue;
                }
                si->flavor = scm_paths[i].flavor;
                si->idpath = scm_paths[i].idpath;
                si->rlpath = scm_paths[i].rlpath;
                si->what   = scm_paths[i].rlevel;
                if (!(si->services = sight_arr_new(16))) {
                    rv = apr_get_os_error();
                    closedir(sd);
                    scm_cleanup(0, no);
                    SIGHT_LOCAL_BRK(no);
                    return rv;
                }
                break;
            }
            i++;
        }
        if (sd) {
            struct dirent *sent, sbuf;
            while (!readdir_r(sd, &sbuf, &sent)) {
                char sname[PATH_MAX];
                struct stat sb;
                if (!sent)
                    break;
                strcpy(sname, si->idpath);
                strcat(sname, sent->d_name);
                if (stat(sname, &sb) < 0)
                    continue;
                if (!S_ISREG(sb.st_mode))
                    continue;
                if (!(sb.st_mode & (S_IXUSR | S_IXGRP | S_IXOTH)))
                    continue;
                sight_arr_add(si->services, sent->d_name);
            }
            closedir(sd);
            no->clean = scm_cleanup;
            rv = APR_SUCCESS;
        }
        else {
            rv = apr_get_os_error();
            scm_cleanup(0, no);
        }
    } SIGHT_LOCAL_END(no);
    return rv;
}

SIGHT_EXPORT_DECLARE(void, ServiceControlManager, close0)(SIGHT_STDARGS,
                                                          jlong instance)
{
    sight_object_t *no = J2P(instance, sight_object_t *);

    UNREFERENCED_STDARGS;

    if (no) {
        scm_cleanup(0, no);
        no->clean = NULL;
    }
}

SIGHT_EXPORT_DECLARE(jobjectArray, ServiceControlManager,
                     enum0)(SIGHT_STDARGS, jlong instance,
                            jint drivers, jint what)
{
    sight_object_t *no = J2P(instance, sight_object_t *);
    scm_instance_t *si;
    jobjectArray ea = NULL;
    jsize cnt = 0;
    jint  idx = 0;

    UNREFERENCED_O;
    if (!no || !no->pool || !no->native) {
        return NULL;
    }
    SIGHT_LOCAL_TRY(no) {
        if (drivers) {
            /* There are no drivers on linux ?
             * Perhaps we should have here the kernel modules
             */
            SIGHT_LOCAL_BRK(no);
            return NULL;
        }
        si = (scm_instance_t *)no->native;
        if (what > 0)
            si->what = what;
        if (si->services && si->services->siz) {
            jint i;
            /* TODO: Calculate size according to the flags */
            cnt = si->services->siz;
            ea  = sight_new_cc_array(_E, SIGHT_CC_STRING, cnt);
            if (!ea || (*_E)->ExceptionCheck(_E)) {
                SIGHT_LOCAL_BRK(no);
                return NULL;
            }
            for (i = 0; i < cnt; i++) {
                jstring s = CSTR_TO_JSTRING(si->services->arr[i]);
                if (s)
                    (*_E)->SetObjectArrayElement(_E, ea, idx++, s);
                else
                    break;
                if ((*_E)->ExceptionCheck(_E)) {
                    ea = NULL;
                    break;
                }
                (*_E)->DeleteLocalRef(_E, s);
            }
        }
    } SIGHT_LOCAL_END(no);
    return ea;
}
