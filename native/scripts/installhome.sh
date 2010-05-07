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
# arrange mod_cluster installation to be able to run it in $HOME instead /opt
#
echo ""
echo "Running : `basename $0` $LastChangedDate: 2010-05-04 18:26:40 +0200 (Tue, 04 May 2010) $"
echo ""
# process help
while [ "x" != "x$1" ]
do
  case  $1 in
    --help)
       echo "mod_cluster installer"
       exit 0
       ;;
  esac
done

# find RPM_BUILD_ROOT
RPM_BUILD_ROOT=`dirname $0`
BASE_NAME=`basename $0`
if [ "x${RPM_BUILD_ROOT}" = "x." ]; then
  # local file
  if [ -x $RPM_BUILD_ROOT/$BASE_NAME ]; then
    $0 --help 2>/dev/null | grep "mod_cluster installer" 1>/dev/null
    if [ $? -eq 0 ]; then
      RPM_BUILD_ROOT=`(cd $RPM_BUILD_ROOT/..; pwd)`
    fi
  fi
else
  RPM_BUILD_ROOT=`(cd $RPM_BUILD_ROOT/..; pwd)`
fi
if [ "x${RPM_BUILD_ROOT}" = "x." ]; then
  # in $PATH)
  for path in `echo $PATH | sed "s/:/ /g"`
  do
    file=$path/$0
    $path/$0 --help 2>/dev/null | grep "mod_cluster installer" 1>/dev/null
    if [ $? -eq 0 ]; then
      RPM_BUILD_ROOT=`(cd $path/..; pwd)`
      break
    fi
  done
fi
echo "Installing in $RPM_BUILD_ROOT"

# Process httpd configuration files.
BASEHTTPD=/opt/jboss/httpd
HTTPDCONF=httpd/conf
HTTPDSBIN=sbin
HTTPDBUILD=htdocs/build
files="${HTTPDSBIN}/apachectl ${HTTPDCONF}/httpd.conf ${HTTPDSBIN}/envvars ${HTTPDSBIN}/apxs ${HTTPDBUILD}/config_vars.mk"
for FILE in `echo $files`
do
  file=${RPM_BUILD_ROOT}/$FILE
  echo "$file"
  cp -p $file $file.new
  echo "s:${BASEHTTPD}:${RPM_BUILD_ROOT}:" > sed.cmd
  echo "s/Listen 80.*/Listen 8000/" >> sed.cmd
  sed -f sed.cmd $file > $file.new
  mv $file.new $file
  rm -f sed.cmd
done
# Arrange apachectl
file=$RPM_BUILD_ROOT/${HTTPDSBIN}/apachectl
cp -p $file $file.new
echo "s:\$HTTPD -k \$ARGV:\$HTTPD -k \$ARGV -d $RPM_BUILD_ROOT/httpd:" > sed.cmd
sed -f sed.cmd $file > $file.new
mv $file.new $file
rm -f sed.cmd
