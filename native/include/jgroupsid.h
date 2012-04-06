/*
 *  mod_cluster
 *
 *  Copyright(c) 2012 Red Hat Middleware, LLC,
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

#ifndef JGROUPSID_H
#define JGROUPSID_H

/**
 * @file  jgroupsid.h
 * @brief jgroupsid description Storage Module for Apache
 *
 * @defgroup MEM jgroupsid
 * @ingroup  APACHE_MODS
 * @{
 */

#define JGROUPSIDEXE ".jgroupsids"

#ifndef MEM_T
typedef struct mem mem_t; 
#define MEM_T
#endif

#include "mod_clustersize.h"

/* jgroupsid information of the node received from jboss cluster. */
struct jgroupsidinfo {
    char jgroupsid[JGROUPSIDSZ];        /* jgroupuuid */
    char data[JGROUPSDATASZ];           /* jgroupdata */

    apr_time_t updatetime;    /* time of last received message */
    int id;                      /* id in table */
};
typedef struct jgroupsidinfo jgroupsidinfo_t; 

/**
 * Insert(alloc) and update a jgroups record in the shared table
 * @param pointer to the shared table.
 * @param jgroupsid jgroupsid to store in the shared table.
 * @return APR_SUCCESS if all went well
 *
 */
apr_status_t insert_update_jgroupsid(mem_t *s, jgroupsidinfo_t *jgroupsid);

/**
 * read a jgroupsid record from the shared table
 * @param pointer to the shared table.
 * @param jgroupsid jgroupsid to read from the shared table.
 * @return address of the read jgroupsid or NULL if error.
 */
jgroupsidinfo_t * read_jgroupsid(mem_t *s, jgroupsidinfo_t *jgroupsid);

/**
 * get a jgroups record from the shared table
 * @param pointer to the shared table.
 * @param jgroupsid address of the jgroupsid read from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t get_jgroupsid(mem_t *s, jgroupsidinfo_t **jgroupsid, int ids);

/**
 * remove(free) a jgroups record from the shared table
 * @param pointer to the shared table.
 * @param jgroupsid jgroupsid to remove from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t remove_jgroupsid(mem_t *s, jgroupsidinfo_t *jgroupsid);

/**
 * find a jgroups record from the shared table using JVMRoute
 * @param pointer to the shared table.
 * @param jgroupsid address where the jgroupsid is located in the shared table.
 * @param route JVMRoute to search
 * @return APR_SUCCESS if all went well
 */
apr_status_t find_jgroupsid(mem_t *s, jgroupsidinfo_t **jgroupsid, const char *route);

/*
 * get the ids for the used (not free) jgroups records in the table
 * @param pointer to the shared table.
 * @param ids array of int to store the used id (must be big enough).
 * @return number of jgroupsid existing or -1 if error.
 */
int get_ids_used_jgroupsid(mem_t *s, int *ids);

/*
 * get the size of the table (max size).
 * @param pointer to the shared table.
 * @return size of the existing table or -1 if error.
 */
int get_max_size_jgroupsid(mem_t *s);

/**
 * attach to the shared jgroupsid table
 * @param name of an existing shared table.
 * @param address to store the size of the shared table.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * get_mem_jgroupsid(char *string, int *num, apr_pool_t *p, slotmem_storage_method *storage);
/**
 * create a shared jgroupsid table
 * @param name to use to create the table.
 * @param size of the shared table.
 * @param persist tell if the slotmem element are persistent.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * create_mem_jgroupsid(char *string, int *num, int persist, apr_pool_t *p, slotmem_storage_method *storage);

/**
 * provider for the mod_proxy_cluster or mod_jk modules.
 */
struct jgroupsid_storage_method {
/**
 * the jgroupsid corresponding to the ident
 * @param ids ident of the jgroupsid to read.
 * @param jgroupsid address of pointer to return the jgroupsid.
 * @return APR_SUCCESS if all went well
 */
apr_status_t (* read_jgroupsid)(int ids, jgroupsidinfo_t **jgroupsid);
/**
 * read the list of ident of used jgroupsid records.
 * @param ids address to store the idents.
 * @return APR_SUCCESS if all went well
 */
int (* get_ids_used_jgroupsid)(int *ids);
/**
 * read the max number of jgroupsid records in the shared table
 */
int (*get_max_size_jgroupsid)();
/*
 * Remove the jgroupsid from shared memory (free the slotmem)
 */
int (*remove_jgroupsid)(jgroupsidinfo_t *jgroupsid);
/*
 * Find the jgroupsid using the JVMRoute information
 */
apr_status_t (*find_jgroupsid)(jgroupsidinfo_t **jgroupsid, const char *route);
};
#endif /*JGROUPSID_H*/
