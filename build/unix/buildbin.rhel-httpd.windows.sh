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
# @author Mladen Turk
#
echo ""
echo "Running : `basename $0` $LastChangedDate: 2007-07-02 16:22:00 +0200 (Mon, 02 Jul 2007) $"
echo ""
echo "Started : `date`"
echo "Common  : $1"
echo "Prefix  : $2"
echo "Output  : $3"
echo "OpenSSL : $4"
echo "Static  : $5"
echo "Sources : $6"
echo ""


# parameters
# $1: Location of the common libraries.
# $2: Destination location.
# $3: Location where to put the binaries.
# $4: Use OpenSSL.
# $5: Use static build.
# $6: Location of the sources.

common_loc=$1
prefix_loc=$2
output_loc=$3
has_openssl=$4
has_static=$5
sources_loc=$6
current_loc=`pwd`

win_common_loc=`cygpath -w -a ${common_loc}`
win_prefix_loc=`cygpath -w -a ${prefix_loc}`

echo "Configuring Httpd"
add_conf="PREFIX=${win_prefix_loc}"
if $has_openssl ; then
  add_conf="${add_conf} WITH_OPENSSL=${win_common_loc}"
fi

mkdir -p ${output_loc}/httpd-2.2
win_output_loc=`cygpath -w -a ${output_loc}/httpd-2.2`

native_sources=srclib/`ls srclib | grep httpd-2.2`
src_dir=`cygpath -w -a ${native_sources}` 

(cd $native_sources
 echo "Running nmake at `pwd`"
 nmake -f NMAKEmakefile PREFIX=${win_output_loc} ${add_conf} install
)

echo "Done"
exit 0
