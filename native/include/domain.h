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

#ifndef DOMAIN_H
#define DOMAIN_H

/**
 * @file  domain.h
 * @brief domain description Storage Module for Apache
 *
 * @defgroup MEM domain
 * @ingroup  APACHE_MODS
 * @{
 */

#define DOMAINEXE ".domain"

#ifndef MEM_T
typedef struct mem mem_t; 
#define MEM_T
#endif

#include "mod_clustersize.h"

/* status of the domain as read/store in httpd. */
struct domaininfo {
    char domain[DOMAINNDSZ];     /* domain value */
    char JVMRoute[JVMROUTESZ];   /* corresponding node */
    char balancer[BALANCERSZ];   /* name of the balancer */

    apr_time_t updatetime;    /* time of last received message */
    int id;                      /* id in table */
};
typedef struct domaininfo domaininfo_t; 


/**
 * Insert(alloc) and update a domain record in the shared table
 * @param pointer to the shared table.
 * @param domain domain to store in the shared table.
 * @return APR_SUCCESS if all went well
 *
 */
apr_status_t insert_update_domain(mem_t *s, domaininfo_t *domain);

/**
 * read a domain record from the shared table
 * @param pointer to the shared table.
 * @param domain domain to read from the shared table.
 * @return address of the read domain or NULL if error.
 */
domaininfo_t * read_domain(mem_t *s, domaininfo_t *domain);

/**
 * get a domain record from the shared table
 * @param pointer to the shared table.
 * @param domain address of the domain read from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t get_domain(mem_t *s, domaininfo_t **domain, int ids);

/**
 * remove(free) a domain record from the shared table
 * @param pointer to the shared table.
 * @param domain domain to remove from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t remove_domain(mem_t *s, domaininfo_t *domain);

/**
 * find a domain record from the shared table using JVMRoute and balancer
 * @param pointer to the shared table.
 * @param domain address where the node is located in the shared table.
 * @param route JVMRoute to search
 * @return APR_SUCCESS if all went well
 */
apr_status_t find_domain(mem_t *s, domaininfo_t **domain, const char *route, const char *balancer);

/*
 * get the ids for the used (not free) domains in the table
 * @param pointer to the shared table.
 * @param ids array of int to store the used id (must be big enough).
 * @return number of domain existing or -1 if error.
 */
int get_ids_used_domain(mem_t *s, int *ids);

/*
 * get the size of the table (max size).
 * @param pointer to the shared table.
 * @return size of the existing table or -1 if error.
 */
int get_max_size_domain(mem_t *s);

/**
 * attach to the shared domain table
 * @param name of an existing shared table.
 * @param address to store the size of the shared table.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * get_mem_domain(char *string, int *num, apr_pool_t *p, slotmem_storage_method *storage);
/**
 * create a shared domain table
 * @param name to use to create the table.
 * @param size of the shared table.
 * @param persist tell if the slotmem element are persistent.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * create_mem_domain(char *string, int *num, int persist, apr_pool_t *p, slotmem_storage_method *storage);

/**
 * provider for the mod_proxy_cluster or mod_jk modules.
 */
struct domain_storage_method {
/**
 * the domain corresponding to the ident
 * @param ids ident of the domain to read.
 * @param domain address of pointer to return the domain.
 * @return APR_SUCCESS if all went well
 */
apr_status_t (* read_domain)(int ids, domaininfo_t **domain);
/**
 * read the list of ident of used domains.
 * @param ids address to store the idents.
 * @return APR_SUCCESS if all went well
 */
int (* get_ids_used_domain)(int *ids);
/**
 * read the max number of domains in the shared table
 */
int (*get_max_size_domain)();
/*
 * Remove the domain from shared memory (free the slotmem)
 */
apr_status_t (*remove_domain)(domaininfo_t *domain);
/*
 * Insert a new domain or update existing one.
 */
apr_status_t (*insert_update_domain)(domaininfo_t *domain);
/*
 * Find the domain using the JVMRoute and balancer information
 */
apr_status_t (*find_domain)(domaininfo_t **node, const char *route, const char *balancer);
};
#endif /*DOMAIN_H*/
