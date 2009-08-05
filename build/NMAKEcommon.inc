# Copyright 2001-2006 The Apache Software Foundation or its licensors, as
# applicable.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ====================================================================
#
# NMAKEcommon.inc Master makefile definitions.
#                 This file defines CPU architecture and basic compiler
#                 and linker parameters.
# Common params:
#                 CPU       Compile for specified CPU. Supported CPU's are:
#                           X86 (Common x86 architecture)
#                           X64 (AMD64/EMT64 architecture)
#                           I64 (Intel IA64 architecture)
#                           If not specified it will default to the
#                           PROCESSOR_ARCHITECTURE environment variable
#                           or to the X86 if not specified.
#                 WINVER    Compile for specified Windows version
#                           WINNT   for Windows 2000 and up(default)
#                           WINXP   for Windows XP and up
#                           WIN2003 for Windows 2003 and up
#                           VSITA   for Windows Vista and up
#                           Deprecated targets (may not even compile):
#                               NT4     for Windows NT4 and up
#                               WIN9X   for Windows 95, 98 and Me
#                 BUILD     Build version
#                           RETAIL or RELEASE (default)
#                           DEBUG
#                 TARGET    Build application target
#                           EXE Console executable (default)
#                           GUI Windows GUI executable
#                           DLL Dynamic Link Library
#                           LIB Static library
# Environment variables  used:
#                 CFLAGS    Added to the common CFLAGS
#                 CPPFLAGS  Added to the common CPPFLAGS
#                 LIBS      Added to the common LIBS
#                 INCLUDES  Added to the common INCLUDES
#                 LFLAGS    Added to the common LFLAGS
#                 RCFLAGS   Added to the common RCFLAGS
#
# Compiler tools environment variables:
#                 CC        C compiler  (defaults to cl.exe)
#                 LINK      Linker (defaults to link.exe)
#                 RC        Resource compiler (defaults to rc.exe)
#                 MT        Manifest toolkit (defaults to mt.exe)
#                 ML        Assembler (defaults to ml.exe or ml64.exe)
#
# Originally contributed by Mladen Turk <mturk jboss.com>
#
# ====================================================================
#

!IF !DEFINED(CC) || "$(CC)" == ""
CC = cl.exe
!ENDIF

!IF !DEFINED(LINK) || "$(LINK)" == ""
LINK = link.exe
!ENDIF

!IF !DEFINED(RC) || "$(RC)" == ""
RC = rc.exe
!ENDIF

!IF !DEFINED(MT) || "$(MT)" == ""
MT = mt.exe
!ENDIF

# Read the PROCESSOR_ARCHITECTURE environment value for a CPU type

!IF "$(BUILD_CPU)" == "i386" || "$(BUILD_CPU)" == "x86" || "$(BUILD_CPU)" == "i686"
CPU=X86
!ENDIF
!IF "$(BUILD_CPU)" == "amd64" || "$(BUILD_CPU)" == "x86_64" || "$(BUILD_CPU)" == "x64"
CPU=X64
!ENDIF
!IF "$(BUILD_CPU)" == "ia64" || "$(BUILD_CPU)" == "i64"
CPU=I64
!ENDIF

!IF !DEFINED(CPU) || "$(CPU)" == ""
!IF "$(PROCESSOR_ARCHITECTURE)" == ""
!IF "$(PROCESSOR_ARCHITEW6432)" == ""
CPU=X86
!ELSE
CPU=$(PROCESSOR_ARCHITEW6432)
!ENDIF
!ELSE
CPU=$(PROCESSOR_ARCHITECTURE)
!ENDIF
!ENDIF


!IF "$(CPU)" == "I386"
CPU=X86
!ENDIF
!IF "$(CPU)" == "AMD64"
CPU=X64
!ENDIF
!IF "$(CPU)" == "IA64"
CPU=I64
!ENDIF

!IF "$(CPU)" != "X86"
!IF "$(CPU)" != "X64"
!IF "$(CPU)" != "I64"
!ERROR Must specify CPU environment variable (X86, X64, I64) $(CPU)
!ENDIF
!ENDIF
!ENDIF

!IF !DEFINED(TARGET) || "$(TARGET)" == ""
TARGET=EXE
!ENDIF

!IF "$(TARGET)" != "EXE"
!IF "$(TARGET)" != "GUI"
!IF "$(TARGET)" != "DLL"
!IF "$(TARGET)" != "LIB"
!ERROR Must specify TARGET environment variable (EXE, GUI, DLL, LIB)
!ENDIF
!ENDIF
!ENDIF
!ENDIF

!IF !DEFINED(WINVER) || "$(WINVER)" == ""
WINVER=WINXP
!ENDIF


