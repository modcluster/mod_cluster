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
echo "Running `basename $0` $LastChangedDate: 2008-06-17 05:31:09 -0400 (Tue, 17 Jun 2008) $"
echo ""
echo "Started : `date`"
echo "Params  : $@"
echo ""

# Globals
build_top=`pwd`
export build_top
build_svn_root=`(cd  ../../; pwd)`
export build_svn_root

NATIVEEOL=LF
build_version=""
PACKAGE=""
BUILD_WIN=false

build_srcp=false
package_list=${build_top}/package.list
if [ -f ${build_top}/package.build ]; then
  package_list=${build_top}/package.build
  build_srcp=true
fi

awk=awk
has_posix_tar=true

# parameters
has_apr=false
has_apu=false
has_api=false
has_openssl=false
has_zlib=false
has_jk=false
has_iconv=false
has_expat=false
has_cluster=false

build_api=false
build_zlib=false
build_iconv=false
build_expat=false
build_cluster=false

has_cache=false
has_version=false
has_package=false
has_static=false
run_test=false

#
# Read local machine parameters.
TMPROOTBASE=/tmp
IPLOCAL=localhost
if  [ -f $HOME/jbossnative.`uname -n` ]; then
  . $HOME/jbossnative.`uname -n`
fi
export TMPROOTBASE
export IPLOCAL

while [ "x" != "x$1" ]
do
  case  $1 in
    -ssl)
      has_openssl=true
      ;;
    -cache)
      has_cache=true
      ;;
    -static)
      has_static=true
      ;;
    -test)
      run_test=true
      ;;
    [0-9]*)
      build_version=`echo $1`
      has_version=true
      ;;
    [a-z]*)
      PACKAGE=`echo $1`
      has_package=true
      ;;
    *)
      echo "Parameter $1 not supported"
      echo "Usage build.sh [-ssl|-cache|-static] [version] [package_name]"
      exit 1
      ;;
  esac
  shift
done

# Check if we just have only to build the binaries
# something like package-version-src or
# package-version-src-ssl (where package is jboss-blalbla).
if $build_srcp; then
  has_package=true
  has_version=true

  PACKAGE=`grep -v '^#' $package_list | head -n1 | awk -F"|" '{print $1}'`
  build_version=`grep -v '^#' $package_list | head -n1 | awk -F"|" '{print $2}'`

  echo "build_version : $build_version"
  echo "PACKAGE       : $PACKAGE"
fi

# check parameters.
if [ $has_version -a ! $has_package ]; then
  echo "Can't use version without package"
  exit 1
fi

# package.
if $has_package; then
  grep "^$PACKAGE" $package_list >/dev/null
  if [ $? -ne 0 ]; then
    echo "Canot find $PACKAGE in `basename ${package_list}`"
    exit 1
  fi
else
  echo "`basename $0`: Missing package name"
  echo "Usage `basename $0` package-name [options]."
  echo ""
  exit 1
fi

# get lastest version if needed.
if $has_version; then
  echo "Checking $PACKAGE version $PACKAGE from `basename ${package_list}`"
  grep "^$PACKAGE" ${package_list} | grep ${build_version} >/dev/null
  if [ $? -ne 0 ]; then
    echo "Canot find $PACKAGE $build_version in `basename ${package_list}`"
    exit 1
  fi
else
  parg=`grep -v '^#' ${package_list} | grep "^$PACKAGE|" | tail -1`
  build_version=`echo "${parg}" | ${awk} -F'|' '{print $2}'`
  has_version=true
  echo "Using version ${build_version} from `basename ${package_list}`"
fi
export build_version

# Check for some gnu mandadory tools.
patch=`util/find_gnu.sh patch`
if [ "x" = "x${patch}" ]; then
  echo "Error missing gnu patch command"
  exit 1
fi
export patch

# Read tag and version of subcomponent/dependencies.
parg=`grep -v '^#' ${package_list} | grep "^$PACKAGE|" | grep "|$build_version|" | sed 's:|: :g'`

