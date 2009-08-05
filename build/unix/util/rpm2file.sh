!/bin/sh
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
# @author Mladen Turk
#

if $xb_verbose; then
  echo ""
  echo "Running `basename $0` $LastChangedDate: 2008-03-25 11:15:54 +0100 (Tue, 25 Mar 2008) $"
  echo ""
  echo "Started : `date`"
  echo "Params  : $@"
  echo ""
fi

xb_rpm2file()
{
    if [ ! -d $xbrpm_srcd ]; then
        mkdir $xbrpm_srcd
    fi
    (cd $xbrpm_srcd
        $xb_progloc/rpm2cpio.pl $xbrpm_srcf | cpio -iduv
        for xbrpm_arch in `ls -1 *.tar.gz`
        do
            gunzip -c ${xbrpm_arch} | tar -xf -
        done
        xbrpm_specf=`ls *.spec | head -n 1`
        echo "Spec file is " $xbrpm_specf
        #
        # TODO: Apply patches from .spec file
        #
    )
}

# Common runtime variables
xb_prognam=`realpath $0`
xb_progloc=`dirname $xb_prognam`

# Extract .src.rpm file
# $1 rpm file

xbrpm_srcf=`realpath $1`
xbrpm_srca=`basename $1 .src.rpm`
xbrpm_srcd=`dirname $1`/$xbrpm_srca

# Call the internal functions
xb_rpm2file
