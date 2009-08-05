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
echo "Running : `basename $0` $LastChangedDate: 2007-05-31 13:40:31 -0400 (Thu, 31 May 2007) $"
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

echo "Configuring Sight"
platform_path="SIGHT-BIN/${BUILD_SYS}/${BUILD_CPU}"
mkdir -p ${output_loc}/${platform_path}
mkdir -p ${output_loc}/lib
mkdir -p ${output_loc}/doc/sight

src_dir=`cygpath -w -a sight/native`

# Build native part.
(cd sight/native
 echo "Building Sight native"
 nmake -f NMAKEmakefile WITH_APR=${win_common_loc} PREFIX=${win_prefix_loc} SRCDIR=${src_dir} install
)

# Build java part.
(cd sight
 echo "Building Sight java"
 ant
 ant javadocs
 ant jar
)

current_loc=`pwd`
cp ${common_loc}/lib/libapr*.dll ${output_loc}/${platform_path} 2>/dev/null
cp ${prefix_loc}/lib/*.dll ${output_loc}/${platform_path} 2>/dev/null

cp sight/dist/sight*.jar ${output_loc}/lib
cp -rp sight/dist/doc/api ${output_loc}/doc/sight

if [ -n "${CRT_REDIST}" ] ; then
  crt_redist_loc=`cygpath -u ${CRT_REDIST}`
  cp `cygpath -u ${CRT_REDIST}`/*.dll ${output_loc}/${platform_path}
fi
