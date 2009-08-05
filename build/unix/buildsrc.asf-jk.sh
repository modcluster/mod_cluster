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
echo "Running : `basename $0` $LastChangedDate: 2007-06-01 18:00:27 +0200 (Fri, 01 Jun 2007) $"
echo ""
echo "Started : `date`"
echo "Tag     : $1"
echo "Target  : $2"
echo "Destdir : $3"
echo ""

# parameters
# $1: The tag to use something like 2.2.6 or trunk
# $2: Directory where to put the sources.

native_tag=$1
native_dist=$2
destdir=$3

# we need something like:
# http://archive.apache.org/dist/tomcat/tomcat-connectors/jk/source/jk-1.2.25/tomcat-connectors-1.2.25-src.tar.gz
URLBASE=http://archive.apache.org/dist/tomcat/tomcat-connectors/jk/source/jk-$1/tomcat-connectors-$1-src
URLBASEBACK=http://ftp.heanet.ie/mirrors/www.apache.org/dist/tomcat/tomcat-connectors/jk/source/jk-$1/tomcat-connectors-$1-src
case $BUILD_SYS in
  windows)
    URL=${URLBASE}.zip
    URLBACK=${URLBASEBACK}.zip
    ;;
  *)
    URL=${URLBASE}.tar.gz
    URLBACK=${URLBASEBACK}.tar.gz
    ;;
esac

util/ckeckdownload.sh $build_cache_dir $package_src_dir $build_top tomcat-connectors-${native_tag}-src $URL $URLBACK
if [ $? -ne 0 ]; then
  exit 1
fi

#
# Copy the files to httpd src.
mkdir -p ${destdir}/modules/jk
cp -rp $package_src_dir/srclib/tomcat-connectors-${native_tag}-src/native/apache-2.0 ${destdir}/modules/jk
cp -rp $package_src_dir/srclib/tomcat-connectors-${native_tag}-src/native/common ${destdir}/modules/jk
cp -rp $package_src_dir/srclib/tomcat-connectors-${native_tag}-src/native/scripts ${destdir}/modules/jk
