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
 * Windows Console implementation
 *
 */
#include "sight.h"
#include "sight_local.h"
#include "sight_types.h"

#define SIGHT_WANT_LATE_DLL
#include "sight_private.h"


#define MAX_CONSOLE_SIZE    1024

typedef struct {
    HANDLE  hConI;
    HANDLE  hConO;
    DWORD   inState;
    WCHAR   echoChar;
    HWINSTA hWss;
    HWINSTA hWsu;
    HDESK   hWds;
    HDESK   hWdu;
} sight_nt_console_t;


static BOOL allocatedConsole = FALSE;
static BOOL shownWindow      = FALSE;

SIGHT_EXPORT_DECLARE(jlong, Console, alloc0)(SIGHT_STDARGS)
{
    sight_nt_console_t *con;
    UNREFERENCED_O;

    if (!(con = (sight_nt_console_t *)sight_calloc(_E, sizeof(sight_nt_console_t),
                                                   THROW_FMARK))) {
        return 0;
    }
    return P2J(con);
}

SIGHT_EXPORT_DECLARE(void, Console, open0)(SIGHT_STDARGS,
                                           jlong instance)
{
    sight_nt_console_t *con = J2P(instance, sight_nt_console_t *);
    UNREFERENCED_STDARGS;

    if (!con)
        return;
    con->hConI = CreateFile("CONIN$",
                            GENERIC_READ | GENERIC_WRITE,
                            FILE_SHARE_READ | FILE_SHARE_WRITE,
                            NULL,
                            OPEN_EXISTING,
                            0,
                            NULL);

    con->hConO = CreateFile("CONOUT$",
                            GENERIC_WRITE,
                            FILE_SHARE_READ | FILE_SHARE_WRITE,
                            NULL,
                            OPEN_EXISTING,
                            0,
                            NULL);
    GetConsoleMode(con->hConI, &con->inState);
}

SIGHT_EXPORT_DECLARE(jint, Console, attach0)(SIGHT_STDARGS,
                                            jint pid)
{
    HWND wnd;
    WINDOWINFO wi;

    UNREFERENCED_STDARGS;

    if ((wnd = GetConsoleWindow())) {
        if (GetWindowInfo(wnd, &wi)) {
            if (!(wi.dwStyle & WS_VISIBLE)) {
                ShowWindow(wnd, SW_SHOW);
                if (GetWindowInfo(wnd, &wi)) {
                    if (!(wi.dwStyle & WS_VISIBLE))
                        return APR_ENOTIMPL;
                }

                shownWindow = TRUE;
                return APR_SUCCESS;
            }
            else
                return APR_SUCCESS;
        }
    }
    if (allocatedConsole)
        FreeConsole();
    if (pid)
        allocatedConsole = AttachConsole((DWORD)pid);
    else
        allocatedConsole = AllocConsole();
    if (!allocatedConsole)
        return apr_get_os_error();
    else {
        if ((wnd = GetConsoleWindow())) {
            if (GetWindowInfo(wnd, &wi)) {
                if (!(wi.dwStyle & WS_VISIBLE)) {
                    FreeConsole();
                    return APR_ENOTIMPL;
                }
                else
                    return APR_SUCCESS;
            }
        }
        else
            return APR_ENOTIMPL;
    }
    return APR_SUCCESS;
}

SIGHT_EXPORT_DECLARE(void, Console, close0)(SIGHT_STDARGS,
                                            jlong instance)
{
    sight_nt_console_t *con = J2P(instance, sight_nt_console_t *);

    UNREFERENCED_STDARGS;

    if (!con)
        return;
    if (!IS_INVALID_HANDLE(con->hConI)) {
        /* Restore original mode */
        SetConsoleMode(con->hConI, con->inState);
        FlushConsoleInputBuffer(con->hConI);
        CloseHandle(con->hConI);
    }
    if (!IS_INVALID_HANDLE(con->hConO)) {
        CloseHandle(con->hConO);
    }
    if (allocatedConsole)
        FreeConsole();
    if (shownWindow) {
        HWND wnd = GetConsoleWindow();
        if (wnd)
            ShowWindow(wnd, SW_HIDE);
    }
    /* Restore window station and desktop.
     */
    if (con->hWds)
        SetThreadDesktop(con->hWds);
    if (con->hWss)
        SetProcessWindowStation(con->hWss);
    if (con->hWdu)
        CloseDesktop(con->hWdu);
    if (con->hWsu)
        CloseWindowStation(con->hWsu);
    con->hWss = NULL;
    con->hWds = NULL;
    con->hWdu = NULL;
    con->hWsu = NULL;
    free(con);
}

