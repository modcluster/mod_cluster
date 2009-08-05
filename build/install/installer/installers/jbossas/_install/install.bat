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
REM -------------------------------------------------------------------------
REM JBoss Installer Script for Windows
REM -------------------------------------------------------------------------
REM @author Mladen Turk
REM

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal enableextensions

SET RETVAL=0
if "%OS%" == "Windows_NT" (
  set "INSTALL_HOME=%~dp0%"
  set "INSTALL_PROG=%~nx0%"
) else (
  echo Detected uncompatible Windows version.
  set RETVAL=2
  goto cmdEnd
)

REM -------------------------------------------------------------------------
REM Start of package definitions
REM -------------------------------------------------------------------------

set PACKAGE_NAME=jboss-5.0.0.Beta4
set INSTALL_NAME=JBoss Application Server
set INSTALL_DESC=JBoss Application Server 5.0.0.Beta4

REM -------------------------------------------------------------------------
REM End of package definitions
REM -------------------------------------------------------------------------

echo Started %INSTALL_PROG% %DATE% %TIME%

pushd %INSTALL_HOME%..
set INSTALL_BASE=%CD%\
popd

echo INSTALL_PROG: %INSTALL_PROG%
echo INSTALL_HOME: %INSTALL_HOME%
echo INSTALL_BASE: %INSTALL_BASE%

set XTOOLRC=
xtool.exe html -q eulamain.htm > .result
set /P XTOOLRC= < .result
del /Q /F .result

if NOT "%XTOOLRC%" == "OK" (
  xtool msg -qt MB_OK -i s "License Agreement" "Cannot continue.##You must accept License Agreement to install this software"
  echo License Agreement not accepted
  SET RETVAL=3
  goto cmdEnd
)  

set INSTALL_DEST=
xtool.exe dir -q "Select Installation Folder##The Software will be installed in the selected location" > .result
for /f "usebackq tokens=*" %%I in (.result) do (
  if exist "%%I" set INSTALL_DEST=%%I
)
del /Q /F .result

set errorlevel=
if NOT "x%INSTALL_DEST%" == "x" (
  xtool msg -qt MB_OKCANCEL -i i "Start Installation" "%INSTALL_DESC% is ready to install.##Location: %INSTALL_DEST%"
) else (
  echo Destination directory not specified
  set RETVAL=1
  goto cmdEnd
)

if not %errorlevel% == 1 (
  echo Installation canceled
  set RETVAL=1
  goto cmdEnd
)  


set errorlevel=
if exist "%INSTALL_DEST%\%PACKAGE_NAME%" (
  xtool msg -qt MB_YESNO -i ? "Directory exist" "%INSTALL_DESC% in##%INSTALL_DEST%\%PACKAGE_NAME% already exist.##Do you wish to overwite the target"
  echo ERR1 %errorlevel%
) else (
  set errorlevel=6
)

if not %errorlevel% == 6 (
  echo Installation overwrite denied
  set RETVAL=1
  goto cmdEnd
)

REM Do an actual installation in INSTALL_DEST/PACKAGE_NAME by copying the files from
REM INSTALL_BASE/PACKAGE_NAME

xtool xcopy -q "Installing %INSTALL_NAME%" "%INSTALL_BASE%%PACKAGE_NAME%" "%INSTALL_DEST%\%PACKAGE_NAME%"
if %errorlevel% == 0 (
  xtool msg -qt MB_OK -i ? "Istallation finished" "%INSTALL_DESC% installed in##%INSTALL_DEST%\%PACKAGE_NAME%"
)

:cmdEnd
echo Finished %INSTALL_PROG% %DATE% %TIME%
exit %RETVAL%
