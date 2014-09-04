/*
 *  mod_cluster
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

/**
 * @file  host.c
 * @brief host description Storage Module for Apache
 *
 * @defgroup MEM hosts
 * @ingroup  APACHE_MODS
 * @{
 */

#include "apr.h"
#include "apr_strings.h"
#include "apr_pools.h"
#include "apr_time.h"

#include "slotmem.h"
#include "host.h"

#include "mod_manager.h"

static mem_t * create_attach_mem_host(char *string, int *num, int type, apr_pool_t *p, slotmem_storage_method *storage) {
    mem_t *ptr;
    const char *storename;
    apr_status_t rv;

    ptr = apr_pcalloc(p, sizeof(mem_t));
    if (!ptr) {
        return NULL;
    }
    ptr->storage =  storage;
    storename = apr_pstrcat(p, string, HOSTEXE, NULL); 
    if (type)
        rv = ptr->storage->ap_slotmem_create(&ptr->slotmem, storename, sizeof(hostinfo_t), *num, type, p);
    else {
        apr_size_t size = sizeof(hostinfo_t);
        rv = ptr->storage->ap_slotmem_attach(&ptr->slotmem, storename, &size, num, p);
    }
    if (rv != APR_SUCCESS) {
        return NULL;
    }
    ptr->num = *num;
    ptr->p = p;
    return ptr;
}
/**
 * Insert(alloc) and update a host record in the shared table
 * @param pointer to the shared table.
 * @param host host to store in the shared table.
 * @return APR_SUCCESS if all went well
 *
 */
static apr_status_t insert_update(void* mem, void **data, int id, apr_pool_t *pool)
{
    hostinfo_t *in = (hostinfo_t *)*data;
    hostinfo_t *ou = (hostinfo_t *)mem;
    if (strcmp(in->host, ou->host) == 0 && in->vhost == ou->vhost && in->node == ou->node) {
        memcpy(ou, in, sizeof(hostinfo_t));
        ou->id = id;
        ou->updatetime = apr_time_sec(apr_time_now());
        *data = ou;
        return APR_SUCCESS;
    }
    return APR_NOTFOUND;
}
apr_status_t insert_update_host(mem_t *s, hostinfo_t *host)
{
    apr_status_t rv;
    hostinfo_t *ou;
    int ident;

    host->id = 0;
    s->storage->ap_slotmem_lock(s->slotmem);
    rv = s->storage->ap_slotmem_do(s->slotmem, insert_update, &host, 1, s->p);
    if (host->id != 0 && rv == APR_SUCCESS) {
        s->storage->ap_slotmem_unlock(s->slotmem);
        return APR_SUCCESS; /* updated */
    }

    /* we have to insert it */
    rv = s->storage->ap_slotmem_alloc(s->slotmem, &ident, (void **) &ou);
    if (rv != APR_SUCCESS) {
        s->storage->ap_slotmem_unlock(s->slotmem);
        return rv;
    }
    memcpy(ou, host, sizeof(hostinfo_t));
    ou->id = ident;
    s->storage->ap_slotmem_unlock(s->slotmem);
    ou->updatetime = apr_time_sec(apr_time_now());

    return APR_SUCCESS;
}

/**
 * read a host record from the shared table
 * @param pointer to the shared table.
 * @param host host to read from the shared table.
 * @return address of the read host or NULL if error.
 */
static apr_status_t loc_read_host(void* mem, void **data, int id, apr_pool_t *pool) {
    hostinfo_t *in = (hostinfo_t *)*data;
    hostinfo_t *ou = (hostinfo_t *)mem;

    if (strcmp(in->host, ou->host) == 0 && in->node == ou->node ) {
        *data = ou;
        return APR_SUCCESS;
    }
    return APR_NOTFOUND;
}
hostinfo_t * read_host(mem_t *s, hostinfo_t *host)
{
    apr_status_t rv;
    hostinfo_t *ou = host;

    if (host->id)
        rv = s->storage->ap_slotmem_mem(s->slotmem, host->id, (void **) &ou);
    else {
        rv = s->storage->ap_slotmem_do(s->slotmem, loc_read_host, &ou, 0, s->p);
    }
    if (rv == APR_SUCCESS)
        return ou;
    return NULL;
}
/**
 * get a host record from the shared table
 * @param pointer to the shared table.
 * @param host address where the host is locate in the shared table.
 * @param ids  in the host table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t get_host(mem_t *s, hostinfo_t **host, int ids)
{
  return(s->storage->ap_slotmem_mem(s->slotmem, ids, (void **) host));
}

/**
 * remove(free) a host record from the shared table
 * @param pointer to the shared table.
 * @param host host to remove from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t remove_host(mem_t *s, hostinfo_t *host)
{
    apr_status_t rv;
    hostinfo_t *ou = host;
    if (host->id) {
        rv = s->storage->ap_slotmem_free(s->slotmem, host->id, host);
    } else {
        /* XXX: for the moment January 2007 ap_slotmem_free only uses ident to remove */
        rv = s->storage->ap_slotmem_do(s->slotmem, loc_read_host, &ou, 0, s->p);
        if (rv == APR_SUCCESS)
            rv = s->storage->ap_slotmem_free(s->slotmem, ou->id, host);
    }
    return rv;
}

/*
 * get the ids for the used (not free) hosts in the table
 * @param pointer to the shared table.
 * @param ids array of int to store the used id (must be big enough).
 * @return number of host existing or -1 if error.
 */
int get_ids_used_host(mem_t *s, int *ids)
{
    return (s->storage->ap_slotmem_get_used(s->slotmem, ids));
}

/*
 * read the size of the table.
 * @param pointer to the shared table.
 * @return number of host existing or -1 if error.
 */
int get_max_size_host(mem_t *s)
{
    return (s->storage->ap_slotmem_get_max_size(s->slotmem));
}

/**
 * attach to the shared host table
 * @param name of an existing shared table.
 * @param address to store the size of the shared table.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * get_mem_host(char *string, int *num, apr_pool_t *p, slotmem_storage_method *storage)
{
    return(create_attach_mem_host(string, num, 0, p, storage));
}
/**
 * create a shared host table
 * @param name to use to create the table.
 * @param size of the shared table.
 * @param persist tell if the slotmem element are persistent.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * create_mem_host(char *string, int *num, int persist, apr_pool_t *p, slotmem_storage_method *storage)
{
    return(create_attach_mem_host(string, num, CREATE_SLOTMEM|persist, p, storage));
}
