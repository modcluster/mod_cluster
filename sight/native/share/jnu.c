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
#include "sight_version.h"

#define MAX_BREAD_LEN   65536
#define MAX_FREAD_LEN   65536
static const char *common_seps = " \t\r\n";


int sight_byteorder()
{
    apr_uint32_t x = 0x01020304;
    apr_byte_t *p;

    p = (apr_byte_t *)&x;
    switch (*p) {
        case 1:
            return SIGHT_BENDIAN;
        case 4:
            return SIGHT_LENDIAN;
    }
    return 0;
}

jsize sight_strparts(const char *s)
{
    jsize n = 0;
    const char *p;
    for (p = s; p && *p; p++) {
        n++;
        while (*p)
            p++;
    }
    return n;
}

jsize sight_wcsparts(const jchar *s)
{
    jsize n = 0;
    const jchar *p;
    for (p = s; p && *p; p++) {
        n++;
        while (*p)
            p++;
    }
    return n;
}

jsize sight_wcslen(const jchar *s)
{
    const jchar *p = s;
    while (*p)
        p++;
    return (jsize)(p - s);
}

char **sight_szparray(apr_pool_t *pool, const char *s, jsize *len)
{
    *len = sight_strparts(s);
    if (*len) {
        int n = 0;
        const char *p;
        char **ra = (char **)apr_palloc(pool, *len * sizeof(char *));
        for (p = s; p && *p; p++) {
            ra[n++] = (char *)p;
            while (*p)
                p++;
        }
        return ra;
    }
    return NULL;
}

char **sight_szarray(const char *s, jsize *len)
{
    *len = sight_strparts(s);
    if (*len) {
        int n = 0;
        const char *p;
        char **ra = (char **)malloc(*len * sizeof(char *));
        for (p = s; p && *p; p++) {
            ra[n++] = (char *)p;
            while (*p)
                p++;
        }
        return ra;
    }
    return NULL;
}

char *sight_rtrim(char *s)
{
    int i;
    /* check for empty strings */
    if (!s || !*s)
        return NULL;
    for (i = (int)strlen(s) - 1; i >= 0 && (apr_iscntrl(s[i]) ||
         apr_isspace(s[i])); i--)
        ;
    s[i + 1] = '\0';
    if (!s[0])
        return NULL;
    else
        return s;
}

char *sight_ltrim(char *s)
{
    int i;
    /* check for empty strings */
    if (!s || !*s)
        return NULL;
    for (i = 0; s[i] != '\0' && apr_isspace(s[i]); i++)
        ;

    if (!s[i])
        return NULL;
    else
        return s + i;
}

char *sight_trim(char *s)
{
    int i;
    /* check for empty strings */
    if (!s || !*s)
        return NULL;
    for (i = (int)strlen(s) - 1; i >= 0 && (apr_iscntrl(s[i]) ||
         apr_isspace(s[i])); i--)
        ;
    s[i + 1] = '\0';
    /* Don't check if the sting is empty */
    if (!s[0])
        return NULL;
    for (i = 0; s[i] != '\0' && apr_isspace(s[i]); i++)
        ;

    if (!s[i])
        return NULL;
    else
        return s + i;
}


int sight_islast(const char *s, int chr)
{
    size_t i;
    /* check for empty strings */
    if (!s || !(i = strlen(s)))
        return 0;
    if (s[i - 1] == chr)
        return -1;
    else
        return 0;
}

char *sight_strup(char *s)
{
    char *p = s;
    int i = 0;

    while (*p)
        s[i++] = apr_toupper(*p++);
    return s;
}

sight_str_t sight_strdupw(const jchar *s)
{
    sight_str_t str;
    apr_size_t siz = 2 * sizeof(jchar);
    const jchar *p = s;

    str.len = 0;
    str.str.w   = NULL;
    if (p == NULL) {
        return str;
    }
    while(*p++) {
        siz += sizeof(jchar);
        str.len++;
    }
    str.str.w = calloc(1, siz);
    if (str.str.w && str.len)
        memcpy(str.str.w, s, str.len * sizeof(jchar));
    return str;
}

