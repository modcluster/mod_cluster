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

/**
 * Console implementation
 *
 */
#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"
#include "sight_private.h"

/* If unistd.h defines _POSIX_VERSION, we conclude that we
 * are on a POSIX system and have sigaction and termios. */
#if defined(_POSIX_VERSION)

# define SIGACTION
# if !defined(TERMIOS) && !defined(TERMIO) && !defined(SGTTY)
#  define TERMIOS
# endif
#endif

/* There are 5 types of terminal interface supported,
 * TERMIO, TERMIOS, VMS, MSDOS and SGTTY
 */

#if defined(__sgi) && !defined(TERMIOS)
# define TERMIOS
# undef  TERMIO
# undef  SGTTY
#endif

#if defined(linux) && !defined(TERMIO)
# undef  TERMIOS
# define TERMIO
# undef  SGTTY
#endif

#ifdef _LIBC
# undef  TERMIOS
# define TERMIO
# undef  SGTTY
#endif

#if !defined(TERMIO) && !defined(TERMIOS)
# undef  TERMIOS
# undef  TERMIO
# define SGTTY
#endif

#ifdef TERMIOS
# include <termios.h>
# define TTY_STRUCT            struct termios
# define TTY_FLAGS            c_lflag
# define TTY_get(tty,data)    tcgetattr(tty,data)
# define TTY_set(tty,data)    tcsetattr(tty,TCSANOW,data)
#endif

#ifdef TERMIO
# include <termio.h>
# define TTY_STRUCT            struct termio
# define TTY_FLAGS            c_lflag
# define TTY_get(tty,data)    ioctl(tty,TCGETA,data)
# define TTY_set(tty,data)    ioctl(tty,TCSETA,data)
#endif

#ifdef SGTTY
# include <sgtty.h>
# define TTY_STRUCT            struct sgttyb
# define TTY_FLAGS            sg_flags
# define TTY_get(tty,data)    ioctl(tty,TIOCGETP,data)
# define TTY_set(tty,data)    ioctl(tty,TIOCSETP,data)
#endif

#if !defined(_LIBC)
# include <sys/ioctl.h>
#endif

#ifndef NX509_SIG
# define NX509_SIG 32
#endif


/* Define globals.  They are protected by a lock */
#ifdef SIGACTION
static struct sigaction savsig[NX509_SIG];
#else
static void (*savsig[NX509_SIG])(int );
#endif

#define DEV_TTY     "/dev/tty"

static TTY_STRUCT   tty_orig;
static TTY_STRUCT   tty_new;

static FILE        *tty_in;
static FILE        *tty_out;
static int          is_a_tty;

static volatile sig_atomic_t intr_signal;

static void recsig(int i)
{
    intr_signal = i;
}


/* Internal functions to handle signals and act on them */
static void pushsig(void)
{
    int i;
#ifdef SIGACTION
    struct sigaction sa;

    memset(&sa, 0, sizeof sa);
    sa.sa_handler = recsig;
#endif

    for (i = 1; i < NX509_SIG; i++) {
#ifdef SIGUSR1
        if (i == SIGUSR1)
            continue;
#endif
#ifdef SIGUSR2
        if (i == SIGUSR2)
            continue;
#endif
#ifdef SIGKILL
        if (i == SIGKILL) /* We can't make any action on that. */
            continue;
#endif
#ifdef SIGACTION
        sigaction(i, &sa, &savsig[i]);
#else
        savsig[i] = signal(i, recsig);
#endif
    }

#ifdef SIGWINCH
    signal(SIGWINCH, SIG_DFL);
#endif
}

static void popsig(void)
{
    int i;

    for (i = 1; i < NX509_SIG; i++) {
#ifdef SIGUSR1
        if (i == SIGUSR1)
            continue;
#endif
#ifdef SIGUSR2
        if (i == SIGUSR2)
            continue;
#endif
#ifdef SIGACTION
        sigaction(i, &savsig[i], NULL);
#else
        signal(i, savsig[i]);
#endif
    }
}