!IF "$(WINVER)" != "WINNT"
!IF "$(WINVER)" != "WINXP"
!IF "$(WINVER)" != "WIN2003"
!IF "$(WINVER)" != "NT4"
!IF "$(WINVER)" != "WIN9X"
!IF "$(WINVER)" != "WVSITA"
!ERROR Must specify WINVER environment variable (WINNT, WINXP, WIN2003, VSITA, NT4, WIN9X)
!ENDIF
!ENDIF
!ENDIF
!ENDIF
!ENDIF
!ENDIF

!IF "$(WINVER)" == "WINNT"
NMAKE_WINVER = 0x0500
_WIN32_IE = 0x0500
!ELSEIF "$(WINVER)" == "WINXP"
NMAKE_WINVER = 0x0501
_WIN32_IE = 0x0600
!ELSEIF "$(WINVER)" == "WIN2003"
NMAKE_WINVER = 0x0502
_WIN32_IE = 0x0600
!ELSEIF "$(WINVER)" == "VSITA"
NMAKE_WINVER = 0x0600
_WIN32_IE = 0x0700
!ELSEIF "$(WINVER)" == "NT4"
NMAKE_WINVER = 0x0400
_WIN32_IE = 0x0400
!ELSE
!ERROR Must specify WINVER environment variable (WINNT, WINXP, WIN2003, VSITA, NT4, WIN9X)
!ENDIF

!IF !DEFINED(NMAKE_WINVER) || "$(NMAKE_WINVER)" == ""
NMAKE_WINVER = 0x0400
_WIN32_IE = 0x0400
NMAKE_WINNT  = -D_WIN95 -D_WIN32_WINDOWS=$(NMAKE_WINVER) -DWINVER=$(NMAKE_WINVER) -D_WIN32_IE=$(_WIN32_IE)
!ELSE
NMAKE_WINNT  = -D_WINNT -D_WIN32_WINNT=$(NMAKE_WINVER) -DWINVER=$(NMAKE_WINVER) -D_WIN32_IE=$(_WIN32_IE)
!ENDIF

!IF !DEFINED(BUILD) || "$(BUILD)" == ""
BUILD=RELEASE
!ENDIF

!IFDEF RELEASE
BUILD=RELEASE
!ENDIF

!IFDEF DEBUG
BUILD=DEBUG
!ENDIF

!IFDEF NODEBUG
BUILD=RELEASE
!ENDIF

!IF "$(BUILD)" != "RELEASE"
!IF "$(BUILD)" != "DEBUG"
!ERROR Must specify BUILD environment variable (RELEASE, DEBUG)
!ENDIF
!ENDIF

# Common flags for all platforms
CMN_CFLAGS =  -c -nologo -DWIN32 -D_WIN32 -D_WINDOWS $(NMAKE_WINNT) -W3
!IF "$(TARGET)" == "EXE"
CMN_CFLAGS = $(CMN_CFLAGS) -D_CONSOLE
!ENDIF

!IF "$(CPU)" == "X86"
CPU_CFLAGS = -D_X86_=1
MACHINE=X86
!ELSEIF "$(CPU)" == "X64"
CPU_CFLAGS = -D_AMD64_=1 -DWIN64 -D_WIN64 -Wp64
MACHINE=AMD64
!ELSEIF "$(CPU)" == "I64"
CPU_CFLAGS = -D_IA64_=1 -DWIN64 -D_WIN64 -Wp64
MACHINE=IA64
!ENDIF

!IF "$(INCLUDE_PRE64PRA)" == "1"
!IF "$(CPU)" != "X86"
CPU_CFLAGS = $(CPU_CFLAGS) /FIPRE64PRA.H
!ENDIF
!ENDIF

!IF "$(BUILD)" == "RELEASE"
!IF "$(CPU)" == "X86"
OPT_CFLAGS = -O2 -Ob2 -Oy- -Zi -DNDEBUG
!ELSE
OPT_CFLAGS = -O2 -Ob2 -Zi -DNDEBUG
!ENDIF
!ELSE
OPT_CFLAGS = -Od -Zi -DDEBUG -D_DEBUG
!ENDIF

!IF DEFINED(STATIC_CRT)
CRT_CFLAGS = -D_MT -MT
!ELSE
CRT_CFLAGS = -D_MT -MD
!ENDIF

!IF "$(BUILD)" == "DEBUG"
CRT_CFLAGS = $(CRT_CFLAGS)d
!ENDIF

CFLAGS = $(CMN_CFLAGS) $(CPU_CFLAGS) $(OPT_CFLAGS) $(CRT_CFLAGS) $(CFLAGS)

