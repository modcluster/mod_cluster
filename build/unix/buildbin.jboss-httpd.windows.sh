#!/bin/sh
# Copyright(c) 2007 Red Hat Middleware, LLC,
# and individual contributors as indicated by the @authors tag.
# See the copyright.txt in the distribution for a
# full listing of individual contributors.
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library in the file COPYING.LIB;
# if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
#
# @author Jean-Frederic Clere
#
echo ""
echo "Running : `basename $0` $LastChangedDate: 2007-05-31 19:40:31 +0200 (Thu, 31 May 2007) $"
echo ""
echo "Started : `date`"
echo "Common  : $1"
echo "Prefix  : $2"
echo "Output  : $3"
echo "OpenSSL : $4"
echo ""


# parameters
# $1: Location of the common libraries.
# $2: Destination location.
# $3: Location where to put the binaries.
# $4: Use OpenSSL.

common_loc=$1
prefix_loc=$2
output_loc=$3
has_openssl=$4

win_common_loc=`cygpath -w -a ${common_loc}`
win_prefix_loc=`cygpath -w -a ${prefix_loc}`

echo "Configuring Httpd"
platform_path="HTTPD-BIN/${BUILD_SYS}/${BUILD_CPU}" 
mkdir -p ${output_loc}/${platform_path} 
win_output_loc=`cygpath -w -a ${output_loc}/${platform_path}`

add_conf="PREFIX=${win_output_loc}"
if $has_openssl ; then
  add_conf="${add_conf} WITH_OPENSSL=${win_common_loc}"
fi

native_sources=srclib/`ls srclib | grep httpd-2.2`

(cd $native_sources
 echo "Running nmake at `pwd`"
 nmake -a -f NMAKEmakefile ${add_conf} install
)
 