SIGHT_EXPORT_DECLARE(void, Console, echo0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jboolean on)
{
    sight_nt_console_t *con = J2P(instance, sight_nt_console_t *);

    UNREFERENCED_STDARGS;

    if (!con)
        return;
    if (!IS_INVALID_HANDLE(con->hConI)) {
        if (on) {
            SetConsoleMode(con->hConI, ENABLE_LINE_INPUT |
                                       ENABLE_PROCESSED_INPUT |
                                       ENABLE_ECHO_INPUT);
            con->echoChar = 0;
        }
        else {
            SetConsoleMode(con->hConI, ENABLE_LINE_INPUT |
                                       ENABLE_PROCESSED_INPUT);
            con->echoChar = L'*';
        }
    }
}

SIGHT_EXPORT_DECLARE(void, Console, stitle0)(SIGHT_STDARGS,
                                             jstring title)
{
    SIGHT_ALLOC_WSTRING(title);
    UNREFERENCED_O;

    SIGHT_INIT_WSTRING(title);
    SetConsoleTitleW(J2C(title));
    SIGHT_FREE_WSTRING(title);
}

SIGHT_EXPORT_DECLARE(jstring, Console, gtitle0)(SIGHT_STDARGS)
{
    WCHAR szBuf[SIGHT_HBUFFER_SIZ];
    DWORD dwLen;
    UNREFERENCED_O;

    if ((dwLen = GetConsoleTitleW(szBuf, SIGHT_HBUFFER_SIZ)) != 0) {
        return ZSTR_TO_JSTRING(&szBuf[0], dwLen);
    }
    else
        return NULL;
}

SIGHT_EXPORT_DECLARE(void, Console, kill3)(SIGHT_STDARGS)
{

    UNREFERENCED_O;
    JVM_DumpAllStacks(_E, NULL);
}


SIGHT_EXPORT_DECLARE(jstring, Console, gets0)(SIGHT_STDARGS,
                                              jlong instance)
{
    sight_nt_console_t *con = J2P(instance, sight_nt_console_t *);
    jchar *wBuf = NULL;
    DWORD  wLen;
    jstring rv = NULL;

    UNREFERENCED_O;
    if (!con) {
        return NULL;
    }
    if (!(wBuf = (jchar *)sight_calloc(_E,
                               MAX_CONSOLE_SIZE * sizeof(jchar),
                               THROW_FMARK))) {
        return NULL;
    }

    if (!ReadConsoleW(con->hConI,
                      wBuf,
                      MAX_CONSOLE_SIZE,
                      &wLen,
                      NULL)) {
        throwAprIOException(_E, apr_get_os_error());
        goto cleanup;
    }
            /* set length of string and null terminate it */

    if (wBuf[wLen] == L'\r') {
        rv = ZSTR_TO_JSTRING(wBuf, wLen - 2);
    }
    else if ((wLen == MAX_CONSOLE_SIZE) && (wBuf[wLen - 1] == L'\r')) {
        rv = ZSTR_TO_JSTRING(wBuf, wLen - 1);
    }
    else {
        if (wLen > 0 && wBuf[wLen - 1] == L'\n')
            --wLen;
        if (wLen > 0 && wBuf[wLen - 1] == L'\r')
            --wLen;
        if (wLen)
            rv = ZSTR_TO_JSTRING(wBuf, wLen);
        else
            rv = CSTR_TO_JSTRING("");
    }

cleanup:
    if (wBuf)
        free(wBuf);
    return rv;

}

SIGHT_EXPORT_DECLARE(jint, Console, getc0)(SIGHT_STDARGS,
                                           jlong instance)
{
    sight_nt_console_t *con = J2P(instance, sight_nt_console_t *);
    WCHAR wBuf[1];
    DWORD  wLen;

    UNREFERENCED_O;
    if (!con) {
        return -1;
    }
    if (!ReadConsoleW(con->hConI,
                      wBuf,
                      1,
                      &wLen,
                      NULL)) {
        throwAprIOException(_E, apr_get_os_error());
        return -1;
    }
    return wBuf[0];

}

SIGHT_EXPORT_DECLARE(void, Console, putc0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jint ch)
{
    sight_nt_console_t *con = J2P(instance, sight_nt_console_t *);
    WCHAR wBuf[1];
    DWORD  wLen;

    UNREFERENCED_O;
    if (!con) {
        return;
    }
    wBuf[0] = (WCHAR)ch;
    if (!WriteConsoleW(con->hConO,
                       wBuf,
                       1,
                       &wLen,
                       NULL)) {
        throwAprIOException(_E, apr_get_os_error());
    }
}