i=0
for arg in `echo "${parg}"`
do
  i=`expr $i + 1`
  if [ $i -eq 3 ]; then
    svn_tagname=$arg
  fi
  if [ $i -gt 3 ]; then
    case $arg in
      apr:*)
        apr_version=`echo "${arg}" | ${awk} -F':' '{print $3}'`
        apr_type=`echo "${arg}" | ${awk} -F':' '{print $2}'`
        if [ "$apr_type" = "v" ]; then
          has_apr=true
        fi
        apr_sversion="apr:$apr_type:$apr_version"
        export apr_version
        ;;
      apu:*)
        apu_version=`echo "${arg}" | ${awk} -F':' '{print $3}'`
        apu_type=`echo "${arg}" | ${awk} -F':' '{print $2}'`
        if [ "$apu_type" = "v" ]; then
          has_apu=true
        fi
        apu_sversion="apu:$apu_type:$apu_version"
        export apu_version
        ;;
      api:*)
        api_version=`echo "${arg}" | ${awk} -F':' '{print $3}'`
        api_type=`echo "${arg}" | ${awk} -F':' '{print $2}'`
        has_api=true
        api_sversion="api:$api_type:$api_version"
        export api_version
        ;;
      ssl:*)
        ssl_version=`echo "${arg}" | ${awk} -F':' '{print $3}'`
        ssl_type=`echo "${arg}" | ${awk} -F':' '{print $2}'`
        has_ssl=true
        ssl_sversion="ssl:$ssl_type:$ssl_version"
        ;;
      zlib:*)
        zlib_version=`echo "${arg}" | ${awk} -F':' '{print $3}'`
        zlib_type=`echo "${arg}" | ${awk} -F':' '{print $2}'`
        has_zlib=true
        zlib_sversion="zlib:$zlib_type:$zlib_version"
        ;;
      jk:*)
        jk_version=`echo "${arg}" | ${awk} -F':' '{print $3}'`
        jk_type=`echo "${arg}" | ${awk} -F':' '{print $2}'`
        has_jk=true
        jk_sversion="jk:$jk_type:$jk_version"
        export jk_version
        ;;
      iconv:*)
        iconv_version=`echo "${arg}" | ${awk} -F':' '{print $3}'`
        iconv_type=`echo "${arg}" | ${awk} -F':' '{print $2}'`
        has_iconv=true
        iconv_sversion="iconv:$iconv_type:$iconv_version"
        ;;
      expat:*)
        expat_version=`echo "${arg}" | ${awk} -F':' '{print $3}'`
        expat_type=`echo "${arg}" | ${awk} -F':' '{print $2}'`
        has_expat=true
        expat_sversion="expat:$expat_type:$expat_version"
        ;;
      cluster:*)
        cluster_version=`echo "${arg}" | ${awk} -F':' '{print $3}'`
        cluster_type=`echo "${arg}" | ${awk} -F':' '{print $2}'`
        has_cluster=true
        cluster_sversion="cluster:$cluster_type:$cluster_version"
        export cluster_version
    esac
  fi
done

# Get plaform information
so_extension=so
if [  "x" = "x$BUILD_CPU" ]; then
  BUILD_CPU=`uname -m`
fi
case ${BUILD_CPU} in
  sun4u*)
    BUILD_CPU=sparcv9
  ;;
  i86pc*)
    BUILD_CPU=x86
  ;;
  i[3-6]86*)
    BUILD_CPU=x86
  ;;
  x86_64*)
    BUILD_CPU=x64
  ;;
  ia64*)
    BUILD_CPU=i64
  ;;
  9000/800*)
    BUILD_CPU=parisc2
    so_extension=sl
  ;;
  Power*)
    BUILD_CPU=ppc
    # Add the default java location
    if [ "x${JAVA_HOME}" = "x" ]; then
      JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/
      export JAVA_HOME
    fi
  ;;
  ppc64)
    add_conf="${add_conf} CFLAGS=-m64"
  ;;
esac

if [  "x" = "x$BUILD_SYS" ]; then
  BUILD_SYS=`uname -s`
fi
case ${BUILD_SYS} in
    Linux*)
    BUILD_SYS="linux2"
    ;;
    SunOS*)
    BUILD_SYS="solaris"
    # Try to use sun studio.
    # we need makedepend for make depend (openssl)
    PATH=/opt/SUNWspro/bin:/usr/ccs/bin:/usr/X/bin:/usr/local/bin:$PATH
    export PATH
    CC=cc
    export CC
    CPP="cc -E"
    export CPP
    if $has_iconv; then
      build_iconv=true
    fi
    if $has_expat; then
      build_expat=true
    fi
    ;;
    HP-UX*)
    BUILD_SYS="hpux"
    has_posix_tar=false
    CC=cc
    export CC
    # http://hpux.cs.utah.edu/ only has a 32 bits one.
    if [ $BUILD_CPU = "i64" ]; then
      if [ $has_zlib ]; then
        build_zlib=true
      fi
    fi
    ;;
    windows)
    so_extension=dll
    BUILD_WIN=true
    NATIVEEOL="CRLF"
    if $has_api; then
      build_api=true
    fi
    if $has_zlib; then
      build_zlib=true
    fi
    ;;
    CYGWIN*)
    so_extension=dll
    BUILD_WIN=true
    NATIVEEOL="CRLF"
    BUILD_SYS=windows
    ;;
esac

