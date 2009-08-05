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
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#define HBUF_SIZE    8192
#define MBUF_SIZE    4096

static SHOWHTMLDIALOGEXFN  *pfnShowHTMLDialog = NULL;

typedef struct {
    IMoniker       *lpImk;
    CHAR            szReturnVal[HBUF_SIZE];
    HWND            hWnd;
    HANDLE          hThread;
    int             iIndex;    
} MC_HTML_DIALOG, *LPMC_HTML_DIALOG;

typedef struct DHTML_THREAD_PARAMS {
    BOOL             bDone;
    HWND             hParent;
    LPMC_HTML_DIALOG hHtmlDlg;
    WCHAR            szOptions[1024];
    WCHAR            szArguments[MBUF_SIZE];
} DHTML_THREAD_PARAMS;

LPWSTR
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

LPSTR
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


HANDLE
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

void
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

extern HICON     gui_h16Icon;

BOOL
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

BOOL
DHTMLDialogStart(
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
    LPMC_HTML_DIALOG    hDlg = (LPMC_HTML_DIALOG)hHtmlDlg;
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
    hDlg->hThread = hThread;
    hDlg->hWnd    = hWnd;
    hDlg->iIndex  = w;
    return TRUE;
}

BOOL
DHTMLDialogStop(HANDLE hHtmlDlg)
{
    DWORD  i;
    BOOL   rv = FALSE;
    LPMC_HTML_DIALOG    hDlg = (LPMC_HTML_DIALOG)hHtmlDlg;

    if (IS_INVALID_HANDLE(hHtmlDlg))
        return FALSE;
    /* Wait for at least one second so that
     * window doesn't close too early
     */
    if ((i = WaitForSingleObject(hDlg->hThread, 1000)) == WAIT_TIMEOUT) {
        if (hDlg->hWnd) {
            SendMessage(hDlg->hWnd, WM_CLOSE, 0, 0);
            WaitForSingleObject(hDlg->hThread, INFINITE);
        }
    }
    if (GetExitCodeThread(hDlg->hThread, &i) && i == 0) {
        rv = TRUE;
    }
    CloseHandle(hDlg->hThread);
    GuiUnregisterWindow(hDlg->iIndex);

    return rv;
}
