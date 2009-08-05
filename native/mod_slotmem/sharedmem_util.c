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

/* Memory handler for a shared memory divided in slot.
 * This one uses shared memory.
 */

#include "apr.h"
#include "apr_file_io.h"
#include "apr_strings.h"
#include "apr_pools.h"
#include "apr_shm.h"

#include "slotmem.h"

#include "httpd.h"
#ifdef AP_NEED_SET_MUTEX_PERMS
#include "unixd.h"
#endif

#if APR_HAVE_UNISTD_H
#include <unistd.h>         /* for getpid() */
#endif

#if HAVE_SYS_SEM_H
#include <sys/shm.h>
#if !defined(SHM_R)
#define SHM_R 0400
#endif
#if !defined(SHM_W)
#define SHM_W 0200
#endif
#endif

/* The description of the slots to reuse the slotmem */
struct sharedslotdesc {
    apr_size_t item_size;
    int item_num;
};

struct ap_slotmem {
    char *name;
    apr_shm_t *shm;
    int *ident; /* integer table to process a fast alloc/free */
    void *base;
    apr_size_t size;
    int num;
    apr_pool_t *globalpool;
    apr_file_t *global_lock; /* file used for the locks */
    struct ap_slotmem *next;
};

/* global pool and list of slotmem we are handling */
static struct ap_slotmem *globallistmem = NULL;
static apr_pool_t *globalpool = NULL;
static apr_thread_mutex_t *globalmutex_lock = NULL;

apr_status_t unixd_set_shm_perms(const char *fname)
{
#ifdef AP_NEED_SET_MUTEX_PERMS
#if APR_USE_SHMEM_SHMGET || APR_USE_SHMEM_SHMGET_ANON
    struct shmid_ds shmbuf;
    key_t shmkey;
    int shmid;

    shmkey = ftok(fname, 1);
    if (shmkey == (key_t)-1) {
        return errno;
    }
    if ((shmid = shmget(shmkey, 0, SHM_R | SHM_W)) == -1) {
        return errno;
    }
#if MODULE_MAGIC_NUMBER_MAJOR > 20081212
    shmbuf.shm_perm.uid  = ap_unixd_config.user_id;
    shmbuf.shm_perm.gid  = ap_unixd_config.group_id;
#else
    shmbuf.shm_perm.uid  = unixd_config.user_id;
    shmbuf.shm_perm.gid  = unixd_config.group_id;
#endif
    shmbuf.shm_perm.mode = 0600;
    if (shmctl(shmid, IPC_SET, &shmbuf) == -1) {
        return errno;
    }
    return APR_SUCCESS;
#else
    return APR_ENOTIMPL;
#endif
#else
    return APR_ENOTIMPL;
#endif
}

/*
 * Persiste the slotmem in a file
 * slotmem name and file name.
 * for example use:
 * anonymous : $server_root/logs/anonymous.slotmem
 * :module.c : $server_root/logs/module.c.slotmem
 * abs_name  : $abs_name.slotmem
 *
 */
static const char *store_filename(apr_pool_t *pool, const char *slotmemname)
{
    const char *storename;
    storename = apr_pstrcat(pool, slotmemname , ".slotmem", NULL); 
    return storename;
}
static void store_slotmem(ap_slotmem_t *slotmem)
{
    apr_file_t *fp;
    apr_status_t rv;
    apr_size_t nbytes;
    const char *storename;

    storename = store_filename(slotmem->globalpool, slotmem->name);

    rv = apr_file_open(&fp, storename,  APR_CREATE | APR_READ | APR_WRITE, APR_OS_DEFAULT, slotmem->globalpool);
    if (APR_STATUS_IS_EEXIST(rv)) {
        apr_file_remove(storename, slotmem->globalpool);
        rv = apr_file_open(&fp, storename,  APR_CREATE | APR_READ | APR_WRITE, APR_OS_DEFAULT, slotmem->globalpool);
    }
    if (rv != APR_SUCCESS) {
        return;
    }
    nbytes = slotmem->size * slotmem->num + sizeof(int) * (slotmem->num + 1);
    apr_file_write(fp, slotmem->ident, &nbytes);
    apr_file_close(fp);
}
void restore_slotmem(void *ptr, const char *name, apr_size_t item_size, int item_num, apr_pool_t *pool)
{
    const char *storename;
    apr_file_t *fp;
    apr_size_t nbytes = item_size * item_num + sizeof(int) * (item_num + 1);
    apr_status_t rv;

    storename = store_filename(pool, name);
    rv = apr_file_open(&fp, storename,  APR_READ | APR_WRITE, APR_OS_DEFAULT, pool);
    if (rv == APR_SUCCESS) {
        apr_finfo_t fi;
        if (apr_file_info_get(&fi, APR_FINFO_SIZE, fp) == APR_SUCCESS) {
            if (fi.size == nbytes) {
                apr_file_read(fp, ptr, &nbytes);
            }
            else {
                apr_file_close(fp);
                apr_file_remove(storename, pool);
                return;
            }
        }
        apr_file_close(fp);
    }
}