# Display what we are going to do.
echo "Building ${PACKAGE} on ${BUILD_SYS} ${BUILD_CPU}"
if $has_apr; then
echo "using apr: ${apr_version}"
fi
if $has_apu; then
echo "using apr-util: ${apu_version}"
fi
if $has_api; then
echo "using apr-iconv: ${api_version}"
fi
if $has_openssl; then
echo "using OpenSSL: ${ssl_version}"
fi
if $has_zlib; then
echo "using zlib: ${zlib_version}"
fi
if $has_jk; then
echo "using mod_jk: ${jk_version}"
fi
if $has_iconv; then
echo "using iconv: ${iconv_version}"
fi
if $has_expat; then
echo "using expat: ${expat_version}"
fi
if $has_cluster; then
echo "using mod_cluster: ${cluster_version}"
fi

export has_jk
export has_cluster
export so_extension
export build_iconv
export build_expat
export build_zlib

export NATIVEEOL
export BUILD_SYS
export BUILD_CPU
export BUILD_WIN
BUILD_TAG=${BUILD_SYS}-${BUILD_CPU}
export BUILD_TAG

# hack use our utilities instead the broken system ones.
if [ -d /home/shared/$BUILD_SYS-$BUILD_CPU/bin ]; then
  PATH=/home/shared/$BUILD_SYS-$BUILD_CPU/bin:$PATH
  export PATH
  echo "PATH changed to $PATH"
fi

build_cache_dir=${build_top}/cache
export build_cache_dir
build_working_dir=${build_top}/work
build_output_dir=${build_top}/output

mkdir -p ${build_working_dir}
mkdir -p ${build_output_dir}

build_common_dir=${build_cache_dir}/common/${BUILD_TAG}
export build_common_dir
if $has_cache ; then
  mkdir -p ${build_common_dir}
else
  rm -rf ${build_common_dir}
  mkdir -p ${build_common_dir}
fi

# download and copy the srclib stuff
package_src_name=${PACKAGE}-${build_version}-src
if $has_openssl; then
  package_src_name=${package_src_name}-ssl
fi
package_build_name=${PACKAGE}-${build_version}-${BUILD_SYS}-${BUILD_CPU}
if $has_openssl; then
  package_build_name=${package_build_name}-ssl
fi
if $build_srcp; then
  package_src_dir=${build_top}
  export package_src_dir
  package_dist_dir=${build_top}/dist/${BUILD_TAG}
  package_output_dir=${build_top}/output/${BUILD_TAG}
else
  package_src_dir=${build_working_dir}/${package_src_name}
  export package_src_dir
  package_dist_dir=${build_working_dir}/${package_src_name}/dist/${BUILD_TAG}
  package_output_dir=${build_working_dir}/${package_src_name}/output/${BUILD_TAG}
fi

# Override a shell command
# # $1=script $2..n
override()
{
  basedir=`dirname $1`
  cmd=`basename $1 .sh`
  if [ -x  ${basedir}/${cmd}.${BUILD_SYS}.${BUILD_CPU}.sh ]; then
    run=${cmd}.${BUILD_SYS}.${BUILD_CPU}.sh
  elif [ -x ${basedir}/${cmd}.${BUILD_SYS}.sh ]; then
    run=${cmd}.${BUILD_SYS}.sh
  else
    run=${cmd}.sh
  fi
  shift
  echo "Running ${basedir}/$run $@"
  ${basedir}/$run $@
  ret=$?
  if [ ${ret} -eq 2 ]; then
    echo "$run not supported on this platform"
    return 0
  elif [ ${ret} -ne 0 ]; then
    echo "$run returned ${ret}"
    return 1
  fi
}

# Download the sources. It guess how to do it.
# $1: URL.
# $1: Back URL. (archive for apache)
downloadtaredsrc()
{
  URL=$1
  URLBACK=$2
  if [ -z $URLBACK ]; then
    URLBACK=$URL
  fi
  wget --tries=0 --retry-connrefused $URL
  if [ $? -ne 0 ]; then
    echo "downloadtaredsrc Retrying on $URLBACK"
    wget --tries=0 --retry-connrefused $URLBACK
  fi
  fname=`basename $URL`
  case ${fname} in
    *.tar.gz)
      gunzip -c $fname | tar -xf -
      ;;
    *.zip)
      unzip -q -o $fname
      ;;
  esac
  rm -f $fname
}

# Wrap command line arguments to windows
# $1: Use back slashes
# $@: Arguments
wrapped_args=""
cygwinpath()
{
    back=$1
    shift

    # Convert the mountpoint in parameters to Win32 filenames
    # For instance: /cygdrive/c/foo -> c:/foo
    wrapped_args=`echo $1 | sed 's/\/cygdrive\/\(.\)\(.*\)/\1:\2/'`
    if ${back} ; then
        # Convert forward to back slashes
        # For instance: c:/foo - > c:\foo
        wrapped_args=`echo ${wrapped_args} | sed "s/\\//\\\\\\/g"`
    fi
    shift
    for i in "${@}"
    do
        i=`echo ${i} | sed 's/\/cygdrive\/\(.\)\(.*\)/\1:\2/'`
        if ${back} ; then
            # Convert forward to back slashes
            # For instance: c:/foo - > c:\foo
            i=`echo ${i} | sed "s/\\//\\\\\\/g"`
        fi
        wrapped_args="${wrapped_args} ${i}"
    done
}