!IF DEFINED(EXTRA_CFLAGS)
CFLAGS = $(CFLAGS) $(EXTRA_CFLAGS)
!ENDIF


# Linker section
LIBS = kernel32.lib advapi32.lib ws2_32.lib mswsock.lib ole32.lib shell32.lib rpcrt4.lib $(LIBS)
!IF "$(INCLUDE_BUFFEROVERFLOWU)" == "1"
!IF "$(CPU)" != "X86"
LIBS = $(LIBS) bufferoverflowu.lib
!ENDIF
!ENDIF

!IF DEFINED(EXTRA_LIBS)
LIBS = $(LIBS) $(EXTRA_LIBS)
!ENDIF

COMMON_LFLAGS = /NOLOGO

# Always add debugging to the linker
OPT_LFLAGS = /INCREMENTAL:NO /DEBUG
!IF "$(BUILD)" == "RELEASE"
OPT_LFLAGS = /OPT:REF
!ENDIF

!IF "$(TARGET)" == "EXE"
LFLAGS = $(COMMON_LFLAGS) /SUBSYSTEM:CONSOLE /MACHINE:$(MACHINE)
!ELSEIF "$(TARGET)" == "GUI"
LFLAGS = $(COMMON_LFLAGS) /SUBSYSTEM:WINDOWS /MACHINE:$(MACHINE)
!ELSEIF "$(TARGET)" == "DLL"
LFLAGS = $(COMMON_LFLAGS) /DLL /SUBSYSTEM:WINDOWS /MACHINE:$(MACHINE)
!ELSEIF "$(TARGET)" == "LIB"
LFLAGS = -lib $(COMMON_LFLAGS)
!ENDIF

!IF DEFINED(EXTRA_LFLAGS)
LFLAGS = $(LFLAGS) $(EXTRA_LFLAGS)
!ENDIF

!IF "$(TARGET)" != "LIB"
LFLAGS = $(LFLAGS) $(OPT_LFLAGS)
!ENDIF

# Resource compiler flags

BASE_RCFLAGS=/l 0x409
!IF "$(BUILD)" == "RELEASE"
BASE_RCFLAGS = $(BASE_RCFLAGS) /d "NDEBUG"
!ELSE
BASE_RCFLAGS = $(BASE_RCFLAGS) /d "_DEBUG" /d "DEBUG"
!ENDIF
RCFLAGS = $(BASE_RCFLAGS) $(RCFLAGS)

# Build Target dir e.g. WINNT_I386_RELEASE_DLL
!IF !DEFINED(WORKDIR) || "$(WORKDIR)" == ""
!IF !DEFINED(WORKDIR_EXT) || "$(WORKDIR_EXT)" == ""
WORKDIR = $(WINVER)_$(CPU)_$(TARGET)_$(BUILD)
WORKDIR_DLL = $(WINVER)_$(CPU)_DLL_$(BUILD)
WORKDIR_LIB = $(WINVER)_$(CPU)_LIB_$(BUILD)
WORKDIR_EXE = $(WINVER)_$(CPU)_EXE_$(BUILD)
!ELSE
WORKDIR = $(WINVER)_$(CPU)_$(BUILDIR_EXT)_$(BUILD)
!ENDIF
!ENDIF

!IF "$(OS)" == "Windows_NT"
CLEANTARGET=if exist "$(WORKDIR)\$(NULL)" rd /s /q $(WORKDIR)
!ELSE
CLEANTARGET=deltree /y $(BUILDIR)
!ENDIF

MAKEWORKDIR=if not exist "$(WORKDIR)\$(NULL)" mkdir $(WORKDIR)
MAKEINSTALL=if not exist "$(INSTALLLOC)" mkdir $(INSTALLLOC)

!IF DEFINED(JAVA_HOME) && "$(JAVA_HOME)" != ""
JAVA_INCLUDES=-I "$(JAVA_HOME)\include"
!IF "$(CPU)" != "X86"
JAVA_INCLUDES=$(JAVA_INCLUDES) -I "$(JAVA_HOME)\include\win64"
!ENDIF
JAVA_INCLUDES=$(JAVA_INCLUDES) -I "$(JAVA_HOME)\include\win32"
!ENDIF

!IF !DEFINED(ML) || "$(ML)" == ""
!IF "$(CPU)" == "X86"
ML = ml.exe
AFLAGS = /coff /Zi /c
!ELSEIF "$(CPU)" == "X64"
ML = ml64.exe
AFLAGS = /Zi /c
!ELSEIF "$(CPU)" == "I64"
ML = ml64.exe
AFLAGS = /coff /Zi /c
!ENDIF
!ENDIF
