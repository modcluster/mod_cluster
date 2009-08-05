/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @file  xtools.c
 * @brief Windows generic build tool
 * @author mturk@redhat.com
 */

#if defined(_MSC_VER) && _MSC_VER >= 1200
#pragma warning(push, 3)
#endif

/*
 * disable or reduce the frequency of...
 *   C4057: indirection to slightly different base types
 *   C4075: slight indirection changes (unsigned short* vs short[])
 *   C4100: unreferenced formal parameter
 *   C4127: conditional expression is constant
 *   C4163: '_rotl64' : not available as an intrinsic function
 *   C4201: nonstandard extension nameless struct/unions
 *   C4244: int to char/short - precision loss
 *   C4514: unreferenced inline function removed
 */
#pragma warning(disable: 4100 4127 4163 4201 4514; once: 4057 4075 4244)

/*
 * Ignore Microsoft's interpretation of secure development
 * and the POSIX string handling API
 */
#if defined(_MSC_VER) && _MSC_VER >= 1400
#define _CRT_SECURE_NO_DEPRECATE
#endif
#pragma warning(disable: 4996)

#define WIN32_LEAN_AND_MEAN
/*
 * Restrict the server to a subset of Windows NT 4.0 header files by default
 */
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0500
#endif
#include <windows.h>
#include <windowsx.h>
#include <commdlg.h>
#include <commctrl.h>
#include <objbase.h>
#include <shlobj.h>
#include <shlwapi.h>
#include <shellapi.h>
#include <zmouse.h>
#include <richedit.h>
#include <psapi.h>
#include <imagehlp.h>
#include <urlmon.h>
#include <mshtmhst.h>
#include <lm.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <ctype.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <time.h>
#include <sys/timeb.h>
#include <errno.h>
#include <conio.h>
#include <process.h>
#include <direct.h>
#include <io.h>
#include <fcntl.h>
#include <assert.h>
#include <crtdbg.h>

/*
 * ---------------------------------------------------------------------
 * begin of local defines
 * ---------------------------------------------------------------------
 */

/* An arbitrary size that is digestable. True max is a bit less than 32000 */
#define X_MAX_PATH          8192
/* Make hash size always as 0xFF value */
#define DEFAULT_HASH_SIZE   256
/* Maximum size for environment expansion */
#define INFO_BUFFER_SIZE    32767
/* This is used to cache lengths in x_strvcat */
#define MAX_SAVED_LENGTHS   8

/* Resource definitions from .rc file */
#define IDI_MAINICON            101
#define IDI_RHELICON            102

#define DEF_WWIDTH              505
#define DEF_WHEIGHT             360
#define SIZ_BUFLEN              1024
#define SIZ_BUFMAX              (SIZ_BUFLEN - 1)
#define IS_INVALID_HANDLE(x)    (((x) == NULL || (x) == INVALID_HANDLE_VALUE))

/*
 * Alignment macros
 */

/* X_ALIGN() is only to be used to align on a power of 2 boundary */
#define X_ALIGN(size, boundary) \
    (((size) + ((boundary) - 1)) & ~((boundary) - 1))

/** Default alignment */
#define X_ALIGN_DEFAULT(size) X_ALIGN(size, 8)

#define x_isalnum(c) (isalnum(((unsigned char)(c))))
#define x_isalpha(c) (isalpha(((unsigned char)(c))))
#define x_isdigit(c) (isdigit(((unsigned char)(c))))
#define x_isspace(c) (isspace(((unsigned char)(c))))
#define x_tolower(c) (tolower(((unsigned char)(c))))
#define x_toupper(c) (toupper(((unsigned char)(c))))


typedef enum {
    SYSDLL_KERNEL32 = 0,    // kernel32 From WinBase.h
    SYSDLL_NTDLL    = 1,    // ntdll    From our real kernel
    SYSDLL_USER32   = 2,    // user32   From WinUser.h
    SYSDLL_defined  = 3     // must define as last idx_ + 1
} x_dlltoken_e;

static FARPROC x_load_dll_func(x_dlltoken_e, const char *);

