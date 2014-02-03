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

#ifndef CONTEXT_H
#define CONTEXT_H

/**
 * @file  context.h
 * @brief context description Storage Module for Apache
 *
 * @defgroup MEM contexts
 * @ingroup  APACHE_MODS
 * @{
 */

#define CONTEXTEXE ".contexts"

#ifndef MEM_T
typedef struct mem mem_t; 
#define MEM_T
#endif

/* Status of the application */
#define ENABLED  1
#define DISABLED 2
#define STOPPED  3
#define REMOVE   4 /* That status not stored but used by the logic to remove the entry */

#include "mod_clustersize.h"

/* status of the context as read/store in httpd. */
struct contextinfo {
    char context[CONTEXTSZ]; /* Context where the application is mapped. */
    int vhost;        /* id of the correspond virtual host in hosts table */
    int node;         /* id of the correspond node in nodes table */
    int status;       /* status: ENABLED/DISABLED/STOPPED */
    int nbrequests;   /* number of request been processed */

    apr_time_t updatetime; /* time of last received message */ 
    int id;           /* id in table */
};
typedef struct contextinfo contextinfo_t; 


/**
 * Insert(alloc) and update a context record in the shared table
 * @param pointer to the shared table.
 * @param context context to store in the shared table.
 * @return APR_SUCCESS if all went well
 *
 */
apr_status_t insert_update_context(mem_t *s, contextinfo_t *context);

/**
 * read a context record from the shared table
 * @param pointer to the shared table.
 * @param context context to read from the shared table.
 * @return address of the read context or NULL if error.
 */
contextinfo_t * read_context(mem_t *s, contextinfo_t *context);

/**
 * get a context record from the shared table
 * @param pointer to the shared table.
 * @param context address of the context read from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t get_context(mem_t *s, contextinfo_t **context, int ids);

/**
 * remove(free) a context record from the shared table
 * @param pointer to the shared table.
 * @param context context to remove from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t remove_context(mem_t *s, contextinfo_t *context);

/*
 * lock the context table
 * @param pointer to the shared table.
 */
void lock_contexts(mem_t *s);

/*
 * unlock the context table
 * @param pointer to the shared table.
 */
void unlock_contexts(mem_t *s);

/*
 * get the ids for the used (not free) contexts in the table
 * @param pointer to the shared table.
 * @param ids array of int to store the used id (must be big enough).
 * @return number of context existing or -1 if error.
 */
int get_ids_used_context(mem_t *s, int *ids);

/*
 * get the size of the table (max size).
 * @param pointer to the shared table.
 * @return size of the existing table or -1 if error.
 */
int get_max_size_context(mem_t *s);

/**
 * attach to the shared context table
 * @param name of an existing shared table.
 * @param address to store the size of the shared table.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * get_mem_context(char *string, int *num, apr_pool_t *p, slotmem_storage_method *storage);
/**
 * create a shared context table
 * @param name to use to create the table.
 * @param size of the shared table.
 * @param persist tell if the slotmem element are persistent.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * create_mem_context(char *string, int *num, int persist, apr_pool_t *p, slotmem_storage_method *storage);

/**
 * provider for the mod_proxy_cluster or mod_jk modules.
 */
struct context_storage_method {
/**
 * the context corresponding to the ident
 * @param ids ident of the context to read.
 * @param context address of pointer to return the context.
 * @return APR_SUCCESS if all went well
 */
apr_status_t (* read_context)(int ids, contextinfo_t **context);
/**
 * read the list of ident of used contexts.
 * @param ids address to store the idents.
 * @return APR_SUCCESS if all went well
 */
int (* get_ids_used_context)(int *ids);
/**
 * read the max number of contexts in the shared table
 */
int (*get_max_size_context)();
/*
 * lock the context table
 */
void (*lock_contexts)();
/*
 * unlock the context table
 */
void (*unlock_contexts)();

};
#endif /*CONTEXT_H*/