typedef struct {
    FILE        *coni;
    FILE        *cono;
    TTY_STRUCT   ttyo;
    TTY_STRUCT   ttyn;
    int          is_a_tty;
    int          echo_char;
} sight_console_t;

/* Internal functions to read a string without echoing */
static void read_till_nl(FILE *in)
{
#define SIZE 4
    char buf[SIZE+1];

    do {
        fgets(buf, SIZE, in);
    } while (strchr(buf, '\n') == NULL);
}

static int echo_console(sight_console_t *con, jboolean on)
{

#ifdef TTY_FLAGS
    memcpy(&(con->ttyn), &(con->ttyo), sizeof(con->ttyo));
    if (on) {
        con->ttyn.TTY_FLAGS |= ECHO;
    }
    else {
        con->ttyn.TTY_FLAGS &= ~ECHO;
    }
#endif

#if defined(TTY_set)
    if (con->is_a_tty && (TTY_set(fileno(con->coni), &(con->ttyn)) == -1)) {
        return errno;
    }
#endif
    return 0;
}


SIGHT_EXPORT_DECLARE(jlong, Console, alloc0)(SIGHT_STDARGS)
{
    sight_console_t *con;
    UNREFERENCED_O;

    if (!(con = (sight_console_t *)calloc(1, sizeof(sight_console_t)))) {
        throwAprMemoryException(_E, THROW_FMARK,
                                apr_get_os_error());
        return 0;
    }
    return P2J(con);
}

SIGHT_EXPORT_DECLARE(void, Console, open0)(SIGHT_STDARGS,
                                           jlong instance)
{
    sight_console_t *con = J2P(instance, sight_console_t *);
    UNREFERENCED_STDARGS;

    if (!con)
        return;
    con->is_a_tty = 1;
    if ((con->coni = fopen(DEV_TTY, "r")) == NULL)
        con->coni  = stdin;
    if ((con->cono = fopen(DEV_TTY, "w")) == NULL)
        con->cono  = stdout;

    if (TTY_get(fileno(con->coni), &con->ttyo) == -1) {
#ifdef ENOTTY
        if (errno == ENOTTY)
            con->is_a_tty = 0;
        else
#endif
#ifdef EINVAL
        /* Ariel Glenn ariel@columbia.edu reports that solaris
         * can return EINVAL instead.  This should be ok
         */
        if (errno == EINVAL)
            con->is_a_tty = 0;
        else
#endif
        {
            /* TODO: Trow an exception ? */
            return;
        }
    }

}

SIGHT_EXPORT_DECLARE(jint, Console, attach0)(SIGHT_STDARGS,
                                            jint pid)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(pid);
    return APR_SUCCESS;
}

SIGHT_EXPORT_DECLARE(void, Console, close0)(SIGHT_STDARGS,
                                            jlong instance)
{
    sight_console_t *con = J2P(instance, sight_console_t *);

    UNREFERENCED_STDARGS;

    if (!con)
        return;
    if (con->coni && con->echo_char) {

#ifdef TTY_FLAGS
        memcpy(&(con->ttyn), &(con->ttyo), sizeof(con->ttyo));
        con->ttyn.TTY_FLAGS |= ECHO;
#endif

#if defined(TTY_set)
        if (con->is_a_tty && (TTY_set(fileno(con->coni), &(con->ttyn)) == -1)) {
            /* TODO: Do we need throw on error here? */
        }
#endif
    }
    if (con->coni && con->coni != stdin)
        fclose(con->coni);
    if (con->cono && con->cono != stdout)
        fclose(con->cono);
    free(con);
}

SIGHT_EXPORT_DECLARE(void, Console, echo0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jboolean on)
{
    sight_console_t *con = J2P(instance, sight_console_t *);
    UNREFERENCED_STDARGS;

    if (on)
        con->echo_char = 0;
    else
        con->echo_char = '*';
}

SIGHT_EXPORT_DECLARE(void, Console, stitle0)(SIGHT_STDARGS,
                                             jstring title)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(title);
}

