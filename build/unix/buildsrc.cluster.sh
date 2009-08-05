#!/bin/sh
# Copyright(c) 2008 Red Hat Middleware, LLC,
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

tag=$1
dist=$2
destdir=$3

# we need something like:
# http://anonsvn.jboss.org/repos/jbossnative/trunk/mod_cluster/
if [ "$tag" = "trunk" ]; then
  URLBASE=http://anonsvn.jboss.org/repos/jbossnative/trunk/mod_cluster/
else
  URLBASE=http://anonsvn.jboss.org/repos/jbossnative/tag/${tag}/mod_cluster/
fi

(cd $package_src_dir/srclib
svn export ${URLBASE} mod_cluster
)
if [ $? -ne 0 ]; then
  echo "svn co ${URLBASE} mod_cluster FAILED"
  exit 1
fi

#
# Copy the files to httpd src.
mkdir -p ${destdir}/modules/advertise
mkdir -p ${destdir}/modules/mod_manager
mkdir -p ${destdir}/modules/mod_slotmem
cp -p $package_src_dir/srclib/mod_cluster/native/advertise/* ${destdir}/modules/advertise
cp -p $package_src_dir/srclib/mod_cluster/native/mod_manager/* ${destdir}/modules/mod_manager
cp -p $package_src_dir/srclib/mod_cluster/native/mod_slotmem/* ${destdir}/modules/mod_slotmem

cp -p $package_src_dir/srclib/mod_cluster/native/include/* ${destdir}/modules/proxy
cp -p $package_src_dir/srclib/mod_cluster/native/mod_proxy_cluster/mod_proxy_cluster.c ${destdir}/modules/proxy
cp -p $package_src_dir/srclib/mod_cluster/native/mod_proxy_cluster/*.patch ${destdir}/modules/proxy

# Fix the config.m4 to build mod_cluster
(cd ${destdir}/modules/proxy
 patch -p0 < config.m4.patch
)
