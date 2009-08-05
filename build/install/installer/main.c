/*
 *  SINSTALL - JBoss Installer
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
 */

#include "sinstall.h"

#include <direct.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <process.h>

#define __UNZIP_C       /* identifies this source module */
#define UNZIP_INTERNAL
#include "unzip.h"      /* includes, typedefs, macros, prototypes, etc. */
#include "crypt.h"
#include "unzvers.h"

#define DEF_WWIDTH              505
#define DEF_WHEIGHT             360
#define MAX_CMDLINE             16384
#define IS_INVALID_HANDLE(x)    (((x) == NULL || (x) == INVALID_HANDLE_VALUE))

static LPCSTR CMD_PREFIX   = "%s /E:ON /S /C \"SET JBINSTALL_PPID=%d&&CALL %s ";
static LPCSTR CMD_SUFIX    = "> install.log 2>&1\"";
static LPCSTR CMD_BATCH     = "install.bat";
static LPCSTR CMD_PARAM     = "install";
static LPCSTR DIR_BATCH     = "\\_install";
static LPCSTR CMD_QUOTE     = " &()[]{}^=;!'+,`~";
static LPSTR  ppUnizpArgs[] = { "unzip", "-qq", "-d", NULL, NULL };

#define EMBED_SIZE      2048
#define EMBED_MAX       2044

typedef struct {
    char    s_signature[16];
    char    s_class[EMBED_SIZE];
    char    s_flags[32];
    UINT32  s_iopts[4];
    char    e_signature[16];
} st_config;

static BYTE c_signature[] = {
    's', 't', 'a', 'r', 't', 'u', 's', 'e', 'r', 's', 'b', 'l', 'o', 'c', 'k', '\0'
};

/* space in exe stub for user configuration */
static BYTE conf_in_exe[EMBED_SIZE + 64] = {
    'S', 'T', 'A', 'R', 'T', 'U', 'S', 'E', 'R', 'S', 'B', 'L', 'O', 'C', 'K', '\0',

    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',

    '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0',
    'E', 'N', 'D', 'O', 'F', 'U', 'S', 'E', 'R', 'S', 'B', 'L', 'O', 'C', 'K', '\0',
};

static st_config *pConfig = NULL;

BOOL   opt_Quiet            = FALSE;
BOOL   opt_Verbose          = FALSE;


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

static LPSTR  szProgramName = NULL;
static LPSTR  szProgramPath = NULL;
static LPSTR  szProgramExe  = NULL;
static LPSTR  szTempPath    = NULL;
static HANDLE hStub         = NULL;

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

static int
ErrorMessage(
    LPCSTR szTitle,
    LPCSTR szError,
    BOOL bFatal)
{
    LPVOID lpMsgBuf = NULL;
    UINT   nType;
    int    nRet  = 0;
    DWORD  dwErr = GetLastError();

    if (bFatal)
        nType = MB_ICONERROR | MB_ABORTRETRYIGNORE | MB_SYSTEMMODAL;
    else
        nType = MB_ICONEXCLAMATION | MB_OK | MB_APPLMODAL;
    if (szError) {
#ifdef _CONSOLE
        fprintf(stderr, "Application Error (08X): %s\n", dwErr, szError);
#else
        nRet = MessageBoxA(NULL, szError, "Application Error", nType);
#endif
    }
    else {
        if (!szTitle)
            szTitle = "Application System Error";
        FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER |
                       FORMAT_MESSAGE_FROM_SYSTEM |
                       FORMAT_MESSAGE_IGNORE_INSERTS,
                       NULL,
                       dwErr,
                       MAKELANGID(LANG_NEUTRAL, SUBLANG_NEUTRAL),
                       (LPSTR) &lpMsgBuf, 0, NULL);
#ifdef _CONSOLE
        fprintf(stderr, "%s (08X): %s\n", dwErr,
                szTitle, (LPCSTR)lpMsgBuf);
#else
        nRet = MessageBoxA(NULL, (LPCSTR)lpMsgBuf,
                           szTitle, nType);
#endif

        LocalFree(lpMsgBuf);
    }
    return nRet;
}

