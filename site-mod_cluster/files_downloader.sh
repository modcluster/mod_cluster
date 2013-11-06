source ${0%\/*}/constants.sh || die "constants.sh failed!"

# TODO:
#  Linux ia64 - as soon as we have some stable build

declare -a errors

function error_report_and_exit () {
    if [ ${#errors[@]} -gt 0 ]; then
        echo "ERRORS:"
        for error in "${errors[@]}"; do
            echo "    $error"
        done
        exit 1;
    else
        echo "DONE with no errors :-)"
        exit 0;
    fi
}

function downloadit () {
    local build="$1"
    local file="$2"
    local overwrite_archive_path="$3"
    if [ "${overwrite_archive_path}X" == "X" ]; then
        url="${JENKINS_JOB_URL}mod_cluster-${build}/${JENKINS_ARCHIVE_PATH}${file}"
    else
        url="${JENKINS_JOB_URL}mod_cluster-${build}/${overwrite_archive_path}${file}"
    fi
    if [ -f ${VERSION}/${build}/${file} ] && [ !$OVERWRITE ]; then
        echo "SKIPPING DOWNLOAD for ${url}"
    else
        wget ${url} -O ${VERSION}/${build}/${file} || errors+=( "Failed to download: ${url} Wget returned:$?" )
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
     *hp-ux-9000_800*)
          downloadit $build "archive.zip" "lastSuccessfulBuild/artifact/"
          unzip ${VERSION}/${build}/archive.zip -d to_be_deleted
          for file in $HP_UX_9000_800; do
              mv to_be_deleted/jbossnative/build/unix/output/${file} ${VERSION}/${build}/
          done
          rm -rf to_be_deleted
        ;;
     *hp-ux-ia64*)
          for file in $HP_UX_IA64; do
              downloadit $build $file "lastSuccessfulBuild/artifact/jbossnative/build/unix/output/"
          done
        ;;
  esac
done
cd ${VERSION}
find -type f ! -name archive.zip ! -name MD5SUMS | xargs md5sum > MD5SUMS
error_report_and_exit

#Let's move upload to some other script...
#rsync $VERSION --exclude "archive.zip" -r -a -v --protocol=29 mod_cluster@filemgmt.jboss.org:downloads_htdocs/mod_cluster/
