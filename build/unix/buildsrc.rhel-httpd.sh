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

#
# Build the sources from the rhel repo according to the information in data.
util/buildrhelsrc.sh http://cvs.devel.redhat.com/repo/dist/httpd httpd/RHEL-5 ${native_tag} ${native_dist} httpd rhel-httpd ${build_version}
if [ $? -ne 0 ]; then
  echo "util/buildrhelsrc.sh httpd failed"
  exit 1
fi

dirsources=`ls ${native_dist}/srclib/ | grep httpd-`
dirsources=${native_dist}/srclib/${dirsources}

#
# Build the sources from the rhel repo according to the information in data.
# http://cvs.devel.redhat.com/cgi-bin/cvsweb.cgi/rpms/apr/RHEL-5/
if [ "x${apr_version}" = "x" ]; then
  tag=trunk
else
  tag=${apr_version}
fi
util/buildrhelsrc.sh http://cvs.devel.redhat.com/repo/dist/apr apr/RHEL-5 ${tag} ${native_dist} apr rhel-httpd ${build_version}
if [ $? -ne 0 ]; then
  echo "util/buildrhelsrc.sh apr failed"
  exit 1
fi

# http://cvs.devel.redhat.com/cgi-bin/cvsweb.cgi/rpms/apr-util/RHEL-5/
if [ "x${apu_version}" = "x" ]; then
  tag=trunk
else
  tag=${apu_version}
fi
util/buildrhelsrc.sh http://cvs.devel.redhat.com/repo/dist/apr-util apr-util/RHEL-5 ${tag} ${native_dist} apr-util rhel-httpd ${build_version}
if [ $? -ne 0 ]; then
  echo "util/buildrhelsrc.sh apr-util failed"
  exit 1
fi

# We get apr-iconv
if [ "${BUILD_SYS}" = "windows" ]; then
  util/ckeckdownload.sh $build_cache_dir $package_src_dir $build_top apr-iconv-${api_version} $APIURL $APIURLBACK
  if [ $? -ne 0 ]; then
    echo "util/ckeckdownload.sh apr-iconv failed"
    exit 1
  fi
  rm -rf ${dirsources}/srclib/apr-iconv
  apidirsources=`ls ${build_cache_dir}/ | grep apr-iconv-`
  echo "Replacing  ${dirsources}/srclib/apr-iconv by ${build_cache_dir}/${apidirsources}"
  cp -rp ${build_cache_dir}/${apidirsources} ${dirsources}/srclib/apr-iconv
fi


# Copy apr and apr-util to the build location.
if [ -d ${dirsources}/srclib/apr ]; then
  rm -rf ${dirsources}/srclib/apr
  aprdirsources=`ls ${native_dist}/srclib/ | grep apr- | grep -v apr-util | grep -v apr-iconv`
  echo "Replacing  ${dirsources}/srclib/apr by ${native_dist}/srclib/${aprdirsources}"
  mv ${native_dist}/srclib/${aprdirsources} ${dirsources}/srclib/apr
fi
if [ -d ${dirsources}/srclib/apr-util ]; then
  rm -rf ${dirsources}/srclib/apr-util
  apudirsources=`ls ${native_dist}/srclib/ | grep apr-util-`
  echo "Replacing  ${dirsources}/srclib/apr-util by ${native_dist}/srclib/${apudirsources}"
  mv ${native_dist}/srclib/${apudirsources} ${dirsources}/srclib/apr-util
fi
if [ "${BUILD_SYS}" = "windows" ]; then
  rm -rf ${dirsources}/srclib/zlib
  zlibdirsources=`ls ${native_dist}/srclib/ | grep zlib-`
  echo "Replacing  ${dirsources}/srclib/zlib by ${native_dist}/srclib/${zlibdirsources}"
  cp -rp ${native_dist}/srclib/${zlibdirsources} ${dirsources}/srclib/zlib
fi

#
# Add mod_jk sources.
if $has_jk; then
  ${build_top}/util/override.sh ${build_top}/buildsrc.asf-jk.sh ${jk_version} ${package_src_dir} ${dirsources}
  if [ $? -ne 0 ]; then
    echo "buildsrc.asf-jk.sh failed"
    exit 1
  fi
  cp ${build_top}/buildsrc.asf-jk.sh ${package_src_dir}
fi

#
# Add mod_cluster sources.
if $has_cluster; then
  ${build_top}/util/override.sh ${build_top}/buildsrc.cluster.sh ${cluster_version} ${package_src_dir} ${dirsources}
  if [ $? -ne 0 ]; then
    echo "buildsrc.cluster.sh failed"
    exit 1
  fi
  cp ${build_top}/buildsrc.cluster.sh ${package_src_dir}
fi

#
# Copy the windows NMAKE files (and additional stuff).
dirnmake=${build_svn_root}/httpd/httpd-2.2/
if [ -d ${dirnmake} ]; then
  svn export --force ${dirnmake} ${dirsources}
  if [ $? -ne 0 ]; then
    echo "svn export --force ${dirnmake} ${dirsources} FAILED"
    exit 1
  fi
else
  echo "Cannot find package sources in ${dirnmake}"
  exit 1
fi

# mturk Hack.
if [ "${BUILD_SYS}" != "windows" ]; then
  rm -rf ${dirsources}/srclib/apr-iconv
fi

# Replace libtool by our libtool
case ${BUILD_SYS} in
  hpux*)
    cp ${build_top}/util/jlibtool.c ${dirsources}/srclib/apr/build
    ;;
esac

#
# regenerate configure scripts
# XXX: It seems that produces a destroyed configure (on f8 and hpux at least).
#(cd ${dirsources}
# autoheader && autoconf || exit 1
#)

if [ "${BUILD_SYS}" != "windows" ]; then
  (cd ${dirsources}
   bash buildconf
  )
  if $has_jk; then
    (cd ${dirsources}/modules/jk
     autoconf
    )
  fi
fi