#
# Build a tarball or a zip file
# $1: From where to take the files.
# $2: regexp with the files name (quoted string)
# $3: Where to put the tarball
# $4: Basename of the tarball
buildtar()
{
  src_dir=$1
  src_files=$2
  dst_dir=$3
  dst_name=$4
  if $BUILD_WIN ; then
    package_extension=zip
  else
    package_extension=tar.gz
  fi
  export package_extension

  # Remove any previous tarball
  rm -f ${dst_dir}/${dst_name}.$package_extension

  (cd ${src_dir}
  if $BUILD_WIN ; then
    zip -9rqo ${dst_dir}/${dst_name}.zip ${src_files}
  else
    if $has_posix_tar; then
      tar --owner=root --group=bin -cf - ${src_files} | gzip -c > ${dst_dir}/${dst_name}.tar.gz
    else
      tar -cf - * | gzip -c > ${dst_dir}/${dst_name}.tar.gz
    fi
  fi
  )
  ls -l ${dst_dir}/${dst_name}.$package_extension
  if [ $? -ne 0 ]; then
    echo "buildtar failed"
    exit 1
  fi
}

#
# Build openssl
# $1 directory of the openssl sources
# $2 static or shared
buildopenssl()
{
  ssl_srcdir=$1
  ssl_static=$2
  if $ssl_static ; then
    ssl_common_dir=${build_common_dir}-static
  else
    ssl_common_dir=${build_common_dir}
  fi

  if [ -f ${ssl_common_dir}/include/openssl/opensslv.h ]; then
    echo "Using cached openssl in ${ssl_common_dir}"
    return 0
  fi

  if [ ! -d ${ssl_srcdir} ]; then
    ssl_srcdir=srclib/`ls srclib | grep openssl-`
  fi
  SSLNUM=`basename ${ssl_srcdir} | awk -F - '{ print $2}' | sed 's:[a-z]::'`
  echo "Configuring OpenSSL ${ssl_version} for ${BUILD_TAG} ..."
  ssl_common_flags="threads no-zlib no-zlib-dynamic no-gmp no-krb5 no-rc5 no-mdc2 no-idea no-ec"
  if $ssl_static ; then
    ssl_build_flags="no-asm no-shared"
  else
    ssl_build_flags="shared"
  fi
  ssl_target="--prefix=${ssl_common_dir} --openssldir=${ssl_common_dir}/ssl"
  echo "ssl_srcdir: $ssl_srcdir in: `pwd`"
  (cd ${ssl_srcdir}
   case ${BUILD_TAG} in
     solaris-sparcv9)
        ./Configure ${ssl_target} ${ssl_common_flags} ${ssl_build_flags} solaris-sparcv9-cc || exit 1
      ;;
     solaris-x86)
        ./Configure ${ssl_target} ${ssl_common_flags} ${ssl_build_flags} solaris-x86-cc || exit 1
      ;;
     hpux-parisc2)
        ./Configure ${ssl_target} ${ssl_common_flags} ${ssl_build_flags} hpux-parisc2-cc || exit 1
      ;;
     hpux-i64)
        ./Configure ${ssl_target} ${ssl_common_flags} ${ssl_build_flags} hpux-ia64-cc || exit 1
      ;;
     linux2-ppc64)
        ./Configure ${ssl_target} ${ssl_common_flags} ${ssl_build_flags} linux-ppc64 || exit 1
      ;;
    windows-x86)
        ./Configure ${ssl_target} VC-NT
        ms/do_masm.bat
      ;;
    windows-x64)
        ./Configure ${ssl_target} VC-WIN64A
        ms/do_win64a.bat
      ;;
    windows-i64)
        ./Configure ${ssl_target} VC-WIN64I
        ms/do_win64i.bat
      ;;
     *)
        ./config ${ssl_target} -fPIC ${ssl_common_flags} ${ssl_build_flags} || exit 1
      ;;
   esac
   echo "Building OpenSSL ${ssl_version} for ${BUILD_TAG} ..."
   if $BUILD_WIN ; then
     if $ssl_static ; then
        nmake -f ms/nt.mak install || exit 1
     else
        nmake -f ms/ntdll.mak install || exit 1
     fi
   else
     make depend || exit 1
     make || exit 1
     # To hack broken openssl Makefiles.
     case ${BUILD_TAG} in
       hpux-i64)
         gmake install_sw CFLAGS=DSO_DLHACK || exit 1
         ;;
       hpux-parisc2)
         make install_sw || exit 1
         # openssl-0.9.8b is broken (work-around it).
         if $ssl_static; then
           :
         else
           cat ${ssl_common_dir}/lib/libssl.${so_extension} > /dev/null
           if [ $? -ne 0 ]; then
             rm -f ${ssl_common_dir}/lib/libssl.${so_extension}
             cp -p libssl.${so_extension} ${ssl_common_dir}/lib
             rm -f ${ssl_common_dir}/lib/libcrypto.${so_extension}
             cp -p libcrypto.${so_extension} ${ssl_common_dir}/lib
           fi
         fi
         ;;
       *)
         make install_sw || exit 1
         ;;
     esac
     if $ssl_static; then
       :
     else
       (cd ${ssl_common_dir}/lib
        if [ ! -f libcrypto.${so_extension}.${SSLNUM} ]; then
          ln -s libcrypto.${so_extension} libcrypto.${so_extension}.${SSLNUM}
        fi
        ln -s libcrypto.${so_extension}.${SSLNUM} libcrypto.${so_extension}.0
        if [ ! -f libssl.${so_extension}.${SSLNUM} ]; then
          ln -s libssl.${so_extension} libssl.${so_extension}.${SSLNUM}
        fi
        ln -s libssl.${so_extension}.${SSLNUM} libssl.${so_extension}.0
       )
     fi
   fi
  ) || return 1
}

