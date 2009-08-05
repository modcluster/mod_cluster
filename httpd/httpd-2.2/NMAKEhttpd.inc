# Copyright 2001-2007 The Apache Software Foundation or its licensors, as
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
# NMAKEhttpd.inc  Master HTTPD definitions.
#
# Originally contributed by Mladen Turk <mturk redhat.com>
#
# ====================================================================
#

BUILDINC = $(PREFIX)\include
BUILDLIB = $(PREFIX)\lib
BUILDOUT = $(PREFIX)\bin
BUILDMOD = $(PREFIX)\modules
BUILDMAN = $(PREFIX)\manual
BUILDDOC = $(PREFIX)\htdocs
BUILDCFG = $(PREFIX)\conf
BUILDERR = $(PREFIX)\error
BUILDICO = $(PREFIX)\icons
BUILDCGI = $(PREFIX)\cgi-bin
BUILDLOG = $(PREFIX)\logs
BUILDINS = $(PREFIX)\~

LIBAPR   = libapr-1.lib
LIBAPU   = libaprutil-1.lib
LIBAPI   = libapriconv-1.lib
APR      = apr-1.lib
APU      = aprutil-1.lib
API      = apriconv-1.lib
PCRE     = pcre.lib
ZLIB     = zlib-1.lib
LIBZLIB  = libzlib-1.lib
LIBHTTPD = libhttpd.lib
LIBICONV = libiconv-1.lib

!IF "$(TARGET)" != "LIB" 
LFLAGS = $(LFLAGS) /version:2.2
!ENDIF

!IFDEF WITH_GNU_ICONV
API = $(LIBICONV)
LIBAPI = $(LIBICONV)
!ENDIF



DISTSUBDIRS = \
	$(PREFIX)\include\arch\win32\~ \
	$(PREFIX)\lib\~ \
	$(PREFIX)\bin\~ \
	$(PREFIX)\modules\~ \
	$(PREFIX)\manual\~ \
	$(PREFIX)\htdocs\~ \
	$(PREFIX)\icons\~ \
	$(PREFIX)\cgi-bin\~ \
	$(PREFIX)\logs\~ \
	$(PREFIX)\error\include\~ \
	$(PREFIX)\conf\extra\~ \
	$(PREFIX)\conf\default\extra\~

$(DISTSUBDIRS):
	@if not exist "$(@D)\$(NULL)" mkdir $(@D)

$(BUILDINS): $(DISTSUBDIRS)