int x_cerror(int err)
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

static void x_perror(int err, const char *msg)
{
    if (err == 0)
        err = x_cerror(errno);

#ifdef _CONSOLE
    if (!opt_Quiet) {
        errno = err;
        perror(msg);
    }
    if (opt_Verbose)
        fprintf(stderr, "(%s): exit(%d)\n", GetProgramName(), err);
#else
    if (GetLastError() == 0 && err)
        SetLastError(err);
    if (!opt_Quiet)
        ErrorMessage(msg, NULL, TRUE);
#endif
    exit(err);
}

static void x_free(void *ptr)
{
    if (ptr)
        free(ptr);
}

static void *x_malloc(size_t len)
{
    void *ptr = calloc(1, len);

    if (!ptr) {
        x_perror(0, "Malloc");
    }
    return ptr;
}

static char *x_strdup(const char *s)
{
    char *p;
    size_t size;

    if (s != NULL)
        size = strlen(s);
    else
        return NULL;
    if (!size)
        return NULL;
    p = (char *)x_malloc(size + 2);
    memcpy(p, s, size);
    return p;
}

/*
 * Makes a full path from partial path
 */
static char *x_fullpath(const char *path, char *buf)
{
    char    full[MAX_PATH];
    char    *fn;
    size_t  len;

    len = GetFullPathNameA(path,
                           MAX_PATH,
                           full, &fn);
    if (len >= MAX_PATH) {
        errno = ERANGE;
        return NULL;
    }
    if (len == 0) {
        errno = EINVAL;
        return NULL;
    }
    if (buf) {
        strcpy(buf, full);
        return buf;
    }
    else
        return x_strdup(full);
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

static const unsigned char padchar[] =
"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
static int randseed = 0;

static char *x_mktemp(const char *pattern, int file)
{
    char path[MAX_PATH] = { 0 };
    int  randnum;
    char pbuf[MAX_PATH];
    register char *start, *trv, *suffp;

    if (pattern)
        x_strncpy(pbuf, pattern, MAX_PATH);
    else
        strcpy(pbuf, "_sx.XXXXXXXX");

    if (GetTempPathA(MAX_PATH - strlen(pbuf) - 1, path) == 0) {
        x_perror(0, "Temp Path");
        return NULL;
    }
    strcat(path, pbuf);
    for (trv = path; *trv; ++trv)
        ;
    suffp = trv;
    --trv;
    if (trv < path) {
        x_perror(EINVAL, "Temp Path");
        return NULL;
    }

    if (randseed == 0) {
        randseed = (int)time(NULL);
        srand(randseed);
    }
    /* Fill space with random characters */
    while (*trv == 'X') {
        randnum = rand() % (sizeof(padchar) - 1);
        *trv--  = padchar[randnum];
    }
    start = trv + 1;

    for (;;) {
        if (access(path, 0)) {
            if (errno == ENOENT) {
                if (file) {
                    FILE *fp = fopen(path, "w");
                    if (!fp) {
                        x_perror(0, path);
                        return NULL;
                    }
                    fclose(fp);
                }
                else {
                    if (mkdir(path)) {
                        x_perror(0, path);
                        return NULL;
                    }
                }
                return x_strdup(path);
            }
        }
        /* If we have a collision, cycle through the space of filenames */
        for (trv = start;;) {
            char *pad;
            if (*trv == '\0' || trv == suffp) {
                x_perror(ENOENT, "Temp Path");
                return NULL;
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
    return NULL;
}

LPCSTR
GetProgramName()
{
    char *p;

    if (szProgramName)
        return szProgramName;

    szProgramPath = (LPSTR)x_malloc(MAX_PATH);
    if (!GetModuleFileNameExA(GetCurrentProcess(), NULL,
                              szProgramPath, MAX_PATH))
        exit(GetLastError());
    szProgramExe = x_strdup(szProgramPath);
    if ((p = strrchr(szProgramPath, '\\')))
        *(p++) = '\0';
    else
        p = szProgramPath;
    szProgramName = x_strdup(p);
    /* Remove extension */
    if ((p = strrchr(szProgramName, '.')))
        *p = '\0';
    return szProgramName;
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

LPCSTR
GetProgramPath()
{
    char *p;

    if (szProgramPath)
        return szProgramPath;

    szProgramPath = (LPSTR)x_malloc(MAX_PATH);
    if (!GetModuleFileNameExA(GetCurrentProcess(), NULL,
                              szProgramPath, MAX_PATH))
        exit(GetLastError());
    szProgramExe = x_strdup(szProgramPath);
    if ((p = strrchr(szProgramPath, '\\')))
        *(p++) = '\0';
    else
        p = szProgramPath;
    szProgramName = x_strdup(p);
    /* Remove extension */
    if ((p = strrchr(szProgramName, '.')))
        *p = '\0';
    return szProgramPath;
}

static void vwarnx(const char *fmt, va_list ap)
{

    if (!opt_Quiet) {
        fprintf(stderr, "%s: ", GetProgramName());
        if (fmt != NULL)
            vfprintf(stderr, fmt, ap);
        fprintf(stderr, "\n");
    }
}

static void warnx(const char *fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    vwarnx(fmt, ap);
    va_end(ap);
}

/*
 * getopt --
 * Parse argc/argv argument vector.
 * However this one allows both - and / as argument options
 */
static int getopt(int nargc, const char **nargv, const char *ostr)
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
            if (strchr(ostr, first) == NULL)
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
    if (optopt == BADARG || (oli = strchr(ostr, optopt)) == NULL) {
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
                  optopt, GetProgramName());
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
                      optopt, GetProgramName());
            return BADCH;
        }
        place = EMSG;
        ++optind;
    }
    return optopt;            /* return option letter */
}

static int
SXDeleteDirectory(LPCSTR szName)
{
    SHFILEOPSTRUCTA shfop;

    memset(&shfop, 0, sizeof(shfop));
    shfop.wFunc     = FO_DELETE;
    shfop.pFrom     = szName;
    shfop.fFlags    = FOF_SILENT | FOF_NOERRORUI | FOF_NOCONFIRMATION;
    return SHFileOperationA(&shfop);
}

static int
SXCreateDirectory(LPCSTR szName)
{
    return SHCreateDirectoryExA(NULL, szName, NULL);
}


static LPSTR
BuildCommandLine(
    LPCSTR szCmdExe,
    LPCSTR szBatchFile,
    ...)
{
    char *buf;
    char *p;
    va_list vl;

    va_start(vl, szBatchFile);
    buf = x_malloc(MAX_CMDLINE);
    sprintf(buf, CMD_PREFIX, szCmdExe,
            _getpid(), szBatchFile);
    while ((p = va_arg(vl, char *)) != NULL) {
        if (p[strcspn(p, CMD_QUOTE)]) {
            strcat(buf, "\"");
            strcat(buf, p);
            strcat(buf, "\"");
        }
        else
            strcat(buf, p);
        strcat(buf, " ");
    }
    va_end(vl);
    strcat(buf, CMD_SUFIX);
    return buf;
}


static BOOL
RunChildProcess(
    LPCSTR szApplication,
    LPSTR szCmdLine,
    LPCSTR szWorkingPath,
    LPPROCESS_INFORMATION lpprInfo)
{
    STARTUPINFO stInfo;
    BOOL bResult;

    if (opt_Verbose) {
        fprintf(stdout, "Executing: %s\n", szApplication);
        fprintf(stdout, "           %s\n", szCmdLine);
        fprintf(stdout, "       in  %s\n", szWorkingPath);
    }
    ZeroMemory(&stInfo, sizeof(stInfo));
    stInfo.cb = sizeof(stInfo);
    stInfo.dwFlags = STARTF_USESHOWWINDOW;
    stInfo.wShowWindow = SW_HIDE;
    bResult = CreateProcess(szApplication,
                            szCmdLine,
                            NULL,
                            NULL,
                            TRUE,
                            CREATE_NEW_PROCESS_GROUP,
                            NULL,
                            szWorkingPath,
                            &stInfo,
                            lpprInfo);

    return bResult;
}

static BOOL
AcceptLicensePage(LPCSTR szPage)
{
    HANDLE  hHtml;
    CHAR    sBuf[MAX_PATH] = { 0 };
    CHAR   *retVal;
    BOOL    rv = FALSE;

    hHtml = DHTMLDialogInit(GetModuleHandle(NULL), szPage);
    if (IS_INVALID_HANDLE(hHtml))
        return FALSE;

    sprintf(sBuf, "%s", GetProgramName());
    DHTMLDialogRun(NULL, hHtml,
                   GuiLoadResource(IDS_LICTITLE, 0),
                   DEF_WWIDTH, DEF_WHEIGHT, 0, INFINITE,
                   sBuf);
    retVal = DHTMLDialogResult(hHtml);
    if (retVal && !strncmp(retVal, "OK", 2)) {
        rv = TRUE;
    }
    DHTMLDialogClose(hHtml);

    return rv;
}

static HANDLE
StartSplash(LPCSTR szPage, DWORD dwTimeout)
{
    HANDLE  hHtml;
    CHAR    sBuf[MAX_PATH] = { 0 };

    hHtml = DHTMLDialogInit(GetModuleHandle(NULL), szPage);
    if (IS_INVALID_HANDLE(hHtml))
        return NULL;

    sprintf(sBuf, "%s", GetProgramName());
    DHTMLDialogStart(NULL, hHtml,
                     "Initializing JBoss Installer",
                     DEF_WWIDTH, DEF_WHEIGHT, 1, dwTimeout,
                     sBuf);
    return hHtml;
}

static void
StopSplash(HANDLE hHtml)
{
    if (IS_INVALID_HANDLE(hHtml))
        return;
    DHTMLDialogStop(hHtml);
    DHTMLDialogClose(hHtml);
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
    if (IS_INVALID_HANDLE(hHtml))
        return FALSE;

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
    DHTMLDialogClose(hHtml);
    return rv;
}

static INT
AplMemCmp(LPCVOID lpA, LPCVOID lpB, SIZE_T nBytes)
{
    if (nBytes != 0) {
        const BYTE *p1 = lpA, *p2 = lpB;

        do {
            if (*p1++ != *p2++)
                return (*--p1 - *--p2);
        } while (--nBytes != 0);
    }
    return 0;
}

/*
 * Find the first occurrence of lpFind in lpMem.
 * dwLen:   The length of lpFind
 * dwSize:  The length of lpMem
 */
static LPBYTE
ApcMemSearch(LPCVOID lpMem, LPCVOID lpFind, SIZE_T dwLen, SIZE_T dwSize)
{
    BYTE   c, sc;
    SIZE_T cnt = 0;
    const BYTE *s = lpMem, *find = lpFind;

    if ((c = *find++) != 0) {
        do {
            do {
                sc = *s++;
                if (cnt++ > dwSize)
                    return NULL;
            } while (sc != c);
        } while (AplMemCmp(s, find, dwLen - 1) != 0);
        s--;
    }
    return (LPBYTE)s;
}


/* Merge the stub with jar file
 * Fix the userblock bounded code.
 */
static BOOL MergeConfig(size_t slen)
{
    HANDLE hmap;
    BOOL   rv = FALSE;
    BYTE   bmatch[16];

    BYTE *map, *ss;
    int i;
    if ((hmap = CreateFileMapping(hStub, NULL,
                                  PAGE_READWRITE, 0, 0, NULL)) == NULL) {
        x_perror(0, "Mapping Stub");
    }
    if ((map = MapViewOfFile(hmap, FILE_MAP_ALL_ACCESS, 0, 0, 0)) == NULL) {
        x_perror(0, "Mapping Stub View");
    }
    /* convert to upper case */
    for (i = 0; i < 15; i++)
        bmatch[i] = c_signature[i] - 32;
    bmatch[15] = 0;
    if ((ss = ApcMemSearch(map, bmatch, 16, slen)) != NULL) {
        st_config cfg;
        memcpy(&cfg, ss, sizeof(st_config));
        memcpy(ss, pConfig, sizeof(st_config));
        rv = TRUE;
    }
    UnmapViewOfFile(map);
    CloseHandle(hmap);
    return rv;
}

static void ExitCleanup(void)
{
    static int cleanup = 0;

    if (cleanup++)
        return;
    if (szTempPath)
        SXDeleteDirectory(szTempPath);
    x_free(szTempPath);
    GuiTerminate();
}

LRESULT CALLBACK MainWndProc(HWND hWnd, UINT uMsg,
                             WPARAM wParam, LPARAM lParam)
{
    switch (uMsg) {
        case WM_QUIT:
            return DefWindowProc(hWnd, uMsg, wParam, lParam);
        break;
        default:
            return DefWindowProc(hWnd, uMsg, wParam, lParam);
        break;
    }

    return FALSE;
}

#ifdef _CONSOLE
int
main(
    int argc,
    char *argv[])
{

    HINSTANCE hInstance = NULL;
#else
int WINAPI
WinMain(
    HINSTANCE hInstance,
    HINSTANCE hPrevInstance,
    LPSTR lpCmdLine,
    int nShowCmd)
{
#endif
    int    r, ch;
    int    ap = 1;
    BOOL   bRunScript;
    BOOL   bMakeTemp;
    BOOL   bShowSplash;
    BOOL   bShowLicense;
    BOOL   selectDest;
    DWORD  dwWinTimeout;
    DWORD  dwSplTimeout;
    BOOL   bMerge    = FALSE;

    LPSTR  szCmdLine = NULL;
    size_t l;
    CHAR   szBuf[MAX_PATH];
    CHAR   szCmdExe[MAX_PATH];
    CHAR   szWorkDir[MAX_PATH];
    CHAR   szDest[MAX_PATH] = { 0, 0 };

    LPSTR  szHtmlPage   = NULL;
    LPSTR  szHtmlTitle  = NULL;
    LPSTR  szHtmlParams = NULL;
    LPSTR  szHtmlResult = NULL;
    DWORD  dwHtmlWidth  = DEF_WWIDTH;
    DWORD  dwHtmlHeight = DEF_WHEIGHT;
    DWORD  dwFlags      = 0;
    LPCSTR szEulaPage   = "/HTML_EULAMAIN";
    HANDLE hSplash      = NULL;
    PROCESS_INFORMATION prInfo;

    atexit(ExitCleanup);
    pConfig = (st_config *)conf_in_exe;

    /* Read defaults from embedded config
     */
    if (pConfig->s_flags[0]) {
        bRunScript   = pConfig->s_flags[1];
        bMakeTemp    = pConfig->s_flags[2];
        selectDest   = pConfig->s_flags[3];
        opt_Verbose  = pConfig->s_flags[4];
        bShowSplash  = pConfig->s_flags[5];
        bShowLicense = pConfig->s_flags[6];
        dwWinTimeout = pConfig->s_iopts[0];
        dwSplTimeout = pConfig->s_iopts[1];
    }
    else {
        /* Default config
         * -q -x -S 0
         */
        bRunScript   = TRUE;
        bMakeTemp    = TRUE;
        selectDest   = FALSE;
        opt_Verbose  = FALSE;
        bShowSplash  = TRUE;
        bShowLicense = FALSE;
        dwWinTimeout = INFINITE;
        dwSplTimeout = INFINITE;
    }

    if (!GuiInitialize()) {
        exit(-1);
    }
    while ((ch = getopt(__argc, __argv, "aAd:Df:gh:lLen:p:qQr:s:S:t:T:uUvVw:xX")) != EOF) {
        switch (ch) {
            case 'A':
            case 'a':
                GuiAboutBox(NULL);
                r = 0;
                goto cleanup;
            break;
            case 'q':
            case 'Q':
                opt_Quiet   = TRUE;
                opt_Verbose = FALSE;
            break;
            case 'v':
            case 'V':
#ifdef _CONSOLE
                /* Verbosity makes sense in console
                 * mode only
                 */
                if (!opt_Quiet)
                    opt_Verbose = TRUE;
#endif
            break;
            case 'g':
                szEulaPage = "/HTML_LGPLMAIN";
            break;
            case 'D':
                szDest[0]  =  '~';
                selectDest = FALSE;
            break;
            case 'd':
                if (*optarg == '~') {
                    szDest[0] =  '~';
                    selectDest = FALSE;
                }
                if (*optarg == '?') {
                    selectDest = TRUE;
                }
                else {
                    if (x_fullpath(optarg, szDest))
                        selectDest = FALSE;
                }
            break;
            case 'n':
                szHtmlTitle  = x_strdup(optarg);
            break;
            case 'r':
                szHtmlPage   = x_fullpath(optarg, NULL);
            break;
            case 's':
                bShowSplash  = TRUE;
                dwSplTimeout = (DWORD)atoi(optarg);
            break;
            case 'S':
                bShowSplash  = TRUE;
                dwSplTimeout = (DWORD)atoi(optarg) * 1000;
            break;
            case 'f':
                szHtmlResult = x_fullpath(optarg, NULL);
            break;
            case 'p':
                szHtmlParams = x_strdup(optarg);
                for (l = 0; l < strlen(szHtmlParams); l++) {
                    if (szHtmlParams[l] == ',')
                        szHtmlParams[l] = '\t';
                }
            break;
            case 'l':
                bShowLicense = TRUE;
            break;
            case 'L':
                bShowLicense = FALSE;
            break;
            case 'e':
                bMerge       = TRUE;
            break;
            case 't':
                dwWinTimeout = (DWORD)atoi(optarg);
            break;
            case 'T':
                dwWinTimeout = (DWORD)atoi(optarg) * 1000;
            break;
            case 'w':
                dwHtmlWidth  = (DWORD)atoi(optarg);
            break;
            case 'h':
                dwHtmlHeight = (DWORD)atoi(optarg);
            break;
            case 'u':
                bMakeTemp    = TRUE;
            break;
            case 'U':
                bMakeTemp    = FALSE;
            break;
            case 'x':
                bRunScript   = TRUE;
            break;
            case 'X':
                bRunScript   = FALSE;
            break;
            case '?':
                r = EINVAL;
                goto cleanup;
            break;
        }
    }
    __argc -= optind;
    __argv += optind;

    if (dwWinTimeout == 0)
        dwWinTimeout = INFINITE;

    if (bMerge) {
        BY_HANDLE_FILE_INFORMATION si;
        if (__argc < 1) {
            x_perror(EINVAL, "Missing Argument");
        }
        GetProgramName();
        if ((hStub = CreateFile(__argv[0], GENERIC_READ | GENERIC_WRITE,
                                0, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL,
                                NULL)) == INVALID_HANDLE_VALUE) {
            x_perror(0, szProgramExe);
        }
        /* Get original stub size */
        GetFileInformationByHandle(hStub, &si);
        pConfig->s_flags[0] = 1;
        pConfig->s_flags[1] = bRunScript;
        pConfig->s_flags[2] = bMakeTemp;
        pConfig->s_flags[3] = selectDest;
        pConfig->s_flags[4] = opt_Verbose;
        pConfig->s_flags[5] = bShowSplash;
        pConfig->s_flags[6] = bShowLicense;
        pConfig->s_iopts[0] = dwWinTimeout;
        pConfig->s_iopts[1] = dwSplTimeout;
        r = MergeConfig(si.nFileSizeLow);
        CloseHandle(hStub);
        goto cleanup;
    }
    if (szHtmlPage) {
        if (!szHtmlTitle) {
            r = EINVAL;
            goto cleanup;
        }
        if (RunCustomPage(szHtmlPage,
                          szHtmlTitle,
                          szHtmlParams,
                          dwHtmlWidth,
                          dwHtmlHeight,
                          dwWinTimeout,
                          dwFlags,
                          szHtmlResult)) {
            r = 0;
        }
        else
            r = EACCES;
        x_free(szHtmlPage);
        x_free(szHtmlTitle);
        x_free(szHtmlParams);
        x_free(szHtmlResult);
        goto cleanup;
    }
    if (bShowSplash) {
        hSplash = StartSplash("/HTML_SPLASH", dwSplTimeout);
    }

    /* Standard Install */
    if (bShowLicense && !AcceptLicensePage(szEulaPage)) {
        r = EPERM;
        goto cleanup;
    }
    if (selectDest) {
        if (!GuiBrowseForFolder(NULL,
                                GuiLoadResource(IDS_SELDEST, 0),
                                szDest)) {
            r = EACCES;
            goto cleanup;
        }
    }
    if (szDest[0] && szDest[0] != '~' && access(szDest, 06)) {
        if (errno == ENOENT) {
            char msg[MAX_PATH + 64];
            sprintf(msg, GuiLoadResource(IDS_NDIRMSG, 0),
                         szDest);
            if (GuiYesNoMessage(NULL,
                                GuiLoadResource(IDS_NDIRTITLE, 1),
                                msg,
                                FALSE)) {
                if (SXCreateDirectory(szDest)) {
                    x_perror(0, szDest);
                }
            }
            else {
                r = 0;
                goto cleanup;
            }
        }
        else {
            x_perror(0, szDest);
        }
    }
    if (GetSystemDirectory(szBuf, MAX_PATH - 20)) {
        strcat(szBuf, "\\cmd.exe");
        if (strchr(szBuf, ' ')) {
            szCmdExe[0] = '"';
            strcpy(&szCmdExe[1], szBuf);
            strcat(szCmdExe, "\"");
        }
        else
            strcpy(szCmdExe, szBuf);
    }
    else {
        x_perror(0, "System Directory");
    }

    if (bMakeTemp) {
        if (!(szTempPath = x_mktemp(NULL, 0))) {
            x_perror(EPERM, "Creating Temp directory");
        }
        strcpy(szWorkDir, szTempPath);
        strcat(szWorkDir, DIR_BATCH);
        ppUnizpArgs[3] = szTempPath;
    }
    else {
        if (szDest[0] && szDest[0] != '~')
            ppUnizpArgs[3] = szDest;
        else {
            /* Nothing to do */
            r = 0;
            goto cleanup;
        }
    }
    if (opt_Verbose) {
        fprintf(stdout, "Running main: %s\n", szTempPath);
    }
    /* Unzip the embeded archive */
    CONSTRUCTGLOBALS();
    r = unzip(4,  ppUnizpArgs);
    if (r != 0) {
        SetLastError(ERROR_INVALID_BLOCK);
        x_perror(EINVAL, "Uncompressing");
    }
    DESTROYGLOBALS();
    StopSplash(hSplash);
    if (bRunScript) {
        szCmdLine = BuildCommandLine(szCmdExe,
                                     CMD_BATCH,
                                     CMD_PARAM,
                                     szDest,
                                     NULL);
        if (RunChildProcess(szCmdExe, szCmdLine, szWorkDir, &prInfo)) {
            WaitForSingleObject(prInfo.hProcess, INFINITE);
            r = 0;
        }
        else
            r = GetLastError();
    }
#if 0
    GuiOkMessage(NULL,
                 "Continue",
                 "Click OK to continue",
                 TRUE);
#endif

cleanup:
    if (opt_Verbose) {
        fprintf(stdout, "(%s) exit(%d)\n", GetProgramName(), r);
    }
    x_free(szCmdLine);
    ExitCleanup();
    return r;
}
