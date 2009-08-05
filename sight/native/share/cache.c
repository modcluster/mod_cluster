/*
 *  SIGHT - System information gathering hybrid tool
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
 * @author Mladen Turk
 *
 */

#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"

static unsigned int times33hash(const char *key)
{
    const char *p;
    unsigned int hash = 0;
    for (p = key; *p; p++) {
        hash = hash * 33 + *p;
    }
    return hash & CACHE_HASH_MASK;
}

cache_table_t *cache_new(size_t init)
{
    cache_table_t *t = (cache_table_t *)calloc(1, sizeof(cache_table_t));
    if (!t)
        return NULL;
    t->siz = 0;
    t->len = init;
    if (!(t->list = (cache_entry_t **)malloc(init * sizeof(cache_entry_t *)))) {
        int saved = errno;
        free(t);
        t = NULL;
        errno = saved;
    }
    return t;
}

cache_entry_t *cache_add(cache_table_t *t, const char *key)
{
    cache_entry_t *e;
    unsigned int hash;

    if (!key || !*key) {
        errno = EINVAL;
        return NULL;     /* Skip empty and null strings */
    }
    hash = times33hash(key);

    if (t->hash[hash]) {
       /*
        * This spot in the table is already in use.  See if the current string
        * has already been inserted, and if so, increment its count.
        */
        for (e = t->hash[hash]; e; e = e->next) {
            if (!strcmp(key, e->key))
                return e;
        }
    }
    if (!(e = (cache_entry_t *)malloc(sizeof(cache_entry_t ))))
        return NULL;
    e->data = NULL;
    /* Insert new bucket into the list */
    if (t->siz < t->len) {
        t->list[t->siz++] = e;
    }
    else {
        cache_entry_t **nl;
        size_t len = t->len << 2;
        if (!(nl = (cache_entry_t **)malloc(len * sizeof(cache_entry_t *))))
            return NULL;
        memcpy(nl, t->list, t->siz * sizeof(cache_entry_t *));
        free(t->list);
        t->len  = len;
        t->list = nl;
        t->list[t->siz++] = e;
    }
    if (!(e->key = strdup(key)))
        return NULL;
    e->next = t->hash[hash];
    t->hash[hash] = e;
    return e;
}

cache_entry_t *cache_find(cache_table_t *t, const char *key)
{
    cache_entry_t *e;
    unsigned int hash;

    if (!key || !*key) {
        return NULL;     /* Skip empty and null strings */
    }
    hash = times33hash(key);

    if (t->hash[hash]) {
       /*
        * This spot in the table is already in use.  See if the current string
        * has already been inserted, and if so, return existing entry.
        */
        for (e = t->hash[hash]; e; e = e->next) {
            if (!strcmp(key, e->key))
                return e;
        }
    }
    return NULL;
}

void cache_free(cache_table_t *t, void (*destroy)(const char *, void *))
{
    size_t i;

    if (!t)
        return;
    for (i = 0; i < t->siz; i++) {
        if (t->list[i]) {
            if (t->list[i]->data) {
                if (destroy)
                    (*destroy)(t->list[i]->key, t->list[i]->data);
                else
                    free(t->list[i]->data);
            }
            if (t->list[i]->key)
                free(t->list[i]->key);
        }
    }
    free(t->list);
    free(t);
}