char *sight_fread(const char *name)
{
    FILE *f;
    size_t i, rd = MAX_FREAD_LEN;
    char *b;

    if (!(f = fopen(name, "r")))
        return NULL;
    if ((b = malloc(rd))) {
        rd = fread(b, 1, rd - 2, f);

        if (rd > 0) {
            /* Remove all trailing zero and space characters */
            for (i = rd - 1; i >= 0 && (apr_iscntrl(b[i]) ||
                 apr_isspace(b[i])); i--)
                ;
            b[i + 1] = '\0';
            b[i + 2] = '\0';
        }
        else {
            free(b);
            fclose(f);
            return NULL;
        }
    }
    fclose(f);
    return b;
}

char *sight_strdupj(JNIEnv *_E, jstring js)
{
    char *result = NULL;
    const char *cs;

    cs = (const char *)((*_E)->GetStringUTFChars(_E, js, 0));
    if (cs) {
        if (!(result = strdup(cs))) {
            throwAprMemoryException(_E, THROW_NMARK,
                                    apr_get_os_error());
        }
        (*_E)->ReleaseStringUTFChars(_E, js, cs);
    }
    return result;
}

char *sight_pstrdupj(apr_pool_t *p, JNIEnv *_E, jstring js)
{
    char *result = NULL;
    const char *cs;

    cs = (const char *)((*_E)->GetStringUTFChars(_E, js, 0));
    if (cs) {
        if (!(result = apr_pstrdup(p, cs))) {
            throwAprMemoryException(_E, THROW_NMARK,
                                    apr_get_os_error());
        }
        (*_E)->ReleaseStringUTFChars(_E, js, cs);
    }
    return result;
}

jobjectArray sight_mc_to_sa(JNIEnv *_E, const char *str)
{
    jobjectArray arr = NULL;
    jsize n = 0;
    const char *p;

    if (str) {
        if ((n = sight_strparts(str)) > 0)
            arr = sight_new_cc_array(_E, SIGHT_CC_STRING, n);
    }
    if (!arr)
        return NULL;

    n = 0;
    for (p = str; p && *p; p++) {
        jstring s = CSTR_TO_JSTRING(p);
        if (s) {
            (*_E)->SetObjectArrayElement(_E, arr, n, s);
            (*_E)->DeleteLocalRef(_E, s);
        }
        n++;
        while (*p)
            p++;
    }
    return arr;
}

jobjectArray sight_mw_to_sa(JNIEnv *_E, const jchar *str)
{
    jobjectArray arr = NULL;
    jsize n = 0;
    const jchar *p;

    if (str) {
        if ((n = sight_wcsparts(str)) > 0)
            arr = sight_new_cc_array(_E, SIGHT_CC_STRING, n);
    }
    if (!arr)
        return NULL;
    n = 0;
    for (p = str; p && *p; p++) {
        const jchar *ptr = p;
        jstring s;
        jsize len;
        while (*p)
            p++;
        len = (jsize)(p - ptr);
        s = ZSTR_TO_JSTRING(ptr, len);
        if (s) {
            (*_E)->SetObjectArrayElement(_E, arr, n, s);
            (*_E)->DeleteLocalRef(_E, s);
        }
        n++;
    }
    return arr;
}

jobjectArray sight_ac_to_sa(JNIEnv *_E, const char **str, jsize n)
{
    jobjectArray arr = NULL;
    jsize i;

    if (str && n)
        arr = sight_new_cc_array(_E, SIGHT_CC_STRING, n);
    if (!arr)
        return NULL;

    for (i = 0; i < n; i++) {
        jstring s = CSTR_TO_JSTRING(str[i]);
        if (s) {
            (*_E)->SetObjectArrayElement(_E, arr, i, s);
            (*_E)->DeleteLocalRef(_E, s);
        }
    }
    return arr;
}

jobjectArray sight_aw_to_sa(JNIEnv *_E, const jchar **str, jsize n)
{
    jobjectArray arr = NULL;
    jsize i;

    if (str && n)
        arr = sight_new_cc_array(_E, SIGHT_CC_STRING, n);
    if (!arr)
        return NULL;

    for (i = 0; i < n; i++) {
        jstring s = WSTR_TO_JSTRING(str[i]);
        if (s) {
            (*_E)->SetObjectArrayElement(_E, arr, i, s);
            (*_E)->DeleteLocalRef(_E, s);
        }
    }
    return arr;
}

