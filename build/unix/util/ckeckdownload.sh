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

# Download the sources. It guess how to do it.
# $1: URL.
# $1: Back URL. (archive for apache)
downloadtaredsrc()
{
  URL=$1
  URLBACK=$2
  if [ -z $URLBACK ]; then
    URLBACK=$URL
  fi
  wget --tries=0 --retry-connrefused $URL
  if [ $? -ne 0 ]; then
    echo "downloadtaredsrc Retrying on $URLBACK"
    wget --tries=0 --retry-connrefused $URLBACK
  fi
  fname=`basename $URL`
  case ${fname} in
    *.tar.gz)
      gunzip -c $fname | tar -xf -
      ;;
    *.zip)
      unzip -q -o $fname
      ;;
  esac
  rm -f $fname
}

# Apply patch for the component
# $1 directory of the component like apr-1.2.8
applypatch()
{
  DIR=$1
  if [ -f $build_top/../patch/$DIR.patch ];
  then
    (cd $DIR
    patch -tfs -p0 -i $build_top/../patch/$DIR.patch
    )
  fi
}

# Check and download
# $1 directory of the checkout directory
# $2 url for the download
# $3 backup url for the download
ckeckdownload()
{
  src_dir=$1
  src_url=$2
  src_url_back=$3
  cd $build_cache_dir
  if [ ! -d ${src_dir} ]; then
    downloadtaredsrc $src_url $src_url_back
    applypatch $src_dir
  fi
  cp -rp ${src_dir} ${package_src_dir}/srclib
  cd $build_top
}

# ckeckdownload sub shell version.
# Check and download
# $1 cache directory.
# $2 directory where the sources are located.
# $3 build_top. Where we start the build.sh
# $4 directory of the checkout directory
# $5 url for the download
# $6 backup url for the download
build_cache_dir=$1
package_src_dir=$2
build_top=$3
src_dir=$4
url=$5
urlback=$6

echo "url: $url urlback: $urlback"

# Call the internal functions.
ckeckdownload $src_dir $url $urlback
