@echo off
REM Copyright(c) 2009 Red Hat Middleware, LLC,
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
REM @author Jean-Frederic Clere
REM
@echo on

IF NOT EXIST "@BASELOC@\httpd-2.4" (
  SET HTTPD_VERSION=2.2
) ELSE (
  SET HTTPD_VERSION=2.4
)

@if "%1" == "stop" goto stop
@if "%1" == "start" goto start

"@BASELOC@\httpd-%HTTPD_VERSION%\bin\httpd.exe" -k install
dir "@BASELOC@\httpd-%HTTPD_VERSION%\logs"
goto end

:start
REM install will test httpd but using the current user so remove the logs files.
REM wait a little (2 ways)
REM choice /c C /D C /t 30
REM ping -n 31 localhost > NUL
dir "@BASELOC@\httpd-%HTTPD_VERSION%\logs"
del "@BASELOC@\httpd-%HTTPD_VERSION%\logs\access_log"
del "@BASELOC@\httpd-%HTTPD_VERSION%\logs\error_log"
REM "@BASELOC@\httpd-%HTTPD_VERSION%\bin\httpd.exe" -k start
net start Apache%HTTPD_VERSION%
dir "@BASELOC@\httpd-%HTTPD_VERSION%\logs"
goto end

:stop
"@BASELOC@\httpd-%HTTPD_VERSION%\bin\httpd.exe" -k stop
"@BASELOC@\httpd-%HTTPD_VERSION%\bin\httpd.exe" -k uninstall
:end
