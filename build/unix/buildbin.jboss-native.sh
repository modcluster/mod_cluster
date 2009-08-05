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
echo "Running : `basename $0` $LastChangedDate: 2008-03-26 11:40:55 -0400 (Wed, 26 Mar 2008) $"
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

output_lib="META-INF/lib/${BUILD_SYS}/${BUILD_CPU}"
output_bin="META-INF/bin/${BUILD_SYS}/${BUILD_CPU}"

echo "Configuring tomcat-native in `pwd`"
add_conf=""
if [ "x$JAVA_HOME" = "x" ]; then
  add_conf=""
else
  add_conf="--with-java-home=$JAVA_HOME"
fi
if $has_openssl ; then
  add_conf="${add_conf} --with-ssl=${common_loc}"
else
  add_conf="${add_conf} --with-ssl=no"
fi

mkdir -p ${output_loc}/bin/${output_lib}
mkdir -p ${output_loc}/bin/${output_bin}
native_sources=srclib/`ls srclib | grep tomcat-native`

# Build native part.
(cd $native_sources
 ./configure ${add_conf} --with-apr=${common_loc} --prefix=${prefix_loc} -enable-layout=generic
 if [ $? -ne 0 ]; then
   echo "Configure tomcat-native failed"
   exit 1
 fi
 echo "Building tomcat-native"
 make
 if [ $? -ne 0 ]; then
   echo "Make tomcat-native failed"
   exit 1
 fi
 echo "Installing tomcat-native"
 make install
 if [ $? -ne 0 ]; then
   echo "Make install tomcat-native failed"
   exit 1
 fi
)
if [ $? -ne 0 ]; then
  exit 1
fi

# Now put the files in their location
current_loc=`pwd`
(cd ${prefix_loc}/lib
 tar -cf ${current_loc}/x.tar *.${so_extension}*
)
(cd ${output_loc}/bin/${output_lib}
 tar -xf ${current_loc}/x.tar
)
rm -f ${current_loc}/x.tar

(cd ${common_loc}/lib
 tar -cf ${current_loc}/x.tar *.${so_extension}*
)
(cd ${output_loc}/bin/${output_lib}
 tar -xf ${current_loc}/x.tar
)
rm -f ${current_loc}/x.tar

if $has_openssl; then
  echo "Adding OpenSSL libraries ..."
  (cd ${common_loc}/lib
   tar -cf ${current_loc}/x.tar engines
  )
  (cd ${output_loc}/bin/${output_lib}
   tar -xf ${current_loc}/x.tar
  )
  rm -f ${current_loc}/x.tar

  cp ${common_loc}/bin/openssl ${output_loc}/bin/${output_bin}
fi
