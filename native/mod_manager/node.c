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

/**
 * @file  node.c
 * @brief node description Storage Module for Apache
 *
 * @defgroup MEM nodes
 * @ingroup  APACHE_MODS
 * @{
 */

#include "apr.h"
#include "apr_strings.h"
#include "apr_pools.h"
#include "apr_time.h"

#include "slotmem.h"
#include "node.h"

#include "mod_manager.h"

static mem_t * create_attach_mem_node(char *string, int *num, int type, apr_pool_t *p, slotmem_storage_method *storage) {
    mem_t *ptr;
    const char *storename;
    apr_status_t rv;

    ptr = apr_pcalloc(p, sizeof(mem_t));
    if (!ptr) {
        return NULL;
    }
    ptr->storage =  storage;
    storename = apr_pstrcat(p, string, NODEEXE, NULL); 
    if (type) {
        rv = ptr->storage->ap_slotmem_create(&ptr->slotmem, storename, sizeof(nodeinfo_t), *num, type, p);
    } else {
        apr_size_t size = sizeof(nodeinfo_t);
        rv = ptr->storage->ap_slotmem_attach(&ptr->slotmem, storename, &size, num, p);
    }
    if (rv != APR_SUCCESS) {
        ptr->laststatus = rv;
        return ptr;
    }
    ptr->laststatus = APR_SUCCESS;
    ptr->num = *num;
    ptr->p = p;
    return ptr;
}

/**
 * return the last stored in the mem structure
 * @param pointer to the shared table
 * @return APR_SUCCESS if all went well
 *
 */
apr_status_t get_last_mem_error(mem_t *mem) {
    return mem->laststatus;
}


/**
 * Insert(alloc) and update a node record in the shared table
 * @param pointer to the shared table.
 * @param node node to store in the shared table.
 * @return APR_SUCCESS if all went well
 *
 */
static apr_status_t insert_update(void* mem, void **data, int id, apr_pool_t *pool)
{
    nodeinfo_t *in = (nodeinfo_t *)*data;
    nodeinfo_t *ou = (nodeinfo_t *)mem;
    if (strcmp(in->mess.JVMRoute, ou->mess.JVMRoute) == 0) {
        apr_size_t vsize = sizeof(void *);
        /*
         * The node information is made of several pieces:
         * Information from the cluster (nodemess_t).
         * updatetime (time of last received message).
         * offset (of the area shared with the proxy logic).
         * stat (shared area with the proxy logic we shouldn't modify it here).
         */
        memcpy(ou, in, sizeof(nodemess_t));
        ou->mess.id = id;
        ou->updatetime = apr_time_now();
        ou->offset = sizeof(nodemess_t) + sizeof(apr_time_t) + sizeof(int);
        ou->offset = ou->offset % vsize ? (((ou->offset / vsize) +1 ) * vsize) : ou->offset;
        *data = ou;
        return APR_SUCCESS;
    }
    return APR_NOTFOUND;
}
apr_status_t insert_update_node(mem_t *s, nodeinfo_t *node, int *id)
{
    apr_status_t rv;
    nodeinfo_t *ou;
    int ident;
    apr_size_t vsize = sizeof(void *);

    node->mess.id = 0;
    rv = s->storage->ap_slotmem_do(s->slotmem, insert_update, &node, s->p);
    if (node->mess.id != 0 && rv == APR_SUCCESS) {
        *id = node->mess.id;
        return APR_SUCCESS; /* updated */
    }

    /* we have to insert it */
    rv = s->storage->ap_slotmem_alloc(s->slotmem, &ident, (void **) &ou);
    if (rv != APR_SUCCESS) {
        return rv;
    }
    memcpy(ou, node, sizeof(nodeinfo_t));
    ou->mess.id = ident;
    *id = ident;
    ou->updatetime = apr_time_now();

    /* set of offset to the proxy_worker_stat */
    ou->offset = sizeof(nodemess_t) + sizeof(apr_time_t) + sizeof(int);
    ou->offset = ou->offset % vsize ? (((ou->offset / vsize) +1 ) * vsize) : ou->offset;

    /* blank the proxy status information */
    memset(&(ou->stat), '\0', SIZEOFSCORE);

    return APR_SUCCESS;
}

