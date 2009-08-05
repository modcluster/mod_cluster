@echo off
REM Copyright(c) 2006 Red Hat Middleware, LLC,
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
@if "%OS%" == "Windows_NT"  setlocal
REM
set BUILDNAM=jboss-native
set BUILDVER=2.0.0
set BUILDSRC=windows-src
set BUILDROOT=%CD%
REM
@del /Q /F %BUILDNAM%-*.zip 2>NUL
@rmdir /S /Q %BUILDNAM%-%BUILDVER%-%BUILDSRC% 2>NUL
@rmdir /S /Q jbossnative 2>NUL
REM set JBATIVESVN=http://anonsvn.jboss.org/repos/jbossnative/tags/JBNATIVE_2_0_0
set JBNATIVESVN=http://anonsvn.jboss.org/repos/jbossnative/trunk/

svn export %JBNATIVESVN% jbossnative
cd jbossnative/build
call buildprep.bat
cd %BUILDROOT%
cd %BUILDNAM%-%BUILDVER%-%BUILDSRC%
call buildworld.bat sdk dll x86
call buildworld.bat sdk dll amd64
call buildworld.bat sdk dll ia64
REM Make APR and OpenSSL static builds
call buildworld.bat sdk lib x86
call buildworld.bat sdk lib amd64
call buildworld.bat sdk lib ia64
cd %BUILDROOT%
REM
REM Second run for nossl
REM
@rmdir /S /Q %BUILDNAM%-%BUILDVER%-%BUILDSRC% 2>NUL
cd jbossnative/build
call buildprep.bat nossl
cd %BUILDROOT%
cd %BUILDNAM%-%BUILDVER%-%BUILDSRC%
call buildworld.bat nossl sdk dll x86
call buildworld.bat nossl sdk dll amd64
call buildworld.bat nossl sdk dll ia64
REM Make APR static builds
call buildworld.bat nossl sdk lib x86
call buildworld.bat nossl sdk lib amd64
call buildworld.bat nossl sdk lib ia64
