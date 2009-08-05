#!/bin/sh
# Copyright(c) 2006 Red Hat Middleware, LLC,
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

BUILDNAM=jboss-native
BUILDVER=2.0.0
SIGHTNAM=jboss-sight
SIGHTVER=1.0.0
JBNATIVE_BASE=`pwd`

# read the parameters
PARAMETERS=
while [ "x" != "x$1" ]
do
  PARAMETERS="$PARAMETERS $1"
  case $1 in
    --with-openssl-version=*)
      SSLVER=`echo $1 | awk -F = '{ print $2 }'`
      echo "Using openssl-$SSLVER"
      ;;
    --with-tcnative-version=*)
      TCNVER=`echo $1 | awk -F = '{ print $2 }'`
      ;;
    --with-apr-version=*)
      APRVER=`echo $1 | awk -F = '{ print $2 }'`
      echo "Using apr-$APRVER"
      ;;
    --build-version=*)
      BUILDVER=`echo $1 | awk -F = '{ print $2 }'`
      echo "Build version $BUILDVER"
      ;;
    *)
      echo "$1: not (yet) supported"
      ;;
  esac
  shift
done
if [ "x" = "x$PARAMETERS" ]; then
  echo "Using defaut values"
else
  echo "Using $PARAMETERS"
fi

rm -f ${BUILDNAM}-*.tar.gz
rm -f ${SIGHTNAM}-*.tar.gz
rm -rf ${BUILDNAM}-${BUILDVER}-src
rm -rf ${BUILDNAM}-${BUILDVER}-src-ssl
rm -rf ${SIGHTNAM}-${SIGHTVER}-src
rm -rf jbossnative

# JBATIVESVN=http://anonsvn.jboss.org/repos/jbossnative/tags/JBNATIVE_2_0_0
JBNATIVESVN=http://anonsvn.jboss.org/repos/jbossnative/trunk/

svn export ${JBNATIVESVN} jbossnative
cd jbossnative/build
./buildprep.sh $PARAMETERS
if [ $? -ne 0 ]; then
  echo "buildprep.sh failed"
  exit 1
fi
cd ${JBNATIVE_BASE}
cd ${BUILDNAM}-${BUILDVER}-src-ssl
./buildworld.sh $PARAMETERS
if [ $? -ne 0 ]; then
  echo "buildworld.sh failed"
  exit 1
fi
cd ${JBNATIVE_BASE}

cd jbossnative/build
./buildprep.sh --disable-openssl $PARAMETERS
if [ $? -ne 0 ]; then
  echo "no_SSL buildprep.sh failed"
  exit 1
fi

cd ${JBNATIVE_BASE}
cd ${BUILDNAM}-${BUILDVER}-src
./buildworld.sh --disable-openssl $PARAMETERS
if [ $? -ne 0 ]; then
  echo "no_SSL buildworld.sh failed"
  exit 1
fi
