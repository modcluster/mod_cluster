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

static const char *common_seps = " \t\r\n";

static apr_int64_t get_multiplier(char *trail)
{
    if (trail && *trail) {
        trail = sight_ltrim(trail);
        if (*trail) {
            if (apr_strnatcasecmp(trail, "KB") == 0)
                return SIGHT_KB;
            else if (apr_strnatcasecmp(trail, "MB") == 0)
                return SIGHT_MB;
            else if (apr_strnatcasecmp(trail, "GB") == 0)
                return SIGHT_GB;
        }
    }
    return 1;
}

jlong sight_strtoi64(const char *val)
{
    if (val) {
        char *ep = NULL;
        apr_int64_t v = (jlong)apr_strtoi64(val, &ep, 10);
        v = v * get_multiplier(ep);
        return (jlong)v;
    }
    else
        return 0;
}

jint sight_strtoi32(const char *val)
{
    if (val)
        return atoi(val);
    else
        return 0;
}

/* APR Hash table implementation */
char *sight_table_get_s(apr_table_t *table, const char *key)
{
    return (char *)apr_table_get(table, key);
}

sight_str_t sight_table_get_w(apr_table_t *table, const char *key)
{
    sight_str_t rv;
    rv.len = 0;
    rv.str.c = (char *)apr_table_get(table, key);
    return rv;
}

char *sight_table_get_sp(apr_table_t *table, int part, const char *key)
{
    int i = 0;
    char *val = (char *)apr_table_get(table, key);
    if (val) {
        do {
            while (*val && apr_isspace(*val))
                val++;
            if (part == i++)
                return val;
            while (*val && !apr_isspace(*val))
                val++;
        } while (*val);
    }
    return NULL;
}

jlong sight_table_get_jp(apr_table_t *table, int part, const char *key)
{
    int i = 0;
    char *val = (char *)apr_table_get(table, key);
    if (val) {
        char *ep = NULL;
        do {
            while (*val && apr_isspace(*val))
                val++;
            if (part == i++)
                return (jlong)apr_strtoi64(val, &ep, 10);
            while (*val && !apr_isspace(*val))
                val++;
        } while (*val);
    }
    return 0;
}

jlong sight_table_get_xp(apr_table_t *table, int part, const char *key)
{
    int i = 0;
    char *val = (char *)apr_table_get(table, key);
    if (val) {
        char *ep = NULL;
        do {
            while (*val && apr_isspace(*val))
                val++;
            if (part == i++)
                return (jlong)apr_strtoi64(val, &ep, 16);
            while (*val && !apr_isspace(*val))
                val++;
        } while (*val);
    }
    return 0;
}

jint sight_table_get_ip(apr_table_t *table, int part, const char *key)
{
    return (jint)sight_table_get_jp(table, part, key);
}

jlong sight_table_get_j(apr_table_t *table, const char *key)
{
    const char *val = apr_table_get(table, key);
    if (val) {
        char *ep = NULL;
        apr_int64_t v = (jlong)apr_strtoi64(val, &ep, 10);
        v = v * get_multiplier(ep);
        return (jlong)v;
    }
    else
        return 0;
}

jlong sight_table_get_x(apr_table_t *table, const char *key)
{
    const char *val = apr_table_get(table, key);
    if (val) {
        char *ep = NULL;
        apr_int64_t v = (jlong)apr_strtoi64(val, &ep, 16);
        v = v * get_multiplier(ep);
        return (jlong)v;
    }
    else
        return 0;
}

jint sight_table_get_i(apr_table_t *table, const char *key)
{
    return (jint)sight_table_get_j(table, key);
}

jboolean sight_table_get_z(apr_table_t *table, const char *key)
{
    const char *v = apr_table_get(table, key);
    if (v) {
        if (*v == 'y' || *v == 'Y' || *v == 't' || *v == 'T' || *v == '1')
            return JNI_TRUE;
    }
    return JNI_FALSE;
}

jdouble sight_table_get_d(apr_table_t *table, const char *key)
{
    jdouble rv = 0.0;
    const char *v = apr_table_get(table, key);
    if (v) {
        sscanf(v, "%lf", &rv);
    }
    return rv;
}

apr_table_t *sight_ftable(const char *file, int sep, apr_pool_t *ctx)
{
    apr_table_t *t = NULL;
    FILE *f;
    char line[SIGHT_HBUFFER_SIZ];
    char *pline, *first;

    if (!(f = fopen(file, "r")))
        return NULL;
    if (!(t = apr_table_make(ctx, 16))) {
        fclose(f);
        return NULL;
    }

    while (fgets(&line[0], SIGHT_HBUFFER_LEN, f)) {
        char *val = NULL;
        char *key = NULL;
        pline = sight_trim(&line[0]);
        if (pline && *pline != '#') {
            if ((first = strchr(pline, sep)) != NULL) {
                *first++ = '\0';
                key = sight_rtrim(pline);
                val = sight_ltrim(first);
            }
            if (key) {
                /* We can have NULL values for valid keys */
                apr_table_add(t, key, val);
            }
        }
    }
    fclose(f);
    return t;
}
