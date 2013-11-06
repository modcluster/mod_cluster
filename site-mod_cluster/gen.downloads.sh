source ${0%\/*}/constants.sh || die "constants.sh failed!"
source ${0%\/*}/constants_xml.sh || die "constants_xml.sh failed!"

count=0

# Print a file entry in the project.xml file
# $1 file name
# $2 type binaries or sources or java
# $3 description
printfile_return=""
printfile() {
    local file=$1
    local type=$2
    local description=$3
    echo "Processing: \"${file}\" \"${type}\" \"${description}\""
    size=`ls -lh $file | awk ' { print $5 } '`
    date=`ls -lh $file | awk ' { print $6" "$7 }'`
    date=`date -d "$date" +'%FT%H:%M:%S.000Z'`
    count=`expr $count + 1`
    num=`echo $count | awk ' { if ($1<10) print "0"$1; else print $1 } '`
    read -d '' printfile_return << EOF
        <sv:property sv:name="desc${num}" sv:type="String">
          <sv:value>${description}</sv:value>
        </sv:property>
        <sv:property sv:name="license${num}" sv:type="String">
          <sv:value>LGPL</sv:value>
        </sv:property>
        <sv:property sv:name="name${num}" sv:type="String">
          <sv:value>${type}</sv:value>
        </sv:property>
        <sv:property sv:name="releaseDate${num}" sv:type="Date">
          <sv:value>${date}</sv:value>
        </sv:property>
        <sv:property sv:name="size${num}" sv:type="String">
          <sv:value>${size}</sv:value>
        </sv:property>
        <sv:property sv:name="text${num}" sv:type="String">
          <sv:value>mod_cluster-${VERSION}</sv:value>
        </sv:property>
        <sv:property sv:name="url${num}" sv:type="String">
          <sv:value>http://downloads.jboss.org/mod_cluster/${version}/${file}</sv:value>
        </sv:property>
EOF
}

print_formatter() {
    local build=$1
    local file=$2
    local src_or_bin=$3
    # Should we print at all?
    if [[ "$file" == *-src* && "$src_or_bin" == "src" ]] ||  [[ "$file" != *-src* && "$src_or_bin" == "bin" ]]; then
        # Java bundles?
        if [[ "$file" == *parent* ]]; then
            printfile "${VERSION}/${build}/${file}" "java bundles" "java budles for JBoss AS, Tomcat..."
        # Sources for openssl enabled httpd bundle
        else if [[ "$file" == *src-ssl* ]]; then
            printfile "${VERSION}/${build}/${file}" "${build} httpd+ssl sources" "mod_cluster native budles with httpd and openssl"
        # Sources for httpd bundle
        else if [[ "$file" == *src.* ]]; then
            printfile "${VERSION}/${build}/${file}" "${build} httpd sources" "mod_cluster native budles with httpd"
        # Native bundle with httpd and openssl
        else if [[ "$file" == *-ssl.* ]]; then
            printfile "${VERSION}/${build}/${file}" "${build} httpd+ssl binaries" "mod_cluster native budles with httpd and openssl"
        # Only the mod_cluster modules
        else if [[ "$file" == *-so* ]]; then
            printfile "${VERSION}/${build}/${file}" "${build} mod_cluster binaries" "mod_cluster modules for httpd"
        # Native bundle with httpd
        else
            printfile "${VERSION}/${build}/${file}" "${build} httpd binaries" "mod_cluster native budles with httpd"
        fi
        fi
        fi
        fi
        fi
    fi
    constructed_xml="${constructed_xml}${printfile_return}"
}

generate_document() {
    local src_or_bin=$1
    constructed_xml="${FIRSTNODE}"
    constructed_xml="${constructed_xml}`echo -e '\n'`${ITEMDOWNLOAD_1}"
    
    printfile "${VERSION}/MD5SUMS" "MD5SUMS" "MD5 hash sums"
    constructed_xml="${constructed_xml}${printfile_return}"
    
    for build in $BUILDS; do
      case $build in
         *linux-i686*)
              for file in $LINUX_I686; do
                print_formatter "${build}" "${file}" "$src_or_bin"
              done
            ;;
         *linux-x86_64*)
              for file in $LINUX_X86_64; do
                print_formatter "${build}" "${file}" "$src_or_bin"
              done
            ;;
         *macosx*)
              for file in $MACOSX; do
                print_formatter "${build}" "${file}" "$src_or_bin"
              done
            ;;
         *solaris10-sparc*)
              for file in $SOLARIS10_SPARC; do
                print_formatter "${build}" "${file}" "$src_or_bin"
              done
            ;;
         *solaris10-sparc64*)
              for file in $SOLARIS10_SPARC64; do
                print_formatter "${build}" "${file}" "$src_or_bin"
              done
            ;;
         *solaris10-x86*)
              for file in $SOLARIS10_X86; do
                print_formatter "${build}" "${file}" "$src_or_bin"
              done
            ;;
         *windows*)
              for file in $WINDOWS; do
                print_formatter "${build}" "${file}" "$src_or_bin"
              done
            ;;
         *hp-ux-9000_800*)
              for file in $HP_UX_9000_800; do
                print_formatter "${build}" "${file}" "$src_or_bin"
              done
            ;;
         *hp-ux-ia64*)
              for file in $HP_UX_IA64; do
                print_formatter "${build}" "${file}" "$src_or_bin"
              done
            ;;
      esac
    done
    
    constructed_xml="${constructed_xml}${ITEMDOWNLOAD_2}"
    
    echo "${constructed_xml}" > ${0%\/*}/${src_or_bin}-${OUTPUT_XML_FILE} || die "Output xml file generation failed".
    web_page_name=`echo ${VERSION} | sed 's/\./-/g'`
    sed -i "s/@DOWNLOADS@/${web_page_name}-${src_or_bin}/g" ${0%\/*}/${src_or_bin}-${OUTPUT_XML_FILE} 
    sed -i "s/@VERSION@/mod_cluster ${VERSION} ${src_or_bin}/g" ${0%\/*}/${src_or_bin}-${OUTPUT_XML_FILE} 
    echo "DONE! File ${src_or_bin}-${OUTPUT_XML_FILE} successfully generated!"
}

generate_document "src"
count=0
generate_document "bin"
