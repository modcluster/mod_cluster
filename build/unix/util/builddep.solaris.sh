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
# $1: binary build directory.
# $2: ouput file.
# $3: work directory.
LD_LIBRARY_PATH=$1
for dir in `find $1 -name "*.${so_extension}" -exec dirname {} \; | sort -u`
do
  LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$dir
done
echo "LD_LIBRARY_PATH: $LD_LIBRARY_PATH"
export LD_LIBRARY_PATH

find $1 -name "*.${so_extension}" -exec ldd {} \; > $3/$2.all
cat $3/$2.all | awk 'NF==4 { print $3 } ' | sort -u | grep -v "$1" > $3/$2
> $3/$2.tmp
for file in `cat $3/$2`
do
  pkgchk -l -p $file | grep "^	" >> $3/$2.tmp
done
sort -u $3/$2.tmp > $3/$2.pkg
