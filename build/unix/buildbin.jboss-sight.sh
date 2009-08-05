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
echo "Running : `basename $0` $LastChangedDate: 2007-09-20 10:07:11 -0400 (Thu, 20 Sep 2007) $"
echo ""
echo "Started : `date`"
echo "Prefix  : $1"
echo "Source  : $2"
echo "Apr     : $3"
echo "OpenSSL : $4"
echo ""

# parameters
# $1: Location where to install the package.
# $2: Source location.
# $3: APR distribution location.
# $4: OpenSSL distribuition location.

common_loc=$1
prefix_loc=$2
output_loc=$3
has_openssl=$4

echo "Configuring Sight"
platform_path="SIGHT-BIN/${BUILD_SYS}/${BUILD_CPU}"
mkdir -p ${output_loc}/${platform_path}
mkdir -p ${output_loc}/lib
mkdir -p ${output_loc}/doc/sight

case ${BUILD_CPU} in
  ppc64)
    add_conf="${add_conf} CFLAGS=-m64 --with-java-home=/opt/jdk"
esac

# Build native part.
(cd sight/native
 ./configure ${add_conf} --with-apr=${common_loc} --prefix=${prefix_loc} -enable-layout=generic
 if [ $? -ne 0 ]; then
   echo "Configure Sight native failed"
   exit 1
 fi
 echo "Building Sight native"
 make
 if [ $? -ne 0 ]; then
   echo "Make Sight native failed"
   exit 1
 fi
 make install
 if [ $? -ne 0 ]; then
   echo "Make install Sight native failed"
   exit 1
 fi
)
if [ $? -ne 0 ]; then
   exit 1
fi

# Build java part.
if [ "x" != "x$JAVA_HOME" ]; then
  jadd_conf="${jadd_conf} -Djava.home=$JAVA_HOME"
fi
(cd sight
 echo "Building Sight java"
 ant ${jadd_conf}
 ant ${jadd_conf} javadocs
 ant ${jadd_conf} jar
)

current_loc=`pwd`
(cd ${prefix_loc}/lib
 tar -cf ${current_loc}/x.tar *.${so_extension}*
)
(cd ${output_loc}/${platform_path}
 tar -xf ${current_loc}/x.tar
)
rm -f ${current_loc}/x.tar

(cd ${common_loc}/lib
 tar -cf ${current_loc}/x.tar libapr*.${so_extension}* libex*.${so_extension}*
)
(cd ${output_loc}/${platform_path}
 tar -xf ${current_loc}/x.tar
)
rm -f ${current_loc}/x.tar
cp sight/dist/sight*.jar ${output_loc}/lib
cp -rp sight/dist/doc/api ${output_loc}/doc/sight