int sight_get_fs_type(const char *name)
{
    char fs[16];
    apr_size_t len = strlen(name);
    const char *p;

    /* ACCEPT-LANGUAGE is the longest headeer
     * that is of interest.
     */
    if (len < 3 || len > 15)
        return SIGHT_FS_UNKNOWN;
    strcpy(fs, name);
    sight_strup(fs);
    p = &fs[1];

    switch (fs[0]) {
        case 'C':
            if (memcmp(p, "DFS", 3) == 0)
                return SIGHT_FS_ISO9660;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'D':
            if (memcmp(p, "EV", 2) == 0)
                return SIGHT_FS_DEV;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'E':
            if (memcmp(p, "XT2", 3) == 0)
                return SIGHT_FS_EXT2;
            if (memcmp(p, "XT3", 3) == 0)
                return SIGHT_FS_EXT3;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'F':
            if (memcmp(p, "AT32", 4) == 0)
                return SIGHT_FS_VFAT;
            if (memcmp(p, "AT", 2) == 0)
                return SIGHT_FS_MSDOS;
            if (memcmp(p, "FS", 2) == 0)
                return SIGHT_FS_FFS;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'H':
            if (memcmp(p, "PFS", 3) == 0)
                return SIGHT_FS_HPFS;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'I':
            if (memcmp(p, "SO9660", 6) == 0)
                return SIGHT_FS_ISO9660;
            if (memcmp(p, "SO_9660", 7) == 0)
                return SIGHT_FS_ISO9660;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'J':
            if (memcmp(p, "FS", 2) == 0)
                return SIGHT_FS_JFS;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'M':
            if (memcmp(p, "SDOS", 4) == 0)
                return SIGHT_FS_MSDOS;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'N':
            if (memcmp(p, "FS", 2) == 0)
                return SIGHT_FS_NFS;
            if (memcmp(p, "TFS", 3) == 0)
                return SIGHT_FS_NTFS;
            if (memcmp(p, "ONE", 3) == 0)
                return SIGHT_FS_NONE;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'P':
            if (memcmp(p, "ROC", 3) == 0)
                return SIGHT_FS_PROC;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'R':
            if (memcmp(p, "PC", 2) == 0)
                return SIGHT_FS_RPC;
            if (memcmp(p, "OMFS", 4) == 0)
                return SIGHT_FS_ROMFS;
            if (memcmp(p, "AMFS", 4) == 0)
                return SIGHT_FS_RAMFS;
            if (memcmp(p, "AISERFS", 7) == 0)
                return SIGHT_FS_RAISERFS;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'S':
            if (memcmp(p, "FS", 2) == 0)
                return SIGHT_FS_SFS;
            if (memcmp(p, "YSFS", 4) == 0)
                return SIGHT_FS_SYSFS;
            if (memcmp(p, "UNRPC", 5) == 0)
                return SIGHT_FS_NFS;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'T':
            if (memcmp(p, "MPFS", 4) == 0)
                return SIGHT_FS_TMPFS;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'U':
            if (memcmp(p, "DF", 2) == 0)
                return SIGHT_FS_UDF;
            if (memcmp(p, "SBFS", 4) == 0)
                return SIGHT_FS_USBFS;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'V':
            if (memcmp(p, "FAT", 3) == 0)
                return SIGHT_FS_VFAT;
            if (memcmp(p, "MHGFS", 5) == 0)
                return SIGHT_FS_VMHGFS;
            if (memcmp(p, "MBLOCK", 6) == 0)
                return SIGHT_FS_VMBLOCK;
            else
                return SIGHT_FS_UNKNOWN;
        break;
        case 'X':
            if (memcmp(p, "FS", 2) == 0)
                return SIGHT_FS_XFS;
            if (memcmp(p, "IAFS", 4) == 0)
                return SIGHT_FS_XIAFS;
            else
                return SIGHT_FS_UNKNOWN;
        break;
    }

    /* NOTREACHED */
    return SIGHT_FS_UNKNOWN;
}

