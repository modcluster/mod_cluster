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
set BUILD_HOME=%CD%
popd

echo.
echo Running : %PROGNAME% $LastChangedDate: 2008-04-21 05:25:11 -0400 (Mon, 21 Apr 2008) $
echo.
echo Started : %DATE% %TIME%
echo Params  : %*
echo.

REM Check for cygwin in the PATH
@if "%CYGWIN_INSTALL_DIR%" == "" set CYGWIN_INSTALL_DIR=C:\cygwin
@if exist "%CYGWIN_INSTALL_DIR%\" goto cmdFoundCygwin
echo CYGWIN_INSTALL_DIR points to invalid directory %CYGWIN_INSTALL_DIR%
goto cmdEnd

:cmdFoundCygwin
set PATH=%CYGWIN_INSTALL_DIR%\bin;%PATH%

REM Then for an acceptable location.
bash check_path.sh
@if ERRORLEVEL 1 (
    echo Can't run inside cygwin install path
    echo Install the source in another location c:\home\my for example
    goto cmdEnd
)

@if exist "%LOCAL_DIR%\conf.%COMPUTERNAME%.bat" (
    echo "Using %COMPUTERNAME% specific configuration"
    call "%LOCAL_DIR%\conf.%COMPUTERNAME%"
) else (
    echo "Using default configuration"
    @if "%TOOLS_ROOT%" == "" set TOOLS_ROOT=C:\opt
    @if exist "%TOOLS_ROOT%\" goto cmdSetTools
    echo TOOLS_ROOT points to invalid directory %TOOLS_ROOT%
    goto cmdEnd

    :cmdSetTools
    @echo Using Tools from %TOOLS_ROOT%
    set MSVS8VC=%TOOLS_ROOT%\MSVS8\VC
    set MSVS6VC=%TOOLS_ROOT%\MSVS6
    set MSWPSDK=%TOOLS_ROOT%\PSDK6
)

set BUILD_OS=windows
set CRT_REDIST=
set USE_PLATFORM_SDK=
set APR_DECLARE_STATIC=
set INCLUDE_PRE64PRA=
set INCLUDE_BUFFEROVERFLOWU=

@if not "%1" == "sdk" goto checkSTATIC
shift
set USE_PLATFORM_SDK=1

:checkSTATIC
@if /i "%1" == "static" goto makeSTATIC
goto checkCPU

:makeSTATIC
shift
set APR_DECLARE_STATIC=1

:checkCPU
set BUILD_CPU=%1
shift
@if /i "%BUILD_CPU%" == "i686" goto cpuX86
@if /i "%BUILD_CPU%" == "x86" goto cpuX86
@if /i "%BUILD_CPU%" == "amd64" goto cpuX64
@if /i "%BUILD_CPU%" == "emt64" goto cpuX64
@if /i "%BUILD_CPU%" == "x86_64" goto cpuX64
@if /i "%BUILD_CPU%" == "x64" goto cpuX64
@if /i "%BUILD_CPU%" == "ia64" goto cpuI64
@if /i "%BUILD_CPU%" == "i64" goto cpuI64
echo Usage: %PROGNAME% CPU where CPU is x86 x64 or ia64
echo or   : %PROGNAME% sdk CPU if you are cross compiling
goto cmdEnd

:cpuX86
set BUILD_CPU=x86
@if "%USE_PLATFORM_SDK%" == "1" (
    call "%MSVS6VC%\vs6vars"
) else (
    call "%MSVS8VC%\bin\vcvars32"
    set CRT_REDIST=%MSVS8VC%\redist\x86\Microsoft.VC80.CRT
)
goto cmdBuild

:cpuX64
set BUILD_CPU=x64
@if "%USE_PLATFORM_SDK%" == "1" (
    call "%MSWPSDK%\SetEnv" /XP64 /RETAIL
) else (
    call "%MSVS8VC%\bin\amd64\vcvarsamd64"
    set CRT_REDIST=%MSVS8VC%\redist\amd64\Microsoft.VC80.CRT
)
goto cmdBuild

:cpuI64
set BUILD_CPU=i64
@if "%USE_PLATFORM_SDK%" == "1" (
    call "%MSWPSDK%\SetEnv" /SRV64 /RETAIL
) else (
    REM Update to correct IA64 paths
    call "%MSVS8VC%\bin\amd64\vcvarsamd64"
    set CRT_REDIST=%MSVS8VC%\redist\amd64\Microsoft.VC80.CRT
)
goto cmdBuild

:cmdBuild
@for /D %%i IN (%INCLUDE%) DO (
    @if exist "%%i\PRE64PRA.H" set INCLUDE_PRE64PRA=1
)

@for /D %%i IN (%LIB%) DO (
    @if exist "%%i\bufferoverflowu.lib" set INCLUDE_BUFFEROVERFLOWU=1
)

set INIT=%LOCAL_DIR%
pushd %BUILD_HOME%\unix
bash build.sh %1 %2 %3 %4 %5
popd

:cmdEnd
