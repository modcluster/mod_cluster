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
# @author Mladen Turk
#
echo ""
echo "Running `basename $0` $LastChangedDate: 2008-07-23 10:17:15 -0400 (Wed, 23 Jul 2008) $"
echo ""
echo "Started : `date`"
echo "Params  : $@"
echo ""

build_ssl=true

while [ "x" != "x$1" ]
do
  case  $1 in
    -env)
        echo "Dumping environment"
        env
        echo ""
     ;;
    -no-ssl)
        build_ssl=false
     ;;
  esac
  shift
done

(cd ../unix
  if $build_ssl; then
    ./build.sh jboss-native -ssl -cache
    ./build.sh rhel-httpd -ssl -cache
  fi
  ./build.sh jboss-native -cache
  ./build.sh rhel-httpd -cache
  # Uncomment for building sight
  # ./build.sh jboss-sight -cache
)