SIGHT_EXPORT_DECLARE(jstring, Console, gtitle0)(SIGHT_STDARGS)
{
    UNREFERENCED_O;
    return CSTR_TO_JSTRING("Unknown");
}

SIGHT_EXPORT_DECLARE(void, Console, kill3)(SIGHT_STDARGS)
{

    UNREFERENCED_STDARGS;
    kill(getpid(), SIGQUIT);
}


SIGHT_EXPORT_DECLARE(jstring, Console, gets0)(SIGHT_STDARGS,
                                              jlong instance)
{
    sight_console_t *con = J2P(instance, sight_console_t *);
    static int ps;
    char buff[SIGHT_HBUFFER_SIZ];
    char *p;
    jstring rv = NULL;

    UNREFERENCED_O;
    if (!con) {
        return NULL;
    }

    fflush(stdout);
    ps = 0;
    pushsig();
    ps = 1;
    if (con->echo_char && echo_console(con, JNI_FALSE))
        goto cleanup;
    ps = 2;
    p = fgets(buff, SIGHT_HBUFFER_LEN, con->coni);
    if (!p)
        goto cleanup;

    if (feof(con->coni)) {
        goto cleanup;
    }
    if (ferror(con->coni)) {
        throwAprIOException(_E, apr_get_os_error());
        goto cleanup;
    }
    if ((p = (char *)strchr(buff, '\n')) != NULL)
        *p = '\0';
    else
        read_till_nl(con->coni);

    rv = CSTR_TO_JSTRING(buff);

cleanup:
    if (intr_signal == SIGINT) {

        /* TODO: Throw some exception */
    }
    if (ps >= 2 && con->echo_char)
        echo_console(con, JNI_TRUE);
    if (ps >= 1)
        popsig();
    return rv;
}

SIGHT_EXPORT_DECLARE(jint, Console, getc0)(SIGHT_STDARGS,
                                           jlong instance)
{
    sight_console_t *con = J2P(instance, sight_console_t *);
    int ch;
    static int ps;
    UNREFERENCED_O;
    if (!con) {
        return -1;
    }

    ps = 0;
    pushsig();
    ps = 1;
    if (con->echo_char && echo_console(con, JNI_FALSE))
        goto cleanup;
    ps = 2;

    ch = fgetc(con->coni);
    if (ch == -1) {
        if (ferror(con->coni)) {
            throwAprIOException(_E, apr_get_os_error());
        }
    }

    if (intr_signal == SIGINT) {
        /* TODO: Throw some exception */
        ch = -1;
    }

cleanup:
    if (ps >= 2 && con->echo_char)
        echo_console(con, JNI_TRUE);
    if (ps >= 1)
        popsig();
    return ch;
}

SIGHT_EXPORT_DECLARE(void, Console, putc0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jint ch)
{
    sight_console_t *con = J2P(instance, sight_console_t *);
    UNREFERENCED_STDARGS;
    if (con->cono) {
        fputc(ch, con->cono);
    }
}

SIGHT_EXPORT_DECLARE(void, Console, flush0)(SIGHT_STDARGS,
                                            jlong instance)
{
    sight_console_t *con = J2P(instance, sight_console_t *);
    if (con->cono)
        fflush(con->cono);
    UNREFERENCED_STDARGS;
}

SIGHT_EXPORT_DECLARE(void, Console, puts0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jstring str)
{
    sight_console_t *con = J2P(instance, sight_console_t *);
    SIGHT_ALLOC_CSTRING(str);

    UNREFERENCED_O;
    if (con->cono) {
        fputs(J2S(str), con->cono);
    }
    SIGHT_FREE_CSTRING(str);
}


SIGHT_EXPORT_DECLARE(jint, Console, denable0)(SIGHT_STDARGS,
                                              jlong instance)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
    return APR_SUCCESS;
}

SIGHT_EXPORT_DECLARE(void, Console, ddisable0)(SIGHT_STDARGS,
                                               jlong instance)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
}
