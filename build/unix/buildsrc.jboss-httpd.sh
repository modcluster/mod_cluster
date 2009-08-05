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
echo ""

# parameters
# $1: The tag to use something like 2.2.6 or trunk
# $2: Directory where to put the sources.

native_tag=$1
native_dist=$2

if [ $native_tag  = "trunk" ]; then
  native_svn=http://svn.apache.org/repos/asf/httpd/httpd/trunk
  native_tag_opt="-r HEAD"
  native_ext=current
  svn export --native-eol=${NATIVEEOL} ${native_tag_opt} ${native_svn} ${native_dist}/srclib/httpd-${native_ext}
else
  # here we use a released tarball
  if $BUILD_WIN ; then
    URL=http://archive.apache.org/dist/httpd/httpd-${native_tag}-win32-src.zip
  else
    URL=http://archive.apache.org/dist/httpd/httpd-${native_tag}.tar.gz
  fi 
  util/ckeckdownload.sh $build_cache_dir $package_src_dir $build_top httpd-${native_tag} $URL NONE
fi

dirsources=`ls ${native_dist}/srclib/ | grep httpd-`
dirsources=${native_dist}/srclib/${dirsources}

# We get apr-iconv
if [ "${BUILD_SYS}" = "windows" ]; then
  rm -rf ${dirsources}/srclib/apr-iconv
  apidirsources=`ls ${build_cache_dir}/ | grep apr-iconv-`
  echo "Replacing  ${dirsources}/srclib/apr-iconv by ${build_cache_dir}/${apidirsources}"
  cp -rp ${build_cache_dir}/${apidirsources} ${dirsources}/srclib/apr-iconv

  rm -rf ${dirsources}/srclib/zlib
  zlibdirsources=`ls ${native_dist}/srclib/ | grep zlib-`
  echo "Replacing  ${dirsources}/srclib/zlib by ${native_dist}/srclib/${zlibdirsources}"
  cp -rp ${native_dist}/srclib/${zlibdirsources} ${dirsources}/srclib/zlib
fi

#
# Copy the windows NMAKE files (and additional stuff).
dirnmake=${build_svn_root}/httpd/httpd-2.2/
if [ -d ${dirnmake} ]; then
  svn export --force ${dirnmake} ${dirsources}
else
  echo "Cannot find package sources in ${dirnmake}"
  exit 1
fi

# mturk Hack.
if [ "${BUILD_SYS}" != "windows" ]; then
  rm -rf ${dirsources}/srclib/apr-iconv
fi

# Remove jk directory after export if not enabled.
if ! $has_jk ; then
  rm -rf ${dirsources}/modules/jk
fi

echo ""
echo "Done : `basename $0`"
echo ""
