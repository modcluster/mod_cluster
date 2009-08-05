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

# Add to httpd.conf
cat <<EOF >> ${base}/${root}/opt/jboss/httpd/httpd/conf/httpd.conf
LoadModule ssl_module /opt/jboss/httpd/lib/httpd/modules/mod_ssl.so
Listen ${IPLOCAL}:443
<VirtualHost ${IPLOCAL}:443>
SSLEngine on
SSLCipherSuite ALL:!ADH:!EXPORT56:RC4+RSA:+HIGH:+MEDIUM:+LOW:+SSLv2:+EXP:+eNULL
SSLCertificateFile /opt/jboss/httpd/httpd/conf/server.crt
SSLCertificateKeyFile /opt/jboss/httpd/httpd/conf/server.key
</VirtualHost>
EOF

# Copy the certificate and key files.
cp $build_top/util/server.crt ${base}/${root}/opt/jboss/httpd/httpd/conf
cp $build_top/util/server.key ${base}/${root}/opt/jboss/httpd/httpd/conf
