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

#ifndef SINSTALL_H
#define SINSTALL_H

/*
 * Ignore most warnings (back down to /W3) for poorly constructed headers
 */
#if defined(_MSC_VER) && _MSC_VER >= 1200
#pragma warning(push, 3)
#endif

/* disable or reduce the frequency of...
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

/* Ignore Microsoft's interpretation of secure development
 * and the POSIX string handling API
 */
#if defined(_MSC_VER) && _MSC_VER >= 1400
#ifndef _CRT_SECURE_NO_DEPRECATE
#define _CRT_SECURE_NO_DEPRECATE
#endif
#pragma warning(disable: 4996)
#endif

#ifndef _WINDOWS_
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#ifndef _WIN32_WINNT
/* Restrict the server to a subset of Windows 2000 header files by default
 */
#define _WIN32_WINNT 0x0500
#endif
#endif
// #define _WIN32_DCOM

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
#include <urlmon.h>
#include <mshtmhst.h>
#include <lm.h>
#include <psapi.h>

#define IDC_STATIC          -1
#define IDC_APPLICATION     100
#define IDI_MAINICON        101
#define IDI_RHELICON        102

#define IDS_APPLICATION     150
#define IDS_APPDESCRIPTION  151
#define IDS_APPVERSION      152
#define IDS_APPCOPYRIGHT    153
#define IDS_APPFULLNAME     154
#define IDS_ALLFILES        155
#define IDS_DLLFILES        156
#define IDS_EXEFILES        157
#define IDS_PPIMAGE         158
#define IDS_SELDEST         159
#define IDS_NDIRMSG         160
#define IDS_NDIRTITLE       162
#define IDS_LICTITLE        163

#define IDD_ABOUTBOX        250
#define IDC_LICENSE         251
#define IDR_LICENSE         252
#define IDR_EULA            253
#define IAB_SYSINF          254
#define IDC_ABOUTAPP        255

#define SIZ_BUFLEN              1024
#define SIZ_BUFMAX              (SIZ_BUFLEN - 1)
#define IS_INVALID_HANDLE(x)    (((x) == NULL || (x) == INVALID_HANDLE_VALUE))

LPCSTR  GetProgramName();

LPWSTR  AnsiToWide(LPCSTR, LPWSTR, DWORD);
LPSTR   WideToAnsi(LPCWSTR, LPSTR, DWORD);

BOOL    GuiInitialize();
BOOL    GuiTerminate();
int     GuiRegisterWindow();
void    GuiUnregisterWindow(int);


LPSTR   GuiLoadResource(UINT, UINT);
void    GuiCenterWindow(HWND);
BOOL    GuiBrowseForFolder(HWND, LPCSTR, LPSTR);
void    GuiAboutBox(HWND);
BOOL    GuiYesNoMessage(HWND, LPCTSTR, LPCTSTR, BOOL);
BOOL    GuiOkMessage(HWND, LPCTSTR, LPCTSTR, BOOL);

HICON   gui_h16Icon;
HICON   gui_h32Icon;
HICON   gui_h48Icon;
HMODULE gui_hMSHTML;
HWND    gui_DialogWnd;

HANDLE  DHTMLDialogInit(HINSTANCE, LPCSTR);
void    DHTMLDialogClose(HANDLE);
BOOL    DHTMLDialogRun(HWND, HANDLE, LPCSTR, DWORD, DWORD, DWORD, DWORD, LPCSTR);
LPSTR   DHTMLDialogResult(HANDLE);
BOOL    DHTMLDialogStart(HWND, HANDLE, LPCSTR, DWORD, DWORD, DWORD, DWORD, LPCSTR);
BOOL    DHTMLDialogStop(HANDLE);

#endif /* SINSTALL_H */
