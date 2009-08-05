@echo off
REM
REM JBoss, the OpenSource J2EE webOS
REM
REM Distributable under LGPL license.
REM See terms of license at gnu.org.
REM
REM
REM @author Mladen Turk
REM @version $Revision: 4368 $, $Date: 2006-05-23 09:39:38 +0200 (uto, 23 svi 2006) $
REM
REM
REM call vsvars32
REM
@if "%OS%" == "Windows_NT"  setlocal
@rmdir /S /Q out32
@rmdir /S /Q out32dll
@rmdir /S /Q tmp32
@rmdir /S /Q tmp32dll

perl Configure VC-NT
call ms\do_nt

@if "%1" == "dll" goto DLL
@goto LIB
:DLL
nmake -f ms\ntdll.mak
@goto END
:LIB
nmake -f ms\nt.mak
:END
