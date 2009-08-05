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

# Install jboss(as) in the chrootable environment for testing.
#
# $1 : Base directory for the test.
# $2 : Directory where the package was built.
# $3 : Directory where downloaded packages are stored.
# $4 : Name or IP to use for the tests.

base=$1
root=$2
build_cache_dir=$3
IPLOCAL=$4

echo "$0 : in ${base}/${root}"

# Arrange listen
mv ${base}/${root}/opt/jboss/httpd/httpd/conf/httpd.conf ${base}/${root}/opt/jboss/httpd/httpd/conf/httpd.conf.org

sed "s/^Listen 80/Listen ${IPLOCAL}:80/" ${base}/${root}/opt/jboss/httpd/httpd/conf/httpd.conf.org > ${base}/${root}/opt/jboss/httpd/httpd/conf/httpd.conf


# Add to httpd.conf
cat <<EOF >> ${base}/${root}/opt/jboss/httpd/httpd/conf/httpd.conf
LoadModule jk_module /opt/jboss/httpd/lib/httpd/modules/mod_jk.so
JkWorkersFile conf/workers.properties
JkLogFile logs/mod_jk.log
JkLogLevel debug
JkMount /myapp/* balancer
JkMount /jkstatus jkstatus
EOF

# Create the workers file.
cat <<EOF > ${base}/${root}//opt/jboss/httpd/httpd/conf/workers.properties
worker.list=jkstatus,balancer
worker.maintain=600

worker.node1.type=ajp13
worker.node1.host=${IPLOCAL}

worker.balancer.type=lb
worker.balancer.balance_workers=node1

worker.jkstatus.type=status
EOF