/* const char *
 * inet_ntop4(src, dst, size)
 *  format an IPv4 address, more or less like inet_ntoa()
 * return:
 *  `dst' (as a const)
 * notes:
 *  (1) uses no statics
 *  (2) takes a u_char* not an in_addr as input
 * author:
 *  Paul Vixie, 1996.
 */
const char *sight_inet_ntop4(const unsigned char *src,
                             char *dst, size_t size)
{
    const size_t MIN_SIZE = 16; /* space for 255.255.255.255\0 */
    int n = 0;
    char *next = dst;

    if (size < MIN_SIZE) {
        errno = ENOSPC;
        return NULL;
    }
    do {
        unsigned char u = *src++;
        if (u > 99) {
            *next++ = '0' + u/100;
            u %= 100;
            *next++ = '0' + u/10;
            u %= 10;
        }
        else if (u > 9) {
            *next++ = '0' + u/10;
            u %= 10;
        }
        *next++ = '0' + u;
        *next++ = '.';
        n++;
    } while (n < 4);
    *--next = 0;
    return dst;
}

#ifndef IN6ADDRSZ
#define IN6ADDRSZ   16
#endif

#ifndef INT16SZ
#define INT16SZ sizeof(apr_int16_t)
#endif

#if !defined(EAFNOSUPPORT) && defined(WSAEAFNOSUPPORT)
#define EAFNOSUPPORT WSAEAFNOSUPPORT
#endif

/* const char *
 * inet_ntop6(src, dst, size)
 *  convert IPv6 binary address into presentation (printable) format
 * author:
 *  Paul Vixie, 1996.
 */
const char *sight_inet_ntop6(const unsigned char *src, char *dst,
                             size_t size)
{
    /*
     * Note that int32_t and int16_t need only be "at least" large enough
     * to contain a value of the specified size.  On some systems, like
     * Crays, there is no such thing as an integer variable with 16 bits.
     * Keep this in mind if you think this function should have been coded
     * to use pointer overlays.  All the world's not a VAX.
     */
    char tmp[sizeof "ffff:ffff:ffff:ffff:ffff:ffff:255.255.255.255"], *tp;
    struct { int base, len; } best = {-1, 0}, cur = {-1, 0};
    unsigned int words[IN6ADDRSZ / INT16SZ];
    int i;
    const unsigned char *next_src, *src_end;
    unsigned int *next_dest;

    /*
     * Preprocess:
     *  Copy the input (bytewise) array into a wordwise array.
     *  Find the longest run of 0x00's in src[] for :: shorthanding.
     */
    next_src  = src;
    src_end   = src + IN6ADDRSZ;
    next_dest = words;
    i = 0;
    do {
        unsigned int next_word = (unsigned int)*next_src++;
        next_word <<= 8;
        next_word |= (unsigned int)*next_src++;
        *next_dest++ = next_word;

        if (next_word == 0) {
            if (cur.base == -1) {
                cur.base = i;
                cur.len = 1;
            }
            else {
                cur.len++;
            }
        } else {
            if (cur.base != -1) {
                if (best.base == -1 || cur.len > best.len) {
                    best = cur;
                }
                cur.base = -1;
            }
        }

        i++;
    } while (next_src < src_end);

    if (cur.base != -1) {
        if (best.base == -1 || cur.len > best.len) {
            best = cur;
        }
    }
    if (best.base != -1 && best.len < 2) {
        best.base = -1;
    }

    /*
     * Format the result.
     */
    tp = tmp;
    for (i = 0; i < (IN6ADDRSZ / INT16SZ);) {
        /* Are we inside the best run of 0x00's? */
        if (i == best.base) {
            *tp++ = ':';
            i += best.len;
            continue;
        }
        /* Are we following an initial run of 0x00s or any real hex? */
        if (i != 0) {
            *tp++ = ':';
        }
        /* Is this address an encapsulated IPv4? */
        if (i == 6 && best.base == 0 &&
            (best.len == 6 || (best.len == 5 && words[5] == 0xffff))) {
            if (!sight_inet_ntop4(src+12, tp, sizeof tmp - (tp - tmp))) {
                return (NULL);
            }
            tp += strlen(tp);
            break;
        }
        tp += apr_snprintf(tp, sizeof tmp - (tp - tmp), "%x", words[i]);
        i++;
    }
    /* Was it a trailing run of 0x00's? */
    if (best.base != -1 && (best.base + best.len) == (IN6ADDRSZ / INT16SZ)) {
        *tp++ = ':';
    }
    *tp++ = '\0';

    /*
     * Check for overflow, copy, and we're done.
     */
    if ((apr_size_t)(tp - tmp) > size) {
        errno = ENOSPC;
        return (NULL);
    }
    strcpy(dst, tmp);
    return (dst);
}

