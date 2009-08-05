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
echo "Running : `basename $0` $LastChangedDate: 2007-05-31 19:40:31 +0200 (ÄTet, 31 svi 2007) $"
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

echo "Platform is currently unsupported."
exit 2
