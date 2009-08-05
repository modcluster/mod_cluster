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

#ifndef HOST_H
#define HOST_H

/**
 * @file  host.h
 * @brief host description Storage Module for Apache
 *
 * @defgroup MEM hosts
 * @ingroup  APACHE_MODS
 * @{
 */

#define HOSTEXE ".hosts"

#ifndef MEM_T
typedef struct mem mem_t; 
#define MEM_T
#endif

#include "mod_clustersize.h"

/* status of the host as read/store in httpd. */
struct hostinfo {
    char host[HOSTALIASZ]; /* Alias element of the virtual host */
    int vhost;             /* id of the correspond virtual host */
    int node;              /* id of the node containing the virtual host */

    apr_time_t updatetime; /* time of last received message */
    int id;           /* id in table */
};
typedef struct hostinfo hostinfo_t; 


/**
 * Insert(alloc) and update a host record in the shared table
 * @param pointer to the shared table.
 * @param host host to store in the shared table.
 * @return APR_SUCCESS if all went well
 *
 */
apr_status_t insert_update_host(mem_t *s, hostinfo_t *host);

/**
 * read a host record from the shared table
 * @param pointer to the shared table.
 * @param host host to read from the shared table.
 * @return address of the read host or NULL if error.
 */
hostinfo_t * read_host(mem_t *s, hostinfo_t *host);

/**
 * get a host record from the shared table
 * @param pointer to the shared table.
 * @param host address of the host read from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t get_host(mem_t *s, hostinfo_t **host, int ids);

/**
 * remove(free) a host record from the shared table
 * @param pointer to the shared table.
 * @param host host to remove from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t remove_host(mem_t *s, hostinfo_t *host);

/*
 * get the ids for the used (not free) hosts in the table
 * @param pointer to the shared table.
 * @param ids array of int to store the used id (must be big enough).
 * @return number of host existing or -1 if error.
 */
int get_ids_used_host(mem_t *s, int *ids);

/*
 * get the size of the table (max size).
 * @param pointer to the shared table.
 * @return size of the existing table or -1 if error.
 */
int get_max_size_host(mem_t *s);

/**
 * attach to the shared host table
 * @param name of an existing shared table.
 * @param address to store the size of the shared table.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * get_mem_host(char *string, int *num, apr_pool_t *p, slotmem_storage_method *storage);
/**
 * create a shared host table
 * @param name to use to create the table.
 * @param size of the shared table.
 * @param persist tell if the slotmem element are persistent.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * create_mem_host(char *string, int *num, int persist, apr_pool_t *p, slotmem_storage_method *storage);

/**
 * provider for the mod_proxy_cluster or mod_jk modules.
 */
struct host_storage_method {
/**
 * the host corresponding to the ident
 * @param ids ident of the host to read.
 * @param host address of pointer to return the host.
 * @return APR_SUCCESS if all went well
 */
apr_status_t (* read_host)(int ids, hostinfo_t **host);
/**
 * read the list of ident of used hosts.
 * @param ids address to store the idents.
 * @return APR_SUCCESS if all went well
 */
int (* get_ids_used_host)(int *ids);
/**
 * read the max number of hosts in the shared table
 */
int (*get_max_size_host)();
};
#endif /*HOST_H*/
