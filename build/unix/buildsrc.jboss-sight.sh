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
echo "Running : `basename $0` $LastChangedDate: 2007-06-01 12:00:27 -0400 (Fri, 01 Jun 2007) $"
echo ""
echo "Started : `date`"
echo "Tag     : $1"
echo "Target  : $2"
echo ""

# parameters
# $1: The tag to use something like TOMCAT_NATIVE_1_1_11 or trunk
# $2: Directory where to put the sources.

native_tag=$1
native_dist=$2

svn export --force ../../sight ${native_dist}/sight
# Generate configure.
apr_sources=${native_dist}/srclib/`ls ${native_dist}/srclib | grep apr-1`
if $BUILD_WIN ; then
  echo "Skipping buildconf for Sight"
else
  (cd ${native_dist}/sight/native
   ./buildconf --with-apr=${apr_sources}
  )
fi
