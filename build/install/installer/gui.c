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

int
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

void
GuiUnregisterWindow(int nIndex)
{
    if (nIndex >= 0 && nIndex < MGUI_WINDOWS)
        gui_Windows[nIndex] = NULL;
}

BOOL
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

BOOL
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
LPSTR
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

void
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

BOOL
GuiBrowseForFolder(
    HWND hWnd,
    LPCSTR szTitle,
    LPSTR  szPath)
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
    bi.ulFlags        = BIF_USENEWUI;
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

static void
guiShellAbout(HWND hWnd)
{
    CHAR szApplication[512];

    sprintf(szApplication , "About - %s#Windows",
            GuiLoadResource(IDS_APPLICATION, 0));
    ShellAboutA(hWnd, szApplication,
                GuiLoadResource(IDS_APPDESCRIPTION, 1),
                gui_h48Icon);
}


static LRESULT CALLBACK
guiAboutDlgProc(
    HWND hDlg,
    UINT uMsg,
    WPARAM wParam,
    LPARAM lParam)
{
    static  HWND  hRich = NULL;
    static  POINT ptScroll;
    static  int   nIndex = -1;
    HRSRC   hRsrc;
    HGLOBAL hGlob;
    LPSTR   szTxt;

    switch (uMsg) {
        case WM_DESTROY:
            GuiUnregisterWindow(nIndex);
            nIndex = -1;
            PostQuitMessage(0);
        break;
        case WM_CLOSE:
            GuiUnregisterWindow(nIndex);
            nIndex = -1;
            return FALSE;
        break;
        case WM_INITDIALOG:
            nIndex = GuiRegisterWindow(hDlg);
            GuiCenterWindow(hDlg);
            hRich = GetDlgItem(hDlg, IDC_LICENSE);
            hRsrc = FindResourceA(GetModuleHandleA(NULL),
                                  MAKEINTRESOURCE(IDR_LICENSE), "RTF");
            hGlob = LoadResource(GetModuleHandleA(NULL), hRsrc);
            szTxt = (LPSTR)LockResource(hGlob);

            SendMessageA(hRich, WM_SETTEXT, 0, (LPARAM)szTxt);
            SetDlgItemText(hDlg, IDC_ABOUTAPP,
                           GuiLoadResource(IDS_APPFULLNAME, 0));
            ptScroll.x = 0;
            ptScroll.y = 0;
            return TRUE;
        break;
        case WM_COMMAND:
            if (LOWORD(wParam) == IDOK || LOWORD(wParam) == IDCANCEL) {
                EndDialog(hDlg, LOWORD(wParam));
                return TRUE;
            }
            else if (LOWORD(wParam) == IAB_SYSINF)
                guiShellAbout(hDlg);
        break;
        case WM_MOUSEWHEEL:
            {
                int nScroll, nLines;
                if ((SHORT)HIWORD(wParam) < 0)
                    nScroll = gui_ucNumLines;
                else
                    nScroll = gui_ucNumLines * (-1);
                ptScroll.y += (nScroll * 11);
                if (ptScroll.y < 0)
                    ptScroll.y = 0;
                nLines = (int)SendMessage(hRich, EM_GETLINECOUNT,
                                          0, 0) + 1;
                if (ptScroll.y / 11 > nLines)
                    ptScroll.y = nLines * 11;
                SendMessage(hRich, EM_SETSCROLLPOS, 0, (LPARAM)&ptScroll);
            }
        break;

    }
    return FALSE;
}

void
GuiAboutBox(HWND hWnd)
{
    DialogBox(gui_hInstance,
              MAKEINTRESOURCE(IDD_ABOUTBOX),
              hWnd,
              (DLGPROC)guiAboutDlgProc);
}

BOOL
GuiYesNoMessage(
    HWND hWnd,
    LPCTSTR szTitle,
    LPCTSTR szMessage,
    BOOL bStop)
{
    UINT uType = MB_YESNO | MB_APPLMODAL;

    if (bStop)
        uType |= MB_DEFBUTTON2 | MB_ICONEXCLAMATION;
    else
        uType |= MB_DEFBUTTON1 | MB_ICONQUESTION;

    return (MessageBoxA(hWnd, szMessage, szTitle, uType) == IDYES);
}

BOOL
GuiOkMessage(
    HWND hWnd,
    LPCTSTR szTitle,
    LPCTSTR szMessage,
    BOOL bStop)
{
    UINT uType = MB_OK | MB_APPLMODAL;

    if (bStop)
        uType |= MB_ICONSTOP;
    else
        uType |= MB_ICONINFORMATION;

    return (MessageBoxA(hWnd, szMessage, szTitle, uType) == IDYES);
}
