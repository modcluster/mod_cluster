@echo off
REM Copyright(c) 2007 Red Hat Middleware, LLC,
REM and individual contributors as indicated by the @authors tag.
REM See the copyright.txt in the distribution for a
REM full listing of individual contributors.
REM
REM This library is free software; you can redistribute it and/or
REM modify it under the terms of the GNU Lesser General Public
REM License as published by the Free Software Foundation; either
REM version 2 of the License, or (at your option) any later version.
REM
REM This library is distributed in the hope that it will be useful,
REM but WITHOUT ANY WARRANTY; without even the implied warranty of
REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
REM Lesser General Public License for more details.
REM
REM You should have received a copy of the GNU Lesser General Public
REM License along with this library in the file COPYING.LIB;
REM if not, write to the Free Software Foundation, Inc.,
REM 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
REM
REM @author Mladen Turk
REM


@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal

if "%OS%" == "Windows_NT" (
set "DIRNAME=%~dp0%"
set "PROGNAME=%~nx0%"
) else (
echo Detected uncompatible Windows version.
goto cmdEnd
)

set LOCAL_DIR=%CD%
pushd %DIRNAME%..
set INSTALL_HOME=%CD%
popd

echo.
echo Running : %PROGNAME% $LastChangedDate: 2008-04-21 11:25:11 +0200 (Mon, 21 Apr 2008) $
echo.
echo Started : %DATE% %TIME%
echo Params  : %*
echo.

set HTTPD_SERVERNAME=localhost
set HTTPD_DOMAINNAME=localdomain
set HTTPD_PORT=8080
set HTTPD_SSLPORT=8443

set HTTPD_ADMIN=%USERNAME%@%HTTPD_SERVERNAME%.%HTTPD_DOMAINNAME%
set HTTPD_ROOT=%INSTALL_HOME%


nawk.exe -f installconf.awk -v WINDOWS=1 %HTTPD_DOMAINNAME% %HTTPD_SERVERNAME% %HTTPD_ADMIN% %HTTPD_PORT% %HTTPD_SSLPORT% "%HTTPD_ROOT%"
if "x%NOPAUSE%" == "x" pause
:cmdEnd
