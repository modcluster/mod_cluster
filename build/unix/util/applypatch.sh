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

if $xb_verbose; then
  echo ""
  echo "Running `basename $0` $LastChangedDate: 2008-07-24 03:26:47 -0400 (Thu, 24 Jul 2008) $"
  echo ""
  echo "Started : `date`"
  echo "Params  : $@"
  echo ""
fi

# Apply patch for the component
# $1 directory of the component like apr-1.2.8
# $2 also apply the rhel patches.
#
# TODO: rename $patch to $XB_PATCH
#
xb_applypatch()
{
  basefilename=`basename $xbap_package_dir`
  if [ -f $xbap_build_top/../patch/$basefilename.patch ]; then
    (cd $xbap_package_dir
    echo "Applying patch $ap_build_top/../patch/$basefilename.patch in $xbap_package_dir"
    $patch -tf -p0 -i $xbap_build_top/../patch/$basefilename.patch
    ) || return 1
  fi
  if $xbap_is_rhel; then
    if [ -f $xbap_build_top/../patch/$basefilename.rhel.patch ]; then
      (cd $xbap_package_dir
       echo "Applying rhel patch $ap_build_top/../patch/$basefilename.rhel.patch in $xbap_package_dir"
      $patch -tf -p0 -i $xbap_build_top/../patch/$basefilename.rhel.patch
      ) || return 1
    fi
  fi
}

# Apply patch for the component
# $1 Build top. Where we start the build.sh
# $2 Component directory like apr-1.2.8
# $3 Also apply the rhel patches.

xbap_build_top=$1
xbap_package_dir=$2
xbap_is_rhel=$3

# Call the internal functions.
xb_applypatch
