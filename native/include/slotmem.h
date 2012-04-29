/*
 *  ALOHA - Apache Httpd Native Java Library
 *
 *  Copyright(c) 2006 Red Hat Middleware, LLC,
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

#ifndef MC_SLOTMEM_H
#define MC_SLOTMEM_H

/* Memory handler for a shared memory divided in slot.
 */
/**
 * @file  slotmem.h
 * @brief Memory Slot Extension Storage Module for Apache
 *
 * @defgroup MEM mem
 * @ingroup  APACHE_MODS
 * @{
 */

#include "apr.h"
#include "apr_strings.h"
#include "apr_pools.h"
#include "apr_shm.h"

#define SLOTMEM_STORAGE "slotmem"

#define ATTACH_SLOTMEM 0 /* Attach to existing slotmem */
#define CREATE_SLOTMEM 1 /* create a not persistent slotmem */
#define CREPER_SLOTMEM 2 /* create a persisitent slotmem */

typedef struct ap_slotmem ap_slotmem_t; 

/**
 * callback function used for slotmem.
 * @param mem is the memory associated with a worker.
 * @param data is what is passed to slotmem.
 * @param int is id of the slotmem in the table.
 * @param pool is pool used to create scoreboard
 * @return APR_SUCCESS if all went well
 */
typedef apr_status_t mc_slotmem_callback_fn_t(void* mem, void **data, int ident, apr_pool_t *pool);

struct slotmem_storage_method {
/**
 * call the callback on all worker slots
 * @param s ap_slotmem_t to use.
 * @param funct callback function to call for each element.
 * @param data parameter for the callback function.
 * @param pool is pool used to create scoreboard
 * @return APR_SUCCESS if all went well
 */
apr_status_t (* ap_slotmem_do)(ap_slotmem_t *s, mc_slotmem_callback_fn_t *func, void *data, apr_pool_t *pool);

/**
 * create a new slotmem with each item size is item_size.
 * This would create shared memory, basically.
 * @param pointer to store the address of the scoreboard.
 * @param name is a key used for debugging and in mod_status output or allow another process to share this space.
 * @param item_size size of each item
 * @param item_num number of item to create.
 * @param persistent indicator to know if the data should be persisted or not.
 * @param pool is pool used to create scoreboard
 * @return APR_SUCCESS if all went well
 */
apr_status_t (* ap_slotmem_create)(ap_slotmem_t **new, const char *name, apr_size_t item_size, int item_num, int persistent, apr_pool_t *pool);

/**
 * attach to an existing slotmem.
 * This would attach to  shared memory, basically.
 * @param pointer to store the address of the scoreboard.
 * @param name is a key used for debugging and in mod_status output or allow another process to share this space.
 * @param item_size size of each item
 * @param item_num max number of item.
 * @param pool is pool to memory allocate.
 * @return APR_SUCCESS if all went well
 */
apr_status_t (* ap_slotmem_attach)(ap_slotmem_t **new, const char *name, apr_size_t *item_size, int *item_num, apr_pool_t *pool);
/**
 * get the memory associated with this worker slot.
 * @param s ap_slotmem_t to use.
 * @param item_id item to return for 0 to item_num
 * @param mem address to store the pointer to the slot
 * @return APR_SUCCESS if all went well (the slot must be an allocated slot).
 */
apr_status_t (* ap_slotmem_mem)(ap_slotmem_t *s, int item_id, void**mem);
/**
 * alloc a slot from the slotmem free idems.
 * @param s ap_slotmem_t to use.
 * @param item_id address to return the id of the slot allocates.
 * @param mem address to store the pointer to the slot
 * @return APR_SUCCESS if all went well
 */
apr_status_t (* ap_slotmem_alloc)(ap_slotmem_t *s, int *item_id, void**mem); 
/**
 * free a slot (return it to the free list).
 * @param s ap_slotmem_t to use.
 * @param item_id the id of the slot in the slotmem.
 * @param mem pointer to the slot
 * @return APR_SUCCESS if all went well
 */
apr_status_t (* ap_slotmem_free)(ap_slotmem_t *s, int item_id, void*mem); 
/**
 * Return the used id in the slotmem table.
 * @param s ap_slotmem_t to use.
 * @param ids array of int where to store the free idents.
 * @return number of slotmem in use.
 */
int (*ap_slotmem_get_used)(ap_slotmem_t *s, int *ids);
/**
 * Return the size of the slotmem table.
 * @param s ap_slotmem_t to use.
 * @return number of slotmem that cant be stored in the slotmem table.
 */
int (*ap_slotmem_get_max_size)(ap_slotmem_t *s);
};

typedef struct slotmem_storage_method slotmem_storage_method;

/* Memory handler for a shared memory divided in slot.
 * This one uses shared memory.
 * @param pool is a global pool to allocate memory.
 * @param type name of the module provider of charged memory.
 * @return structure to the storage routines.
 */
const slotmem_storage_method *mem_getstorage(apr_pool_t *p, char *type);

void sharedmem_initialize_cleanup(apr_pool_t *p);

#endif /*MC_SLOTMEM_H*/