#
# Build apr
# $1 directory of the sources directory
# $2 static or shared
buildapr()
{
  apr_srcdir=$1
  apr_static=$2

  if $apr_static ; then
    apr_common_dir=${build_common_dir}-static
  else
    apr_common_dir=${build_common_dir}
  fi

  if [ -f ${apr_common_dir}/include/apr-1/apr.h ]; then
    echo "Using cached apr in ${apr_common_dir}"
    return 0
  fi
  echo ""
  echo "Configuring apr-${apr_version} with --prefix=${apr_common_dir}"
  echo ""
  if $BUILD_WIN ; then
    (cd ${apr_srcdir}
      cp -f include/apr.hw include/apr.h
      echo "Building APR ${apr_version} ..."
      cygwinpath true PREFIX=${apr_common_dir} SRCDIR=`pwd`
      if $apr_static ; then
        nmake -f NMAKEmakefile $wrapped_args APR_DECLARE_STATIC=true install
      else
        nmake -f NMAKEmakefile $wrapped_args install
      fi
    )
  else
    (cd ${apr_srcdir}
      ./configure ${add_conf} --prefix=${apr_common_dir} || exit 1
      echo "Building APR ${apr_version} ..."
      make || exit 1
      make install || exit 1
    ) || return 1
  fi
}

#
# Build apr-util
# $1 directory of the sources directory
# $2 static or shared
buildapu()
{
  apu_srcdir=$1
  apu_static=$2

  if $apu_static ; then
    apu_common_dir=${build_common_dir}-static
  else
    apu_common_dir=${build_common_dir}
  fi

  if [ -f ${apu_common_dir}/include/apr-1/apu.h ]; then
    echo "Using cached apr-util in ${apu_common_dir}"
    return
  fi
  echo ""
  echo "Configuring apr-util-${apu_version} with --prefix=${apu_common_dir}"
  echo ""
  if $BUILD_WIN ; then
    (cd ${apu_srcdir}
      cp -f include/apu.hw include/apu.h
      cp -f xml/expat/lib/winconfig.h xml/expat/lib/config.h
      cp -f xml/expat/lib/expat.h.in xml/expat/lib/expat.h
      echo "Building APR utilities ${apu_version} ..."
      cygwinpath true WITH_APR=${apu_common_dir} PREFIX=${apu_common_dir} SRCDIR=`pwd`
      if $apu_static ; then
        nmake -f NMAKEmakefile $wrapped_args APR_DECLARE_STATIC=true install
      else
        nmake -f NMAKEmakefile $wrapped_args install
      fi
    )
  else
    (cd ${apu_srcdir}
      ./configure ${add_conf} --with-apr=${apu_common_dir} --prefix=${apu_common_dir} --with-expat=builtin --with-dbm=sdbm || exit  1
      echo "Building APR utilities ${apu_version} ..."
      make || exit  1
      make install || exit  1
    ) || return 1
  fi
}

