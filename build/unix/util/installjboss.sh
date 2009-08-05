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

# Install jboss(as) in the chrootable environment for testing.
#
# $1 : Base directory for the test.
# $2 : Directory where the package was built.
# $3 : Directory where downloaded packages are stored.

base=$1
root=$2
build_cache_dir=$3

# Install jbossas
URL=http://surfnet.dl.sourceforge.net/sourceforge/jboss/jboss-4.2.0.GA.zip
FILE=`basename $URL`
mkdir -p $build_cache_dir
if [ ! -f ${build_cache_dir}/$FILE ]; then
  (cd ${build_cache_dir}
  wget --tries=0 --retry-connrefused $URL
  )
fi
mkdir ${base}/${root}/jbossas
(cd ${base}/${root}/jbossas
unzip ${build_cache_dir}/${FILE}
)
JBOSSDIR=`ls ${base}/${root}/jbossas`
echo "Installed ${JBOSSDIR} for tests."