SIGHT_EXPORT_DECLARE(void, Console, flush0)(SIGHT_STDARGS,
                                            jlong instance)
{
    UNREFERENCED_STDARGS;
    UNREFERENCED(instance);
}

SIGHT_EXPORT_DECLARE(void, Console, puts0)(SIGHT_STDARGS,
                                           jlong instance,
                                           jstring str)
{
    sight_nt_console_t *con = J2P(instance, sight_nt_console_t *);
    SIGHT_ALLOC_WSTRING(str);
    DWORD  wLen;

    UNREFERENCED_O;
    SIGHT_INIT_WSTRING(str);
    if (!con) {
        SIGHT_FREE_WSTRING(str);
        return;
    }

    if (!WriteConsoleW(con->hConO,
                       J2C(str),
                       JWL(str),
                       &wLen,
                       NULL)) {
        throwAprIOException(_E, apr_get_os_error());
    }

    SIGHT_FREE_WSTRING(str);
}


SIGHT_EXPORT_DECLARE(jint, Console, denable0)(SIGHT_STDARGS,
                                              jlong instance)
{
    sight_nt_console_t *con = J2P(instance, sight_nt_console_t *);
    USEROBJECTFLAGS uof;
    DWORD dwLen;
    UNREFERENCED_STDARGS;
    if (!con) {
        return APR_EINVAL;
    }
    if (con->hWdu)
        return APR_SUCCESS;
    /* Ensure connection to service window station and desktop, and
     * save their handles.
     */
    GetDesktopWindow();
    con->hWss = GetProcessWindowStation();
    if (GetUserObjectInformation(con->hWss, UOI_FLAGS,
                                 &uof, sizeof(USEROBJECTFLAGS),
                                 &dwLen)) {
        if (!(uof.dwFlags & WSF_VISIBLE)) {
            con->hWss = NULL;
            return APR_ENOTIMPL;
        }
        else {
            // However, note that the following registry key contains
            // a value, NoInteractiveServices, that controls the effect
            // of SERVICE_INTERACTIVE_PROCESS:
            // HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Windows
            //
            // The NoInteractiveServices value defaults to 0 (zero), which
            // means that services with SERVICE_INTERACTIVE_PROCESS are
            // allowed to run interactively. When NoInteractiveServices
            // is set to a nonzero value, no service started thereafter is
            // allowed to run interactively, regardless of whether it has
            // SERVICE_INTERACTIVE_PROCESS
        }
    }
    else {
        con->hWss = NULL;
        return apr_get_os_error();
    }

    con->hWds = GetThreadDesktop(GetCurrentThreadId());

    /* Impersonate the client and connect to the User's
     * window station and desktop.
     */
    con->hWsu = OpenWindowStation("WinSta0", FALSE, MAXIMUM_ALLOWED);
    if (con->hWsu == NULL) {
        con->hWss = NULL;
        con->hWds = NULL;
        return apr_get_os_error();
    }
    SetProcessWindowStation(con->hWsu);
    con->hWdu = OpenDesktop("Default", 0, FALSE, MAXIMUM_ALLOWED);
    if (con->hWdu == NULL) {
        DWORD rc = GetLastError();
        SetProcessWindowStation(con->hWss);
        CloseWindowStation(con->hWsu);
        con->hWss = NULL;
        con->hWds = NULL;
        con->hWsu = NULL;
        return APR_FROM_OS_ERROR(rc);
    }
    SetThreadDesktop(con->hWdu);

    return APR_SUCCESS;
}

SIGHT_EXPORT_DECLARE(void, Console, ddisable0)(SIGHT_STDARGS,
                                               jlong instance)
{
    sight_nt_console_t *con = J2P(instance, sight_nt_console_t *);

    UNREFERENCED_STDARGS;
    if (!con) {
        return;
    }
    /* Restore window station and desktop.
     */
    if (con->hWds)
        SetThreadDesktop(con->hWds);
    if (con->hWss)
        SetProcessWindowStation(con->hWss);
    if (con->hWdu)
        CloseDesktop(con->hWdu);
    if (con->hWsu)
        CloseWindowStation(con->hWsu);
    con->hWss = NULL;
    con->hWds = NULL;
    con->hWdu = NULL;
    con->hWsu = NULL;
}
