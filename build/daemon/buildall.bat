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
set DIRNAME=.\
set PROGNAME=buildall.bat
)

pushd %DIRNAME%..
set BUILD_HOME=%CD%
popd

echo.
echo Running : %PROGNAME% $LastChangedDate: 2008-07-23 10:17:15 -0400 (Wed, 23 Jul 2008) $
echo.
echo Started : %DATE% %TIME%
echo.


pushd %BUILD_HOME%\windows

call build.bat sdk x86 jboss-native -ssl -cache
call build.bat sdk x64 jboss-native -ssl -cache
call build.bat sdk i64 jboss-native -ssl -cache

REM build http using rhel repos
call build.bat sdk x86 rhel-httpd -ssl -cache
call build.bat sdk x64 rhel-httpd -ssl -cache
call build.bat sdk i64 rhel-httpd -ssl -cache

REM Uncomment for building sight
REM call build.bat sdk x86 jboss-sight -cache
REM call build.bat sdk x64 jboss-sight -cache
REM call build.bat sdk i64 jboss-sight -cache

popd