apr_status_t cleanup_slotmem(void *param)
{
    ap_slotmem_t **mem = param;

    if (*mem) {
        ap_slotmem_t *next = *mem;
        while (next) {
            store_slotmem(next);
            apr_shm_destroy(next->shm);
            /* XXX: remove the lock file ? */
            next = next->next;
        }
    }
    return APR_SUCCESS;
}

static apr_status_t ap_slotmem_do(ap_slotmem_t *mem, ap_slotmem_callback_fn_t *func, void *data, apr_pool_t *pool)
{
    int i, j, isfree, *ident;
    char *ptr;
    apr_status_t rv;

    if (!mem) {
        return APR_ENOSHMAVAIL;
    }

    /* performs the func only on allocated slots! */
    ptr = mem->base;
    for (i = 1; i < mem->num+1; i++) {
        ident = mem->ident;
        isfree = 0;
        for (j=0; j<mem->num+1; j++) {
            if (ident[j] == i) {
                isfree = 1;
                break;
            }
        }
        if (!isfree) {
            rv = func((void *)ptr, data, i, pool);
            if (rv == APR_SUCCESS)
                return(rv);
        }
        ptr = ptr + mem->size;
    }
    return APR_NOTFOUND;
}
static apr_status_t ap_slotmem_create(ap_slotmem_t **new, const char *name, apr_size_t item_size, int item_num, int persist, apr_pool_t *pool)
{
    char *ptr;
    struct sharedslotdesc desc;
    ap_slotmem_t *res;
    ap_slotmem_t *next = globallistmem;
    apr_status_t rv;
    const char *fname;
    const char *filename;
    apr_size_t nbytes = item_size * item_num + sizeof(int) * (item_num + 1) + sizeof(struct sharedslotdesc);
    int i, *ident;

    if (globalpool == NULL)
        return APR_ENOSHMAVAIL;
    if (name) {
        fname = name;

        /* first try to attach to existing slotmem */
        if (next) {
            for (;;) {
                if (strcmp(next->name, fname) == 0) {
                    /* we already have it */
                    *new = next;
                    return APR_SUCCESS;
                }
                if (!next->next) {
                    break;
                }
                next = next->next;
            }
        }
    }
    else {
        fname = "anonymous";
    }

    /* first try to attach to existing shared memory */
    res = (ap_slotmem_t *) apr_pcalloc(globalpool, sizeof(ap_slotmem_t));
    if (name) {
        rv = apr_shm_attach(&res->shm, fname, globalpool);
    }
    else {
        rv = APR_EINVAL;
    }
    if (rv == APR_SUCCESS) {
        /* check size */
        if (apr_shm_size_get(res->shm) != nbytes) {
            apr_shm_detach(res->shm);
            res->shm = NULL;
            return APR_EINVAL;
        }
        ptr = apr_shm_baseaddr_get(res->shm);
        memcpy(&desc, ptr, sizeof(desc));
        if (desc.item_size != item_size || desc.item_num != item_num) {
            apr_shm_detach(res->shm);
            res->shm = NULL;
            return APR_EINVAL;
        }
        ptr = ptr +  sizeof(desc);
    }
    else  {
        if (name) {
            apr_shm_remove(fname, globalpool);
            rv = apr_shm_create(&res->shm, nbytes, fname, globalpool);
        }
        else {
            rv = apr_shm_create(&res->shm, nbytes, NULL, globalpool);
        }
        if (rv != APR_SUCCESS) {
            return rv;
        }
        if (name) {
            /* Set permissions to shared memory
             * so it can be attached by child process
             * having different user credentials
             */
            unixd_set_shm_perms(fname);
        }
        ptr = apr_shm_baseaddr_get(res->shm);
        desc.item_size = item_size;
        desc.item_num = item_num;
        memcpy(ptr, &desc, sizeof(desc));
        ptr = ptr +  sizeof(desc);
        /* write the idents table */
        ident = (int *) ptr;
        for (i=0; i<item_num+1; i++) {
            ident[i] = i + 1;
        }
        /* clean the slots table */
        memset(ptr + sizeof(int) * (item_num + 1), 0, item_size * item_num);
        /* try to restore the _whole_ stuff from a persisted location */
        if (persist & CREPER_SLOTMEM)
            restore_slotmem(ptr, fname, item_size, item_num, pool);
    }

    /* create the lock */
    filename = apr_pstrcat(pool, fname , ".lock", NULL);
    rv = apr_file_open(&res->global_lock, filename, APR_WRITE|APR_CREATE, APR_OS_DEFAULT, globalpool);
    if (rv != APR_SUCCESS) {
        return rv;
    }

    /* For the chained slotmem stuff */
    res->name = apr_pstrdup(globalpool, fname);
    res->ident = (int *) ptr;
    res->base = ptr + sizeof(int) * (item_num + 1);
    res->size = item_size;
    res->num = item_num;
    res->globalpool = globalpool;
    res->next = NULL;
    if (globallistmem==NULL) {
        globallistmem = res;
    }
    else {
        next->next = res;
    }

    *new = res;
    return APR_SUCCESS;
}
static apr_status_t ap_slotmem_attach(ap_slotmem_t **new, const char *name, apr_size_t *item_size, int *item_num, apr_pool_t *pool)
{
    char *ptr;
    ap_slotmem_t *res;
    ap_slotmem_t *next = globallistmem;
    struct sharedslotdesc desc;
    const char *fname;
    const char *filename;
    apr_status_t rv;

    if (globalpool == NULL) {
        return APR_ENOSHMAVAIL;
    }
    if (name) {
        fname = name;
    }
    else {
        return APR_ENOSHMAVAIL;
    }

    /* first try to attach to existing slotmem */
    if (next) {
        for (;;) {
            if (strcmp(next->name, fname) == 0) {
                /* we already have it */
                *new = next;
                *item_size = next->size;
                *item_num = next->num;
                return APR_SUCCESS;
            }
            if (!next->next)
                break;
            next = next->next;
        }
    }

    /* first try to attach to existing shared memory */
    res = (ap_slotmem_t *) apr_pcalloc(globalpool, sizeof(ap_slotmem_t));
    rv = apr_shm_attach(&res->shm, fname, globalpool);
    if (rv != APR_SUCCESS) {
        return rv;
    }
    /* get the corresponding lock */
    filename = apr_pstrcat(pool, fname , ".lock", NULL);
    rv = apr_file_open(&res->global_lock, filename, APR_WRITE|APR_CREATE, APR_OS_DEFAULT, globalpool);
    if (rv != APR_SUCCESS) {
        return rv;
    }

    /* Read the description of the slotmem */
    ptr = apr_shm_baseaddr_get(res->shm);
    memcpy(&desc, ptr, sizeof(desc));
    ptr = ptr + sizeof(desc);

    /* For the chained slotmem stuff */
    res->name = apr_pstrdup(globalpool, fname);
    res->ident = (int *)ptr;
    res->base = ptr + sizeof(int) * (desc.item_num + 1);
    res->size = desc.item_size;
    res->num = desc.item_num;
    res->globalpool = globalpool;
    res->next = NULL;
    if (globallistmem==NULL) {
        globallistmem = res;
    }
    else {
        next->next = res;
    }

    *new = res;
    *item_size = desc.item_size;
    *item_num = desc.item_num;
    return APR_SUCCESS;
}
static apr_status_t ap_slotmem_mem(ap_slotmem_t *score, int id, void**mem)
{

    char *ptr;
    int i;
    int *ident;

    if (!score) {
        return APR_ENOSHMAVAIL;
    }
    if (id<0 || id>score->num) {
        return APR_ENOSHMAVAIL;
    }

    /* Check that it is not a free slot */
    ident = score->ident;
    for (i=0; i<score->num+1; i++) {
        if (ident[i] == id)
            return APR_NOTFOUND;
    } 

    ptr = (char *) score->base + score->size * (id - 1);
    if (!ptr) {
        return APR_ENOSHMAVAIL;
    }
    *mem = ptr;
    return APR_SUCCESS;
}

