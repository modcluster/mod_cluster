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
echo ""
echo "Running : `basename $0` $LastChangedDate: 2007-06-01 18:00:27 +0200 (Fri, 01 Jun 2007) $"
echo ""
echo "Started : `date`"
echo "URL     : $1"
echo "repo    : $2"
echo "tag     : $3"
echo "dir     : $4"
echo "compo   : $5"
echo "package : $6"
echo "version : $7"
echo ""

# parameters
# $1: URL where download sources and patches. something like http://cvs.devel.redhat.com/repo/dist/httpd.
# $2: repo (something like httpd/RHEL-5)
# $3: tag for cvs repo.
# $4: Directory where to put the sources.
# $5: Component (in data for the list of patch that shouldn't be applied).
# $6: package. (to find the patch that should be applied).
# $7: package version. (to find the patch that should be applied).

URL=$1
baserepo=$2
tag=$3
destdir=$4
compo=$5
package=$6
version=$7

# we need something like:
#cvs -d :pserver:anonymous@cvs.devel.redhat.com:/cvs/dist export \
#    -r httpd-2_2_3-11_el5 httpd/RHEL-5
# or
#   -r 2.2.8-1.el5s2 httpd/RHEL-5-Stack-V2
case ${tag} in
  *el5s*)
     repo=${baserepo}-Stack-V2
     ;;
  *)
     repo=${baserepo}
     ;;
esac

cvsloc=:pserver:anonymous@cvs.devel.redhat.com:/cvs/dist
if [ "$tag" = "trunk" ];then
  native_tag_opt="-r HEAD"
else
  native_tag_opt="-r $tag"
fi

rhel_loc=${destdir}/srclib
mkdir -p ${rhel_loc}/${compo}
echo "Doing cvs -d ${cvsloc} export ${native_tag_opt} ${repo}"
cvs -d ${cvsloc} export ${native_tag_opt} ${repo}
if [ $? -ne 0 ];then
  echo "cvs failed"
  exit 1
fi
mv ${repo} ${rhel_loc}/${compo}
repobase=`dirname ${repo}`
if [ "${repobase}" != "." ]; then
  rm -rf ${repobase}
fi
rhel_loc=${destdir}/srclib/${compo}/`basename ${repo}`


# download something like:
# http://cvs.devel.redhat.com/repo/dist/httpd/httpd-2.2.3.tar.gz/f72ffb176e2dc7b322be16508c09f63c/httpd-2.2.3.tar.gz
fname=`cat ${rhel_loc}/sources | awk '{ print $2 }'`
dir=`cat ${rhel_loc}/sources | awk '{ print $1 }'`
sum="NONE"
if [ -f ${build_cache_dir}/${fname} ]; then
  sum=`openssl md5 ${build_cache_dir}/${fname} | awk ' { print $2 } '`
  if [ "x${sum}" = "x" ];then
    sum=`md5sum ${build_cache_dir}/${fname} | awk ' { print $1 } '`
  fi
fi
if [ ${dir} = ${sum} ]; then
  echo "${build_cache_dir}/$fname already available"
else
  rm -f ${build_cache_dir}/$fname
  rm -f $fname
  wget --tries=0 --retry-connrefused ${URL}/${fname}/${dir}/${fname}
  if [ $? -ne 0 ];then
    echo "wget $fname failed"
    exit 1
  fi
  mv $fname ${build_cache_dir}
fi

# expand the file.
case $fname in
  *.tar.bz2)
    bzip2 -dc ${build_cache_dir}/${fname} | tar -xf -
    if [ $? -ne 0 ];then
      echo "extract $fname failed"
      exit 1
    fi
    dirsources=`bzip2  -dc ${build_cache_dir}/${fname} | tar -tf - | head -1 | awk '{ print $1 }'`
    ;;
  *.tar.gz)
    gunzip -c ${build_cache_dir}/${fname} | tar -xf -
    if [ $? -ne 0 ];then
      echo "extract $fname failed"
      exit 1
    fi
    dirsources=`gunzip -c ${build_cache_dir}/${fname} | tar -tf - | head -1 | awk '{ print $1 }'`
    ;;
  *)
    echo "$fname can't expanded"
    exit 1
    ;;
esac

# Move the tree.
mv ${dirsources} ${destdir}/srclib
dirsources=${destdir}/srclib/${dirsources}

#
# Read the patches list and apply them
WHERE=${destdir}/tools
mkdir -p ${WHERE}
grep "^%patch" ${rhel_loc}/${compo}.spec | sed 's:%:@:' | sed 's: :@ :' | awk ' { print $1 " " $2 } ' > ${WHERE}/patch.cmd
grep "^Patch" ${rhel_loc}/${compo}.spec | sed 's:^Patch:@patch:' | sed 's/:/@/' |  awk ' { print "s:" $1 ": @PATCH@ -i @DIR@" $2 ":" } ' > ${WHERE}/patch.files
sed -f ${WHERE}/patch.files ${WHERE}/patch.cmd | sed "s:@DIR@:${rhel_loc}/:" | sed "s:@PATCH@:${patch}:" > ${WHERE}/patch.sh

#
# Remove linux specific patches.
# The file containing the patch that shouldn't be applied are build the following way.
# ${package}.${version}.${compo}.${tag}.${BUILD_SYS}.${BUILD_CPU}
echo "Don't apply patch search in ${build_top}/data/${package}.${version}.${compo}.${tag}.${BUILD_SYS}.${BUILD_CPU}"
if [ -f ${build_top}/data/${package}.${version}.${compo}.${tag}.${BUILD_SYS}.${BUILD_CPU} ]; then
  remove_list=`cat ${build_top}/data/${package}.${version}.${compo}.${tag}.${BUILD_SYS}.${BUILD_CPU}`
elif [ -f ${build_top}/data/${package}.${compo}.${tag}.${BUILD_SYS}.${BUILD_CPU} ]; then
  remove_list=`cat ${build_top}/data/${package}.${compo}.${tag}.${BUILD_SYS}.${BUILD_CPU}`
elif [ -f ${build_top}/data/${package}.${compo}.${tag}.${BUILD_SYS} ]; then
  remove_list=`cat ${build_top}/data/${package}.${compo}.${tag}.${BUILD_SYS}`
elif [ -f ${build_top}/data/${package}.${compo}.${BUILD_SYS} ]; then
  remove_list=`cat ${build_top}/data/${package}.${compo}.${BUILD_SYS}`
elif [ -f ${build_top}/data/${package}.${compo} ]; then
  remove_list=`cat ${build_top}/data/${package}.${compo}`
else
  remove_list=""
fi
if [ -z "${remove_list}" ]; then
  echo "applying all the rhel patches"
else
  echo "Not applying the following rhel patches: ${remove_list}"
  for patchfile in `echo "$remove_list"`
  do
    cat ${WHERE}/patch.sh | grep -v ${patchfile} > ${WHERE}/patch.${compo}.sh
    mv ${WHERE}/patch.${compo}.sh ${WHERE}/patch.sh
  done
fi
echo "Applying the rhel patches to ${dirsources}"
chmod a+x ${WHERE}/patch.sh
(cd ${dirsources}
 ${WHERE}/patch.sh
)

echo "Applying the jboss patches to ${dirsources}"
$build_top/util/applypatch.sh $build_top ${dirsources} true
