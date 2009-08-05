/*
 *  mod_cluster
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
 * @author Jean-Frederic Clere
 * @version $Revision$
 */

#ifndef NODE_H
#define NODE_H

/**
 * @file  node.h
 * @brief node description Storage Module for Apache
 *
 * @defgroup MEM nodes
 * @ingroup  APACHE_MODS
 * @{
 */

#define NODEEXE ".nodes"

#ifndef MEM_T
typedef struct mem mem_t; 
#define MEM_T
#endif

#include "mod_clustersize.h"

/* configuration of the node received from jboss cluster. */
struct nodemess {
    char balancer[BALANCERSZ];        /* name of the balancer */
    char JVMRoute[JVMROUTESZ];
    char Domain[DOMAINNDSZ];
    char Host[HOSTNODESZ];
    char Port[PORTNODESZ];
    char Type[SCHEMENDSZ];
    int  reversed; /* 1 : reversed... 0 : normal */
    int  remove;   /* 1 : removed     0 : normal */

    /* node conf part */
    int flushpackets;
    int	flushwait;
    apr_time_t	ping;
    int	smax;
    apr_time_t ttl;
    apr_time_t timeout;

    /* part updated in httpd */
    int id;                   /* id in table and worker id */
    apr_time_t updatetimelb; /* time of last update of the lbstatus value */
    int oldelected;          /* value of s->elected when calculating the lbstatus */
    apr_time_t lastcleantry; /* time of last unsuccessful try to clean the worker in proxy part */
};
typedef struct nodemess nodemess_t; 

#define SIZEOFSCORE 200 /* size of the proxy_worker_stat structure */

/* status of the node as read/store in httpd. */
struct nodeinfo {
    /* config from jboss/tomcat */
    nodemess_t mess;
    /* filled by httpd */
    apr_time_t updatetime;   /* time of last received message */
    int offset;              /* offset to the proxy_worker_stat structure */
    char stat[SIZEOFSCORE];  /* to store the status */ 
};
typedef struct nodeinfo nodeinfo_t; 


/**
 * Insert(alloc) and update a node record in the shared table
 * @param pointer to the shared table.
 * @param node node to store in the shared table.
 * @return APR_SUCCESS if all went well
 *
 */
apr_status_t insert_update_node(mem_t *s, nodeinfo_t *node, int *id);

/**
 * read a node record from the shared table
 * @param pointer to the shared table.
 * @param node node to read from the shared table.
 * @return address of the read node or NULL if error.
 */
nodeinfo_t * read_node(mem_t *s, nodeinfo_t *node);

/**
 * get a node record from the shared table
 * @param pointer to the shared table.
 * @param node address of the node read from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t get_node(mem_t *s, nodeinfo_t **node, int ids);

/**
 * remove(free) a node record from the shared table
 * @param pointer to the shared table.
 * @param node node to remove from the shared table.
 * @return APR_SUCCESS if all went well
 */
apr_status_t remove_node(mem_t *s, nodeinfo_t *node);

/**
 * find a node record from the shared table using JVMRoute
 * @param pointer to the shared table.
 * @param node address where the node is located in the shared table.
 * @param route JVMRoute to search
 * @return APR_SUCCESS if all went well
 */
apr_status_t find_node(mem_t *s, nodeinfo_t **node, const char *route);

/*
 * get the ids for the used (not free) nodes in the table
 * @param pointer to the shared table.
 * @param ids array of int to store the used id (must be big enough).
 * @return number of node existing or -1 if error.
 */
int get_ids_used_node(mem_t *s, int *ids);

/*
 * get the size of the table (max size).
 * @param pointer to the shared table.
 * @return size of the existing table or -1 if error.
 */
int get_max_size_node(mem_t *s);

/**
 * attach to the shared node table
 * @param name of an existing shared table.
 * @param address to store the size of the shared table.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * get_mem_node(char *string, int *num, apr_pool_t *p, slotmem_storage_method *storage);
/**
 * create a shared node table
 * @param name to use to create the table.
 * @param size of the shared table.
 * @param persist tell if the slotmem element are persistent.
 * @param p pool to use for allocations.
 * @return address of struct used to access the table.
 */
mem_t * create_mem_node(char *string, int *num, int persist, apr_pool_t *p, slotmem_storage_method *storage);

/**
 * provider for the mod_proxy_cluster or mod_jk modules.
 */
struct node_storage_method {
/**
 * the node corresponding to the ident
 * @param ids ident of the node to read.
 * @param node address of pointer to return the node.
 * @return APR_SUCCESS if all went well
 */
apr_status_t (* read_node)(int ids, nodeinfo_t **node);
/**
 * read the list of ident of used nodes.
 * @param ids address to store the idents.
 * @return APR_SUCCESS if all went well
 */
int (* get_ids_used_node)(int *ids);
/**
 * read the max number of nodes in the shared table
 */
int (*get_max_size_node)();
/**
 * check the nodes for modifications.
 * XXX: void *data is server_rec *s in fact.
 */
apr_time_t (*worker_nodes_need_update)(void *data, apr_pool_t *pool);
/*
 * mark that the worker node are now up to date.
 */
int (*worker_nodes_are_updated)(void *data);
/*
 * Remove the node from shared memory (free the slotmem)
 */
int (*remove_node)(nodeinfo_t *node);
/*
 * Find the node using the JVMRoute information
 */
apr_status_t (*find_node)(nodeinfo_t **node, const char *route);
};
#endif /*NODE_H*/