/* Lock the file lock (between processes) and then the mutex */
static apr_status_t ap_slotmem_lock(ap_slotmem_t *s)
{
    apr_status_t rv;
    rv = apr_file_lock(s->global_lock, APR_FLOCK_EXCLUSIVE);
    if (rv != APR_SUCCESS)
        return rv;
    rv = apr_thread_mutex_lock(globalmutex_lock);
    if (rv != APR_SUCCESS)
        apr_file_unlock(s->global_lock);
    return rv;
}
static apr_status_t ap_slotmem_unlock(ap_slotmem_t *s)
{
    apr_thread_mutex_unlock(globalmutex_lock);
    return(apr_file_unlock(s->global_lock));
}
static apr_status_t ap_slotmem_alloc(ap_slotmem_t *score, int *item_id, void**mem)
{
    int ff;
    int *ident;
    apr_status_t rv;
    ap_slotmem_lock(score);
    ident = score->ident;
    ff = ident[0];
    if (ff > score->num) {
        rv = APR_ENOMEM;
    } else {
        ident[0] = ident[ff];
        ident[ff] = 0;
        *item_id = ff;
        *mem = (char *) score->base + score->size * (ff - 1);
        rv = APR_SUCCESS;
    }
    
    ap_slotmem_unlock(score);
    return rv;
}
static apr_status_t ap_slotmem_free(ap_slotmem_t *score, int item_id, void*mem)
{
    int ff;
    int *ident;
    if (item_id > score->num || item_id <=0) {
        return APR_EINVAL;
    } else {
        ap_slotmem_lock(score);
        ident = score->ident;
        if (ident[item_id]) {
            ap_slotmem_unlock(score);
            return APR_SUCCESS;
        }
        ff = ident[0];
        ident[0] = item_id;
        ident[item_id] = ff;
        ap_slotmem_unlock(score);
        return APR_SUCCESS;
    }
}
static int ap_slotmem_get_used(ap_slotmem_t *score, int *ids)
{
    int i, ret = 0;
    int *ident;

    ident = score->ident;
    for (i=0; i<score->num+1; i++) {
        if (ident[i] == 0) {
            *ids = i;
            ids++;
            ret++;
        }
    }
    return ret;
}
static int ap_slotmem_get_max_size(ap_slotmem_t *score)
{
    return score->num;
}
static const slotmem_storage_method storage = {
    &ap_slotmem_do,
    &ap_slotmem_create,
    &ap_slotmem_attach,
    &ap_slotmem_mem,
    &ap_slotmem_alloc,
    &ap_slotmem_free,
    &ap_slotmem_get_used,
    &ap_slotmem_get_max_size
};

/* make the storage usuable from outside
 * and initialise the global pool */
const slotmem_storage_method *mem_getstorage(apr_pool_t *p, char *type)
{
    if (globalpool == NULL && p != NULL)
        globalpool = p;
    return(&storage);
}
/* Add the pool_clean routine */
void sharedmem_initialize_cleanup(apr_pool_t *p)
{
    apr_pool_cleanup_register(p, &globallistmem, cleanup_slotmem, apr_pool_cleanup_null);
}
/* Create the mutex for insert/remove logic */
apr_status_t sharedmem_initialize_child(apr_pool_t *p)
{
    return (apr_thread_mutex_create(&globalmutex_lock, APR_THREAD_MUTEX_DEFAULT, globalpool));
}
