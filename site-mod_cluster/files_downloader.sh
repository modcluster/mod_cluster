# Change the version...
VERSION=1.2.6.Final
# Should I overwrite already downloaded files?
OVERWRITE=false

# TODO:
#  HP-UX      - as soon as we have some stable build
#  Linux ia64 - as above

export VERSION
declare -a errors
JENKINS_JOB_URL="http://hudson.qa.jboss.com/hudson/view/mod_cluster/view/mod_cluster/job/"
JENKINS_ARCHIVE_PATH="lastSuccessfulBuild/artifact/jbossnative/build/unix/output/"

BUILDS="\
    linux-i686 \
    linux-x86_64 \
    macosx \
    solaris10-sparc \
    solaris10-sparc64 \
    solaris10-x86 \
    windows \
"

LINUX_I686="\
    mod_cluster-${VERSION}-linux2-x86-so.tar.gz \
    mod_cluster-${VERSION}-linux2-x86-ssl.tar.gz \
    mod_cluster-${VERSION}-linux2-x86.tar.gz \
    mod_cluster-${VERSION}-src-ssl.tar.gz \
    mod_cluster-${VERSION}-src.tar.gz \
"
LINUX_X86_64="\
    mod_cluster-${VERSION}-linux2-x64-so.tar.gz \
    mod_cluster-${VERSION}-linux2-x64-ssl.tar.gz \
    mod_cluster-${VERSION}-linux2-x64.tar.gz \
    mod_cluster-${VERSION}-src-ssl.tar.gz \
    mod_cluster-${VERSION}-src.tar.gz \
"
MACOSX="\
    mod_cluster-${VERSION}-macosx-x64-ssl.tar.gz \
    mod_cluster-${VERSION}-macosx-x64.tar.gz \
    mod_cluster-${VERSION}-macosx-x86-so.tar.gz \
    mod_cluster-${VERSION}-src-ssl.tar.gz \
    mod_cluster-${VERSION}-src.tar.gz \
"
SOLARIS10_SPARC="\
    mod_cluster-${VERSION}-solaris10-sun4v-so.tar.gz \
    mod_cluster-${VERSION}-solaris10-sun4v-ssl.tar.gz \
    mod_cluster-${VERSION}-solaris10-sun4v.tar.gz \
    mod_cluster-${VERSION}-src-ssl.tar.gz \
    mod_cluster-${VERSION}-src.tar.gz \
"
SOLARIS10_SPARC64="\
    mod_cluster-${VERSION}-solaris10-sun4v-so.tar.gz \
    mod_cluster-${VERSION}-solaris10-sun4v-ssl.tar.gz \
    mod_cluster-${VERSION}-solaris10-sun4v.tar.gz \
    mod_cluster-${VERSION}-src-ssl.tar.gz \
    mod_cluster-${VERSION}-src.tar.gz \
"
SOLARIS10_X86="\
    mod_cluster-${VERSION}-solaris10-x86-so.tar.gz \
    mod_cluster-${VERSION}-solaris10-x86-ssl.tar.gz \
    mod_cluster-${VERSION}-solaris10-x86.tar.gz \
    mod_cluster-${VERSION}-src-ssl.tar.gz \
    mod_cluster-${VERSION}-src.tar.gz \
"
WINDOWS="\
    mod_cluster-${VERSION}-src-ssl.zip \
    mod_cluster-${VERSION}-src.zip \
    mod_cluster-${VERSION}-windows-amd64-ssl.zip \
    mod_cluster-${VERSION}-windows-amd64.zip \
    mod_cluster-${VERSION}-windows-x86-ssl.zip \
    mod_cluster-${VERSION}-windows-x86.zip \
"

function error_report () {
    if [ ${#errors[@]} -gt 0 ]; then
        echo "ERRORS:"
        for error in "${errors[@]}"; do
            echo "    $error"
        done
    exit 1;
    fi
}

function downloadit () {
    local build="$1"
    local file="$2"
    url="${JENKINS_JOB_URL}mod_cluster-${build}/${JENKINS_ARCHIVE_PATH}${file}"
    if [ -f ${VERSION}/${build}/${file} ] && [ !$OVERWRITE ]; then
        echo "SKIPPING DOWNLOAD for ${url}"
    else
        wget ${url} -O ${VERSION}/${build}/${file} || errors+=( "Failed to download: ${url}" )
    fi
}

for build in $BUILDS; do
  mkdir -p ${VERSION}/${build}
  case $build in
     *linux-i686*)
          for file in $LINUX_I686; do
              downloadit $build $file
          done
        ;;
     *linux-x86_64*)
          for file in $LINUX_X86_64; do
              downloadit $build $file
          done
        ;;
     *macosx*)
          for file in $MACOSX; do
              downloadit $build $file
          done
        ;;
     *solaris10-sparc*)
          for file in $SOLARIS10_SPARC; do
              downloadit $build $file
          done
        ;;
     *solaris10-sparc64*)
          for file in $SOLARIS10_SPARC64; do
              downloadit $build $file
          done
        ;;
     *solaris10-x86*)
          for file in $SOLARIS10_X86; do
              downloadit $build $file
          done
        ;;
     *windows*)
          for file in $WINDOWS; do
              downloadit $build $file
          done
        ;;
  esac
done
cd ${VERSION}
md5sum */* > MD5SUMS
error_report

#Let's move upload to some other script...
#rsync $VERSION -r -a -v --protocol=29 mod_cluster@filemgmt.jboss.org:downloads_htdocs/mod_cluster/
