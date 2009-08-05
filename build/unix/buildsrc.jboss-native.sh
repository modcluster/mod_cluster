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
echo ""
echo "Running : `basename $0` $LastChangedDate: 2008-03-25 06:06:39 -0400 (Tue, 25 Mar 2008) $"
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

if [ $native_tag  = "trunk" ]; then
  native_svn=http://svn.apache.org/repos/asf/tomcat/connectors/trunk
  native_tag_opt="-r HEAD"
  native_ext=current
else
  # here we convert something like TOMCAT_NATIVE_1_1_11 to 1.1.11
  native_ext=`echo ${native_tag} | awk -F _ '{ print $3 "." $4 "." $5 }'`
  native_svn=http://svn.apache.org/repos/asf/tomcat/connectors/tags/other/${native_tag}
fi
svn export --native-eol=${NATIVEEOL} ${native_tag_opt} ${native_svn}/jni/native ${native_dist}/srclib/tomcat-native-${native_ext}
if [ $? -ne 0 ]; then
  echo "svn export ${native_tag_opt} failed"
  exit 1
fi

# Apply patches
$build_top/util/applypatch.sh $build_top ${native_dist}/srclib/tomcat-native-${native_ext} false

# Generate configure.
apr_sources=${native_dist}/srclib/`ls ${native_dist}/srclib | grep apr-1`
if $BUILD_WIN ; then
  echo "Skipping buildconf for Tomcat Native ${native_ext}"
else
  (cd ${native_dist}/srclib/tomcat-native-${native_ext}
   ./buildconf --with-apr=${apr_sources}
  )
fi

# Copy other files.
cp ../../srclib/tomcat-native/NMAKEmakefile ${native_dist}/srclib/tomcat-native-${native_ext}

if $BUILD_WIN ; then
  svn export --force ../../utils/windows/native/service ${native_dist}/service
  rm -rf ${native_dist}/service/classes
  rm -f ${native_dist}/service/build.xml
fi
