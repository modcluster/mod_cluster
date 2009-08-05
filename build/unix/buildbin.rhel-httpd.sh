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
echo "Running : `basename $0` $LastChangedDate: 2007-07-02 16:22:00 +0200 (Mon, 02 Jul 2007) $"
echo ""
echo "Started : `date`"
echo "Common  : $1"
echo "Prefix  : $2"
echo "Output  : $3"
echo "OpenSSL : $4"
echo "Static  : $5"
echo "Sources : $6"
echo ""


# parameters
# $1: Location of the common libraries.
# $2: Destination location.
# $3: Location where to put the binaries.
# $4: Use OpenSSL.
# $5: Use static build.
# $6: Location of the sources.

common_loc=$1
prefix_loc=$2
output_loc=$3
has_openssl_loc=$4
has_static_loc=$5
sources_loc=$6
current_loc=`pwd`

#
# Function from the httpd.spec file
# removed for easy testing.
#        --with-apr=${_prefix} --with-apr-util=${_prefix} \
mpmbuild()
{
mpm=$1; shift
mkdir $mpm
#(cd $mpm
(
bash ./configure \
        --enable-layout=Apache \
        --with-included-apr \
        --prefix=${_sysconfdir}/httpd \
        --exec-prefix=${_prefix} \
        --bindir=${_bindir} \
        --sbindir=${_sbindir} \
        --mandir=${_mandir} \
        --libdir=${_libdir} \
        --sysconfdir=${_sysconfdir}/httpd/conf \
        --includedir=${_includedir}/httpd \
        --libexecdir=${_libdir}/httpd/modules \
        --datadir=${contentdir} \
        --with-installbuilddir=${_libdir}/httpd/build \
        --with-mpm=$mpm \
        --enable-suexec --with-suexec \
        --with-suexec-caller=${suexec_caller} \
        --with-suexec-docroot=${contentdir} \
        --with-suexec-logfile=${_localstatedir}/log/httpd/suexec.log \
        --with-suexec-bin=${_sbindir}/suexec \
        --with-suexec-uidmin=500 --with-suexec-gidmin=100 \
        --with-pcre=builtin \
        $* || exit 1
make clean || exit 1
make || exit 1
) || return 1
}

#
# Set needed variables. (defaulted to hacked values).
PREFIX=/opt/jboss/httpd

_sysconfdir=${PREFIX}
_prefix=${PREFIX}
_sbindir=${PREFIX}/sbin
_bindir=${PREFIX}/bin
_mandir=${PREFIX}/man
_libdir=${PREFIX}/lib
_includedir=${PREFIX}/include
contentdir=${PREFIX}/htdocs
suexec_caller=${PREFIX}/bin/suexec
_localstatedir=${PREFIX}/logs

if $has_openssl_loc ; then
  add_conf="--with-ssl=${common_loc} --enable-ssl"
else
  add_conf=""
fi
if $build_iconv; then
  # Use the static iconv we built before.
  LDFLAGS="$LDFLAGS -L${build_common_dir}-static/lib -liconv"
  export LDFLAGS
  # Add the LD_LIBRARY_PATH if we want to use dynamic library. 
  #LD_LIBRARY_PATH="$LD_LIBRARY_PATH:${build_common_dir}-static/lib"
  #export LD_LIBRARY_PATH
fi
if $build_expat; then
  add_conf_sys="--with-expat=${build_common_dir}-static/"
else
  add_conf_sys="--with-expat=builtin"
fi
if $build_zlib; then
  add_conf_sys="$add_conf_sys --with-z=${build_common_dir}-static/"
fi
# Process mod_jk if needed.
native_sources=srclib/`ls srclib | grep httpd-`
cd $native_sources

if $has_jk; then
  add_conf="$add_conf --enable-jk"
  (cd modules/jk; ./configure)
fi

if $has_cluster; then
  add_conf="$add_conf --enable-proxy-cluster --enable-advertise --enable-slotmem --enable-manager"
fi

# Ajust some more platform dependent stuff.
case ${BUILD_SYS} in
    linux*)
    add_conf_sys="$add_conf_sys --enable-pie"
    ;;
    hpux*)
    add_conf_sys="$add_conf_sys --enable-experimental-libtool --enable-shared"
    ;;
esac
case ${BUILD_CPU} in
    ppc64)
    # XXX: It is also in build.sh but not exported.
    add_conf_sys="$add_conf_sys CFLAGS=-m64"
    ;;
esac
echo "Building prefork and mpm with add_conf: $add_conf $add_conf_sys"

# Build everything and the kitchen sink with the prefork build
# removed because of ldap dependencies.
#        --enable-ldap --enable-authnz-ldap \
mpmbuild prefork \
        --enable-mods-shared=all \
        ${add_conf} \
        ${add_conf_sys} \
        --enable-proxy \
        --enable-cache --enable-mem-cache \
        --enable-file-cache --enable-disk-cache \
        --enable-cgid \
        --enable-authn-anon --enable-authn-alias
if [ $? -ne 0 ]; then
  echo "mpmbuild prefork failed"
  exit 1
fi

# Install it
RPM_BUILD_ROOT=$output_loc
make DESTDIR=$RPM_BUILD_ROOT install
if [ $? -ne 0 ]; then
  echo "mpmbuild prefork install failed"
  exit 1
fi

# Clean it before building worker.
make clean

# For the other MPMs, just build httpd and no optional modules
mpmbuild worker --enable-modules=none ${add_conf_sys}
if [ $? -ne 0 ]; then
  echo "mpmbuild worker failed"
  exit 1
fi

# Install it
#install -m 755 worker/httpd ${RPM_BUILD_ROOT}/${_sbindir}/httpd.worker
install -m 755 httpd ${RPM_BUILD_ROOT}/${_sbindir}/httpd.worker

# Add the needed openssl stuff.
current_loc=`pwd`
install_loc=${output_loc}/${PREFIX}
if $has_openssl_loc; then
  echo "Adding OpenSSL libraries ..."
  (cd ${common_loc}/lib
   tar -cf ${current_loc}/x.tar engines *.${so_extension}*
  )
  (cd ${install_loc}/lib
   tar -xf ${current_loc}/x.tar
  )
  rm -f ${current_loc}/x.tar

  cp ${common_loc}/bin/openssl ${install_loc}/bin
fi

# Arrange the sl file into so on hpux
if [ "$BUILD_SYS" = "hpux" ]; then
  for file in `find ${RPM_BUILD_ROOT}/opt/jboss/httpd/lib/httpd -name *.sl`
  do
    nfile=`echo $file | sed 's:\.sl:\.so:'`
    mv $file $nfile
  done
fi

echo "Done"
exit 0