#
# Build apr-iconv
# $1 directory of the sources directory
# $2 static or shared
buildapi()
{
  api_srcdir=$1
  api_static=$2

  if $api_static ; then
    api_common_dir=${build_common_dir}-static
  else
    api_common_dir=${build_common_dir}
  fi

  if [ -f ${api_common_dir}/include/apr-1/apr_iconv.h ]; then
    echo "Using cached apr-iconv in ${api_common_dir}"
    return 0
  fi
  echo ""
  echo "Configuring apr-iconv-${api_version} with --prefix=${api_common_dir}"
  echo ""
  if $BUILD_WIN ; then
    (cd ${api_srcdir}
      echo "Building APR iconv ${api_version} ..."
      cygwinpath true WITH_APR=${api_common_dir} PREFIX=${api_common_dir} SRCDIR=`pwd`
      if $api_static ; then
        nmake -f NMAKEmakefile $wrapped_args APR_DECLARE_STATIC=true install
      else
        nmake -f NMAKEmakefile $wrapped_args install
      fi
    )
  else
    (cd ${api_srcdir}
      ./configure ${add_conf} --with-apr=${api_common_dir} --prefix=${api_common_dir} || exit 1
      echo "Building APR iconv ${api_version} ..."
      make || exit 1
      make install || exit 1
    ) || return 1
  fi
}

#
# Build any package (standard).
# $1 directory of the sources directory
# $2 static or shared
buildany()
{
  any_srcdir=$1
  any_static=$2

  if $any_static ; then
    any_common_dir=${build_common_dir}-static
  else
    any_common_dir=${build_common_dir}
  fi

  echo ""
  echo "Configuring ${any_srcdir} with --prefix=${any_common_dir}"
  echo ""
  if $BUILD_WIN ; then
    (cd ${any_srcdir}
      echo "Building in ${any_srcdir} ..."
      cygwinpath true PREFIX=${any_common_dir} SRCDIR=`pwd`
      if $any_static ; then
        # XXX: That won't work probably.
        nmake -f NMAKEmakefile $wrapped_args ANY_DECLARE_STATIC=true install
      else
        nmake -f NMAKEmakefile $wrapped_args install
      fi
    )
  else
    (cd ${any_srcdir}
      echo "Building in ${any_srcdir} ..."
      if $any_static ; then
        add_static="--enable-static --disable-shared"
      else
        add_static=""
      fi
      ./configure ${add_conf}  --prefix=${any_common_dir} ${add_static} || exit 1
      make || exit 1
      make install || exit 1
    ) || return 1
  fi
}

#
# Build zlib
# $1 directory of the sources directory
# $2 static or shared
buildzlib()
{
  zlib_srcdir=$1
  zlib_static=$2

  if $zlib_static ; then
    zlib_common_dir=${build_common_dir}-static
  else
    zlib_common_dir=${build_common_dir}
  fi

  if [ -f ${zlib_common_dir}/include/apr-1/zlib.h ]; then
    echo "Using cached zlib in ${zlib_common_dir}"
    return
  fi
  echo ""
  echo "Configuring zlib-${zlib_version} with --prefix=${zlib_common_dir}"
  echo ""
  if $BUILD_WIN ; then
    (cd ${zlib_srcdir}
      echo "Building Zlib ${zlib_version} ..."
      cygwinpath true PREFIX=${zlib_common_dir} SRCDIR=`pwd`
      if $zlib_static ; then
        nmake -f NMAKEmakefile $wrapped_args ZLIB_DECLARE_STATIC=true install
      else
        nmake -f NMAKEmakefile $wrapped_args install
      fi
    )
  else
    (cd ${zlib_srcdir}
      echo "Building in ${zlib_srcdir} ..."
      if $zlib_static ; then
        add_static=""
      else
        add_static="--shared"
      fi
      ./configure ${add_conf}  --prefix=${zlib_common_dir} ${add_static} || exit 1
      make || exit 1
      make install || exit 1
    ) || return 1
  fi
}

#
# Build the binaries
# $1 directory where to put the resulting tarball.
#    XXX: It is always $build_output_dir !
buildbin()
{
  echo "Building binaries at `pwd`"
  dst_dir=$1

  if  $has_zlib; then
    # build both static and dynamic zlib
    buildzlib srclib/zlib-${zlib_version} false || return 1
    buildzlib srclib/zlib-${zlib_version} true || return 1
  fi
  # apr-util has a --with-openssl=DIR so we must be build openssl before apu. 
  ls srclib | grep openssl >/dev/null
  if [ $? -eq 0 ]; then
    buildopenssl srclib/openssl-${ssl_version} false || return 1
    if $has_static ; then
      buildopenssl srclib/openssl-${ssl_version} true || return 1
    fi
  fi

  # build a static libconv.
  if $build_iconv; then
    buildany srclib/libiconv-${iconv_version} true || return 1
  fi

  # build a static expat.
  if $build_expat; then
    buildany srclib/expat-${expat_version} true || return 1
  fi

  if  $has_apr; then
    buildapr srclib/apr-${apr_version} false || return 1
    if $has_static ; then
      buildapr srclib/apr-${apr_version} true || return 1
    fi
    if  $build_api; then
      buildapi srclib/apr-iconv-${api_version} false || return 1
      if $has_static ; then
        buildapi srclib/apr-iconv-${api_version} true || return 1
      fi
    fi
    if  $has_apu; then
      buildapu srclib/apr-util-${apu_version} false || return 1
      if $has_static ; then
        buildapu srclib/apr-util-${apu_version} true || return 1
      fi
    fi
  fi

  # Do our specific part.
  override ${package_src_dir}/buildbin.${PACKAGE}.sh ${build_common_dir} ${package_dist_dir} ${package_output_dir} $has_openssl $has_static ${package_src_dir}
  if [ $? -ne 0 ]; then
    echo "${package_src_dir}/buildbin.${PACKAGE}.sh failed"
    return 1
  fi

  # General part
  cp -rp licenses ${package_output_dir}

  # Build the binary distribution tarball
  buildtar ${package_output_dir} "*" ${dst_dir} ${package_build_name}
  if [ $? -ne 0 ]; then
    echo "buildtar ${package_output_dir} failed"
    return 1
  fi
}