void *sight_malloc(JNIEnv *_E, apr_size_t len,
                   const char *file, int line)
{
    void *p;
    if (!(p = malloc(len))) {
        if (_E)
            throwAprMemoryException(_E, file, line,
                                    apr_get_os_error());
    }
    return p;
}

void *sight_realloc(JNIEnv *_E, void *oldp, apr_size_t len,
                    const char *file, int line)
{
    void *p;
    if (!(p = realloc(oldp, len))) {
        if (_E)
            throwAprMemoryException(_E, file, line,
                                    apr_get_os_error());
    }
    return p;
}

char *sight_strdup(JNIEnv *_E, const char *src,
                   const char *file, int line)
{
    char *p;
    if (!src || !*src)
        return NULL;
    if (!(p = strdup(src))) {
        if (_E)
            throwAprMemoryException(_E, file, line,
                                    apr_get_os_error());
    }
    return p;
}

void *sight_calloc(JNIEnv *_E, apr_size_t len,
                   const char *file, int line)
{
    void *p;
    if (!(p = calloc(1, len))) {
        if (_E)
            throwAprMemoryException(_E, file, line,
                                    apr_get_os_error());
    }
    return p;
}

void *sight_palloc(JNIEnv *_E, apr_pool_t *ctx, apr_size_t len,
                   const char *file, int line)
{
    void *p;
    if (!(p = apr_palloc(ctx, len))) {
        if (_E)
            throwAprMemoryException(_E, file, line,
                                    apr_get_os_error());
    }
    return p;
}

void *sight_pcalloc(JNIEnv *_E, apr_pool_t *ctx, apr_size_t len,
                   const char *file, int line)
{
    void *p;
    if (!(p = apr_pcalloc(ctx, len))) {
        if (_E)
            throwAprMemoryException(_E, file, line,
                                    apr_get_os_error());
    }
    return p;
}

sight_arr_t *sight_arr_new(jsize init)
{
    sight_arr_t *a = (sight_arr_t *)malloc(sizeof(sight_arr_t));
    if (!a)
        return NULL;
    a->siz = 0;
    a->len = init;
    if (!(a->arr = (char **)malloc(init * sizeof(char *)))) {
        int saved = errno;
        free(a);
        a = NULL;
        errno = saved;
    }
    return a;
}

int sight_arr_add(sight_arr_t *a, const char *str)
{
    if (!str || !*str)
        return 0;     /* Skip empty and null strings */
    if (a->siz < a->len) {
        a->arr[a->siz++] = strdup(str);
    }
    else {
        char **na;
        size_t len = a->len << 2;
        if (!(na = (char **)malloc(len * sizeof(char *))))
            return errno;
        memcpy(na, a->arr, a->siz * sizeof(char *));
        free(a->arr);
        a->len = len;
        a->arr = na;
        a->arr[a->siz] = strdup(str);
        if (!a->arr[a->siz])
            return errno;
        else
            a->siz++;
    }
    return 0;
}

void sight_arr_free(sight_arr_t *a)
{
    jsize i;

    if (!a)
        return;
    for (i = 0; i < a->siz; i++) {
        SIGHT_FREE(a->arr[i]);
    }
    free(a->arr);
    free(a);
}

