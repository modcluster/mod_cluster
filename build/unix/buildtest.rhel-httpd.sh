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
# Run a test of the httpd.
#
# $1 : Base directory for the test.
# $2 : Directory where the package was built.
# $3 : Directory where downloaded packages are stored.
# $4 : Name or IP to use for the tests.
# $5 : Do the SSL stuff.

base=$1
root=$2
build_cache_dir=$3
IPLOCAL=$4
loc_has_ssl=$5

echo ""
echo "Running : `basename $0` $LastChangedDate$"
echo ""

# Install jboss (as)
util/installjboss.sh $base $root $build_cache_dir

# start it.
JBOSSDIR=`ls ${base}/${root}/jbossas`
${base}/${root}/jbossas/${JBOSSDIR}/bin/run.sh -b ${IPLOCAL} > ${base}/${root}/jboss.out.txt &

# Copy the package to test.
cp -rp $root/opt/ $base/$root

# Add a minimal mod_jk conf to httpd.conf
util/confjk.sh $base $root $build_cache_dir ${IPLOCAL}

# Run SSL if needed
if $loc_has_ssl; then
  util/confssl.sh $base $root $build_cache_dir ${IPLOCAL}
fi

# start pwgrd-jfclere if needed
pid=""
if [ -f $base/$root/usr/sbin/pwgrd-jfclere ] ; then
  sudo /usr/sbin/chroot $base/$root /usr/sbin/pwgrd-jfclere
  pid=`ps -ef | grep pwgrd-jfclere | grep -v grep  |awk ' { print $2 }'`
fi

# Test it.
sudo /usr/sbin/chroot $base/$root /opt/jboss/httpd/sbin/apachectl start
sleep 65
curl -v http://${IPLOCAL} | grep "It works\!"
if [ $? -ne 0 ]; then
  sudo /usr/sbin/chroot $base/$root /opt/jboss/httpd/sbin/apachectl stop
  ${base}/${root}/jbossas/${JBOSSDIR}/bin/shutdown.sh -S -s ${IPLOCAL}
  if [ "x$pid" != "x" ]; then
    sudo kill -15 $pid
  fi
  echo "Test FAILED cant start?"
  exit 1
fi

curl -v http://${IPLOCAL}/myapp/toto | grep "JBossWeb"
if [ $? -ne 0 ]; then
  sudo /usr/sbin/chroot $base/$root /opt/jboss/httpd/sbin/apachectl stop
  ${base}/${root}/jbossas/${JBOSSDIR}/bin/shutdown.sh -S -s ${IPLOCAL}
  if [ "x$pid" != "x" ]; then
    sudo kill -15 $pid
  fi
  echo "Test FAILED cant connect to Jboss?"
  exit 1
fi

# The -k is for the test self signed certificate.
curl -k -v https://${IPLOCAL} | grep "It works\!"
if [ $? -ne 0 ]; then
  sudo /usr/sbin/chroot $base/$root /opt/jboss/httpd/sbin/apachectl stop
  ${base}/${root}/jbossas/${JBOSSDIR}/bin/shutdown.sh -S -s ${IPLOCAL}
  if [ "x$pid" != "x" ]; then
    sudo kill -15 $pid
  fi
  echo "Test FAILED cant use SSL?"
  exit 1
fi

# Stop it.
sudo /usr/sbin/chroot $base/$root /opt/jboss/httpd/sbin/apachectl stop
${base}/${root}/jbossas/${JBOSSDIR}/bin/shutdown.sh -S -s ${IPLOCAL}
if [ "x$pid" != "x" ]; then
  sudo kill -15 $pid
fi
sleep 30
curl -v http://${IPLOCAL} 
if [ $? -eq 0 ]; then
  echo "Test FAILED can't stop"
  exit 1
fi
echo ""
echo "SUCCESS : `basename $0` $LastChangedDate$"
echo ""
exit 0
