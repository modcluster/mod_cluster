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
echo "Running : `basename $0` $LastChangedDate: 2008-04-03 08:13:02 -0400 (Thu, 03 Apr 2008) $"
echo ""
echo "Started : `date`"
echo "Common  : $1"
echo "Prefix  : $2"
echo "Output  : $3"
echo "OpenSSL : $4"
echo "Static  : $5"
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
has_static=$5

output_lib="META-INF/lib/${BUILD_SYS}/${BUILD_CPU}"
output_bin="META-INF/bin/${BUILD_SYS}/${BUILD_CPU}"

if $has_static; then
  common_loc=${common_loc}-static
fi

win_common_loc=`cygpath -w -a ${common_loc}`
win_prefix_loc=`cygpath -w -a ${prefix_loc}`

echo "Configuring tomcat-native"
add_conf="PREFIX=${win_prefix_loc}"
if $has_openssl ; then
  add_conf="${add_conf} WITH_OPENSSL=${win_common_loc}"
fi

if $has_static; then
  add_conf="${add_conf} APR_DECLARE_STATIC=true"
fi

mkdir -p ${output_loc}/bin/${output_lib}
mkdir -p ${output_loc}/bin/${output_bin}
native_sources=srclib/`ls srclib | grep tomcat-native`
src_dir=`cygpath -w -a ${native_sources}`
(cd $native_sources
 nmake -f NMAKEmakefile WITH_APR=${win_common_loc} ${add_conf} SRCDIR=${src_dir} install
)

# Uncomment to build jbosssch
# (cd service/jbosssch
#  nmake -f NMAKEmakefile PREFIX=${win_prefix_loc} install
# )

(cd service/jbosssvc
 nmake -f NMAKEmakefile PREFIX=${win_prefix_loc} install
)

(cd service/examples
 awk -f rewritesvc.awk service.bat ${output_loc}/bin/service.bat "5.0.0 GA" 5 0 "Windows ${BUILD_CPU}"
 cp README-service.txt ${output_loc}/bin
)

(cd service/procrun
 nmake -f NMAKEsvc PREFIX=${win_prefix_loc} install
 nmake -f NMAKEmgr PREFIX=${win_prefix_loc} install
)

cp ${common_loc}/lib/libapr*.dll ${output_loc}/bin/${output_lib} 2>/dev/null
if $has_openssl ; then
  cp ${common_loc}/bin/*eay32.dll ${output_loc}/bin/${output_lib} 2>/dev/null
  cp ${common_loc}/bin/openssl.exe ${output_loc}/bin/${output_bin}
fi
cp ${prefix_loc}/lib/*.dll ${output_loc}/bin/${output_lib} 2>/dev/null
cp ${prefix_loc}/bin/jboss*.dll ${output_loc}/bin/${output_lib} 2>/dev/null
cp ${prefix_loc}/bin/jboss*.exe ${output_loc}/bin 2>/dev/null

if [ -n "${CRT_REDIST}" ] ; then
  crt_redist_loc=`cygpath -u "${CRT_REDIST}"`
  cp "${crt_redist_loc}"/*.dll ${output_loc}/bin/${output_lib}
fi