sight_arr_t *sight_arr_rload(const char *fname)
{
    sight_arr_t *array;
    char buf[8192];
    FILE *file;

    if (!(file = fopen(fname, "r")))
        return NULL;
    if (!(array = sight_arr_new(16))) {
        fclose(file);
        return NULL;
    }
    while (fgets(&buf[0], 8192, file)) {
        char *pline = sight_trim(&buf[0]);
        /* Skip empty lines */
        if (pline) {
            if (sight_arr_add(array, pline))
                break;
        }
    }
    fclose(file);
    return array;
}

sight_arr_t *sight_arr_cload(const char *fname, const char *cmnt)
{
    sight_arr_t *array;
    char buf[8192];
    FILE *file;

    /* Filename and comment are mandatory */
    if (!fname || !cmnt) {
        errno = EINVAL;
        return NULL;
    }
    if (!(file = fopen(fname, "r")))
        return NULL;
    if (!(array = sight_arr_new(16))) {
        int saved_errno = errno;
        fclose(file);
        errno = saved_errno;
        return NULL;
    }
    while (fgets(&buf[0], 8192, file)) {
        char *pline = sight_trim(&buf[0]);
        /* Skip empty lines */
        if (pline) {
            /* Skip comments */
            int    ic = 0;
            size_t ci;
            size_t cl = strlen(cmnt);
            for (ci = 0; ci < cl; ci++) {
                if (*pline == cmnt[ci]) {
                    ic = 1;
                    break;
                }
            }
            if (!ic) {
                if (sight_arr_add(array, pline))
                    break;
           }
        }
    }
    fclose(file);
    return array;
}

sight_arr_t *sight_arr_lload(const char *fname, int max_lines)
{
    sight_arr_t *array;
    char buf[8192];
    FILE *file;
    int lc = 0;
    if (!(file = fopen(fname, "r")))
        return NULL;
    if (!(array = sight_arr_new(max_lines))) {
        fclose(file);
        return NULL;
    }
    while (fgets(&buf[0], 8192, file)) {
        char *pline = sight_trim(&buf[0]);
        /* Skip empty lines */
        if (pline) {
            if (sight_arr_add(array, pline))
                break;
            if (++lc >= max_lines)
                break;
        }
    }
    fclose(file);
    return array;
}

char *sight_strtok_c(char *str, int sep, char **last)
{
    char *sp;
    if (!str)           /* subsequent call */
        str = *last;    /* start where we left off */
    if (!str)           /* no more tokens */
        return NULL;
    if (sp = strchr(str, sep)) {
        *sp++ = '\0';
        *last = sp;
        return str;
    }
    else {
        /* Check for last empty token */
        return *str ? str : NULL;
    }
}

static int uch4hex(int i)
{
    if (apr_isdigit(i))
        return i - '0';
    else if (apr_isupper(i))
        return i - 'A' + 10;
    else
        return i - 'a' + 10;
}

/*
 * Convert hex to array
 */
int sight_hex2bin(const char *src, unsigned char *dst,
                  size_t size)
{
    unsigned char tmp[64], *tp;
    int  i, len = 0;
    const char *next = src;

    tp = tmp;
    for (i = 0; i < 62, *next; i++) {
        int ch = uch4hex(*next++);
        ch = (ch << 4) | uch4hex(*next++);
        *tp++ = (unsigned char)ch;
    }
    /*
     * Check for overflow, copy, and we're done.
     */
    if ((size_t)(tp - tmp) > size) {
        errno = ENOSPC;
        return -1;
    }
    else
        len = (int)(tp - tmp);
    if (len)
        memcpy(dst, tmp, size);
    return len;
}

/* Match = 0, NoMatch = 1, Abort = -1
 * Based loosely on sections of wildmat.c by Rich Salz
 */
int sight_wmatch(const char *str, const char *exp)
{
    int x, y;

    for (x = 0, y = 0; exp[y]; ++y, ++x) {
        if (!str[x] && exp[y] != '*')
            return -1;
        if (exp[y] == '*') {
            while (exp[++y] == '*');
            if (!exp[y])
                return 0;
            while (str[x]) {
                int ret;
                if ((ret = sight_wmatch(&str[x++], &exp[y])) != 1)
                    return ret;
            }
            return -1;
        }
        else if (exp[y] != '?') {
            if (str[x] != exp[y])
                return 1;
        }
    }
    return (str[x] != '\0');
}
