/*
 *  mod_cluster.
 *
 *  Copyright(c) 2008 Red Hat Middleware, LLC,
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
 * @author Jean-Frederic Clere
 * @version $Revision$
 */

/* Memory handler for a shared memory divided in slot.
 * This one uses shared memory.
 */
#define CORE_PRIVATE

#include "apr.h"
#include "apr_pools.h"
#include "apr_shm.h"

#include "httpd.h"
#include "http_config.h"
#include "http_log.h"
#include "ap_provider.h"

#include  "slotmem.h"
#include "sharedmem_util.h"

/* make sure the shared memory is cleaned */
static int initialize_cleanup(apr_pool_t *p, apr_pool_t *plog, apr_pool_t *ptemp, server_rec *s)
{
    void *data;
    const char *userdata_key = "mod_sharedmem_init";

    apr_pool_userdata_get(&data, userdata_key, s->process->pool);
    if (!data) {
        apr_pool_userdata_set((const void *)1, userdata_key,
                               apr_pool_cleanup_null, s->process->pool);
        return OK;
    }

    sharedmem_initialize_cleanup(p);
    return OK;
}

/* XXX: The global pool is clean up upon graceful restart,
 * that is to allow the resize the shared memory area using a graceful start
 * No sure that is a very good idea...
 */
static int pre_config(apr_pool_t *p, apr_pool_t *plog,
                             apr_pool_t *ptemp)
{
    apr_pool_t *global_pool;
    apr_status_t rv;

    rv = apr_pool_create(&global_pool, NULL);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_CRIT, rv, NULL,
                     "Fatal error: unable to create global pool for shared slotmem");
        return rv;
    }
    mem_getstorage(global_pool, "");
    return OK;
}

/*
 * Create the mutex of the insert/remove logic
 */
static void child_init(apr_pool_t *p, server_rec *s)
{
    sharedmem_initialize_child(p);
}

static void ap_sharedmem_register_hook(apr_pool_t *p)
{
    const slotmem_storage_method *storage = mem_getstorage(NULL, "");
    ap_register_provider(p, SLOTMEM_STORAGE, "shared", "0", storage);
    ap_hook_post_config(initialize_cleanup, NULL, NULL, APR_HOOK_LAST);
    ap_hook_pre_config(pre_config, NULL, NULL, APR_HOOK_MIDDLE);

    ap_hook_child_init(child_init, NULL, NULL, APR_HOOK_FIRST);
}

module AP_MODULE_DECLARE_DATA cluster_slotmem_module = {
    STANDARD20_MODULE_STUFF,
    NULL,       /* create per-directory config structure */
    NULL,       /* merge per-directory config structures */
    NULL,       /* create per-server config structure */
    NULL,       /* merge per-server config structures */
    NULL,       /* command apr_table_t */
    ap_sharedmem_register_hook /* register hooks */
};
