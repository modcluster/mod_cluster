/*
 *  mod_cluster
 *
 *  Copyright(c) 2009 Red Hat Middleware, LLC,
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

#ifndef SESSIONID_H
#define SESSIONID_H

/**
 * @file  sessionid.h
 * @brief sessionid description Storage Module for Apache
 *
 * @defgroup MEM sessionids
 * @ingroup  APACHE_MODS
 * @{
 */

#define SESSIONIDEXE ".sessionid"

#ifndef MEM_T
typedef struct mem mem_t; 
#define MEM_T
#endif

#include "mod_clustersize.h"

/* status of the sessionid as read/store in httpd. */
struct sessionidinfo {
    char sessionid[SESSIONIDSZ]; /* Sessionid value */
    char JVMRoute[JVMROUTESZ];   /* corresponding node */

    apr_time_t updatetime;    /* time of last received message */
    int id;                      /* id in table */
};
typedef struct sessionidinfo sessionidinfo_t; 


/**
 * Insert(alloc) and update a sessionid record in the shared table
 * @param pointer to the shared table.
 * @param sessionid sessionid to store in the shared table.
 * @return APR_SUCCESS if all went well
 *
 */
apr_status_t insert_update_sessionid(mem_t *s, sessionidinfo_t *sessionid);

/**
 * read a sessionid record from the shared table
 * @param pointer to the shared table.
 * @param sessionid sessionid to read from the shared table.
 * @return address of the read sessionid or NULL if error.
 */
sessionidinfo_t * read_sessionid(mem_t *s, sessionidinfo_t *sessionid);

/**
 * get a sessionid record from the shared table
 * @param pointer to the shared table.
 * @param sessionid address of the sessionid read from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t get_sessionid(mem_t *s, sessionidinfo_t **sessionid, int ids);

/**
 * remove(free) a sessionid record from the shared table
 * @param pointer to the shared table.
 * @param sessionid sessionid to remove from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t remove_sessionid(mem_t *s, sessionidinfo_t *sessionid);

/*
 * get the ids for the used (not free) sessionids in the table
 * @param pointer to the shared table.
 * @param ids array of int to store the used id (must be big enough).
 * @return number of sessionid existing or -1 if error.
 */
int get_ids_used_sessionid(mem_t *s, int *ids);

/*
 * get the size of the table (max size).
 * @param pointer to the shared table.
 * @return size of the existing table or -1 if error.
 */
int get_max_size_sessionid(mem_t *s);

/**
 * attach to the shared sessionid table
 * @param name of an existing shared table.
 * @param address to store the size of the shared table.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * get_mem_sessionid(char *string, int *num, apr_pool_t *p, slotmem_storage_method *storage);
/**
 * create a shared sessionid table
 * @param name to use to create the table.
 * @param size of the shared table.
 * @param persist tell if the slotmem element are persistent.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * create_mem_sessionid(char *string, int *num, int persist, apr_pool_t *p, slotmem_storage_method *storage);

/**
 * provider for the mod_proxy_cluster or mod_jk modules.
 */
struct sessionid_storage_method {
/**
 * the sessionid corresponding to the ident
 * @param ids ident of the sessionid to read.
 * @param sessionid address of pointer to return the sessionid.
 * @return APR_SUCCESS if all went well
 */
apr_status_t (* read_sessionid)(int ids, sessionidinfo_t **sessionid);
/**
 * read the list of ident of used sessionids.
 * @param ids address to store the idents.
 * @return APR_SUCCESS if all went well
 */
int (* get_ids_used_sessionid)(int *ids);
/**
 * read the max number of sessionids in the shared table
 */
int (*get_max_size_sessionid)();
/*
 * Remove the sessionid from shared memory (free the slotmem)
 */
apr_status_t (*remove_sessionid)(sessionidinfo_t *sessionid);
/*
 * Insert a new sessionid or update existing one.
 */
apr_status_t (*insert_update_sessionid)(sessionidinfo_t *sessionid);
};
#endif /*SESSIONID_H*/
