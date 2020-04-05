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

#ifndef BALANCER_H
#define BALANCER_H

/**
 * @file  balancer.h
 * @brief balancer description Storage Module for Apache
 *
 * @defgroup MEM balancers
 * @ingroup  APACHE_MODS
 * @{
 */

#define BALANCEREXE ".balancers"

#ifndef MEM_T
typedef struct mem mem_t; 
#define MEM_T
#endif

#include "mod_clustersize.h"

/* status of the balancer as read/store in httpd. */
struct balancerinfo {
    char balancer[BALANCERSZ]; /* Name of the balancer */
    int StickySession; /* 0 : Don't use, 1: Use it */
    char StickySessionCookie[COOKNAMESZ];
    char StickySessionPath[PATHNAMESZ];
    int StickySessionRemove; /* 0 : Don't remove, 1: Remove it */
    int StickySessionForce;  /* 0: Don't force, 1: return error */
    int Timeout;
    int	Maxattempts;

    apr_time_t updatetime; /* time of last received message */
    int id;           /* id in table */
};
typedef struct balancerinfo balancerinfo_t; 


/**
 * Insert(alloc) and update a balancer record in the shared table
 * @param pointer to the shared table.
 * @param balancer balancer to store in the shared table.
 * @return APR_SUCCESS if all went well
 *
 */
apr_status_t insert_update_balancer(mem_t *s, balancerinfo_t *balancer);

/**
 * read a balancer record from the shared table
 * @param pointer to the shared table.
 * @param balancer balancer to read from the shared table.
 * @return address of the read balancer or NULL if error.
 */
balancerinfo_t * read_balancer(mem_t *s, balancerinfo_t *balancer);

/**
 * get a balancer record from the shared table
 * @param pointer to the shared table.
 * @param balancer address of the balancer read from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t get_balancer(mem_t *s, balancerinfo_t **balancer, int ids);

/**
 * remove(free) a balancer record from the shared table
 * @param pointer to the shared table.
 * @param balancer balancer to remove from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t remove_balancer(mem_t *s, balancerinfo_t *balancer);

/*
 * get the ids for the used (not free) balancers in the table
 * @param pointer to the shared table.
 * @param ids array of int to store the used id (must be big enough).
 * @return number of balancer existing or -1 if error.
 */
int get_ids_used_balancer(mem_t *s, int *ids);

/*
 * get the size of the table (max size).
 * @param pointer to the shared table.
 * @return size of the existing table or -1 if error.
 */
int get_max_size_balancer(mem_t *s);

/**
 * attach to the shared balancer table
 * @param name of an existing shared table.
 * @param address to store the size of the shared table.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * get_mem_balancer(char *string, int *num, apr_pool_t *p, slotmem_storage_method *storage);
/**
 * create a shared balancer table
 * @param name to use to create the table.
 * @param size of the shared table.
 * @param persist tell if the slotmem element are persistent.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * create_mem_balancer(char *string, int *num, int persist, apr_pool_t *p, slotmem_storage_method *storage);

/**
 * provider for the mod_proxy_cluster or mod_jk modules.
 */
struct balancer_storage_method {
/**
 * the balancer corresponding to the ident
 * @param ids ident of the balancer to read.
 * @param balancer address of pointer to return the balancer.
 * @return APR_SUCCESS if all went well
 */
apr_status_t (* read_balancer)(int ids, balancerinfo_t **balancer);
/**
 * read the list of ident of used balancers.
 * @param ids address to store the idents.
 * @return APR_SUCCESS if all went well
 */
int (* get_ids_used_balancer)(int *ids);
/**
 * read the max number of balancers in the shared table
 */
int (*get_max_size_balancer)(void);
};
#endif /*BALANCER_H*/