# Only build binaries?
if $build_srcp; then
  buildbin ${build_output_dir}
  exit 0
fi

# Apply patch for the component
# $1 directory of the component like apr-1.2.8
# $2 also apply the rhel patches.
applypatch()
{
  DIR=$1
  isrhel=$2
  basefilename=`basename $DIR`
  if [ -f $build_top/../patch/$basefilename.patch ]; then
    (cd $DIR
    echo "Applying patch $build_top/../patch/$basefilename.patch in $DIR"
    $patch -tf -p0 -i $build_top/../patch/$basefilename.patch
    ) || return 1
  fi
  if $isrhel; then
    if [ -f $build_top/../patch/$basefilename.rhel.patch ]; then
      (cd $DIR
       echo "Applying patch $build_top/../patch/$basefilename.rhel.patch in $DIR"
      $patch -tf -p0 -i $build_top/../patch/$basefilename.rhel.patch
      ) || return 1
    fi
  fi
}

# Check and download
# $1 directory of the checkout directory
# $2 url for the download
# $3 backup url for the download
ckeckdownload()
{
  src_dir=$1
  src_url=$2
  src_url_back=$3
  cd $build_cache_dir
  if [ ! -d ${src_dir} ]; then
    downloadtaredsrc $src_url $src_url_back
    applypatch $src_dir false
  fi
  cp -rp ${src_dir} ${package_src_dir}/srclib
  cd $build_top
}

# set the urls
if $BUILD_WIN ; then
  APRURL=http://www.apache.org/dist/apr/apr-${apr_version}-win32-src.zip
  APUURL=http://www.apache.org/dist/apr/apr-util-${apu_version}-win32-src.zip
  APIURL=http://www.apache.org/dist/apr/apr-iconv-${api_version}-win32-src.zip
  APRURLBACK=http://archive.apache.org/dist/apr/apr-${apu_version}-win32-src.zip
  APUURLBACK=http://archive.apache.org/dist/apr/apr-util-${apu_version}-win32-src.zip
  APIURLBACK=http://archive.apache.org/dist/apr/apr-iconv-${api_version}-win32-src.zip
else
  APRURL=http://www.apache.org/dist/apr/apr-${apr_version}.tar.gz
  APUURL=http://www.apache.org/dist/apr/apr-util-${apu_version}.tar.gz
  APIURL=http://www.apache.org/dist/apr/apr-iconv-${api_version}.tar.gz
  APRURLBACK=http://archive.apache.org/dist/apr/apr-${apr_version}.tar.gz
  APUURLBACK=http://archive.apache.org/dist/apr/apr-util-${apu_version}.tar.gz
  APIURLBACK=http://archive.apache.org/dist/apr/apr-iconv-${api_version}.tar.gz
fi
SSLURL=http://www.openssl.org/source/openssl-${ssl_version}.tar.gz
ZLIBURL=http://www.zlib.net/zlib-${zlib_version}.tar.gz
ICONVURL=http://ftp.gnu.org/pub/gnu/libiconv/libiconv-${iconv_version}.tar.gz
EXPATURL=http://heanet.dl.sourceforge.net/sourceforge/expat/expat-${expat_version}.tar.gz
export APIURL
export APIURLBACK
export api_version

rm -rf ${package_src_dir}
mkdir -p ${package_src_dir}/srclib

if $has_zlib; then
  ckeckdownload zlib-${zlib_version} $ZLIBURL "NONE"