#define DECLARE_LATE_DLL_FUNC(lib, rettype, def,                    \
                                    calltype, fn, args, names)      \
    typedef rettype (calltype *x_late_fpt_##fn) args;               \
    static x_late_fpt_##fn x_late_pfn_##fn = NULL;                  \
    static __inline rettype x_late_##fn args                        \
    {   if (!x_late_pfn_##fn)                                       \
            x_late_pfn_##fn = (x_late_fpt_##fn)                     \
                                      x_load_dll_func(lib, #fn);    \
        if (x_late_pfn_##fn)                                        \
            return (*(x_late_pfn_##fn)) names;                      \
        else return def; } //

DECLARE_LATE_DLL_FUNC(SYSDLL_KERNEL32, BOOL, FALSE,
                      WINAPI, IsWow64Process, (
    IN HANDLE hProcess,
    OUT PBOOL Wow64Process),
    (hProcess, Wow64Process));
#undef  IsWow64Process
#define IsWow64Process x_late_IsWow64Process


/*
 * ---------------------------------------------------------------------
 * end of local defines
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of local variables
 * ---------------------------------------------------------------------
 */

static int xtools_pause = 0;
static SYSTEM_INFO      win_osinf;
static OSVERSIONINFOEXA win_osver;
static int xtrace = 0;
static int xquiet  = 0;

/*
 * ---------------------------------------------------------------------
 * end of local variables
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of forward declrations
 * ---------------------------------------------------------------------
 */

int      utf8_to_unicode_path(wchar_t*, size_t, const char *, size_t);
int      unicode_to_utf8_path(char *, size_t, const wchar_t *);
int      utf8_to_unicode(wchar_t *, size_t, const char *);
int      unicode_to_utf8(char *, size_t, const wchar_t *);
int      x_wfullpath(wchar_t *, size_t, const char *);
char    *x_forwardslash(char *);


/*
 * ---------------------------------------------------------------------
 * end of forward declrations
 * ---------------------------------------------------------------------
 */


/*
 * ---------------------------------------------------------------------
 * begin of local types
 * ---------------------------------------------------------------------
 */

typedef struct hash_node_t hash_node_t;

struct hash_node_t {
    hash_node_t  *next;
    void         *data;
    char         key[1];
};

typedef struct hash_t {
    int          icase;
    unsigned int size;
    hash_node_t  **nodes;
} hash_t;

typedef struct sbuffer {
    char    *data;
    size_t  pos;
    size_t  size;
} sbuffer;

typedef struct ini_node_t ini_node_t;

struct ini_node_t {
    ini_node_t  *next;
    ini_node_t  **last;
    char  *key;
    char  *val;
};

typedef struct ini_section_t ini_section_t;

struct ini_section_t {
    ini_section_t *next;
    ini_section_t **last;
    char          *name;
    ini_node_t    *nodes;
};

/*
 * ---------------------------------------------------------------------
 * end of of local types
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of stdlib replacement functions
 * ---------------------------------------------------------------------
 */

static int c_errno_table[] = {

    0,          /* NO_ERROR */
    EINVAL,     /* ERROR_INVALID_FUNCTION */
    ENOENT,     /* ERROR_FILE_NOT_FOUND */
    ENOENT,     /* ERROR_PATH_NOT_FOUND */
    EMFILE,     /* ERROR_TOO_MANY_OPEN_FILES */
    EACCES,     /* ERROR_ACCESS_DENIED */
    EBADF,      /* ERROR_INVALID_HANDLE */
    ENOMEM,     /* ERROR_ARENA_TRASHED */
    ENOMEM,     /* ERROR_NOT_ENOUGH_MEMORY */
    ENOMEM,     /* ERROR_INVALID_BLOCK */
    E2BIG,      /* ERROR_BAD_ENVIRONMENT */
    ENOEXEC,    /* ERROR_BAD_FORMAT */
    EINVAL,     /* ERROR_INVALID_ACCESS */
    EINVAL,     /* ERROR_INVALID_DATA */
    14,         /* **** reserved */
    ENOENT,     /* ERROR_INVALID_DRIVE */
    EACCES,     /* ERROR_CURRENT_DIRECTORY */
    EXDEV,      /* ERROR_NOT_SAME_DEVICE */
    ENOENT,     /* ERROR_NO_MORE_FILES */
    0
};

static int x_cerror(int err)
{
    if (err == 0) {
        if ((err = GetLastError()) == 0)
            return 0;
    }
    if (err < 0 || err > ERROR_NO_MORE_FILES) {
        switch (err) {
            case 1026:
                return ENOENT;
            break;
            case ERROR_ALREADY_EXISTS:
                return EEXIST;
            case ERROR_INSUFFICIENT_BUFFER:
                return ENAMETOOLONG;
            break;
            default:
                return err;
            break;
        }
    }
    else
        return c_errno_table[err];
}

static int x_perror(int err, const char *msg)
{
    if (err == 0)
        err = x_cerror(errno);
    if (!xquiet) {
        errno = err;
        perror(msg);
    }
    return err;
}

static void x_free(void *p)
{
    if (p != NULL) {
        free(p);
    }
}

static void *x_malloc(size_t size)
{
    void *p = calloc(1, size);
    if (p == NULL) {
        _exit(x_perror(ENOMEM, "malloc"));
    }
    return p;
}

static void *x_calloc(size_t size)
{
    void *p = calloc(1, size);
    if (p == NULL) {
        _exit(x_perror(ENOMEM, "calloc"));
    }
    return p;
}

static void *x_realloc(void *m, size_t size)
{
    m = realloc(m, size);
    if (m == NULL) {
        _exit(x_perror(ENOMEM, "realloc"));
    }
    return m;
}

static char **a_alloc(size_t size)
{
    if (size)
        return (char **)x_malloc((size + 1) * sizeof(char *));
    else {
        errno = EINVAL;
        return NULL;
    }
}

static void a_free(char **array)
{
    char **ptr = array;

    if (!array)
        return;
    while (*ptr != NULL) {
        x_free(*(ptr++));
    }
    x_free(array);
}

static size_t a_size(const char **array)
{
    const char **ptr = array;
    if (array) {
        size_t siz = 0;
        while (*ptr != NULL) {
            ptr++;
            siz++;
        }
        return siz;
    }
    else
        return 0;
}

static char *x_strdup(const char *s)
{
    char *p;
    size_t size;
    if (s != NULL)
        size = strlen(s);
    else
        return NULL;
    p = (char *)x_malloc(size + 2);
    memcpy(p, s, size);
    p[size++] = '\0';
    p[size]   = '\0';
    return p;
}

static wchar_t *x_wstrdup(const wchar_t *s)
{
    wchar_t *p;
    size_t size;
    if (s != NULL)
        size = wcslen(s);
    else
        return NULL;
    p = (wchar_t *)x_malloc((size + 2) * sizeof(wchar_t));
    memcpy(p, s, size * sizeof(wchar_t));
    p[size++] = L'\0';
    p[size]   = L'\0';
    return p;
}

static char *x_strndup(const char *s, size_t size)
{
    char *p;

    if (s == NULL)
        return NULL;
    if (strlen(s) < size)
        size = strlen(s);
    p = (char *)x_malloc(size + 2);
    memcpy(p, s, size);
    return p;
}

static wchar_t *x_wstrdup_utf8(const char *str)
{
    int     len;
    wchar_t *res;
    if (!str)
        return NULL;
    len = strlen(str);
    res = x_malloc((len + 2) * sizeof(wchar_t));
    if (!MultiByteToWideChar(CP_UTF8, 0, str, -1, res, len + 1)) {
        x_free(res);
        return NULL;
    }
    else {
        len = wcslen(res);
        res[len + 1] = L'\0';
        return res;
    }
}

static char *x_strdup_utf8(const wchar_t *str)
{
    size_t  l;
    char *r;

    if (!(l = WideCharToMultiByte(CP_UTF8, 0, str, -1, NULL, 0, NULL, 0)))
        return NULL;
    r = x_malloc(l + 1);
    if (!(l = WideCharToMultiByte(CP_UTF8, 0, str, -1, r, l, NULL, 0))) {
        x_free(r);
        return NULL;
    }
    else {
        r[l] = '\0';
        return r;
    }
}


/*
 * Replacement for the strncpy() function. We roll our
 * own to implement these specific changes:
 *   (1) strncpy() doesn't always null terminate and we want it to.
 *   (2) strncpy() null fills, which is bogus, esp. when copy 8byte
 *       strings into 8k blocks.
 *   (3) Instead of returning the pointer to the beginning of
 *       the destination string, we return a pointer to the
 *       terminating '\0' to allow us to "check" for truncation
 *
 * x_strncpy() follows the same call structure as strncpy().
 */
static char *x_strncpy(char *d, const char *s, size_t l)
{
    char *dst, *end;
    if (l == 0)
        return d;
    if (s == NULL) {
        *d = '\0';
        return d;
    }
    dst = d;
    end = d + l - 1;

    for (; dst < end; ++dst, ++s) {
        if (!(*dst = *s))
            return dst;
    }

    *dst = '\0';    /* always null terminate */
    return dst;
}

static char *x_strvcat(const char *str, ...)
{
    char *cp, *argp, *res;
    size_t saved_lengths[MAX_SAVED_LENGTHS];
    int nargs = 0;
    size_t len;
    va_list adummy;

    /* Pass one --- find length of required string */


    if (!str)
        return NULL;

    len = strlen(str);
    va_start(adummy, str);
    saved_lengths[nargs++] = len;
    while ((cp = va_arg(adummy, char *)) != NULL) {
        size_t cplen = strlen(cp);
        if (nargs < MAX_SAVED_LENGTHS) {
            saved_lengths[nargs++] = cplen;
        }
        len += cplen;
    }
    va_end(adummy);

    /* Allocate the required string */
    res = (char *)x_malloc(len + 2);
    cp = res;

    /* Pass two --- copy the argument strings into the result space */
    va_start(adummy, str);

    nargs = 0;
    len = saved_lengths[nargs++];
    memcpy(cp, str, len);
    cp += len;

    while ((argp = va_arg(adummy, char *)) != NULL) {
        if (nargs < MAX_SAVED_LENGTHS) {
            len = saved_lengths[nargs++];
        }
        else {
            len = strlen(argp);
        }
        memcpy(cp, argp, len);
        cp += len;
    }

    va_end(adummy);
    return res;
}

static char *x_strcat(char *dst, const char *add)
{
    if (dst) {
        size_t len = strlen(dst) + strlen(add);
        char *res = realloc(dst, len + 2);
        if (!res) {
            res = x_malloc(len + 2);
            strcpy(res, dst);
        }
        strcat(res, add);
        res[len + 1] = '\0';
        return res;
    }
    else
        return x_strdup(add);
}

static char *x_strchr(const char *p, int ch, int icase)
{
    char c;

    c = ch;
    for (;; ++p) {
        if (icase && (x_tolower(*p) == x_tolower(c)))
            return (char *)p;
        else if (*p == c)
            return (char *)p;
        if (*p == '\0')
            return NULL;
    }
    /* NOTREACHED */
}

static char *__argv0 = NULL;
static char *__pname = NULL;
static char *__ppath = NULL;
static char *__paren = NULL;

static const char *getprogname()
{
    char *p;

    if (__pname)
        return __pname;
    if ((p = strrchr(__argv0, '\\')))
        p++;
    else
        p = __argv0;
    __pname = x_strdup(p);
    /* Remove extension */
    if ((p = strrchr(__pname, '.')))
        *p = '\0';
    return __pname;
}

static const char *getexecpath()
{
    char *p;

    if (__ppath)
        return __ppath;
    __ppath = x_strdup(__argv0);
    if ((p = strrchr(__ppath, '\\')))
        *++p = '\0';
    return __ppath;
}

static const char *getexecparent()
{
    char *p;

    if (__paren)
        return __paren;
    __paren = x_strdup(getexecpath());
    if ((p = strrchr(__paren, '\\')))
        *++p = '\0';
    if ((p = strrchr(__paren, '\\')))
        *++p = '\0';
    return __paren;
}

#define progname    getprogname()
#define execpath    getexecpath()
#define execparent  getexecparent()

static const char *wstrerror(int err)
{
    static char buff[X_MAX_PATH] = {0};

    FormatMessageA(FORMAT_MESSAGE_FROM_SYSTEM |
                   FORMAT_MESSAGE_IGNORE_INSERTS,
                   NULL,
                   err,
                   MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                   buff,
                   X_MAX_PATH,
                   NULL);

    return buff;
}

static const char *cstrerror(int err)
{
    if (err > EILSEQ)
        return wstrerror(err);
    else
        return strerror(err);
}

/*
 * ---------------------------------------------------------------------
 * end of stdlib replacement functions
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of string utilities
 * ---------------------------------------------------------------------
 */

/* Match = 0, NoMatch = 1, Abort = -1
 * Based loosely on sections of wildmat.c by Rich Salz
 */
static int wchrmatch(const char *str, const char *exp,
                     size_t *match, int icase)
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
                if (match)
                    *match = (size_t)x;
                if ((ret = wchrmatch(&str[x++], &exp[y], match, icase)) != 1)
                    return ret;
            }
            if (match)
                *match = 0;
            return -1;
        }
        else if (exp[y] != '?') {
            if (icase) {
                if (x_tolower(str[x]) != x_tolower(exp[y]))
                    return 1;
            }
            else if (str[x] != exp[y])
                return 1;
        }
    }
    return (str[x] != '\0');
}

/* Win32 Exceptions:
 *
 * Note that trailing spaces and trailing periods are never recorded
 * in the file system, except by a very obscure bug where any file
 * that is created with a trailing space or period, followed by the
 * ':' stream designator on an NTFS volume can never be accessed again.
 * In other words, don't ever accept them when designating a stream!
 *
 * An interesting side effect is that two or three periods are both
 * treated as the parent directory, although the fourth and on are
 * not [strongly suggest all trailing periods are trimmed off, or
 * down to two if there are no other characters.]
 *
 * Leading spaces and periods are accepted, however.
 * The * ? < > codes all have wildcard side effects
 * The " / \ : are exclusively component separator tokens
 * The system doesn't accept | for any (known) purpose
 * Oddly, \x7f _is_ acceptable ;)
 */

/* apr_c_is_fnchar[] maps Win32's file name and shell escape symbols
 *
 *   element & 1 == valid file name character [excluding delimiters]
 *   element & 2 == character should be shell (caret) escaped from cmd.exe
 *
 * this must be in-sync with Apache httpd's gen_test_char.c for cgi escaping.
 */

static const char is_valid_fnchar[256] = {
    /* Reject all ctrl codes... Escape \n and \r (ascii 10 and 13)      */
    0,0,0,0,0,0,0,0,0,0,2,0,0,2,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    /*   ! " # $ % & ' ( ) * + , - . /  0 1 2 3 4 5 6 7 8 9 : ; < = > ? */
    1,1,2,1,3,3,3,3,3,3,2,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,0,3,2,1,2,2,
    /* @ A B C D E F G H I J K L M N O  P Q R S T U V W X Y Z [ \ ] ^ _ */
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,1,3,2,3,3,1,
    /* ` a b c d e f g h i j k l m n o  p q r s t u v w x y z { | } ~   */
    3,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,1,3,2,3,3,1,
    /* High bit codes are accepted (subject to utf-8->Unicode xlation)  */
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1
};


#define IS_FNCHAR(c) (is_valid_fnchar[(unsigned char)(c)] & 1)
#define IS_SHCHAR(c) ((is_valid_fnchar[(unsigned char)(c)] & 2) == 2)

/* Is = 1, No = 0, Error = -1
 */
static int is_wildpath(const char *path)
{
    size_t i = 0;
    size_t len;

    if (!path) {
        errno = EINVAL;
        return -1;
    }
    len = strlen(path);
    if (len > 8 && !strncmp(path, "\\\\?\\UNC\\", 8))
        path += 8;
    else if (len > 4 && !strncmp(path, "\\\\?\\", 4))
        path += 4;
    if (x_isalpha(*path) && (path[1] == ':')) {
        path += 2;
    }
    while (*path) {
        if (!IS_FNCHAR(*path) && (*path != '\\') && (*path != '/')) {
            if (*path == '?' || *path == '*')
                return 1;
            else {
                errno = ENOENT;
                return -1;
            }
        }
        ++path;
    }
    return 0;
}

static char *rtrim(char *s)
{
    size_t i;
    /* check for empty strings */
    if (!(i = strlen(s)))
        return s;
    for (i = i - 1; i >= 0 && x_isspace(s[i]); i--)
        ;
    s[i + 1] = '\0';
    return s;
}

static char *ltrim(char *s)
{
    size_t i;
    for (i = 0; s[i] != '\0' && x_isspace(s[i]); i++)
        ;

    return s + i;
}

static char *rltrim(char *s)
{
    size_t i;
    /* check for empty strings */
    if (!(i = strlen(s)))
        return s;
    for (i = i - 1; i >= 0 && x_isspace(s[i]); i--)
        ;
    s[i + 1] = '\0';
    for (i = 0; s[i] != '\0' && x_isspace(s[i]); i++)
        ;

    return s + i;
}
/*
 * ---------------------------------------------------------------------
 * end of string utilities
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of path utilities
 * ---------------------------------------------------------------------
 */

static const char *posixmatch[] = {
    "/cygdrive/?/*",
    "/dev/fs/?/*",
    "/bin/*",
    "/dev/*",
    "/etc/*",
    "/home/*",
    "/lib/*",
    "/opt/*",
    "/proc/*",
    "/usr/*",
    "/tmp/*",
    "/var/*",
    NULL
};

static char        windrive[]  = { '\0', ':', '\\', '\0'};
static char       *posixroot   = NULL;
static char       *msvcpath    = NULL;

static int checkposix(const char *str)
{
    const char  *equ;
    const char **mp = posixmatch;

    if ((equ = strchr(str, '='))) {
        /* Special case for environment variables */
        if (*(equ + 1) == '/' && equ < strchr(str, '/'))
            str = equ + 1;
    }
    while (*mp) {
        if (wchrmatch(str, *mp, NULL, 1) == 0) {
            return 1;
        }
        mp++;
    }

    return 0;
}

static char **tokenizepath(const char *str, size_t *tokens)
{
    size_t c     = 1;
    char **array = NULL;
    const  char  *b;
    const  char  *e;
    const  char  *s;

    b = s = str;
    while ((e = strchr(b, ':'))) {
        int ch = *(e + 1);
        if (ch == '/' || ch == '.' || ch == ':' ||
            ch == '\0') {
            /* Is the previous token path or flag */
            if (checkposix(s)) {
                c++;
                s = e + 1;
            }
        }
        b = e + 1;
    }
    array  = a_alloc(c);
    c = 0;
    b = s = str;
    while ((e = strchr(b, ':'))) {
        int ch = *(e + 1);
        if (ch == '/' || ch == '.' || ch == ':' ||
            ch == '\0') {
            /* Is the previous token path or flag */
            if (checkposix(s)) {
                array[c++] = x_strndup(b, e - b);
                s = e + 1;
            }
        }
        b = e + 1;
    }
    array[c++] = x_strdup(s);
    if (tokens)
        *tokens = c;
    return array;
}

static char *mergepaths(char * const *paths)
{
    size_t len = 0;
    char *rv;
    char *const *pp;

    pp = paths;
    while (*pp) {
        len += (strlen(*pp) + 1);
        pp++;
    }
    rv = x_malloc(len + 1);

    len = 0;
    pp = paths;
    while (*pp) {
        if (len++)
            strcat(rv, ";");
        strcat(rv, *pp);
        pp++;
    }
    return rv;
}

static char *posix2win(const char *str, int reverse)
{
    char *rv = NULL;
    char **pa;
    size_t i, tokens;

    pa = tokenizepath(str, &tokens);
    for (i = 0; i < tokens; i++) {
        size_t mx = 0;
        char  *pp;
        char  *ep = NULL;
        const char **mp = posixmatch;

        if ((pp = strchr(pa[i], '='))) {
            /* Special case for environment variables */
            if (*(pp + 1) == '/' && pp < strchr(pa[i], '/')) {
                ep = pa[i];
                *(pp++) = '\0';
            }
            else
                pp = pa[i];
        }
        else
            pp = pa[i];
        while (*mp) {
            size_t match = 0;
            if (wchrmatch(pp, *mp, &match, 1) == 0) {
                char *lp = pp + match;
                const char *wp;
                if (mx < 2) {
                    /* Absolute path */
                    wp  = windrive;
                    lp += (strlen(*mp) - 1);
                }
                else {
                    /* Posix internal path */
                    wp  = posixroot;
                }
                if (reverse) {
                    char *rp = lp;
                    while (IS_FNCHAR(*rp)) {
                        if (*rp == '/')
                            *rp = '\\';
                        rp++;
                    }
                }
                rv = pa[i];
                if (ep)
                    pa[i] = x_strvcat(ep, "=", wp, lp, NULL);
                else
                    pa[i] = x_strvcat(wp, lp, NULL);
                x_free(rv);
                break;
            }
            mx++;
            mp++;
        }

    }
    rv = mergepaths(pa);
    a_free(pa);
    return rv;
}

static void reprelative(char *str)
{
    size_t match;

    while (wchrmatch(str, "*./*", &match, 1) == 0) {
        char *ep = str + match;
        /* Replace till the end of path element */
        while (IS_FNCHAR(*ep)) {
            if (*ep == '/')
                *ep = '\\';
            ep++;
        }
        if (*ep)
            strcat(str, ep);
    }
}

static char *findlibpath(const char *name)
{
    HMODULE handle;
    UINT em;

    em = SetErrorMode(SEM_FAILCRITICALERRORS);
    if (!(handle = LoadLibraryExA(name, NULL, 0))) {
        handle =   LoadLibraryExA(name, NULL,
                                  LOAD_WITH_ALTERED_SEARCH_PATH);
    }
    SetErrorMode(em);

    if (handle) {
        char name[MAX_PATH] = { 0 };
        char *ptr;
        GetModuleFileNameA(handle, name, MAX_PATH);
        FreeLibrary(handle);
        if (ptr = strrchr(name, '\\')) {
            *ptr = '\0';
            if (name[1] == ':') {
                char *rv = x_strdup(name);
                *rv = x_toupper(*rv);
                return rv;
            }
        }
    }
    return NULL;
}

static char *getcygdrive(int *drive, int back)
{

    if (posixroot)
        return posixroot;
    posixroot = findlibpath("bash.exe");

    if (posixroot) {
        char *ptr;
        if (ptr = strrchr(posixroot, '\\')) {
            if (!stricmp(ptr, "\\bin")) {
                *ptr = '\0';
                if (drive)
                    *drive = *posixroot;
                if (!back)
                    x_forwardslash(posixroot);
            }
            else {
                x_free(posixroot);
                posixroot = NULL;
            }
        }
    }
    return posixroot;
}

static char *getsuadrive(int *drive, int back)
{
    char *sr;
    if (posixroot)
        return posixroot;

    if ((sr = getenv("SUA_ROOT"))) {
        if (strstr(sr, "/dev/fs/")) {
            char *r;
            char d[3] = { '\0', ':', '\0' };

            d[0] = *(sr + 8);
            posixroot = x_strvcat(d, sr + 9, NULL);
            r = posixroot;
            while (*r) {
                if (*r == '/')
                    *r = '\\';
                r++;
            }
            if (*(r - 1) == '\\')
                *(r - 1) = '\0';
            if (drive)
                *drive = d[0];
            if (!back)
                x_forwardslash(posixroot);
        }
    }
    return posixroot;
}

static char *getmsvcpath()
{

    if (msvcpath)
        return msvcpath;
    return (msvcpath = findlibpath("cl.exe"));
}

static int checkbinary(const char *exe)
{
    HMODULE handle;
    UINT em;

    em = SetErrorMode(SEM_FAILCRITICALERRORS);
    if (!(handle = LoadLibraryExA(exe, NULL, 0))) {
        handle =   LoadLibraryExA(exe, NULL,
                                  LOAD_WITH_ALTERED_SEARCH_PATH);
    }
    SetErrorMode(em);
    if (handle) {
        FreeLibrary(handle);
        return 1;
    }
    else {
        errno = ENOENT;
        return 0;
    }
}

/*
 * ---------------------------------------------------------------------
 * end of path utilities
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of hash
 * ---------------------------------------------------------------------
 */

static unsigned int strhash(const char *string, int icase)
{
    unsigned int hash = 0;
    int i;

    while (*string) {
        if (icase)
            i = x_toupper(*string);
        else
            i = *string;
        hash = hash * 33 + i;
        string ++;
    }
    return hash;
}

static void free_node(void *unused, void *data)
{
    unused;
    x_free(data);
}

static hash_t *hash_create(unsigned int size, int icase)
{
    hash_t *h;
    if (!size)
        size = DEFAULT_HASH_SIZE;
    else
        size = X_ALIGN_DEFAULT(size);
    h = (hash_t *)x_malloc(sizeof(hash_t));
    h->nodes = (hash_node_t **)x_calloc(sizeof(hash_node_t *) * size);
    h->size  = size;
    h->icase = icase;
    return h;
}

static void *hash_insert(hash_t *ht, const char *key, void *data)
{
    unsigned int ikey;
    if (!ht || !key)
        return NULL;
    ikey = strhash(key, ht->icase) % ht->size;
    if (!ht->nodes[ikey]) {
        ht->nodes[ikey] = (hash_node_t *)x_malloc(sizeof(hash_node_t) +
                                                  strlen(key));
        strcpy(ht->nodes[ikey]->key, key);
        ht->nodes[ikey]->data = data;
        ht->nodes[ikey]->next = NULL;
    }
    else {
        hash_node_t *p;
        for (p = ht->nodes[ikey]; p; p = p->next) {
            if (ht->icase ? stricmp(p->key, key) : strcmp(p->key, key)) {
                void *org = p->data;
                p->data = data;
                return org;
            }
        }
        p = (hash_node_t *)x_malloc(sizeof(hash_node_t) + strlen(key));
        strcpy(p->key, key);
        p->data = data;
        p->next = ht->nodes[ikey];
        ht->nodes[ikey] = p;
    }
    return NULL;
}

static void *hash_find(hash_t *ht, const char *key, int *found)
{
    unsigned int ikey;
    if (!ht || !key)
        return NULL;
    ikey = strhash(key, ht->icase) % ht->size;

    if (found)
        *found = 0;
    if (!ht->nodes[ikey])
        return NULL;
    else {
        hash_node_t *p;
        for (p = ht->nodes[ikey]; p; p = p->next) {
            if (ht->icase ? stricmp(p->key, key) : strcmp(p->key, key)) {
                if (found)
                    *found = 1;
                return p->data;
            }
        }
    }
    return NULL;
}

static void hash_foreach(hash_t *ht, void *opaque,
                         void (*callback)(void *, const char *, void *))
{
    unsigned int i;

    for (i = 0; i < ht->size; i++) {
        if (ht->nodes[i]) {
            hash_node_t *p;
            for (p = ht->nodes[i]; p; p = p->next) {
                (*callback)(opaque, p->key, p->data);
            }
        }
    }
}

static void hash_destroy(hash_t *ht,  void *opaque,
                         void (*callback)(void *, const char *, void *))
{
    unsigned int i;
    for (i = 0; i < ht->size; i++) {
        if (ht->nodes[i]) {
            hash_node_t *p = ht->nodes[i];
            while (p) {
                hash_node_t *next = p->next;
                if (callback)
                    (*callback)(opaque, p->key, p->data);
                x_free(p);
                p = next;
            }
        }
    }
    x_free(ht->nodes);
    x_free(ht);
}

/*
 * ---------------------------------------------------------------------
 * end of hash
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of posix
 * ---------------------------------------------------------------------
 */

static FILE *x_fopen(const char *name, const char *mode)
{
    DWORD   acc = 0;
    DWORD   mod = OPEN_EXISTING;
    HANDLE  fh;
    wchar_t fname[X_MAX_PATH];
    int r;
    int flags = _O_RDONLY;
    FILE *f;

    /* Do some param checking */
    if (!name || !*name || !mode || !*mode) {
        errno = EINVAL;
        return NULL;
    }
    if (strchr(mode, 'r'))
        acc = GENERIC_READ;
    if (strchr(mode, 'w')) {
        acc = GENERIC_READ | GENERIC_WRITE;
        mod = CREATE_ALWAYS;
        flags = _O_RDWR;
    }
    if (strchr(mode, 'a')) {
        acc = GENERIC_WRITE;
        mod = OPEN_ALWAYS;
        flags = _O_WRONLY;
    }
    if (strchr(mode, '+')) {
        acc = GENERIC_READ | GENERIC_WRITE;
        flags = _O_RDWR;
    }
    if (strchr(mode, 't'))
        flags |= _O_TEXT;
    else if (strchr(mode, 'b'))
        flags |= _O_BINARY;
    if ((r = x_wfullpath(fname, X_MAX_PATH, name)) != 0) {
        errno = r;
        return NULL;
    }
    fh = CreateFileW(fname, acc, FILE_SHARE_READ, NULL,
                     mod, FILE_ATTRIBUTE_NORMAL, NULL);
    if (fh == INVALID_HANDLE_VALUE) {
        errno = x_cerror(0);
        return NULL;
    }
    if ((r = _open_osfhandle((long)fh, flags)) < 0) {
        CloseHandle(fh);
        return NULL;
    }
    if (!(f = _fdopen(r, mode))) {
        CloseHandle(fh);
        return NULL;
    }
    return f;
}

static void vwarnx(const char *fmt, va_list ap)
{
    if (xquiet)
        return;
    fprintf(stderr, "%s: ", getprogname());
    if (fmt != NULL)
        vfprintf(stderr, fmt, ap);
    fprintf(stderr, "\n");
}

static void warnx(const char *fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    vwarnx(fmt, ap);
    va_end(ap);
}

static int     optslash = '/'; /* allow slashes as option modifiers */
static int     optsbrk  = 1;   /* Break if slash is unknown modifier */
static int     opterr   = 1;   /* if error message should be printed */
static int     optind   = 1;   /* index into parent argv vector */
static int     optopt   = 0;   /* character checked for validity */
static int     optlong  = 1;   /* Allow long options */
static int     optreset = 1;   /* reset getopt */
static char   *optarg = NULL;  /* argument associated with option */

#define BADCH   '?'
#define BADARG  ':'
#define LNGOPT  '.'
#define EMSG    ""

/*
 * getopt --
 * Parse argc/argv argument vector.
 * However this one allows both - and / as argument options
 */
int getopt(int nargc, const char **nargv, const char *ostr, int icase)
{
    static const char *place = EMSG;    /* option letter processing */
    char *oli = NULL;                   /* option letter list index */
    int first = 0;

    optarg = NULL;
    if (optreset || *place == 0) {      /* update scanning pointer */
        optreset = 0;
        place = nargv[optind];
        if (optind >= nargc) {
            /* Argument is absent or is not an option */
            place = EMSG;
            return EOF;
        }
        first = *place++;
        if (first != '-' && first != optslash) {
            /* Argument is absent or is not an option */
            place = EMSG;
            return EOF;
        }
        optopt = *place++;
        /* Check for invalid sequence */
        if ((optslash == '/') &&
            ((first == '-' && optopt == '/') ||
             (first == '/' && optopt == '-'))) {
            ++optind;
            if (opterr)
                warnx("Invalid sequence -- %c%c",
                      first, optopt);
            place = EMSG;
            return BADCH;

        }
        if (first == '-' && optopt == '-') {
            ++optind;
            if (*place == 0 || !optlong) {
                place = EMSG;
                /* "--" => end of options */
                return EOF;
            }
            else {
                optarg = (char *)place;
                place  = EMSG;
                /* "--long" => long option */
                return LNGOPT;
            }
        }
        if (optopt == 0) {
            /*
             * Solitary '-', treat as a '-' option
             * if the program (eg su) is looking for it.
             */
            place = EMSG;
            if (x_strchr(ostr, first, icase) == NULL)
                return EOF;
            optopt = first;
        }
        if (optopt == optslash) {
            ++optind;
            place = EMSG;
            /* "//" => end of options */
            return EOF;
        }
    }
    else
        optopt = *place++;

    /* See if option letter is one the caller wanted... */
    if (optopt == BADARG || (oli = x_strchr(ostr, optopt, icase)) == NULL) {
        if (!optsbrk && optslash == '/' && first == optslash) {
            /* Non option starting with / */
            place = EMSG;
            return EOF;
        }
        if (*place == 0)
            ++optind;
        if (opterr && *ostr != ':')
            warnx("unknown option -- %c\n"
                  "Try '%s --help' for more information.",
                  optopt, progname);
        return BADCH;
    }

    /* Does this option need an argument? */
    if (oli[1] != ':') {
        /* don't need argument */
        optarg = NULL;
        if (*place == 0) {
            ++optind;
            place = EMSG;
        }
        else if (optslash == '/' && first == optslash) {
            ++optind;
            optarg = (char *)place;
            place  = EMSG;
        }
    } else {
        /*
         * Option-argument is either the rest of this argument or the
         * entire next argument.
         */
        if (*place)
            optarg = (char *)place;
        else if (nargc > ++optind)
            optarg = (char *)nargv[optind];
        else {
            /* option-argument absent */
            place = EMSG;
            if (*ostr == ':')
                return BADARG;
            if (opterr)
                warnx("option requires an argument -- %c\n"
                      "Try '%s --help' for more information.",
                      optopt, progname);
            return BADCH;
        }
        place = EMSG;
        ++optind;
    }
    return optopt;            /* return option letter */
}


/*
 * ---------------------------------------------------------------------
 * end of posix
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of misc
 * ---------------------------------------------------------------------
 */

/*
 * Convert all forward slashes to backward slashes
 */
static char *x_backslash(const char *s)
{
    char *c;
    char *p = x_strdup(s);
    for (c = p; *c; c++) {
        if (*c == '/')
            *c = '\\';
    }
    return p;
}

/*
 * Convert all backward slashes to forward slashes
 * in place.
 */
static char *x_forwardslash(char *s)
{
    char *c = s;
    for (; *c; c++) {
        if (*c == '\\')
            *c = '/';
    }
    return s;
}

static char *expand_envars(char *str)
{
    wchar_t ibuf[INFO_BUFFER_SIZE];
    wchar_t ebuf[INFO_BUFFER_SIZE];
    char *rv = str;

    if (MultiByteToWideChar(CP_UTF8, 0, str, -1, ibuf, INFO_BUFFER_SIZE)) {
        DWORD el = ExpandEnvironmentStringsW(ibuf, ebuf,
                                             INFO_BUFFER_SIZE);
        if (el > INFO_BUFFER_SIZE) {
            if (xtrace)
                warnx("expansion string to large %d", el);
        }
        else if (el) {
            if ((rv = x_strdup_utf8(ebuf)))
                x_free(str);
            else
                rv = str;
        }
    }
    return rv;
}

static char *expand_wenvars(const wchar_t *str)
{
    wchar_t ebuf[INFO_BUFFER_SIZE];
    char *rv = NULL;
    DWORD el;

    el = ExpandEnvironmentStringsW(str, ebuf,
                                   INFO_BUFFER_SIZE);
    if (el > INFO_BUFFER_SIZE) {
        if (xtrace)
            warnx("expansion string to large %d", el);
    }
    else if (el) {
        rv = x_strdup_utf8(ebuf);
    }
    return rv;
}

static char *x_getenv(const char *str)
{
    wchar_t ibuf[256];
    wchar_t ebuf[INFO_BUFFER_SIZE];
    char *rv = NULL;

    if (MultiByteToWideChar(CP_UTF8, 0, str, -1, ibuf, 256)) {
        DWORD el = GetEnvironmentVariableW(ibuf, ebuf,
                                           INFO_BUFFER_SIZE);
        if (el > INFO_BUFFER_SIZE) {
            if (xtrace)
                warnx("expansion string to large %d", el);
        }
        else if (el) {
            rv = x_strdup_utf8(ebuf);
        }
    }
    return rv;
}


/* This is the helper code to resolve late bound entry points
 * missing from one or more releases of the Win32 API
 */

static const char* const late_dll_mames[SYSDLL_defined] = {
    "kernel32",
    "ntdll.dll",
    "user32.dll"
};

static HMODULE late_dll_handles[SYSDLL_defined] = { NULL, NULL, NULL};

static FARPROC x_load_dll_func(x_dlltoken_e fnLib, const char* fnName)
{
    if (!late_dll_handles[fnLib]) {
        /* First see if the .dll is already loaded in the process */
        late_dll_handles[fnLib] = GetModuleHandleA(late_dll_mames[fnLib]);
        if (!late_dll_handles[fnLib]) {
            /* Do not display error messages when loading library */
            UINT em = SetErrorMode(SEM_FAILCRITICALERRORS);
            late_dll_handles[fnLib] = LoadLibraryA(late_dll_mames[fnLib]);
            SetErrorMode(em);
        }
        if (!late_dll_handles[fnLib])
            return NULL;
    }
    return GetProcAddress(late_dll_handles[fnLib], fnName);
}


/*
 * ---------------------------------------------------------------------
 * end of misc
 * ---------------------------------------------------------------------
 */


/*
 * ---------------------------------------------------------------------
 * begin of file functions
 * ---------------------------------------------------------------------
 */

/*
 * Makes a full path from partial path
 */
static char *x_fullpath(const char *s)
{
    wchar_t full[X_MAX_PATH];
    wchar_t path[X_MAX_PATH];
    char    *res;
    wchar_t *fn;
    size_t  len;
    int     r;

    if ((r = utf8_to_unicode_path(path, X_MAX_PATH, s, X_MAX_PATH))) {
        errno = r;
        return NULL;
    }
    len = GetFullPathNameW(path,
                           X_MAX_PATH,
                           full, &fn);
    if (len >= X_MAX_PATH) {
        errno = ERANGE;
        return NULL;
    }
    if (len == 0) {
        errno = EINVAL;
        return NULL;
    }
    res = x_malloc(X_MAX_PATH);
    if ((r = unicode_to_utf8_path(res, X_MAX_PATH, full))) {
        errno = r;
        x_free(res);
        return NULL;
    }
    return res;
}

/*
 * Makes a full path from partial path
 */
static int x_wfullpath(wchar_t *full, size_t len, const char *s)
{
    wchar_t path[X_MAX_PATH];
    wchar_t *fn;
    int     r;
    if (!s || !*s)
        return EINVAL;
    *full = L'\0';
    if ((r = utf8_to_unicode_path(path, X_MAX_PATH, s, X_MAX_PATH)))
        return r;
    len = GetFullPathNameW(path, len,
                           full, &fn);
    if (len >= X_MAX_PATH)
        return ERANGE;
    else if (len == 0)
        return EINVAL;
    else
        return 0;
}


/*
 * Makes a full path ending with \ from partial path
 */
static char *x_fulldir(const char *s)
{
    wchar_t full[X_MAX_PATH];
    wchar_t path[X_MAX_PATH];
    char    *res;
    wchar_t *fn;
    size_t  len;
    int     r;

    if ((r = utf8_to_unicode_path(path, X_MAX_PATH, s, X_MAX_PATH))) {
        errno = r;
        return NULL;
    }
    len = GetFullPathNameW(path,
                           X_MAX_PATH - 2,
                           full, &fn);
    if (len >= (X_MAX_PATH - 2)) {
        errno = ERANGE;
    }
    if (len == 0) {
        errno = x_cerror(GetLastError());
        return NULL;
    }
    res = x_malloc(X_MAX_PATH);
    if ((r = unicode_to_utf8_path(res, X_MAX_PATH - 2, full))) {
        errno = r;
        x_free(res);
        return NULL;
    }
    len = strlen(res);
    if (res[len - 1] != '\\' &&
        res[len - 1] != '*') {
        res[len++] = '\\';
        res[len]   = '\0';
    }
    return res;
}

static int x_createdir(const char *dir)
{
    wchar_t path[X_MAX_PATH];
    int     r;

    if ((r = utf8_to_unicode_path(path, X_MAX_PATH, dir, MAX_PATH)))
        return r;
    if (!CreateDirectoryW(path, NULL)) {
        r = x_cerror(GetLastError());
        if (r == EEXIST) {
            if (xtrace)
                warnx("cannot create directory '%S': %s",
                      path, cstrerror(EEXIST));
            return EEXIST;
        }
        else if (r == ENOENT) {
            return ENOENT;
        }
        else {
            if (xtrace)
                warnx("cannot create directory '%S': %s",
                      path, cstrerror(r));
            return r;
        }
    }
    else {
        if (xtrace > 1)
            warnx("created directory '%S'", path);
        return 0;
    }
}

static int dir_make_parent(char *path)
{
    int rv;
    char *ch = strrchr(path, '\\');
    if (!ch)
        return ENOENT;

    *ch = '\0';
    rv = x_createdir(path); /* Try to make straight off */

    if (rv == ENOENT) {
        /* Missing an intermediate dir */
        rv = dir_make_parent(path);
        if (rv == 0)
            rv = x_createdir(path); /* And complete the path */
    }

    *ch = '\\'; /* Always replace the slash before returning */
    return rv;
}

static int x_createdir_r(const char *path)
{
    int rv;
    char *dir = x_fullpath(path);

    rv = x_createdir(dir);
    if (rv == EEXIST) {
        /* It's OK if PATH exists */
        x_free(dir);
        return 0;
    }
    else if (rv == ENOENT) {        /* Missing an intermediate dir */
        rv = dir_make_parent(dir);  /* Make intermediate dirs */
        if (rv == 0) {
            rv = x_createdir(dir);  /* And complete the path */
        }
    }
    x_free(dir);
    return rv;
}

static int x_rmdir(const char *src)
{
    wchar_t path[X_MAX_PATH];
    int     r;
    char   *loc = x_fulldir(src);

    if ((r = utf8_to_unicode_path(path, X_MAX_PATH, loc, MAX_PATH))) {
        x_free(loc);
        return r;
    }
    if (!RemoveDirectoryW(path))
        r = x_cerror(GetLastError());
    if (xtrace) {
        if (r)
            warnx("cannot remove directory '%S': %s",
                  path, cstrerror(r));
        else
            warnx("removed directory '%S'", path);
    }
    x_free(loc);
    return r;
}

static int x_rmdir_r(const char *src)
{
    wchar_t path[X_MAX_PATH];
    int     r;
    int     full = 0;
    char   *loc = x_fulldir(src);
    SHFILEOPSTRUCTW op;

    if ((r = utf8_to_unicode_path(path, X_MAX_PATH - 4, loc, 0))) {
        x_free(loc);
        return r;
    }
    if (path[wcslen(path) - 1] == L'\\') {
        wcscat(path, L"*.*");
        path[wcslen(path) + 1] = L'\0';
        full = 1;
    }
    op.hwnd     = NULL;
    op.wFunc    = FO_DELETE;
    op.pFrom    = path;
    op.pTo      = NULL;
    op.fFlags   = FOF_NOCONFIRMATION |
                  FOF_NOERRORUI |
                  FOF_SILENT;

    r = x_cerror(SHFileOperationW(&op));
    if (r == 0 && full) {
        path[wcslen(path) - 4] = L'\0';
        if (!RemoveDirectoryW(path))
            r = x_cerror(GetLastError());
    }
    if (xtrace) {
        if (r)
            warnx("cannot remove directory '%s': %s",
                  loc, cstrerror(r));
        else
            warnx("removed directory '%s'", loc);
    }
    x_free(loc);
    return r;
}

static int x_rmfile(const char *src)
{
    wchar_t path[X_MAX_PATH];
    int     r;
    char   *loc = x_fullpath(src);
    SHFILEOPSTRUCTW op;

    if ((r = utf8_to_unicode_path(path, X_MAX_PATH, loc, 0))) {
        x_free(loc);
        return r;
    }
    op.hwnd     = NULL;
    op.wFunc    = FO_DELETE;
    op.pFrom    = path;
    op.pTo      = NULL;
    op.fFlags   = FOF_NOCONFIRMATION |
                  FOF_NOERRORUI |
                  FOF_FILESONLY |
                  FOF_SILENT;

    r = x_cerror(SHFileOperationW(&op));
    if (xtrace) {
        if (r)
            warnx("cannot remove file '%s': %s",
                  loc, cstrerror(r));
        else
            warnx("removed file '%s'", loc);
    }
    x_free(loc);
    return r;
}

static int x_fstat(LPWIN32_FILE_ATTRIBUTE_DATA pa,
                   const char *src)
{

    wchar_t path[X_MAX_PATH];
    int     r = 0;

    if ((r = x_wfullpath(path, X_MAX_PATH, src)))
        return r;
    if (!GetFileAttributesExW(path, GetFileExInfoStandard, pa)) {
        r = x_cerror(GetLastError());
        if (xtrace && r != ENOENT) {
            warnx("cannot stat file '%S': %s",
                  path, cstrerror(errno));
        }
    }
    return r;
}

static int x_fattrib(const char *src)
{

    WIN32_FILE_ATTRIBUTE_DATA pa;
    wchar_t path[X_MAX_PATH];
    int     r = 0;

    if ((errno = x_wfullpath(path, X_MAX_PATH, src))) {
        return 0;
    }
    if (!GetFileAttributesExW(path, GetFileExInfoStandard, &pa)) {
        errno = x_cerror(GetLastError());
        return 0;
    }
    if (pa.dwFileAttributes)
        return (int)pa.dwFileAttributes;
    else
        return FILE_ATTRIBUTE_ARCHIVE;
}


/*
 * ---------------------------------------------------------------------
 * end of file functions
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of ini
 * ---------------------------------------------------------------------
 */

static void ini_free(ini_section_t *ini)
{
    ini_section_t *p;
    while (p = ini) {
        ini_node_t *n;
        while (n = ini->nodes) {
            ini->nodes = n->next;
            x_free(n->key);
            x_free(n->val);
            x_free(n);
        }
        ini = p->next;
        x_free(p->name);
        x_free(p);
    }
}

static ini_section_t *ini_section_get(ini_section_t *ini, const char *name)
{
    ini_section_t *p;
    for (p = ini; p; p = p->next) {
        if (p->name && !stricmp(p->name, name))
            return p;
    }
    return NULL;
}

static ini_section_t *ini_section_new(ini_section_t *top)
{
    ini_section_t *ini = x_calloc(sizeof(ini_section_t));
    if (top) {
        *top->last = ini;
         top->last = &ini->next;
    }
    else
        ini->last = &ini->next;

    return ini;
}

static ini_node_t *ini_node_new(ini_section_t *ini)
{
    ini_node_t *node = x_calloc(sizeof(ini_node_t));
    if (ini->nodes) {
        *ini->nodes->last = node;
        ini->nodes->last  = &node->next;
    }
    else {
        node->last = &node->next;
        ini->nodes = node;
    }
    return node;
}

static ini_section_t *ini_load(const char *fname)
{
    FILE *fp;
    ini_section_t *top  = NULL;
    ini_section_t *ini  = NULL;
    ini_node_t    *node = NULL;
    char buffer[X_MAX_PATH];
    int counter = 0;
    int nextline = 0;

    if (!(fp = x_fopen(fname, "rt"))) {
        if (xtrace)
            warnx("'%s' %s", fname, cstrerror(errno));
        return NULL;
    }
    ini = top = ini_section_new(NULL);
    while (fgets(buffer, X_MAX_PATH, fp)) {
        char *section;
        char *line;
        /* Trim readed line */
        counter++;
        line = rtrim(buffer);
        if (nextline) {
            nextline = 0;
            if (!node || *line == '\0')
                continue;
            node->val = x_strcat(node->val, line);
            if (node->val[strlen(node->val) - 1] == '\\') {
                node->val[strlen(node->val) - 1] = '\0';
                nextline = 1;
            }
            if (!nextline) {
                node->val = expand_envars(node->val);
            }
            continue;
        }
        line = ltrim(buffer);
        /* Skip comments and empty lines*/
        if (*line == '\0' || *line == '#' || *line == ';')
            continue;
        if (*line == '[') {
            char *ends = strchr(++line, ']');
            if (!ends) {
                if (xtrace)
                    warnx("'%s' unterminated section at line %d",
                          fname, counter);
                goto cleanup;
            }
            *ends = '\0';
            section = rltrim(line);
            if (!(ini = ini_section_get(top, section))) {
                ini = ini_section_new(top);
                ini->name = x_strdup(section);
            }
            else {
                if (xtrace > 1)
                    warnx("'%s' duplicate section at line %d",
                          fname, counter);
            }
            continue;
        }
        else {
            char *val = NULL;
            char *equ = line;
            if (*equ == '=')
                equ++;
            if (equ = strchr(equ, '=')) {
                *equ++ = '\0';
                val = rltrim(equ);
                if (val[strlen(val) - 1] == '\\') {
                    val[strlen(val) - 1] = '\0';
                    nextline = 1;
                }
                if (!*val)
                    val = NULL;
            }
            line = rltrim(line);
            if (!*line) {
                /* Skip entries without keys **/
                if (xtrace > 1)
                    warnx("'%s' missing key at line %d",
                          fname, counter);
                nextline = 0;
                continue;
            }
            node = ini_node_new(ini);
            node->key = x_strdup(line);
            node->val = x_strdup(val);
            if (node->val && !nextline) {
                node->val = expand_envars(node->val);
            }
        }
    }
    fclose(fp);

    return top;
cleanup:
    fclose(fp);
    ini_free(top);
    return NULL;
}

/*
 * ---------------------------------------------------------------------
 * end of ini
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of utf-8
 * ---------------------------------------------------------------------
 */

static int utf8_to_unicode_path(wchar_t* retstr, size_t retlen,
                                const char* srcstr, size_t unc)
{
    /* TODO: The computations could preconvert the string to determine
     * the true size of the retstr, but that's a memory over speed
     * tradeoff that isn't appropriate this early in development.
     *
     * Allocate the maximum string length based on leading 4
     * characters of \\?\ (allowing nearly unlimited path lengths)
     * plus the trailing null, then transform /'s into \\'s since
     * the \\?\ form doesn't allow '/' path seperators.
     *
     * Note that the \\?\ form only works for local drive paths, and
     * \\?\UNC\ is needed UNC paths.
     */
    size_t srcremains = strlen(srcstr) + 1;
    wchar_t *t = retstr;

    /* leave an extra space for double zero */
    --retlen;
    /* This is correct, we don't twist the filename if it is will
     * definately be shorter than MAX_PATH.  It merits some
     * performance testing to see if this has any effect, but there
     * seem to be applications that get confused by the resulting
     * Unicode \\?\ style file names, especially if they use argv[0]
     * or call the Win32 API functions such as GetModuleName, etc.
     * Not every application is prepared to handle such names.
     *
     * Note that a utf-8 name can never result in more wide chars
     * than the original number of utf-8 narrow chars.
     */
    if (srcremains > unc) {
        if (srcstr[1] == ':' && (srcstr[2] == '/' || srcstr[2] == '\\')) {
            wcscpy (retstr, L"\\\\?\\");
            retlen -= 4;
            t += 4;
        }
        else if ((srcstr[0] == '/' || srcstr[0] == '\\')
              && (srcstr[1] == '/' || srcstr[1] == '\\')
              && (srcstr[2] != '?')) {
            /* Skip the slashes */
            srcstr += 2;
            srcremains -= 2;
            wcscpy (retstr, L"\\\\?\\UNC\\");
            retlen -= 8;
            t += 8;
        }
    }
    if (!MultiByteToWideChar(CP_UTF8, 0, srcstr, -1, t, retlen))
        return x_cerror(0);
    for (; *t; t++) {
        if (*t == L'/')
            *t = L'\\';
    }
    *t = L'\0';
    return 0;
}

static int unicode_to_utf8_path(char* retstr, size_t retlen,
                                const wchar_t* srcstr)
{
    /* Skip the leading 4 characters if the path begins \\?\, or substitute
     * // for the \\?\UNC\ path prefix, allocating the maximum string
     * length based on the remaining string, plus the trailing null.
     * then transform \\'s back into /'s since the \\?\ form never
     * allows '/' path seperators, and APR always uses '/'s.
     */
    if (srcstr[0] == L'\\' && srcstr[1] == L'\\' &&
        srcstr[2] == L'?'  && srcstr[3] == L'\\') {
        if (srcstr[4] == L'U' && srcstr[5] == L'N' &&
            srcstr[6] == L'C' && srcstr[7] == L'\\') {
            srcstr += 8;
            retstr[0] = '\\';
            retstr[1] = '\\';
            retlen -= 2;
            retstr += 2;
        }
        else {
            srcstr += 4;
        }
    }

    if (!WideCharToMultiByte(CP_UTF8, 0, srcstr, -1,
                             retstr, retlen, NULL, 0))
        return x_cerror(0);

    return 0;
}

static int utf8_to_unicode(wchar_t* retstr, size_t retlen,
                           const char* srcstr)
{
    wchar_t *t = retstr;

    /* leave an extra space for double zero */
    --retlen;
    /* This is correct, we don't twist the filename if it is will
     * definately be shorter than MAX_PATH.  It merits some
     * performance testing to see if this has any effect, but there
     * seem to be applications that get confused by the resulting
     * Unicode \\?\ style file names, especially if they use argv[0]
     * or call the Win32 API functions such as GetModuleName, etc.
     * Not every application is prepared to handle such names.
     *
     * Note that a utf-8 name can never result in more wide chars
     * than the original number of utf-8 narrow chars.
     */
    if (!MultiByteToWideChar(CP_UTF8, 0, srcstr, -1, retstr, retlen))
        return x_cerror(0);
    for (; *t; t++) ;
    *t = L'\0';
    return 0;
}

static int unicode_to_utf8(char* retstr, size_t retlen,
                           const wchar_t* srcstr)
{
    if (!WideCharToMultiByte(CP_UTF8, 0, srcstr, -1,
                             retstr, retlen, NULL, 0))
        return x_cerror(0);

    return 0;
}

/*
 * An internal function to convert an array of strings (either
 * a counted or NULL terminated list, such as an argv[argc] or env[]
 * list respectively) from wide Unicode strings to narrow utf-8 strings.
 * These are allocated from the MSVCRT's _CRT_BLOCK to trick the system
 * into trusting our store.
 */
int wastrtoastr(char const * const **retarr,
                wchar_t const * const *arr, int args)
{
    size_t elesize = 0;
    char **newarr;
    char *elements;
    char *ele;
    int arg;

    if (args < 0) {
        for (args = 0; arr[args]; ++args) {
            /* Nothing. */
        }
    }
    newarr = _malloc_dbg((args + 1) * sizeof(char *),
                         _CRT_BLOCK, __FILE__, __LINE__);

    for (arg = 0; arg < args; ++arg) {
        newarr[arg] = (void*)(wcslen(arr[arg]) + 1);
        elesize += (size_t)newarr[arg];
    }

    /* This is a safe max allocation, we will realloc after
     * processing and return the excess to the free store.
     * 3 ucs bytes hold any single wchar_t value (16 bits)
     * 4 ucs bytes will hold a wchar_t pair value (20 bits)
     */
    elesize = elesize * 3 + 1;
    ele = elements = _malloc_dbg(elesize * sizeof(char),
                                 _CRT_BLOCK, __FILE__, __LINE__);

    for (arg = 0; arg < args; ++arg) {
        size_t len = (size_t)newarr[arg];

        newarr[arg] = ele;
        len = WideCharToMultiByte(CP_UTF8, 0, arr[arg], -1,
                                  newarr[arg], len, NULL, 0);
        ele += len;
        assert(len);
    }

    newarr[arg] = NULL;
    *(ele++) = '\0';

    /* Return to the free store if the heap realloc is the least bit optimized
     */
    ele = _realloc_dbg(elements, ele - elements,
                       _CRT_BLOCK, __FILE__, __LINE__);

    if (ele != elements) {
        size_t diff = ele - elements;
        for (arg = 0; arg < args; ++arg) {
            newarr[arg] += diff;
        }
    }

    *retarr = newarr;
    return args;
}

/*
 * ---------------------------------------------------------------------
 * end of utf-8
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of Windows Registry
 * ---------------------------------------------------------------------
 */

#define SAFE_CLOSE_KEY(k) \
    if ((k) != NULL && (k) != INVALID_HANDLE_VALUE) {   \
        RegCloseKey((k));                               \
        (k) = NULL;                                     \
    }

typedef struct x_registry_t {
    HKEY        key;
    REGSAM      sam;
    WCHAR       name[256];
} x_registry_t;

static HKEY reg_rootnamed(const char *name)
{
    if (!strnicmp(name, "HKLM", 4))
        return HKEY_LOCAL_MACHINE;
    else if (!strnicmp(name, "HKCU", 4))
        return HKEY_CURRENT_USER;
    else if (!strnicmp(name, "HKCR", 4))
        return HKEY_CLASSES_ROOT;
    else if (!strnicmp(name, "HKCC", 4))
        return HKEY_CURRENT_CONFIG;
    else if (!strnicmp(name, "HKU", 3))
        return HKEY_USERS;
    else
        return NULL;
}

static REGSAM reg_flags(const char *s)
{
    REGSAM sam = KEY_QUERY_VALUE;

    if (strchr(s, 'a'))
        sam |= KEY_ALL_ACCESS;
    if (strchr(s, 'r'))
        sam |= KEY_READ;
    if (strchr(s, 'w'))
        sam |= KEY_WRITE;
    if (!(win_osver.dwMajorVersion == 5 &&
          win_osver.dwMinorVersion == 0)) {
        if (strstr(s, "32"))
            sam |= KEY_WOW64_32KEY;
        else if (strstr(s, "64"))
            sam |= KEY_WOW64_64KEY;
    }
    return sam;
}

x_registry_t *reg_open(const char *name, const char *sam)
{
    int   i;
    HKEY  r;
    char *p;
    x_registry_t *k;

    if (!(r = reg_rootnamed(name))) {
        errno = EINVAL;
        return NULL;
    }

    k = x_malloc(sizeof(x_registry_t));
    if (!(p = strchr(name, '\\'))) {
        x_free(k);
        errno = EINVAL;
        return NULL;
    }
    if ((i = utf8_to_unicode(k->name, 255, p + 1))) {
        x_free(k);
        errno = i;
        return NULL;
    }
    k->sam = reg_flags(sam);

    if ((i = RegOpenKeyExW(r, k->name, 0,
                           k->sam, &k->key)) != ERROR_SUCCESS) {
        i = x_cerror(i);
        x_free(k);
        errno = i;
        return NULL;
    }

    return k;
}

static int reg_delete(const char *name, const char *sam, int all, const char *value)
{
    int   i = ERROR_SUCCESS;
    HKEY  r;
    char *p;
    x_registry_t k;
    wchar_t v[256];
    wchar_t *s = NULL;

    if (!(r = reg_rootnamed(name))) {
        errno = EINVAL;
        return errno;
    }

    if (!(p = strchr(name, '\\'))) {
        errno = EINVAL;
        return errno;
    }
    if ((i = utf8_to_unicode(k.name, 255, p + 1))) {
        errno = i;
        return errno;
    }
    k.sam = reg_flags(sam);

    if ((s = wcsrchr(k.name, L'\\'))) {
        *(s++) = L'\0';
    }
    else {
        errno = EINVAL;
        return errno;
    }
    if ((i = RegOpenKeyExW(r, k.name, 0,
                           k.sam, &k.key)) != ERROR_SUCCESS) {
        errno = x_cerror(i);
        return errno;
    }
    if (value) {
        i = SHDeleteValueW(k.key, s, v);
    }
    else {
        if (all)
            i = SHDeleteKeyW(k.key, s);
        else
            i = SHDeleteEmptyKeyW(k.key, s);
    }
    SAFE_CLOSE_KEY(k.key);
    return i;
}

x_registry_t *reg_create(const char *name, const char *sam)
{
    DWORD c;
    int   i;
    HKEY  r;
    char *p;
    x_registry_t *k;

    if (!(r = reg_rootnamed(name))) {
        errno = EINVAL;
        return NULL;
    }

    k = x_malloc(sizeof(x_registry_t));
    if (!(p = strchr(name, '\\'))) {
        x_free(k);
        errno = EINVAL;
        return NULL;
    }
    if ((i = utf8_to_unicode(k->name, 255, p + 1))) {
        x_free(k);
        errno = i;
        return NULL;
    }
    k->sam = reg_flags(sam);

    if ((i = RegCreateKeyExW(r, k->name, 0, NULL, 0,
                k->sam, NULL, &k->key, &c)) != ERROR_SUCCESS) {
        i = x_cerror(i);
        x_free(k);
        errno = i;
        return NULL;
    }

    return k;
}


static void reg_close(x_registry_t *key)
{
    if (key) {
        SAFE_CLOSE_KEY(key->key);
        x_free(key);
    }
}

static int reg_type(x_registry_t *k, const char *name)
{
    int   rc;
    int   rt;
    wchar_t *wn;

    if (k && IS_INVALID_HANDLE(k->key)) {
        errno = EINVAL;
        return -1;
    }

    if (!(wn = x_wstrdup_utf8(name))) {
        errno = EINVAL;
        return -1;
    }
    if ((rc = (LONG)RegQueryValueExW(k->key, wn, NULL,
                        &rt, NULL, NULL)) != ERROR_SUCCESS) {
        errno = rc;
        rt    = -1;
    }
    x_free(wn);
    return rt;
}

static int reg_size(x_registry_t *k, const char *name)
{
    int   rc;
    int   rt;
    wchar_t *wn;

    if (k && IS_INVALID_HANDLE(k->key)) {
        errno = EINVAL;
        return -1;
    }

    if (!(wn = x_wstrdup_utf8(name))) {
        errno = EINVAL;
        return -1;
    }
    if ((rc = (LONG)RegQueryValueExW(k->key, wn, NULL,
                        NULL, NULL, &rt)) != ERROR_SUCCESS) {
        rt    = -1;
    }
    x_free(wn);
    return rt;
}

static const char *reg_stype(int type)
{
    switch (type) {
        case REG_BINARY:
            return "REG_BINARY";
        case REG_DWORD:
            return "REG_DWORD";
        case REG_EXPAND_SZ:
            return "REG_EXPAND_SZ";
        case REG_MULTI_SZ:
            return "REG_MULTI_SZ";
        case REG_QWORD:
            return "REG_QWORD";
        case REG_SZ:
            return "REG_SZ";
        case REG_DWORD_BIG_ENDIAN:
            return "REG_DWORD_BIG_ENDIAN";
        break;
    }
    return "UNKNOWN";
}

static int reg_ntype(const char *type)
{
    if (!stricmp(type, "REG_BINARY"))
        return REG_BINARY;
    else if (!stricmp(type, "REG_DWORD"))
        return REG_DWORD;
    else if (!stricmp(type, "REG_EXPAND_SZ"))
        return REG_EXPAND_SZ;
    else if (!stricmp(type, "REG_MULTI_SZ"))
        return REG_MULTI_SZ;
    else if (!stricmp(type, "REG_QWORD"))
        return REG_QWORD;
    else if (!stricmp(type, "REG_SZ"))
            return REG_SZ;
    else {
        errno = EINVAL;
        return REG_NONE;
    }
}


static char *reg_value(x_registry_t *k, const char *name, int sc)
{
    int   rc = 0;
    DWORD rt;
    DWORD rl, i;
    INT64 qw;
    wchar_t *wn;
    char tbuf[128];
    unsigned char *buff  = NULL;
    char          *value = NULL;
    wchar_t *wp;
    char    *cp;

    if (k && IS_INVALID_HANDLE(k->key)) {
        errno = EINVAL;
        return NULL;
    }
    if (!(wn = x_wstrdup_utf8(name))) {
        errno = EINVAL;
        return NULL;
    }
    if ((rc = (LONG)RegQueryValueExW(k->key, wn, NULL,
                        &rt, NULL, &rl)) != ERROR_SUCCESS) {
        goto cleanup;
    }
    buff = x_malloc((size_t)rl);
    if ((rc = (LONG)RegQueryValueExW(k->key, wn, NULL,
                        &rt, buff, &rl)) != ERROR_SUCCESS) {
        goto cleanup;
    }
    switch (rt) {
        case REG_SZ:
            value = x_strdup_utf8((wchar_t *)buff);
        break;
        case REG_MULTI_SZ:
            for (wp = (wchar_t *)buff; *wp; wp++) {
                while (*wp)
                    wp++;
                if (*(wp + 1) != L'\0')
                    *wp = sc;
            }
            value = x_strdup_utf8((wchar_t *)buff);
        break;
        case REG_EXPAND_SZ:
            value = expand_wenvars((wchar_t *)buff);
        break;
        case REG_DWORD:
            memcpy(&rt, buff, 4);
            value = x_strdup(itoa(rt, tbuf, 10));
        break;
        case REG_QWORD:
            memcpy(&qw, buff, 8);
            value = x_strdup(_i64toa(qw, tbuf, 10));
        break;
        case REG_BINARY:
            value = x_malloc(rl * 4 + 1);
            for (i = 0, cp = value; i < (rl - 1); i++) {
                sprintf(cp, "%02x, ", buff[i]);
                cp += 4;
            }
            sprintf(cp, "%02x", buff[i]);
        break;
        default:
            rc = EBADF;
        break;
    }

cleanup:
    x_free(wn);
    x_free(buff);
    errno = rc;
    return value;
}

static int reg_set(x_registry_t *k, const char *name,
                   int type, const char *value, int sc)
{
    int   rc = 0;
    DWORD rt, st = type;
    DWORD rl, i;
    INT64 qw;
    wchar_t *wn;
    unsigned char *buff  = NULL;
    wchar_t *wp, *p;

    if (k && IS_INVALID_HANDLE(k->key)) {
        errno = EINVAL;
        return errno;
    }
    if (!(wn = x_wstrdup_utf8(name))) {
        errno = EINVAL;
        return errno;
    }
    if ((rc = (LONG)RegQueryValueExW(k->key, wn, NULL,
                        &rt, NULL, &rl)) == ERROR_SUCCESS) {
        if (st != REG_NONE && st != rt) {
            rc = EINVAL;
            goto cleanup;
        }
        st = rt;
    }
    if (st == REG_NONE)
        st = REG_SZ;

    switch (st) {
        case REG_SZ:
        case REG_EXPAND_SZ:
            wp = x_wstrdup_utf8(value);
            rc = RegSetValueExW(k->key, wn, 0, st,
                                (const unsigned char *)wp,
                                (wcslen(wp) + 1) * sizeof(wchar_t));
            x_free(wp);
        break;
        case REG_MULTI_SZ:
            wp = x_wstrdup_utf8(value);
            rl = wcslen(wp);
            for (p = wp; *p; p++) {
                if (*p == sc)
                    *p = L'\0';
            }
            rc = RegSetValueExW(k->key, wn, 0, st,
                                (const unsigned char *)wp,
                                (rl + 2) * sizeof(wchar_t));
            x_free(wp);
        break;
        case REG_DWORD:
            i = atoi(value);
            rc = RegSetValueExW(k->key, wn, 0, st,
                                (const unsigned char *)&i, 4);
        break;
        case REG_QWORD:
            qw = _atoi64(value);
            rc = RegSetValueExW(k->key, wn, 0, st,
                                (const unsigned char *)&qw, 8);
        break;
        case REG_BINARY:

        break;
        default:
            rc = EBADF;
        break;
    }

cleanup:
    x_free(wn);
    x_free(buff);
    errno = rc;
    return errno;
}

/*
 * ---------------------------------------------------------------------
 * end of Windows Registry
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of Windows GUI
 * ---------------------------------------------------------------------
 */

#define MGUI_WINDOWS 32

HICON            gui_h16Icon    = NULL;
HICON            gui_h32Icon    = NULL;
HICON            gui_h48Icon    = NULL;
HMODULE          gui_hMSHTML    = NULL;
HWND             gui_DialogWnd  = NULL;
static HINSTANCE gui_hInstance  = NULL;
static HWND      gui_hMainWnd   = NULL;
static HMODULE   gui_hRichedit  = NULL;

static UINT      gui_ucNumLines = 3;
static CHAR      gui_szWndClass[MAX_PATH];


static HWND     gui_Windows[MGUI_WINDOWS];

static int
GuiRegisterWindow(HWND hWnd)
{
    int i;
    for (i = 0; i < MGUI_WINDOWS; i++) {
        if (!gui_Windows[i]) {
            gui_Windows[i] = hWnd;
            return i;
        }
    }
    return -1;
}

static void
GuiUnregisterWindow(int nIndex)
{
    if (nIndex >= 0 && nIndex < MGUI_WINDOWS)
        gui_Windows[nIndex] = NULL;
}

static BOOL
GuiInitialize()
{
    int i;
    INITCOMMONCONTROLSEX stCmn;

    CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
    gui_hInstance = GetModuleHandle(NULL);
    stCmn.dwSize  = sizeof(INITCOMMONCONTROLSEX);
    stCmn.dwICC   = ICC_WIN95_CLASSES | ICC_USEREX_CLASSES |
                    ICC_COOL_CLASSES | ICC_NATIVEFNTCTL_CLASS |
                    ICC_INTERNET_CLASSES | ICC_PAGESCROLLER_CLASS |
                    ICC_BAR_CLASSES;

    InitCommonControlsEx(&stCmn);
    gui_hRichedit   = LoadLibraryA("RICHED32.DLL");
    gui_hMSHTML     = LoadLibraryA("MSHTML.DLL");

    gui_h16Icon     = LoadImage(gui_hInstance, MAKEINTRESOURCE(IDI_MAINICON),
                                IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR);
    gui_h32Icon     = LoadImage(gui_hInstance, MAKEINTRESOURCE(IDI_MAINICON),
                                IMAGE_ICON, 32, 32, LR_DEFAULTCOLOR);
    gui_h48Icon     = LoadImage(gui_hInstance, MAKEINTRESOURCE(IDI_RHELICON),
                                IMAGE_ICON, 48, 48, LR_DEFAULTCOLOR);

    SystemParametersInfo(SPI_GETWHEELSCROLLLINES, 0,
                         &gui_ucNumLines, 0);
    for (i = 0; i < MGUI_WINDOWS; i++)
        gui_Windows[i] = NULL;
    return TRUE;
}

static BOOL
GuiTerminate()
{
    int i;
    for (i = 0; i < MGUI_WINDOWS; i++) {
        if (gui_Windows[i]) {
            SendMessage(gui_Windows[i], WM_CLOSE, 0, 0);
            gui_Windows[i] = NULL;
        }
    }
    FreeLibrary(gui_hRichedit);
    FreeLibrary(gui_hMSHTML);

    return TRUE;
}

/**
 * Load the resource string with the ID given, and return a
 * pointer to it.  Notice that the buffer is common memory so
 * the string must be used before this call is made a second time.
 */
static LPSTR
GuiLoadResource(
    UINT wID,
    UINT nBuf)
{
    static CHAR szBuf[4][SIZ_BUFLEN];
    if (nBuf > 3)
        return "";
    if (LoadStringA(gui_hInstance, wID, szBuf[nBuf], SIZ_BUFMAX) > 0)
        return szBuf[nBuf];
    else
        return "";
}

static void
GuiCenterWindow(HWND hWnd)
{
   RECT    rc, rw;
   int     cw, ch;
   int     px, py;

   /* Get the Height and Width of the child window */
   GetWindowRect(hWnd, &rc);
   cw = rc.right  - rc.left;
   ch = rc.bottom - rc.top;

   /* Get the limits of the 'workarea' */
   if (!SystemParametersInfo(SPI_GETWORKAREA,
                             sizeof(RECT),
                             &rw,
                             0)) {
      rw.left   = rw.top = 0;
      rw.right  = GetSystemMetrics(SM_CXSCREEN);
      rw.bottom = GetSystemMetrics(SM_CYSCREEN);
   }

   /* Calculate new X and Y position*/
   px = (rw.right  - cw) / 2;
   py = (rw.bottom - ch) / 2;
   SetWindowPos(hWnd, HWND_TOP, px, py, 0, 0,
                SWP_NOSIZE | SWP_SHOWWINDOW);
}

static BOOL
GuiBrowseForFolder(
    HWND hWnd,
    LPCSTR szTitle,
    LPSTR  szPath,
    int    iFlags)
{
    BOOL rv = FALSE;

    BROWSEINFO  bi;
    ITEMIDLIST *il , *ir;
    LPMALLOC    pMalloc;

    memset(&bi, 0, sizeof(BROWSEINFO));
    SHGetSpecialFolderLocation(hWnd, CSIDL_DRIVES, &il);

    bi.lpszTitle      = szTitle;
    bi.pszDisplayName = szPath;
    bi.hwndOwner      = hWnd;
    bi.ulFlags        = iFlags;
    bi.pidlRoot       = il;
    if ((ir = SHBrowseForFolder(&bi)) != NULL) {
        SHGetPathFromIDList(ir, szPath);
        rv = TRUE;
    }

    if (SHGetMalloc(&pMalloc)) {
        pMalloc->lpVtbl->Free(pMalloc, il);
        pMalloc->lpVtbl->Release(pMalloc);
    }
    return rv;
}

#define HBUF_SIZE    8192
#define MBUF_SIZE    4096

static SHOWHTMLDIALOGEXFN  *pfnShowHTMLDialog = NULL;

typedef struct {
    IMoniker       *lpImk;
    CHAR            szReturnVal[HBUF_SIZE];
} MC_HTML_DIALOG, *LPMC_HTML_DIALOG;

typedef struct DHTML_THREAD_PARAMS {
    BOOL             bDone;
    HWND             hParent;
    LPMC_HTML_DIALOG hHtmlDlg;
    WCHAR            szOptions[1024];
    WCHAR            szArguments[MBUF_SIZE];
} DHTML_THREAD_PARAMS;

static LPWSTR
AnsiToWide(
    LPCSTR szAnsi,
    LPWSTR szWide,
    DWORD  dwLength)
{

    if (!szWide) {
        dwLength = MultiByteToWideChar(CP_ACP, 0, szAnsi, -1, NULL, 0);
        if (dwLength == 0)
            return NULL;
        if (!(szWide = (LPWSTR)calloc(sizeof(WCHAR), (dwLength + 1))))
            return NULL;
    }
    if (MultiByteToWideChar(CP_ACP, 0, szAnsi, -1,
                            szWide, dwLength))
        return szWide;
    else
        return NULL;
}

static LPSTR
WideToAnsi(
    LPCWSTR szWide,
    LPSTR   szAnsi,
    DWORD   dwLength)
{
    if (!szAnsi) {
        dwLength = WideCharToMultiByte(CP_ACP, 0, szWide, -1,
                                       NULL, 0, NULL, NULL);
        if (dwLength == 0)
            return NULL;
        if (!(szAnsi = (LPSTR)calloc(sizeof(CHAR), (dwLength + 1))))
            return NULL;
    }

    if (WideCharToMultiByte(CP_ACP, 0, szWide, -1,
                            szAnsi, dwLength, NULL, NULL))
        return szAnsi;
    else
        return NULL;

}

static HANDLE
DHTMLDialogInit(
    HINSTANCE hResInstance,
    LPCSTR    szHtml)
{
    LPMC_HTML_DIALOG hDlg;
    OLECHAR  bstr[MAX_PATH];

    hDlg = (LPMC_HTML_DIALOG)calloc(1, sizeof(MC_HTML_DIALOG));

    if (!hDlg)
        return INVALID_HANDLE_VALUE;

    if (!gui_hMSHTML)
        goto cleanup;
    if (!pfnShowHTMLDialog)
        pfnShowHTMLDialog = (SHOWHTMLDIALOGEXFN*)GetProcAddress(
                                gui_hMSHTML,
                                "ShowHTMLDialogEx");
    if (!pfnShowHTMLDialog)
        goto cleanup;

    if (hResInstance) {
        CHAR szTemp[MAX_PATH];
        strcpy(szTemp, "res://");
        GetModuleFileNameA(hResInstance, szTemp + strlen(szTemp),
                           ARRAYSIZE(szTemp) - strlen(szTemp));
        if (*szHtml != '/')
            strcat(szTemp, "/");
        strcat(szTemp, szHtml);
        AnsiToWide(szTemp, bstr, ARRAYSIZE(bstr));
    }
    else
        AnsiToWide(szHtml, bstr, ARRAYSIZE(bstr));
    CreateURLMoniker(NULL, bstr, &hDlg->lpImk);

    if (!hDlg->lpImk)
        goto cleanup;
    return hDlg;
cleanup:

    free(hDlg);
    return INVALID_HANDLE_VALUE;
}

static void
DHTMLDialogClose(
    HANDLE hHtmlDlg)
{

    LPMC_HTML_DIALOG hDlg = (LPMC_HTML_DIALOG)hHtmlDlg;
    if (IS_INVALID_HANDLE(hHtmlDlg))
        return;
    hDlg->lpImk->lpVtbl->Release(hDlg->lpImk);
    free(hDlg);
}

static DWORD WINAPI
dhtmlRunThread(LPVOID lpParam)
{
    DWORD    rv = ERROR_SUCCESS;
    HRESULT  hr;
    VARIANT  varArgs, varReturn;
    DHTML_THREAD_PARAMS *pD = (DHTML_THREAD_PARAMS *)lpParam;
    LPMC_HTML_DIALOG hDlg   = pD->hHtmlDlg;

    if (IS_INVALID_HANDLE(pD->hHtmlDlg)) {
        rv = ERROR_INVALID_HANDLE;
        goto cleanup;
    }
    VariantInit(&varReturn);
    varArgs.vt = VT_BSTR;
    varArgs.bstrVal = SysAllocString(pD->szArguments);
    hr = (*pfnShowHTMLDialog)(pD->hParent,
                              hDlg->lpImk,
                              HTMLDLG_MODAL | HTMLDLG_VERIFY,
                              &varArgs,
                              pD->szOptions,
                              &varReturn);
    VariantClear(&varArgs);
    if (SUCCEEDED(hr)) {
        switch(varReturn.vt) {
            case VT_BSTR:
                WideToAnsi(varReturn.bstrVal, hDlg->szReturnVal, HBUF_SIZE);
            break;
        }
        VariantClear(&varReturn);
        rv = ERROR_SUCCESS;
    }
    else
        rv = GetLastError();
cleanup:
    pD->bDone = TRUE;
    ExitThread(rv);
    return rv;
}

static BOOL
DHTMLDialogRun(
    HWND   hParent,
    HANDLE hHtmlDlg,
    LPCSTR szTitle,
    DWORD  dwWidth,
    DWORD  dwHeight,
    DWORD  dwFlags,
    DWORD  dwTimeout,
    LPCSTR szArguments)
{
    DWORD  dwThreadId;
    HANDLE hThread;
    HWND   hWnd = NULL;
    DWORD  i;
    BOOL   rv = FALSE;
    DHTML_THREAD_PARAMS pD;
    int    w = -1;

    if (IS_INVALID_HANDLE(hHtmlDlg))
        return FALSE;
    pD.hParent  = hParent;
    pD.hHtmlDlg = hHtmlDlg;
    swprintf(pD.szOptions,
             L"scroll: no; status: no; help: no; dialogHeight: %dpx; dialogWidth: %dpx",
             dwHeight, dwWidth);
    if (dwFlags == 1)
        wcscat(pD.szOptions, L"; unadorned:yes");
    if (szArguments)
        AnsiToWide(szArguments, pD.szArguments,
                   ARRAYSIZE(pD.szArguments));

    hThread = CreateThread(NULL,
                           0,
                           dhtmlRunThread,
                           &pD,
                           0,
                           &dwThreadId);
    if (IS_INVALID_HANDLE(hThread))
        return FALSE;

    /* Hack to change the Icon of HTML Dialog */
    for (i = 0; i < 1000; i++) {
        if ((hWnd = FindWindowEx(hParent, NULL, NULL, szTitle))) {
            w = GuiRegisterWindow(hWnd);
            SetClassLong(hWnd, GCL_HICONSM, (LONG)gui_h16Icon);
            break;
        }
        if (pD.bDone)
            break;
        Sleep(1);
    }
    if ((i = WaitForSingleObject(hThread, dwTimeout)) == WAIT_TIMEOUT) {
        if (hWnd) {
            SendMessage(hWnd, WM_CLOSE, 0, 0);
            WaitForSingleObject(hThread, INFINITE);
        }
    }
    if (GetExitCodeThread(hThread, &i) && i == 0) {
        rv = TRUE;
    }
    CloseHandle(hThread);
    GuiUnregisterWindow(w);
    return rv;
}

LPSTR
DHTMLDialogResult(
    HANDLE hHtmlDlg)
{

    LPMC_HTML_DIALOG hDlg = (LPMC_HTML_DIALOG)hHtmlDlg;
    if (IS_INVALID_HANDLE(hHtmlDlg))
        return NULL;

    return hDlg->szReturnVal;
}

static LPSTR
GetHtmlPath(
    LPCSTR szWorkDir,
    LPCSTR szHtmlPage)
{

    LPSTR p;
    LPSTR pHtml = (LPSTR)x_malloc(MAX_PATH);

    strcpy(pHtml, "file://");
    if (szWorkDir) {
        strcpy(pHtml, szWorkDir);
        if (*szHtmlPage != '/')
            strcat(pHtml, "/");
    }
    strcat(pHtml, szHtmlPage);
    for (p = pHtml; *p; p++) {
        if (*p == '\\')
            *p = '/';
    }
    return pHtml;
}

static BOOL
RunCustomPage(
    LPCSTR szHtmlPage,
    LPCSTR szHtmlTitle,
    LPCSTR szHtmlParams,
    DWORD  dwHtmlWidth,
    DWORD  dwHtmlHeight,
    DWORD  dwTimeout,
    DWORD  dwFlags,
    LPCSTR szFile)
{
    HANDLE hHtml;
    LPSTR  szUrl;
    LPSTR  retVal;
    BOOL   rv = FALSE;
    FILE  *fp;

    szUrl = GetHtmlPath(NULL, szHtmlPage);
    hHtml = DHTMLDialogInit(NULL, szUrl);
    if (IS_INVALID_HANDLE(hHtml)) {
        x_free(szUrl);
        return FALSE;
    }
    DHTMLDialogRun(NULL, hHtml,
                   szHtmlTitle,
                   dwHtmlWidth,
                   dwHtmlHeight,
                   dwFlags,
                   dwTimeout,
                   szHtmlParams);
    retVal = DHTMLDialogResult(hHtml);
    if (szFile)
        fp = fopen(szFile, "w");
    else
        fp = stdout;
    if (fp) {
        if (retVal && *retVal) {
            if (!strncmp(retVal, "OK", 2))
                rv = TRUE;
            fputs(retVal, fp);
        }
        else
            fputs("NULL", fp);
        fflush(fp);
        if (fp != stdout)
            fclose(fp);
    }
    x_free(szUrl);
    DHTMLDialogClose(hHtml);
    return rv;
}

/*
 * ---------------------------------------------------------------------
 * end of Windows GUI
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of tests
 * ---------------------------------------------------------------------
 */

static void test_getopt(int argc, const char **argv)
{
    int ch;

    while ((ch = getopt(argc, argv, "s:V:g:r:qLRS-", 0)) != EOF) {
        printf("Valid argument = %c [%s]\n", ch, optarg);
    }
    argc -= optind;
    argv += optind;
    for (ch = 0; ch < argc; ch++)
        printf("Option %d is %s\n", ch, argv[ch]);
}

static int test_path()
{
    char *test[] = {
        "test",
        "\\test",
        "\\\\?\\UNC\\C:\\..\\test/foo",
        "//C://../../..\\..\\..\\..\\*/foo",
        "C:\\W\\tools\\foo\\..\\..\\*/foo/barsalllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll/dddddddddddddddddddddddddddddddddddddddddddddddddddddddddaAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
        "./",
        NULL
    };
    char **p;
    for (p = test; *p; p++) {
        printf("Full path is: %s\n", x_fulldir(*p));
    }
    printf("Wide is %S\n", x_wstrdup_utf8("Ax"));
    return 0;
}

static int test_ini()
{

    ini_section_t *ini = ini_load("test.ini");
    ini_section_t *top = ini;
    while (ini) {
        ini_node_t *node = ini->nodes;
        printf("Section [%s]\n", ini->name);
        while (node) {
            printf("  %s=%s\n", node->key, node->val);
            node = node->next;
        }
        ini = ini->next;
    }
    ini_free(top);
    return 0;
}

/*
 * ---------------------------------------------------------------------
 * end of tests
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of user interface
 * ---------------------------------------------------------------------
 */

static const char *programs[] = {
    "mkdir",    "Creates a new directory(ies)",
    "rmdir",    "Removes directory(ies)",
    "touch",    "Change file timestamps",
    "exec",     "Execute program",
    "mktemp",   "Make temporary filename (unique)",
    "image",    "Executable image information",
    "html",     "Display html page",
    "reg",      "Registry tool",
    "msg",      "Display MessageBox",
    "jdk",      "Find JDK/JRE location",
    "dir",      "Browse for File or Folder",
    "xcopy",    "Copy files or folders with progress",
    NULL, NULL
};

static const char str_license[] = ""
"Licensed to the Apache Software Foundation (ASF) under one or more\n"
"contributor license agreements.  See the NOTICE file distributed with\n"
"this work for additional information regarding copyright ownership.\n"
"The ASF licenses this file to You under the Apache License, Version 2.0\n"
"(the \"License\"); you may not use this file except in compliance with\n"
"the License.  You may obtain a copy of the License at\n"
"\n"
"     http://www.apache.org/licenses/LICENSE-2.0\n"
"\n"
"Unless required by applicable law or agreed to in writing, software\n"
"distributed under the License is distributed on an \"AS IS\" BASIS,\n"
"WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
"See the License for the specific language governing permissions and\n"
"limitations under the License.\n\n";

static int print_banner(int license)
{
    fprintf(stdout, "xtools (Windows generic build tools) version 1.0.0\n");
    fprintf(stdout, "Written by Mladen Turk (mturk@redhat.com).\n\n");
    if (license)
        fprintf(stdout, "%s", str_license);
    return 0;
}

static void print_programs()
{
    const char **p;
    fprintf(stderr, "Where supported commands are:\n\n");
    for (p = programs; *p; p += 2) {
        fprintf(stderr, " %-16s%s\n", *p, *(p+1));
    }
}

static void print_stdusage()
{
    fprintf(stderr, " -q               Be quiet.\n");
    fprintf(stderr, " -v    --verbose  Explain what is being done.\n");
    fprintf(stderr, "       --version  Output version information and exit.\n");
    fprintf(stderr, " -V               Explain what is being done in detail.\n");
    fprintf(stderr, " -h    --help     Display this help and exit.\n\n");
}

/*
 * ---------------------------------------------------------------------
 * end of user interface
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of programs usage
 * ---------------------------------------------------------------------
 */

static int prog_mkdir_usage(int retval)
{
    fprintf(stderr, "Usage: %s [OPTION]... DIRECTORY...\n", progname);
    fprintf(stderr, "Create the DIRECTORY(ies), if they do not already exist.\n");
    fprintf(stderr, "Any intermediate directories in the path are created, if needed\n\n");
    print_stdusage();
    return retval;
}

static int prog_rmdir_usage(int retval)
{
    fprintf(stderr, "Usage: %s [OPTION]... DIRECTORY...\n", progname);
    fprintf(stderr, "Remove (unlink) the DIRECTORY(ies).\n\n");
    fprintf(stderr, " -r               remove the contents of directories recursively.\n\n");
    print_stdusage();
    return retval;
}

static int prog_touch_usage(int retval)
{
    fprintf(stderr, "Usage: %s [OPTION]... FILE...\n", progname);
    fprintf(stderr, "Update the access and modification times of each FILE to the current time\n\n");
    fprintf(stderr, " -a               change only the access time.\n");
    fprintf(stderr, " -c               do not create any files.\n");
    fprintf(stderr, " -d    STRING     parse STRING and use it instead of current time.\n");
    fprintf(stderr, " -m               change only the modification time.\n");
    fprintf(stderr, " -r    FILE       use this file time's instead current time.\n");
    fprintf(stderr, " -t    STAMP      use [[CC]YY]MMDDhhmm[.ss] instead of current time.\n\n");
    print_stdusage();
    return retval;
}

static int prog_exec_usage(int retval)
{
    fprintf(stderr, "Usage: %s [OPTION]... program [ARGUMENTS]\n", progname);
    fprintf(stderr, "Execute program.\n\n");
    fprintf(stderr, " -c               Replace cygwin paths.\n");
    fprintf(stderr, " -s               Replace SUA paths.\n");
    fprintf(stderr, " -r               Replace relative paths.\n");
    fprintf(stderr, " -e               Keep existing environment.\n");
    fprintf(stderr, " -f               Use forward slashes.\n\n");
    print_stdusage();
    return retval;
}

static int prog_mktemp_usage(int retval)
{
    fprintf(stderr, "Usage: %s [OPTION]... [TEMPLATE]\n", progname);
    fprintf(stderr, "Make temporary file (unique).\n\n");
    fprintf(stderr, " -d               Make a directory instead file.\n");
    fprintf(stderr, " -u               Unlink file or directory befor exiting.\n\n");
    print_stdusage();
    return retval;
}

static int prog_image_usage(int retval)
{
    fprintf(stderr, "Usage: %s [OPTION]... FILE...\n", progname);
    fprintf(stderr, "Get information about PE image\n\n");
    fprintf(stderr, " -a               Print all information.\n");
    fprintf(stderr, " -l               Print long information.\n");
    fprintf(stderr, " -c               Characteristcs (numeric).\n");
    fprintf(stderr, " -C               Characteristcs (described).\n");
    fprintf(stderr, " -d               Provided file is DLL.\n");
    fprintf(stderr, " -m               Architecture type of the image\n");
    fprintf(stderr, " -t               Date and time the image was created by the linker\n");
    fprintf(stderr, " -T               Strftime format for -t\n");
    fprintf(stderr, " -s               Subsystem required to run this image\n");
    fprintf(stderr, " -S               Number of bytes to commit for the stack\n");
    fprintf(stderr, " -b               Preferred address of the image when it is loaded in memory.\n");
    fprintf(stderr, " -L               Linker version used.\n");
    fprintf(stderr, " -o               Required operating system.\n");
    fprintf(stderr, " -i               Version number of the image.\n");
    fprintf(stderr, " -f               File Version (64-Bit number).\n");
    fprintf(stderr, " -F               File Version (from Resource).\n");
    fprintf(stderr, " -p               Product Version (64-Bit number).\n");
    fprintf(stderr, " -P               Product Version (from Resource).\n\n");
    print_stdusage();
    return retval;
}

static int prog_html_usage(int retval)
{
    fprintf(stderr, "Usage: %s [OPTION]... FILE\n", progname);
    fprintf(stderr, "Update the access and modification times of each FILE to the current time\n\n");
    fprintf(stderr, " -g    STRING     Geometry in format 'widthXheight'.\n");
    fprintf(stderr, " -n    STRING     Page title. If not present defaults to html <title> tag.\n");
    fprintf(stderr, " -t    NUMBER     Splash page with NUMBER of miliseconds delay.\n\n");
    fprintf(stderr, " -T    NUMBER     Splash page with NUMBER of seconds delay.\n\n");
    print_stdusage();
    return retval;
}

static int prog_reg_usage(int retval)
{
    fprintf(stderr, "Usage: %s [OPTION]... OPERATION KEY [VALUE]\n", progname);
    fprintf(stderr, "Registry tool\n\n");
    fprintf(stderr, " OPERATION\n");
    fprintf(stderr, " query VALUE      Query Registry Key Value\n");
    fprintf(stderr, " add  [VALUE]     Add Registry Key\n");
    fprintf(stderr, " del  [VALUE]     Delete Registry Key or Value\n");
    fprintf(stderr, " type  VALUE      Query Registry Key Value type\n");
    fprintf(stderr, " size  VALUE      Query Registry Key Value size\n");
    fprintf(stderr, " enum  VALUE      Enumerate Registy Key\n");
    fprintf(stderr, "                  VALUE can be 'key', 'value' or 'all'\n\n");
    fprintf(stderr, " -a               Delete all keys and subkeys.\n");
    fprintf(stderr, " -m    STRING     Registry access mode.\n");
    fprintf(stderr, "                  a     all access\n");
    fprintf(stderr, "                  r     read access\n");
    fprintf(stderr, "                  w     wite access\n");
    fprintf(stderr, "                  32    operate on the 32-bit registry view\n");
    fprintf(stderr, "                  64    operate on the 64-bit registry view\n");
    fprintf(stderr, " -t    STRING     Registry type.\n");
    fprintf(stderr, "                  REG_SZ\n");
    fprintf(stderr, "                  REG_EXPAND_SZ\n");
    fprintf(stderr, "                  REG_MULTI_SZ\n");
    fprintf(stderr, "                  REG_DWORD\n");
    fprintf(stderr, "                  REG_QWORD\n");
    fprintf(stderr, " -f    CHAR       Field separator for REG_MULTI_SZ.\n");
    fprintf(stderr, " -d    STRING     Data to be used for add operation.\n\n");
    print_stdusage();
    return retval;
}

static int prog_msg_usage(int retval)
{
    fprintf(stderr, "Usage: %s [OPTION]... CAPTION TEXT\n", progname);
    fprintf(stderr, "Display message box\n\n");
    fprintf(stderr, " -s               System-modal message box.\n");
    fprintf(stderr, " -t    STRING     Message box type:\n");
    fprintf(stderr, "                    MB_ABORTRETRYIGNORE\n");
    fprintf(stderr, "                    MB_CANCELTRYCONTINUE\n");
    fprintf(stderr, "                    MB_HELP\n");
    fprintf(stderr, "                    MB_OK\n");
    fprintf(stderr, "                    MB_OKCANCEL\n");
    fprintf(stderr, "                    MB_RETRYCANCEL\n");
    fprintf(stderr, "                    MB_YESN\n");
    fprintf(stderr, "                    MB_YESNOCANCEL\n");
    fprintf(stderr, " -i    CHAR       Message box icon:\n");
    fprintf(stderr, "                  '!' Exclamation-point icon appears in the message box\n");
    fprintf(stderr, "                  'w' Exclamation-point icon appears in the message box\n");
    fprintf(stderr, "                  'i' Letter i in a circle appears in the message box\n");
    fprintf(stderr, "                  's' Stop-sign icon appears in the message box\n\n");
    fprintf(stderr, "Return value is set depending on the key pressed\n");
    fprintf(stderr, "  OK           1\n");
    fprintf(stderr, "  CANCEL       2\n");
    fprintf(stderr, "  ABORT        3\n");
    fprintf(stderr, "  RETRY        4\n");
    fprintf(stderr, "  IGNORE       5\n");
    fprintf(stderr, "  YES          6\n");
    fprintf(stderr, "  NO           7\n");
    fprintf(stderr, "  CLOSE        8\n");
    fprintf(stderr, "  HELP         9\n");
    fprintf(stderr, "  TRYAGAIN     10\n");
    fprintf(stderr, "  CONTINUE     11\n\n");
    print_stdusage();
    return retval;
}

static int prog_jdk_usage(int retval)
{
    fprintf(stderr, "Usage: %s [OPTION]... [PATH]\n", progname);
    fprintf(stderr, "Find java SDK or JRE paths\n\n");
    fprintf(stderr, " -e               Use environment variable.\n");
    fprintf(stderr, " -E    STRING     Use environment variable instead JAVA_HOME.\n");
    fprintf(stderr, " -j               Find JRE insted of JDK.\n");
    fprintf(stderr, " -d               Find jvm.dll insted location.\n");
    fprintf(stderr, " -s               Favor server JRE version.\n");
    fprintf(stderr, " -r    STRING     Registry access mode.\n");
    fprintf(stderr, "                  a     all access\n");
    fprintf(stderr, "                  r     read access\n");
    fprintf(stderr, "                  32    operate on the 32-bit registry view\n");
    fprintf(stderr, "                  64    operate on the 64-bit registry view\n\n");
    print_stdusage();
    return retval;
}

static int prog_browse_usage(int retval)
{
    fprintf(stderr, "Usage: %s [OPTION]... TITLE\n", progname);
    fprintf(stderr, "Browse for Folders (or Files)\n\n");
    fprintf(stderr, " -f               Show Files as well.\n");
    fprintf(stderr, " -r               Read-only. Do not show New Folder button.\n\n");
    print_stdusage();
    return retval;
}

static int prog_xcopy_usage(int retval)
{
    fprintf(stderr, "Usage: %s [OPTION]... TITLE SOURCE DESTINATION\n", progname);
    fprintf(stderr, "Copies files from SOURCE to DESTINATION\n\n");
    print_stdusage();
    return retval;
}

/*
 * ---------------------------------------------------------------------
 * end of programs usage
 * ---------------------------------------------------------------------
 */

/*
 * ---------------------------------------------------------------------
 * begin of programs
 * ---------------------------------------------------------------------
 */

static int prog_mkdir(int argc, const char **argv, const char **env)
{
    int i, ch, rv = 0;

    while ((ch = getopt(argc, argv, "hqvV", 1)) != EOF) {
        switch (ch) {
            case '.':
                if (!stricmp(optarg, "verbose"))
                    xtrace = 1;
                else if (!stricmp(optarg, "version"))
                    return print_banner(1);
                else if (!stricmp(optarg, "help"))
                    return prog_mkdir_usage(0);
                else
                    return prog_mkdir_usage(EINVAL);
            break;
            case 'v':
                xtrace  = 1;
            break;
            case 'V':
                xtrace  = 9;
            break;
            case 'q':
                xquiet = 1;
            break;
            case 'h':
                return prog_mkdir_usage(0);
            break;
            case '?':
            case ':':
                return EINVAL;
            break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc < 1) {
        return prog_mkdir_usage(EINVAL);
    }

    for (i = 0; i < argc; i++) {
        if ((rv = x_createdir_r(argv[0])))
            break;
    }
    return rv;
}

static int prog_rmdir(int argc, const char **argv, const char **env)
{
    int i, ch, rv = 0;
    int recurse = 0;

    while ((ch = getopt(argc, argv, "rhqvV", 1)) != EOF) {
        switch (ch) {
            case '.':
                if (!stricmp(optarg, "verbose"))
                    xtrace = 1;
                else if (!stricmp(optarg, "version"))
                    return print_banner(1);
                else if (!stricmp(optarg, "help"))
                    return prog_rmdir_usage(0);
                else
                    return prog_rmdir_usage(EINVAL);
            break;
            case 'v':
                xtrace  = 1;
            break;
            case 'V':
                xtrace  = 9;
            break;
            case 'q':
                xquiet  = 1;
            break;
            case 'h':
                return prog_rmdir_usage(0);
            break;
            case 'r':
                recurse = 1;
            break;
            case '?':
            case ':':
                return EINVAL;
            break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc < 1) {
        return prog_rmdir_usage(EINVAL);
    }
    for (i = 0; i < argc; i++) {
        if (recurse) {
            if ((rv = x_rmdir_r(argv[i])))
                break;
        }
        else {
            if ((rv = x_rmdir(argv[i])))
                break;
        }
    }
    return rv;
}

static int prog_touch(int argc, const char **argv, const char **env)
{
    int i, ch, rv = 0;
    int force  = 0;
    int flags  = 0;
    DWORD create = OPEN_ALWAYS;
    const char *from = NULL;
    const char *tstr = NULL;
    wchar_t file[X_MAX_PATH];
    WIN32_FILE_ATTRIBUTE_DATA ad;

    while ((ch = getopt(argc, argv, "acfmr:t:hqvV", 1)) != EOF) {
        switch (ch) {
            case '.':
                if (!stricmp(optarg, "verbose"))
                    xtrace = 1;
                else if (!stricmp(optarg, "version"))
                    return print_banner(1);
                else if (!stricmp(optarg, "help"))
                    return prog_touch_usage(0);
                else
                    return prog_touch_usage(EINVAL);
            break;
            case 'v':
                xtrace  = 1;
            break;
            case 'V':
                xtrace  = 9;
            break;
            case 'q':
                xquiet = 1;
            break;
            case 'h':
                return prog_touch_usage(0);
            break;
            case 'f':
                force = 1;
            case 'c':
                create = OPEN_EXISTING;
            break;
            case 'a':
                flags |= 1;
            break;
            case 'm':
                flags |= 2;
            break;
            case 'r':
                from = optarg;
            break;
            case 't':
                tstr = optarg;
            break;
            case '?':
            case ':':
                return EINVAL;
            break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc < 1) {
        return prog_touch_usage(EINVAL);
    }
    if (!flags)
        flags = 3;
    if (from) {
        if (rv = x_fstat(&ad, from)) {
            if (xtrace) {
                x_wfullpath(file, X_MAX_PATH, from);
                warnx("'%S' -- %s", file, cstrerror(rv));
            }
            return rv;
        }
    }
    else if (tstr) {
        SYSTEMTIME t;
        size_t tl = strlen(tstr);
        char *sp = strchr(tstr, '.');
        int cen = 0;
        int r = 0;
        memset(&t, 0, sizeof(SYSTEMTIME));
        if (sp)
            tl -= (size_t)(sp - tstr);
        if (tl == 12)
            r = sscanf(tstr, "%4d%2d%2d%2d%2d",
                       &t.wYear, &t.wMonth,
                       &t.wDay, &t.wHour, &t.wMinute);
        else if (tl == 10)
            r = sscanf(tstr, "%2d%2d%2d%2d%2d",
                       &t.wYear, &t.wMonth,
                       &t.wDay, &t.wHour, &t.wMinute);
        else if (tl == 8)
            r = sscanf(tstr, "%2d%2d%2d%2d",
                       &t.wMonth,
                       &t.wDay, &t.wHour, &t.wMinute);

        else {
            if (xtrace)
                warnx("invalid time format %s", tstr);
            return EINVAL;
        }
        if (sp)
            t.wSecond = atoi(sp + 1);
        if (t.wYear < 100) {
            SYSTEMTIME n;
            GetSystemTime(&n);
            if (t.wYear)
                t.wYear = (n.wYear - n.wYear % 100) + t.wYear;
            else
                t.wYear = n.wYear;
        }
        if (!SystemTimeToFileTime(&t, &ad.ftLastAccessTime)) {
            if (xtrace)
                warnx("invalid time format %04d-%02d-%02d-%02d-%02d.%02d",
                      t.wYear, t.wMonth, t.wDay, t.wHour,
                      t.wMinute, t.wSecond);
            return EINVAL;
        }
        ad.ftLastWriteTime = ad.ftLastAccessTime;
    }
    else {
        GetSystemTimeAsFileTime(&ad.ftLastAccessTime);
        ad.ftLastWriteTime = ad.ftLastAccessTime;
    }
    for (i = 0; i < argc; i++) {
        HANDLE hf;
        if ((rv = x_wfullpath(file, X_MAX_PATH, argv[i]))) {
            if (xtrace) {
                warnx("'%s' -- %s", argv[i], cstrerror(rv));
            }
            return rv;
        }
        hf = CreateFileW(file, GENERIC_READ | GENERIC_WRITE,
                         0, NULL, create,
                         FILE_ATTRIBUTE_NORMAL, NULL);
        if (hf == INVALID_HANDLE_VALUE) {
            rv = x_cerror(0);
            if (xtrace) {
                warnx("'%S' -- %s", file, cstrerror(rv));
            }
            return rv;
        }
        else {
            FILETIME *at = NULL;
            FILETIME *mt = NULL;
            FILETIME *ct = NULL;
            if (flags & 1)
                at = &ad.ftLastAccessTime;
            if (flags & 2)
                mt = &ad.ftLastWriteTime;
            GetFileTime(hf, &ad.ftCreationTime, NULL, NULL);
            if (at && CompareFileTime(&ad.ftCreationTime, at) > 0) {
                /* File creation time is higher then access
                 * time we wish to set
                 */
                 at = NULL;
            }
            if (mt && CompareFileTime(&ad.ftCreationTime, mt) > 0) {
                /* File creation time is higher then modification
                 * time we wish to set
                 */
                 mt = NULL;
            }
            if (!SetFileTime(hf, NULL, at, mt)) {
                rv = x_cerror(0);
                if (xtrace)
                    warnx("'%S' -- %s", file, cstrerror(rv));
            }
            if (xtrace) {
                if (at || mt)
                    warnx("'%S' modified.", file);
                else
                    warnx("'%S' not modified.", file);
            }
            CloseHandle(hf);
        }
    }
    return rv;
}

static BOOL WINAPI console_handler(DWORD ctrl)
{
    switch (ctrl) {
        case CTRL_BREAK_EVENT:
            return FALSE;
        case CTRL_C_EVENT:
        case CTRL_CLOSE_EVENT:
        case CTRL_SHUTDOWN_EVENT:
        case CTRL_LOGOFF_EVENT:
            return TRUE;
        break;
    }
    return FALSE;
}

static int prog_exec(int argc, const char **argv, const char **env)
{
    int i, ch, rv = 0;
    int drive   = 0;
    int cygwin  = 0;
    int suawin  = 0;
    int keepenv = 0;
    int relpath = 0;
    int back    = 1;
    size_t l, envsize;
    char **mainenvp = NULL;
    char **mainargv = NULL;
    char wcpath[MAX_PATH + 32] = { 0 };

    while ((ch = getopt(argc, argv, "cefrshqV", 1)) != EOF) {
        switch (ch) {
            case '.':
                if (!stricmp(optarg, "verbose"))
                    xtrace = 1;
                else if (!stricmp(optarg, "version"))
                    return print_banner(1);
                else if (!stricmp(optarg, "help"))
                    return prog_exec_usage(0);
                else
                    return prog_exec_usage(EINVAL);
            break;
            case 'v':
                xtrace  = 1;
            break;
            case 'V':
                xtrace  = 9;
            break;
            case 'q':
                xquiet  = 1;
            break;
            case 'h':
                return prog_exec_usage(0);
            break;
            case 'c':
                cygwin  = 1;
            break;
            case 's':
                suawin  = 1;
            break;
            case 'e':
                keepenv = 1;
            break;
            case 'r':
                relpath = 1;
            break;
            case 'f':
                back = 0;
                windrive[2] = '/';
            break;
            case '?':
            case ':':
                return EINVAL;
            break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc < 1) {
        return prog_exec_usage(EINVAL);
    }
    if (!stricmp(argv[0], "java") ||
        !stricmp(argv[0], "jar")  ||
        !stricmp(argv[0], "javac")) {
        char *jhome = x_strdup(getenv("JAVA_HOME"));
        if (!jhome) {
            /* TODO: Figure out the paths from
             * the registry
             */
            rv = x_perror(ENOENT, "JAVA_HOME environment");
            goto cleanup;
        }
        strcpy(wcpath, jhome);
        strcat(wcpath, "\\bin\\");
        strcat(wcpath, argv[0]);
        strcat(wcpath, ".exe");
        x_free(jhome);
    }
    else if (!stricmp(argv[0], "cc")   ||
             !stricmp(argv[0], "make") ||
             !stricmp(argv[0], "lib")  ||
             !stricmp(argv[0], "link")) {
        if (!getmsvcpath()) {
            rv = x_perror(ENOENT, "Microsoft compiler");
            goto cleanup;
        }
        strcpy(wcpath, getmsvcpath());
        if (!stricmp(argv[0], "cc"))
            strcat(wcpath, "\\cl.exe");
        else if (!stricmp(argv[0], "make"))
            strcat(wcpath, "\\nmake.exe");
        else if (!stricmp(argv[0], "lib"))
            strcat(wcpath, "\\lib.exe");
        else
            strcat(wcpath, "\\link.exe");
    }

    if (cygwin) {
        if (!(posixroot = getcygdrive(&drive, back))) {
            rv = x_perror(ENOENT, "Cygwing drive");
            goto cleanup;
        }
    }
    else if (suawin) {
        if (!(posixroot = getsuadrive(&drive, back))) {
            rv = x_perror(ENOENT, "Posix drive");
            goto cleanup;
        }
    }
    windrive[0] = drive;
    if (wcpath[0]) {
        *argv = wcpath;
    }
    mainargv = a_alloc(argc);
    for (i = 0; i < argc ; i++) {
        if (cygwin || suawin)
            mainargv[i] = posix2win(argv[i], back);
        else {
            mainargv[i] = x_strdup(argv[i]);
            if (!back)
                x_forwardslash(mainargv[i]);
        }
        if (relpath && back)
            reprelative(mainargv[i]);
    }
    envsize  = a_size(env);
    mainenvp = a_alloc(envsize);
    for (l = 0; l < envsize; l++) {
        if (keepenv)
            mainenvp[l] = x_strdup(env[l]);
        else {
            if (cygwin || suawin)
                mainenvp[l] = posix2win(env[l], back);
            else {
                mainenvp[l] = x_strdup(env[l]);
                if (!back)
                    x_forwardslash(mainenvp[l]);
            }
        }
    }
    /* We have a valid environment. Install the console handler */
    SetConsoleCtrlHandler((PHANDLER_ROUTINE)console_handler, TRUE);

    if (checkbinary(mainargv[0])) {
        if (xtrace > 1)
            fprintf(stdout, "Executing %s\n", mainargv[0]);
        rv = _spawnvpe(_P_WAIT, mainargv[0], mainargv, mainenvp);
        if (xtrace)
            fprintf(stdout, "%s: exit(%d)\n", mainargv[0], rv);
    }
    else {
        rv = x_perror(ENOENT, mainargv[0]);
    }

cleanup:
    a_free(mainenvp);
    a_free(mainargv);
    x_free(posixroot);
    x_free(msvcpath);
    return rv;
}

static const unsigned char padchar[] =
"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
static int randseed = 0;

static int prog_mktemp(int argc, const char **argv, const char **env)
{
    int  ch, rv = 0;
    char path[MAX_PATH] = { 0 };
    int  mdir   = 0;
    int  unsafe = 0;
    int  randnum;
    char patern[MAX_PATH];
    register char *start, *trv, *suffp;

    while ((ch = getopt(argc, argv, "dquhqvV", 1)) != EOF) {
        switch (ch) {
            case '.':
                if (!stricmp(optarg, "verbose"))
                    xtrace = 1;
                else if (!stricmp(optarg, "version"))
                    return print_banner(1);
                else if (!stricmp(optarg, "help"))
                    return prog_mktemp_usage(0);
                else
                    return prog_mktemp_usage(1);
            break;
            case 'v':
                xtrace  = 1;
            break;
            case 'V':
                xtrace  = 9;
            break;
            case 'q':
                xquiet  = 1;
            break;
            case 'h':
                return prog_mktemp_usage(0);
            break;
            case 'd':
                mdir    = 1;
            break;
            case 'u':
                unsafe  = 1;
            break;
            case '?':
            case ':':
                return 1;
            break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc > 0)
        x_strncpy(patern, argv[0], MAX_PATH);
    else
        strcpy(patern, "tmp.XXXXXXXX");

    if (GetTempPathA(MAX_PATH - strlen(patern) - 1, path) == 0) {
        x_perror(0, "Temp Path");
        return 1;
    }
    strcat(path, patern);
    x_backslash(path);
    for (trv = path; *trv; ++trv)
        ;
    suffp = trv;
    --trv;
    if (trv < path) {
        x_perror(EINVAL, "Temp Path");
        return 1;
    }

    if (randseed == 0) {
        randseed = (int)time(NULL);
        srand(randseed);
    }
    /* Fill space with random characters */
    while (*trv == 'X') {
        randnum = rand() % (sizeof(padchar) - 1);
        *trv-- = padchar[randnum];
    }
    start = trv + 1;

    for (;;) {
        if (!x_fattrib(path)) {
            if (errno == ENOENT) {
                if (mdir) {
                    if (x_createdir(path)) {
                        x_perror(0, path);
                        return 1;
                    }
                    if (unsafe)
                        x_rmdir(path);
                }
                else {
                    FILE *fp = fopen(path, "w");
                    if (!fp) {
                        x_perror(0, path);
                        return 1;
                    }
                    fclose(fp);
                    if (unsafe)
                        unlink(path);
                }
                fputs(path, stdout);
                return 0;
            }
        }
        /* If we have a collision, cycle through the space of filenames */
        for (trv = start;;) {
            char *pad;
            if (*trv == '\0' || trv == suffp) {
                x_perror(ENOENT, "Temp Path");
                return 1;
            }
            pad = strchr((char *)padchar, *trv);
            if (pad == NULL || !*++pad) {
                *trv++ = padchar[0];
            }
            else {
                *trv++ = *pad;
                break;
            }
        }

    }
    return rv;
}

            struct Var {
                WORD  wLength;
                WORD  wValueLength;
                WORD  wType;
                WCHAR szKey[1];
            };

static char *qresinfo(void *data, DWORD lang, const char *info)
{
    ULONG u;
    void *rv;
    char buff[1024];

    if (lang) {
        sprintf(buff, "\\StringFileInfo\\%02X%02X%02X%02X\\%s",
                (lang & 0x0000ff00) >> 8,
                (lang & 0x000000ff),
                (lang & 0xff000000) >> 24,
                (lang & 0x00ff0000) >> 16,
                info);
    }
    else {
        sprintf(buff, "\\StringFileInfo\\%04X04B0\\%s",
                GetUserDefaultLangID(),
                info);
    }
    if (VerQueryValueA(data, buff, &rv, &u))
          return (char *)rv;
    else
          return 0;
}


static int prog_image(int argc, const char **argv, const char **env)
{
    int  i, ch, rv = 0;
    char *dllpath = NULL;
    int  prall    = 0;
    BOOL dotdll   = FALSE;
    LOADED_IMAGE     img;
    DWORD infosiz = 0;
    void  *pidata = NULL;
    VS_FIXEDFILEINFO *fvi = NULL;

    int  lcnt     = 0;
    int  pname    = 0;
    int  plong    = 0;
    int  pmachine = 0;
    int  ptime    = 0;
    int  pnchar   = 0;
    int  pschar   = 0;
    int  psubsys  = 0;
    int  pstack   = 0;
    int  posver   = 0;
    int  pimgver  = 0;
    int  pbase    = 0;
    int  plinker  = 0;
    int  pnfver   = 0;
    int  pnfveri  = 0;
    int  pnpver   = 0;
    int  pnpveri  = 0;
    char *tfmt    = NULL;

    while ((ch = getopt(argc, argv, "aAbBcCdDiIlLmMnNoOpPfFsStT:hHqQvV", 0)) != EOF) {
        switch (ch) {
            case '.':
                if (!stricmp(optarg, "verbose"))
                    xtrace = 1;
                else if (!stricmp(optarg, "version"))
                    return print_banner(1);
                else if (!stricmp(optarg, "help"))
                    return prog_mktemp_usage(0);
                else if (!stricmp(optarg, "path"))
                    dllpath = x_strdup(optarg);
                else
                    return prog_image_usage(1);
            break;
            case 'v':
                xtrace  = 1;
            break;
            case 'V':
                xtrace  = 9;
            break;
            case 'q':
            case 'Q':
                xquiet   = 1;
            break;
            case 'h':
            case 'H':
                return prog_image_usage(0);
            break;
            case 'a':
            case 'A':
                prall    = 1;
                pname    = 1;
                pmachine = 1;
                ptime    = 1;
                pnchar   = 1;
                psubsys  = 1;
                pstack   = 1;
                pbase    = 1;
                pimgver  = 1;
                posver   = 1;
                plinker  = 1;
                pnpver   = 1;
                pnfver   = 1;
                pnpveri  = 1;
                pnfveri  = 1;
            break;
            case 'b':
            case 'B':
                pbase    = 1;
            break;
            case 'i':
            case 'I':
                pimgver  = 1;
            break;
            case 'l':
                plong    = 1;
            case 'L':
                plinker  = 1;
            break;
            case 'm':
            case 'M':
                pmachine = 1;
            break;
            case 'n':
            case 'N':
                pname    = 1;
            break;
            case 'T':
                tfmt     = x_strdup(optarg);
            case 't':
                ptime    = 1;
            break;
            case 'c':
                pnchar   = 1;
            break;
            case 'C':
                pschar   = 1;
            break;
            case 'o':
            case 'O':
                posver  = 1;
            break;
            case 's':
                psubsys  = 1;
            break;
            case 'S':
                pstack   = 1;
            break;
            case 'F':
                pnfveri  = 1;
            case 'f':
                pnfver   = 1;
            break;
            case 'P':
                pnpveri  = 1;
            case 'p':
                pnpver   = 1;
            break;
            case 'd':
            case 'D':
                dotdll  = TRUE;
            break;
            case '?':
            case ':':
                return 1;
            break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc < 1) {
        return prog_image_usage(EINVAL);
    }
    for (i = 0; i < argc; i++) {
        int lang = 0;
        if (i) {
            lcnt = 0;
            fputc('\n', stdout);
        }
        if (!MapAndLoad((char *)argv[i], dllpath, &img, dotdll, TRUE)) {
            return x_perror(0, argv[i]);
        }
        infosiz = GetFileVersionInfoSizeA(img.ModuleName, NULL);
        if (infosiz) {
            pidata = x_malloc(infosiz);
            if (!GetFileVersionInfoA(img.ModuleName, 0,
                                     infosiz, pidata)) {
                rv = x_perror(0, img.ModuleName);
                UnMapAndLoad(&img);
                x_free(pidata);
                return rv;
            }
            fvi = NULL;
            if (pnpver || pnfver) {
                UINT ul;
                void *p;
                if (!VerQueryValueA(pidata, "\\",
                                    &fvi, &ul)) {
                    x_perror(0, img.ModuleName);
                }
                else {
                    if (fvi->dwSignature != 0xFEEF04BD)
                        fvi = NULL;
                }
                if (!VerQueryValueA(pidata, "\\VarFileInfo\\Translation",
                                    &p, &ul)) {
                    x_perror(0, img.ModuleName);
                }
                else {
                    if (ul == 4)
                        memcpy(&lang, p, 4);
                }
            }
        }
        if (!pname && (pmachine + pstack + psubsys +
            pnchar + pschar + posver + pimgver +
            pbase + plinker + pnpver + pnfver) == 0)
            pname = 1;
        if (pname) {
            fputs(img.ModuleName, stdout);
            if (plong)
                fputc('\n', stdout);
            lcnt++;
        }
        if (pmachine) {
            char machine[8] = { 0 };
            switch (img.FileHeader->FileHeader.Machine) {
                case IMAGE_FILE_MACHINE_I386:
                    strcpy(machine, "x86");
                break;
                case IMAGE_FILE_MACHINE_IA64:
                    strcpy(machine, "i64");
                break;
                case IMAGE_FILE_MACHINE_AMD64:
                    strcpy(machine, "x64");
                break;
                default:
                    sprintf(machine, "0x%04x",
                            img.FileHeader->FileHeader.Machine);
                break;
            }
            if (plong) {
                fputs("Machine:         ", stdout);
            }
            else {
                if (lcnt++)
                    fputc('\t', stdout);
            }
            fputs(machine, stdout);
            if (plong)
                fputc('\n', stdout);
        }
        if (ptime) {
            char      buff[64] = { 0 };
            struct tm *lt;
            time_t t = (time_t)img.FileHeader->FileHeader.TimeDateStamp;
            lt = localtime(&t);
            if (!tfmt)
                tfmt = x_strdup("%Y-%m-%d %H:%M:%S");
            if (!strftime(buff, 64, tfmt, lt)) {
                rv = x_perror(errno, tfmt);
                UnMapAndLoad(&img);
                return rv;
            }
            if (plong) {
                fputs("Linked:          ", stdout);
            }
            else {
                if (lcnt++)
                    fputc('\t', stdout);
            }
            fputs(buff, stdout);
            if (plong)
                fputc('\n', stdout);
        }
        if (psubsys) {
            if (plong) {
                fputs("Subsystem:       ", stdout);
            }
            else {
                if (lcnt++)
                    fputc('\t', stdout);
            }
            switch (img.FileHeader->OptionalHeader.Subsystem) {
                case IMAGE_SUBSYSTEM_UNKNOWN:
                    fputs("unknown", stdout);
                break;
                case IMAGE_SUBSYSTEM_NATIVE:
                    fputs("native", stdout);
                break;
                case IMAGE_SUBSYSTEM_WINDOWS_GUI:
                    fputs("gui", stdout);
                break;
                case IMAGE_SUBSYSTEM_WINDOWS_CUI:
                    fputs("console", stdout);
                break;
                case IMAGE_SUBSYSTEM_WINDOWS_CE_GUI:
                    fputs("ce", stdout);
                break;
                case IMAGE_SUBSYSTEM_POSIX_CUI:
                    fputs("posix", stdout);
                break;
                case 16:
                    fputs("boot", stdout);
                break;
                default:
                    fprintf(stdout, "%d",
                            img.FileHeader->OptionalHeader.Subsystem);
                break;
            }
            if (plong)
                fputc('\n', stdout);
        }
        if (pstack) {
            if (plong) {
                fputs("Stack:           ", stdout);
            }
            else {
                if (lcnt++)
                    fputc('\t', stdout);
            }
#ifdef _WIN64
            fprintf(stdout, "%I64d",
#else
            fprintf(stdout, "%d",
#endif
                    img.FileHeader->OptionalHeader.SizeOfStackReserve);
            if (plong)
                fputc('\n', stdout);
        }
        if (posver) {
            if (plong) {
                fputs("OperatingSystem: ", stdout);
            }
            else {
                if (lcnt++)
                    fputc('\t', stdout);
            }
            fprintf(stdout, "%d.%d",
                    img.FileHeader->OptionalHeader.MajorOperatingSystemVersion,
                    img.FileHeader->OptionalHeader.MinorOperatingSystemVersion);
            if (plong)
                fputc('\n', stdout);
        }
        if (pimgver && fvi) {
            if (plong) {
                fputs("ImageVersion:    ", stdout);
            }
            else {
                if (lcnt++)
                    fputc('\t', stdout);
            }
            fprintf(stdout, "%d.%d",
                    img.FileHeader->OptionalHeader.MajorImageVersion,
                    img.FileHeader->OptionalHeader.MinorImageVersion);
            if (plong)
                fputc('\n', stdout);
        }
        if (pnfver && fvi) {
            LARGE_INTEGER li;
            char  *p;
            if (plong) {
                fputs("FileVersion:     ", stdout);
            }
            else {
                if (lcnt++)
                    fputc('\t', stdout);
            }
            li.HighPart = fvi->dwFileVersionMS;
            li.LowPart  = fvi->dwFileVersionLS;
            if (pnfveri && (p = qresinfo(pidata, lang, "FileVersion"))) {
                fputs(p, stdout);
            }
            else
               fprintf(stdout, "%I64d", li.QuadPart);
            if (plong)
                fputc('\n', stdout);
        }

        if (pnpver && fvi) {
            LARGE_INTEGER li;
            char  *p;
            if (plong) {
                fputs("ProductVersion:  ", stdout);
            }
            else {
                if (lcnt++)
                    fputc('\t', stdout);
            }
            li.HighPart = fvi->dwProductVersionMS;
            li.LowPart  = fvi->dwProductVersionLS;
            if (pnpveri && (p = qresinfo(pidata, lang, "ProductVersion"))) {
                fputs(p, stdout);
            }
            else
               fprintf(stdout, "%I64d", li.QuadPart);
            if (plong)
                fputc('\n', stdout);
        }
        if (plinker) {
            if (plong) {
                fputs("LinkerVersion:   ", stdout);
            }
            else {
                if (lcnt++)
                    fputc('\t', stdout);
            }
            fprintf(stdout, "%d.%d",
                    img.FileHeader->OptionalHeader.MajorLinkerVersion,
                    img.FileHeader->OptionalHeader.MinorLinkerVersion);
            if (plong)
                fputc('\n', stdout);
        }
        if (pbase) {
            if (plong) {
                fputs("ImageBase:       ", stdout);
            }
            else {
                if (lcnt++)
                    fputc('\t', stdout);
            }
#ifdef _WIN64
            fprintf(stdout, "0x%016I64x",
#else
            fprintf(stdout, "0x%08x",
#endif
                    img.FileHeader->OptionalHeader.ImageBase);
            if (plong)
                fputc('\n', stdout);
        }
        if (pnchar || pschar) {
            if (plong) {
                fputs("Characteristics: ", stdout);
            }
            else {
                if (lcnt++)
                    fputc('\t', stdout);
            }
            if (pnchar) {
                fprintf(stdout, "0x%04x",
                        img.FileHeader->FileHeader.Characteristics);
                if (pschar)
                    fputs(" (", stdout);
            }
            if (pschar) {
                int cc = 0;
                if (img.FileHeader->FileHeader.Characteristics &
                    IMAGE_FILE_RELOCS_STRIPPED) {
                        fputs("reloc-stripped", stdout);
                        cc++;
                }
                if (img.FileHeader->FileHeader.Characteristics &
                    IMAGE_FILE_EXECUTABLE_IMAGE) {
                        if (cc++)
                            fputs(", ", stdout);
                        fputs("executable", stdout);
                }
                if (img.FileHeader->FileHeader.Characteristics &
                    IMAGE_FILE_LINE_NUMS_STRIPPED) {
                        if (cc++)
                            fputs(", ", stdout);
                        fputs("coff-lines-stripped", stdout);
                }
                if (img.FileHeader->FileHeader.Characteristics &
                    IMAGE_FILE_LOCAL_SYMS_STRIPPED) {
                        if (cc++)
                            fputs(", ", stdout);
                        fputs("coff-symtable-stripped", stdout);
                }
                if (img.FileHeader->FileHeader.Characteristics &
                    IMAGE_FILE_LARGE_ADDRESS_AWARE) {
                        if (cc++)
                            fputs(", ", stdout);
                        fputs("large-address", stdout);
                }
                if (img.FileHeader->FileHeader.Characteristics &
                    IMAGE_FILE_32BIT_MACHINE) {
                        if (cc++)
                            fputs(", ", stdout);
                        fputs("32-bit", stdout);
                }
                if (img.FileHeader->FileHeader.Characteristics &
                    IMAGE_FILE_REMOVABLE_RUN_FROM_SWAP) {
                        if (cc++)
                            fputs(", ", stdout);
                        fputs("removable-swap", stdout);
                }
                if (img.FileHeader->FileHeader.Characteristics &
                    IMAGE_FILE_NET_RUN_FROM_SWAP) {
                        if (cc++)
                            fputs(", ", stdout);
                        fputs("net-swap", stdout);
                }
                if (img.FileHeader->FileHeader.Characteristics &
                    IMAGE_FILE_DEBUG_STRIPPED) {
                        if (cc++)
                            fputs(", ", stdout);
                        fputs("debug-stripped", stdout);
                }
                if (img.FileHeader->FileHeader.Characteristics &
                    IMAGE_FILE_SYSTEM) {
                        if (cc++)
                            fputs(", ", stdout);
                        fputs("filesystem", stdout);
                }
                if (img.FileHeader->FileHeader.Characteristics &
                    IMAGE_FILE_DLL) {
                        if (cc++)
                            fputs(", ", stdout);
                        fputs("dll", stdout);
                }
                if (pnchar)
                fputc(')', stdout);
            }
            if (plong)
                fputc('\n', stdout);
        }
        UnMapAndLoad(&img);
        x_free(pidata);
        pidata = NULL;
    }
    if (xtrace)
        fputc('\n', stdout);
    return 0;
}

static int prog_html(int argc, const char **argv, const char **env)
{
    int  ch, rv = 0;
    char *p;
    char title[MAX_PATH] = { 0 };
    int  w = 0;
    int  h = 0;
    int  t = INFINITE;
    int  flags = 0;
    char *page   = NULL;
    char *output = NULL;
    char *params = NULL;

    if (!GuiInitialize()) {
        return x_perror(0, "Windows GUI");
    }
    while ((ch = getopt(argc, argv, "g:n:o:p:st:T:qhqv", 0)) != EOF) {
        switch (ch) {
            case '.':
                if (!stricmp(optarg, "verbose"))
                    xtrace = 1;
                else if (!stricmp(optarg, "version"))
                    return print_banner(1);
                else if (!stricmp(optarg, "help"))
                    return prog_html_usage(0);
                else
                    return prog_html_usage(1);
            break;
            case 'v':
                xtrace = 1;
            break;
            case 'V':
                xtrace = 9;
            break;
            case 'q':
                xquiet  = 1;
            break;
            case 'h':
                return prog_html_usage(0);
            break;
            case 'g':
                w = atoi(optarg);
                if (!(p = strchr(optarg, 'x')))
                    p = strchr(optarg, 'X');
                if (p++)
                    h = atoi(p);
                if (w < 1 || h < 1) {
                    return x_perror(EINVAL, "Geometry");
                }
            break;
            case 'n':
                x_strncpy(title, optarg, MAX_PATH);
            break;
            case 'o':
                output = x_fullpath(optarg);
            break;
            case 'p':
                params = x_strdup(optarg);
            break;
            case 's':
                flags  = 1;
            break;
            case 't':
                t = atoi(optarg);
            break;
            case 'T':
                t = atoi(optarg) * 1000;
            break;
            case '?':
            case ':':
                return 1;
            break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc < 1) {
        return prog_html_usage(EINVAL);
    }
    if (!(page = x_fullpath(argv[0]))) {
        return x_perror(EINVAL, argv[0]);
    }
    if (!title[0]) {
        FILE *f;
        char buff[SIZ_BUFLEN];
        char lbuf[SIZ_BUFLEN];
        if (!(f = fopen(page, "r"))) {
            rv = x_perror(0, page);
            goto cleanup;
        }
        while (fgets(&buff[0], SIZ_BUFLEN, f)) {
            char *e;
            strcpy(lbuf, buff);
            strlwr(lbuf);
            if ((p = strstr(lbuf, "<title>")) && *(p += 7)) {
                if ((e = strstr(p, "</title>"))) {
                    size_t ps = p - lbuf;
                    size_t pe = e - lbuf;
                    x_strncpy(lbuf, buff + ps, pe - ps + 1);
                    p = rltrim(lbuf);
                    strcpy(title, p);
                    break;
                }
            }
        }
        fclose(f);
        if (!title[0]) {
            rv = x_perror(EINVAL, "Html <title></title> tag");
            goto cleanup;
        }
    }
    if (w < 1)
        w = DEF_WWIDTH;
    if (h < 1)
        h = DEF_WHEIGHT;

    if (xtrace > 1) {
        warnx("Html     %s", page);
        warnx("Title    %s", title);
        warnx("Geometry %d x %d", w, h);
    }

    if (!RunCustomPage(page,
                       title,
                       params,
                       w,
                       h,
                       t,
                       flags,
                       output)) {
        rv = x_cerror(0);
    }

cleanup:
    x_free(page);
    x_free(params);
    x_free(output);
    return rv;
}

static int prog_reg(int argc, const char **argv, const char **env)
{
    int ch, rv = 0;
    int delall = 0;
    int   sc = ',';
    char *rm = NULL;
    char *rs = NULL;
    char *rd = NULL;
    int   rt = REG_NONE;
    x_registry_t *reg = NULL;

    while ((ch = getopt(argc, argv, "ad:f:m:r:t:hqvV", 0)) != EOF) {
        switch (ch) {
            case '.':
                if (!stricmp(optarg, "verbose"))
                    xtrace = 1;
                else if (!stricmp(optarg, "version"))
                    return print_banner(1);
                else if (!stricmp(optarg, "help"))
                    return prog_reg_usage(0);
                else
                    return prog_reg_usage(EINVAL);
            break;
            case 'a':
                delall = 1;
            break;
            case 'm':
                rm  = x_strdup(optarg);
            break;
            case 'f':
                sc  = *optarg;
            break;
            case 'd':
                rd  = x_strdup(optarg);
            break;
            case 't':
                rt  = reg_ntype(optarg);
                if (rt == REG_NONE) {
                    return prog_reg_usage(EINVAL);
                }
            break;
            case 'v':
                xtrace  = 1;
            break;
            case 'V':
                xtrace  = 9;
            break;
            case 'q':
                xquiet  = 1;
            break;
            case 'h':
                return prog_reg_usage(0);
            break;
            case '?':
            case ':':
                return EINVAL;
            break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc < 2) {
        return prog_reg_usage(EINVAL);
    }
    if (!stricmp(argv[0], "query")) {
        if (argc < 3) {
            return prog_reg_usage(EINVAL);
        }
        if (!(reg = reg_open(argv[1], rm ? rm : "r"))) {
            rv = x_perror(0, argv[1]);
            goto cleanup;
        }
        if (!(rs = reg_value(reg, argv[2], sc))) {
            rv = x_perror(0, argv[2]);
            goto cleanup;
        }
    }
    if (!stricmp(argv[0], "add")) {
        if (argc < 2) {
            return prog_reg_usage(EINVAL);
        }
        if (!(reg = reg_create(argv[1], rm ? rm : "rw"))) {
            rv = x_perror(0, argv[1]);
            goto cleanup;
        }
        if (rd) {
            if ((rv = reg_set(reg, argv[2], rt, rd, sc)))
                x_perror(rv, argv[2]);
        }
    }
    if (!stricmp(argv[0], "del")) {
        const char *v = NULL;
        if (argc < 2)
            return prog_reg_usage(EINVAL);
        else if (argc > 2)
            v = argv[2];
        rv = reg_delete(argv[1], rm ? rm : "rw",
                        delall, v);
    }
    if (!stricmp(argv[0], "type")) {
        if (argc < 3) {
            return prog_reg_usage(EINVAL);
        }
        if (!(reg = reg_open(argv[1], rm ? rm : "r"))) {
            rv = x_perror(0, argv[1]);
            goto cleanup;
        }
        if ((rv = reg_type(reg, argv[2])) >= 0) {
            fputs(reg_stype(rv), stdout);
            rv = 0;
        }
    }
    if (!stricmp(argv[0], "size")) {
        if (argc < 3) {
            return prog_reg_usage(EINVAL);
        }
        if (!(reg = reg_open(argv[1], rm ? rm : "r"))) {
            rv = x_perror(0, argv[1]);
            goto cleanup;
        }
        rv = reg_size(reg, argv[2]);
        fprintf(stdout, "%d", rv);
    }
    if (!stricmp(argv[0], "enum")) {
        DWORD idx, nl;
        int all = 0;
        wchar_t nb[256];
        if (argc < 3) {
            return prog_reg_usage(EINVAL);
        }
        if (!(reg = reg_open(argv[1], rm ? rm : "r"))) {
            rv = x_perror(0, argv[1]);
            goto cleanup;
        }
        idx = 0;
        if (!stricmp(argv[2], "all"))
            all = 1;
        if (all || !stricmp(argv[2], "key")) {
            nl = 256;
            while ((rv = RegEnumKeyExW(reg->key,
                                       idx,
                                       nb,
                                       &nl,
                                       NULL,
                                       NULL,
                                       NULL,
                                       NULL)) == ERROR_SUCCESS) {
                if (idx++)
                    fputc('\n', stdout);
                if (all)
                    fputc('\\', stdout);
                fprintf(stdout, "%S", nb);
                idx++;
                nl = 256;
            }
            if (rv != ERROR_NO_MORE_ITEMS)
                goto cleanup;
        }
        if (all || !strnicmp(argv[2], "val", 3)) {
            nl = 256;
            if (idx++)
                fputc('\n', stdout);
            idx = 0;
            while ((rv = RegEnumValueW(reg->key,
                                       idx,
                                       nb,
                                       &nl,
                                       NULL,
                                       NULL,
                                       NULL,
                                       NULL)) == ERROR_SUCCESS) {
                if (idx++)
                    fputc('\n', stdout);
                fprintf(stdout, "%S", nb);
                idx++;
                nl = 256;
            }
            if (rv != ERROR_NO_MORE_ITEMS)
                goto cleanup;
        }
        rv = 0;
    }

cleanup:
    if (rs) {
        fputs(rs, stdout);
    }
    x_free(rs);
    x_free(rm);
    x_free(rd);
    reg_close(reg);
    return rv;
}

static int prog_msg(int argc, const char **argv, const char **env)
{
    int ch, rv = 0;
    int type  = MB_YESNO;
    int icon  = MB_ICONQUESTION;
    int modal = MB_TASKMODAL;
    char *p, *msg = NULL;

    if (!GuiInitialize()) {
        return x_perror(0, "Windows GUI");
    }
    while ((ch = getopt(argc, argv, "i:st:hqvV", 0)) != EOF) {
        switch (ch) {
            case '.':
                if (!stricmp(optarg, "verbose"))
                    xtrace = 1;
                else if (!stricmp(optarg, "version"))
                    return print_banner(1);
                else if (!stricmp(optarg, "help"))
                    return prog_msg_usage(0);
                else
                    return prog_msg_usage(EINVAL);
            break;
            case 'v':
                xtrace  = 1;
            break;
            case 'V':
                xtrace  = 9;
            break;
            case 'q':
                xquiet  = 1;
            break;
            case 'h':
                return prog_msg_usage(0);
            break;
            case 's':
                modal   = MB_SYSTEMMODAL;
            break;
            case 't':
                if (!stricmp("yn", optarg) ||
                    !stricmp("MB_YESNO", optarg)) {
                    type = MB_YESNO;
                }
                else if (!stricmp("ync", optarg) ||
                         !stricmp("MB_YESNOCANCEL", optarg)) {
                    type = MB_YESNOCANCEL;
                }
                else if (!stricmp("ok", optarg) ||
                         !stricmp("MB_OK", optarg)) {
                    type = MB_OK;
                }
                else if (!stricmp("oc", optarg) ||
                         !stricmp("MB_OKCANCEL", optarg)) {
                    type = MB_OKCANCEL;
                }
                else if (!stricmp("ctc", optarg) ||
                         !stricmp("MB_CANCELTRYCONTINUE", optarg)) {
                    type = MB_CANCELTRYCONTINUE;
                }
                else if (!stricmp("ari", optarg) ||
                         !stricmp("MB_ABORTRETRYIGNORE", optarg)) {
                    type = MB_ABORTRETRYIGNORE;
                }
                else if (!stricmp("rc", optarg) ||
                         !stricmp("MB_RETRYCANCEL", optarg)) {
                    type = MB_RETRYCANCEL;
                }
                else {
                    return prog_msg_usage(EINVAL);
                }
            break;
            case 'i':
                switch (*optarg) {
                    case '!':
                        icon = MB_ICONEXCLAMATION;
                    break;
                    case 'w':
                    case 'W':
                        icon = MB_ICONWARNING;
                    break;
                    case 'i':
                    case 'I':
                        icon = MB_ICONINFORMATION;
                    break;
                    case '*':
                        icon = MB_ICONASTERISK;
                    break;
                    case '?':
                        icon = MB_ICONQUESTION;
                    break;
                    case 's':
                    case 'S':
                        icon = MB_ICONSTOP;
                    break;
                    case 'h':
                    case 'H':
                        icon = MB_ICONHAND;
                    break;
                }
            break;
            case '?':
            case ':':
                return EINVAL;
            break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc < 2) {
        return prog_msg_usage(EINVAL);
    }
    msg = x_strdup(argv[1]);
    for (p = msg; *p; p++) {
        if (*p == '#' && *(p + 1) == '#') {
            *(p++) = '\r';
            *(p++) = '\n';
        }
    }
    rv = MessageBoxA(HWND_DESKTOP, msg, argv[0], type | icon | modal);
    x_free(msg);
    if (rv == 0)
        rv = -1;
    return rv;
}

static const char *jvm_paths[] = {
    "\\server\\jvm.dll",
    "\\client\\jvm.dll",
    "\\jrockit\\jvm.dll",
    "\\server\\jvm.dll",
    NULL
};

static const char *jdk_keys[] = {
    "HKLM\\SOFTWARE\\JavaSoft\\Java Development Kit",
    "HKLM\\SOFTWARE\\JRockit\\Java Development Kit",
    NULL
};

static const char *jre_keys[] = {
    "HKLM\\SOFTWARE\\JavaSoft\\Java Runtime Environment",
    "HKLM\\SOFTWARE\\JRockit\\Java Runtime Environment",
    NULL
};

static int prog_jdk(int argc, const char **argv, const char **env)
{
    int i, ch, rv = 0;
    int useenv = 0;
    int server = 0;
    int getjre = 0;
    int jredll = 0;
    char *penv = NULL;
    char *home = NULL;
    char *jred = NULL;
    char *jbin = NULL;
    char *jreh = NULL;
    char *regm = NULL;

    while ((ch = getopt(argc, argv, "deE:r:jshqvV", 0)) != EOF) {
        switch (ch) {
            case '.':
                if (!stricmp(optarg, "verbose"))
                    xtrace = 1;
                else if (!stricmp(optarg, "version"))
                    return print_banner(1);
                else if (!stricmp(optarg, "help"))
                    return prog_jdk_usage(0);
                else
                    return prog_jdk_usage(EINVAL);
            break;
            case 'v':
                xtrace  = 1;
            break;
            case 'V':
                xtrace  = 9;
            break;
            case 'q':
                xquiet  = 1;
            break;
            case 'h':
                return prog_jdk_usage(0);
            break;
            case 'e':
                useenv  = 1;
            break;
            case 'E':
                useenv  = 1;
                penv    = x_strdup(optarg);
            break;
            case 'r':
                regm    = x_strdup(optarg);
            break;
            case 's':
                server  = 1;
            break;
            case 'j':
                getjre  = 1;
            break;
            case 'd':
                jredll  = 1;
            break;
            case '?':
            case ':':
                return EINVAL;
            break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc > 0) {
        home = x_fullpath(argv[0]);
        printf("Home is %s\n", home);
    }
    if (useenv) {
        const char *senv = NULL;
        if (home) {
            if (getjre) {
                jreh = x_strdup(home);
            }
        }
        else if (penv) {
            senv = penv;
            home = x_getenv(senv);
        }
        else if (getjre) {
            senv = "JRE_HOME";
            if (!(home = x_getenv(senv))) {
                senv = "JAVA_HOME";
                home = x_getenv(senv);
                if (home) {
                    size_t l = strlen(home);
                    if (home[l - 1] == '\\' ||
                        home[l - 1] == '/')
                        home[l - 1] = '\0';
                    jreh = x_strvcat(home, "\\jre", NULL);
                    x_free(home);
                    home = x_strdup(jreh);
                }
            }
            else {
                jreh = x_strdup(home);
            }
        }
        else {
            senv = "JAVA_HOME";
            home = x_getenv(senv);
        }
        if (home) {
            size_t l = strlen(home);
            if (home[l - 1] == '\\' ||
                home[l - 1] == '/')
                home[l - 1] = '\0';
            jbin = x_strvcat(home, "\\bin", NULL);
            if (!x_fattrib(jbin)) {
                rv = x_perror(ENOTDIR, jbin);
                goto cleanup;
            }
            if (!jreh)
                jreh = x_strvcat(home, "\\jre", NULL);
            if (!x_fattrib(jreh)) {
                rv = x_perror(ENOTDIR, jreh);
                goto cleanup;
            }
        }
        else {
            rv = x_perror(ENOENT, senv);
            goto cleanup;
        }
        if (xtrace > 1) {
            warnx("JDK %s", home);
            warnx("JRE %s", jreh);
            warnx("BIN %s", jbin);
        }
    }
    else {
        x_registry_t *reg = NULL;
        const char **keys;
        if (getjre)
            keys = jre_keys;
        else
            keys = jdk_keys;
        for (i = 0; keys[i]; i++) {
            if ((reg = reg_open(keys[i], regm ? regm : "r"))) {
                char *cv = reg_value(reg, "CurrentVersion", 0);
                if (cv) {
                    char *ck = x_strvcat(keys[i], "\\", cv, NULL);
                    reg_close(reg);
                    if ((reg = reg_open(ck, regm ? regm : "r"))) {
                        home = reg_value(reg, "JavaHome", 0);
                        if (home) {
                            size_t l = strlen(home);
                            if (home[l - 1] == '\\' ||
                                home[l - 1] == '/')
                                home[l - 1] = '\0';
                            if (getjre) {
                                jreh = x_strdup(home);
                                jred = reg_value(reg, "RuntimeLib", 0);
                            }
                            else {
                                jreh = x_strvcat(home, "\\jre", NULL);
                                jbin = x_strvcat(jreh, "\\bin", NULL);
                            }
                        }
                    }
                    x_free(ck);
                }
                x_free(cv);
            }
            reg_close(reg);
            if (home)
                break;
        }
        if (!home) {
            rv = x_perror(ENOENT, "JavaHome Registry search");
            goto cleanup;
        }
    }
    if (jredll && jbin) {
        if (server)
            i = 0;
        else
            i = 1;
        for (; jvm_paths[i]; i++) {
            jred = x_strvcat(jbin, jvm_paths[i], NULL);
            if (x_fattrib(jred))
                break;
            x_free(jred);
            jred = NULL;
        }
        if (!jred) {
            rv = x_perror(ENOENT, "jvm.dll");
            goto cleanup;
        }
    }

cleanup:
    if (rv == 0) {
        if (jred) {
            fputs(jred, stdout);
        }
        else if (home)
            fputs(home, stdout);
        fflush(stdout);
    }
    x_free(regm);
    x_free(jreh);
    x_free(jbin);
    x_free(jred);
    x_free(home);
    x_free(penv);
    return rv;
}

static int prog_browse(int argc, const char **argv, const char **env)
{
    int ch, rv = 0;
    int flags  = BIF_USENEWUI;
    char *p, *msg = NULL;
    char path[MAX_PATH] = { 0 };

    if (!GuiInitialize()) {
        return x_perror(0, "Windows GUI");
    }
    while ((ch = getopt(argc, argv, "frhqvV", 0)) != EOF) {
        switch (ch) {
            case '.':
                if (!stricmp(optarg, "verbose"))
                    xtrace = 1;
                else if (!stricmp(optarg, "version"))
                    return print_banner(1);
                else if (!stricmp(optarg, "help"))
                    return prog_browse_usage(0);
                else
                    return prog_browse_usage(EINVAL);
            break;
            case 'v':
                xtrace  = 1;
            break;
            case 'V':
                xtrace  = 9;
            break;
            case 'q':
                xquiet  = 1;
            break;
            case 'h':
                return prog_browse_usage(0);
            break;
            case 'r':
                flags  &= ~(BIF_USENEWUI);
            break;
            case 'f':
                flags  |= BIF_BROWSEINCLUDEFILES;
            break;
            case '?':
            case ':':
                return EINVAL;
            break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc < 1) {
        return prog_browse_usage(EINVAL);
    }
    msg = x_strdup(argv[0]);
    for (p = msg; *p; p++) {
        if (*p == '#' && *(p + 1) == '#') {
            *(p++) = '\r';
            *(p++) = '\n';
        }
    }

    if (GuiBrowseForFolder(HWND_DESKTOP, msg, path, flags)) {
        fputs(path, stdout);
    }
    else
        rv = 1;
    x_free(msg);
    return rv;
}

static int prog_xcopy(int argc, const char **argv, const char **env)
{
    int ch, rv = 0;
    wchar_t *p, *msg = NULL;
    SHFILEOPSTRUCTW shOp;

    if (!GuiInitialize()) {
        return x_perror(0, "Windows GUI");
    }
    while ((ch = getopt(argc, argv, "hqvV", 0)) != EOF) {
        switch (ch) {
            case '.':
                if (!stricmp(optarg, "verbose"))
                    xtrace = 1;
                else if (!stricmp(optarg, "version"))
                    return print_banner(1);
                else if (!stricmp(optarg, "help"))
                    return prog_xcopy_usage(0);
                else
                    return prog_xcopy_usage(EINVAL);
            break;
            case 'v':
                xtrace  = 1;
            break;
            case 'V':
                xtrace  = 9;
            break;
            case 'q':
                xquiet  = 1;
            break;
            case 'h':
                return prog_xcopy_usage(0);
            break;
            case '?':
            case ':':
                return EINVAL;
            break;
        }
    }
    argc -= optind;
    argv += optind;
    if (argc < 3) {
        return prog_xcopy_usage(EINVAL);
    }
    msg = x_wstrdup_utf8(argv[0]);
    for (p = msg; *p; p++) {
        if (*p == L'#' && *(p + 1) == L'#') {
            *(p++) = L'\r';
            *(p++) = L'\n';
        }
    }

    shOp.hwnd   = HWND_DESKTOP;
    shOp.wFunc  = FO_COPY;
    shOp.fFlags = FOF_NOCONFIRMATION |
                  FOF_NOCONFIRMMKDIR |
                  FOF_SIMPLEPROGRESS;

    shOp.pFrom  = x_wstrdup_utf8(argv[1]);
    shOp.pTo    = x_wstrdup_utf8(argv[2]);
    shOp.lpszProgressTitle = msg;

    rv = SHFileOperationW(&shOp);
    x_free(msg);
    x_free((wchar_t *)shOp.pFrom);
    x_free((wchar_t *)shOp.pTo);
    return rv;
}

/*
 * ---------------------------------------------------------------------
 * end of programs
 * ---------------------------------------------------------------------
 */

typedef int (*pmain_t)(int, const char **, const char **);

static struct x_program {
    const char *name;
    pmain_t     pmain;
} x_programs[] = {
  { "mkdir",    prog_mkdir      },
  { "md",       prog_mkdir      },
  { "rmdir",    prog_rmdir      },
  { "rd",       prog_rmdir      },
  { "touch",    prog_touch      },
  { "exec",     prog_exec       },
  { "mktemp",   prog_mktemp     },
  { "image",    prog_image      },
  { "coff",     prog_image      },
  { "html",     prog_html       },
  { "reg",      prog_reg        },
  { "msg",      prog_msg        },
  { "jdk",      prog_jdk        },
  { "dir",      prog_browse     },
  { "xcopy",    prog_xcopy      },

  { NULL,       NULL            }
};

/*
 * ---------------------------------------------------------------------
 * utf-8 main. Arguments passed are utf-8 encoded
 * ---------------------------------------------------------------------
 */
static int umain(int argc, const char **argv, const char **env)
{
    const char *name     = progname;
    struct  x_program *p;

    if (*name == 'x' || *name == 'X')
        name++;

    p = &x_programs[0];
    while (p->name) {
        if (!stricmp(name, p->name)) {
            x_free(__pname);
            __pname = x_strdup(name);
            return (*(p->pmain))(argc, argv, env);
        }
        p++;
    }
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <command>\n",
                progname);
        print_programs();
        return EINVAL;
    }
    --argc;
    ++argv;
    p = &x_programs[0];
    while (p->name) {
        if (!stricmp(argv[0], p->name)) {
            x_free(__pname);
            __pname = x_strdup(argv[0]);
            return (*(p->pmain))(argc, argv, env);
        }
        p++;
    }
    fprintf(stderr, "Usage: %s <command>\n",
            progname);
    print_programs();
    return EINVAL;
}

static void setup_env(const char **env)
{   char *e;
    if ((e = getenv("XTOOLS_PAUSE")) != NULL)
        xtools_pause = 1;
    if ((e = getenv("XTOOLS_VERBOSE")) != NULL) {
        xtrace = atoi(e);
    }
}

static void cleanup(void)
{
    static int cleared = 0;

    if (cleared++)
        return;
    /* Make any cleanup here */

    /* Finally terminate the GUI */
    GuiTerminate();
}

int wmain(int argc, const wchar_t **wargv, const wchar_t **wenv)
{
    char **argv;
    char **env;
    wchar_t *modname;
    int  dupenv;
    int  rv;

    atexit(cleanup);
    /* Find on what we are running */
    GetSystemInfo(&win_osinf);
    win_osver.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEXA);
    GetVersionExA((LPOSVERSIONINFOA)&win_osver);

    if (win_osver.dwMajorVersion < 5) {
        /* Pre WIN2K. bail out */
        fprintf(stderr, "This program cannot be run on < Windows 2000\n");
        _exit(EACCES);
        return EACCES;
    }
    wastrtoastr(&argv, wargv, argc);
    dupenv = wastrtoastr(&env, wenv, -1);

    _environ = _malloc_dbg((dupenv + 1) * sizeof (char *),
                           _CRT_BLOCK, __FILE__, __LINE__);
    memcpy(_environ, env, (dupenv + 1) * sizeof (char *));

    /*
     * MSVCRT will attempt to maintain the wide environment calls
     * on _putenv(), which is bogus if we've passed a non-ascii
     * string to _putenv(), since they use MultiByteToWideChar
     * and breaking the implicit utf-8 assumption we've built.
     *
     * Reset _wenviron for good measure.
     */
    if (_wenviron) {
        wenv = _wenviron;
        _wenviron = NULL;
        free((wchar_t **)wenv);
    }
    modname = x_malloc(X_MAX_PATH * 2);
    __argv0 = x_malloc(X_MAX_PATH);
    if (!GetModuleFileNameExW(GetCurrentProcess(), NULL,
                              modname, X_MAX_PATH))
        exit(GetLastError());
    if ((rv = unicode_to_utf8_path(__argv0, X_MAX_PATH, modname)))
        exit(rv);
    x_free(modname);
    setup_env(env);
    rv = umain(argc, argv, env);
    if (!xquiet && xtools_pause) {
        fprintf(stdout, "\nPress any key to continue...");
        getch();
        fprintf(stdout, "\n");
    }
    if (xtrace > 1)
        warnx("exit(%d)", rv);
    exit(rv);
    /*
     * Never reached.
     * Just to make compiler happy
     */
    return rv;
}