/**
 * read a node record from the shared table
 * @param pointer to the shared table.
 * @param node node to read from the shared table.
 * @return address of the read node or NULL if error.
 */
static apr_status_t loc_read_node(void* mem, void **data, int id, apr_pool_t *pool) {
    nodeinfo_t *in = (nodeinfo_t *)*data;
    nodeinfo_t *ou = (nodeinfo_t *)mem;
    if (strcmp(in->mess.JVMRoute, ou->mess.JVMRoute) == 0) {
        *data = ou;
        return APR_SUCCESS;
    }
    return APR_NOTFOUND;
}
nodeinfo_t * read_node(mem_t *s, nodeinfo_t *node)
{
    apr_status_t rv;
    nodeinfo_t *ou = node;

    if (node->mess.id)
        rv = s->storage->ap_slotmem_mem(s->slotmem, node->mess.id, (void **) &ou);
    else {
        rv = s->storage->ap_slotmem_do(s->slotmem, loc_read_node, &ou, s->p);
    }
    if (rv == APR_SUCCESS)
        return ou;
    return NULL;
}
/**
 * get a node record from the shared table (using ids).
 * @param pointer to the shared table.
 * @param node address where the node is located in the shared table.
 * @param ids  in the node table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t get_node(mem_t *s, nodeinfo_t **node, int ids)
{
  return(s->storage->ap_slotmem_mem(s->slotmem, ids, (void **) node));
}

/**
 * remove(free) a node record from the shared table
 * @param pointer to the shared table.
 * @param node node to remove from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t remove_node(mem_t *s, nodeinfo_t *node)
{
    apr_status_t rv;
    nodeinfo_t *ou = node;
    if (node->mess.id)
        s->storage->ap_slotmem_free(s->slotmem, node->mess.id, node);
    else {
        /* XXX: for the moment January 2007 ap_slotmem_free only uses ident to remove */
        rv = s->storage->ap_slotmem_do(s->slotmem, loc_read_node, &ou, s->p);
        if (rv == APR_SUCCESS)
            rv = s->storage->ap_slotmem_free(s->slotmem, ou->mess.id, node);
    }
    return rv;
}

/**
 * find a node record from the shared table using JVMRoute
 * @param pointer to the shared table.
 * @param node address where the node is located in the shared table.
 * @param route JVMRoute to search
 * @return APR_SUCCESS if all went well
 */
apr_status_t find_node(mem_t *s, nodeinfo_t **node, const char *route)
{
    nodeinfo_t ou;
    apr_status_t rv;

    strcpy(ou.mess.JVMRoute, route);
    *node = &ou;
    rv = s->storage->ap_slotmem_do(s->slotmem, loc_read_node, node, s->p);
    return rv;
}

/*
 * get the ids for the used (not free) nodes in the table
 * @param pointer to the shared table.
 * @param ids array of int to store the used id (must be big enough).
 * @return number of node existing or -1 if error.
 */
int get_ids_used_node(mem_t *s, int *ids)
{
    return (s->storage->ap_slotmem_get_used(s->slotmem, ids));
}

/*
 * read the size of the table.
 * @param pointer to the shared table.
 * @return number of node existing or -1 if error.
 */
int get_max_size_node(mem_t *s)
{
    return (s->storage->ap_slotmem_get_max_size(s->slotmem));
}

/**
 * attach to the shared node table
 * @param name of an existing shared table.
 * @param address to store the size of the shared table.
 * @param p pool to use for allocations.
 * @param storage slotmem logic provider.
 * @return address of struct used to access the table.
 */
mem_t * get_mem_node(char *string, int *num, apr_pool_t *p, slotmem_storage_method *storage)
{
    return(create_attach_mem_node(string, num, 0, p, storage));
}
/**
 * create a shared node table
 * @param name to use to create the table.
 * @param size of the shared table.
 * @param persist tell if the slotmem element are persistent.
 * @param p pool to use for allocations.
 * @param storage slotmem logic provider.
 * @return address of struct used to access the table.
 */
mem_t * create_mem_node(char *string, int *num, int persist, apr_pool_t *p, slotmem_storage_method *storage)
{
    return(create_attach_mem_node(string, num, CREATE_SLOTMEM|persist, p, storage));
}