fi
# Note: type of the download (r: rhel, v: internet, t: tag in svn).
if $has_openssl; then
  if [ "${ssl_type}" = "v" ]; then
    ckeckdownload openssl-${ssl_version} $SSLURL "NONE"
  elif [ "${ssl_type}" = "r" ]; then
    if [ "x${ssl_version}" = "x" ]; then
      tag=trunk
    else
      tag=${ssl_version}
    fi
    util/buildrhelsrc.sh http://cvs.devel.redhat.com/repo/dist/openssl openssl/RHEL-5 ${tag} ${package_src_dir} openssl rhel-httpd ${build_version}
    openssl_dir=${package_src_dir}/srclib/`ls ${package_src_dir}/srclib | grep openssl-`
    applypatch ${openssl_dir} true 
  else
    echo "${ssl_type} not support for openssl"
    exit 1
  fi
fi
if  $has_apr; then
  ckeckdownload apr-${apr_version} $APRURL $APRURLBACK
fi
if $has_apu; then
  ckeckdownload apr-util-${apu_version} $APUURL $APUURLBACK
fi
if $has_api; then
  ckeckdownload apr-iconv-${api_version} $APIURL $APIURLBACK
fi
if $has_iconv; then
  ckeckdownload libiconv-${iconv_version} $ICONVURL "NONE"
fi
if $has_expat; then
  ckeckdownload expat-${expat_version} $EXPATURL "NONE"
fi

#
# Get the repository of what we build.
override ${build_top}/buildsrc.${PACKAGE}.sh ${svn_tagname} ${package_src_dir}
if [ $? -ne 0 ]; then
  echo "override ${build_top}/buildsrc.${PACKAGE}.sh ${svn_tagname} ${package_src_dir} failed"
  exit 1
fi

#
# Copy build files
cp ./build.sh ${package_src_dir}
cp ./buildbin.${PACKAGE}*.sh ${package_src_dir}
mkdir -p ${package_src_dir}/util
cp util/*.sh ${package_src_dir}/util
# TODO: Generate package list instead copying
# XXX: The part below is fishy

cat > ${package_src_dir}/package.build << EOF
# THIS FILE WAS AUTOGENERATED BY `basename $0`
#
${PACKAGE}|${build_version}|${svn_tagname}|${apr_sversion}|${apu_sversion}|${api_sversion}|${ssl_sversion}|${zlib_sversion}|${jk_sversion}

EOF

# Copy other files
cp ../NMAKEcommon.inc $package_src_dir
svn export --force ../../licenses $package_src_dir/licenses

if  $has_apr; then
  cp ../../srclib/apr/NMAKEmakefile $package_src_dir/srclib/apr-${apr_version}/
fi
if  $has_api; then
  cp ../../srclib/apr-iconv/NMAKEmakefile $package_src_dir/srclib/apr-iconv-${api_version}/
fi
if  $has_apu; then
  cp ../../srclib/apr-util/NMAKEmakefile $package_src_dir/srclib/apr-util-${apu_version}/
fi
if  $has_zlib; then
  cp ../../srclib/zlib/NMAKEmakefile $package_src_dir/srclib/zlib-${zlib_version}/
fi

# Build the source tarball
buildtar ${build_working_dir} ${package_src_name} ${build_output_dir} ${package_src_name}

mkdir -p ${package_dist_dir}
mkdir -p ${package_output_dir}

# Now build the binaries
(cd ${package_src_dir}
  buildbin $build_output_dir || exit 1
)
if [ $? -ne 0 ]; then
  echo "buildbin $build_output_dir failed"
  exit 1
fi

# generate a list of dependencies
override ${build_top}/util/builddep $build_output_dir ${PACKAGE}.${build_version}.${BUILD_SYS}.${BUILD_CPU}.depends ${package_output_dir}

if $run_test; then
  # create a chrootable environment for testing:
  if [ ! -d ${TMPROOTBASE}/${package_output_dir} ]; then
    override ${build_top}/util/buildroot ${TMPROOTBASE}/${package_output_dir}
    if [ $? -ne 0 ]; then
      echo "buildroot ${TMPROOTBASE}/${package_output_dir} failed"
      exit 1
    fi
  fi

  # run a test
  override ${build_top}/buildtest.${PACKAGE}.sh ${TMPROOTBASE} ${package_output_dir} ${build_cache_dir} ${IPLOCAL} ${has_ssl}
  if [ $? -ne 0 ]; then
    echo "buildtest.${PACKAGE} ${TMPROOTBASE} failed"
    exit 1
  fi

  # try to build from the source tarball
  override ${build_top}/util/buildfromtar ${TMPROOTBASE}/src ${build_output_dir} ${package_src_name} ${BUILD_WIN} ${has_ssl} ${build_version} ${PACKAGE}
  if [ $? -ne 0 ]; then
    echo "buildfromtar in ${TMPROOTBASE}/src failed"
    exit 1
  fi
fi
echo ""
echo "SUCCESS : `basename $0` $LastChangedDate: 2008-06-17 05:31:09 -0400 (Tue, 17 Jun 2008) $"
echo ""
