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

kstat_ctl_t *kc = NULL;

/*
 * read one module information.
 */
apr_status_t sight_solaris_getone(char *module, void *ptr)
{
    kstat_t *ksp;

    if (!kc)
        return APR_ENOTIMPL;
    kstat_chain_update(kc);
    for (ksp = kc->kc_chain; ksp; ksp = ksp->ks_next) {
        if (strcmp(ksp->ks_module, module))
            continue;
        if (kstat_read(kc, ksp, ptr) < 0) {
            return APR_ENOTIMPL;
        } else {
            return APR_SUCCESS;
        }
    }
    return APR_ENOTIMPL;
}

/*
 * Return the kstat_ctl_t to use in a loop (for processors for example)
 */
kstat_ctl_t * sight_solaris_get()
{
    if (!kc)
        return NULL;
    kstat_chain_update(kc);
    return kc;
}

/*
 * Read the psinfo_t structure from /proc/<pid>/psinfo
 */
apr_status_t sight_solaris_get_psinfo(apr_pool_t *pool, int pid, psinfo_t *psinfo)
{
    char pname[SIGHT_STYPE_SIZ];
    apr_finfo_t i;
    int fd;
    apr_status_t rc;
  
    /* Don't check directory on Solaris */ 
    sprintf(pname, "/proc/%d/psinfo", pid);
    /* XXX: use APR */
    if ((fd = open(pname, O_RDONLY)) < 0) {
        return apr_get_os_error();
    }

    if (read(fd, psinfo, sizeof(psinfo_t)) != sizeof(psinfo_t)) {
        close(fd);
        return apr_get_os_error();
    }
    close(fd);

    
    return APR_SUCCESS;
}

/*
 * Called when initializing of the library
 */
apr_status_t sight_main(apr_pool_t *pool)
{
    kc = kstat_open();
    if (!kc)
        return APR_ENOTIMPL;
    if (kstat_chain_update(kc) == -1) {
         kstat_close(kc);
         return APR_ENOTIMPL;
    }

    return APR_SUCCESS;
}
