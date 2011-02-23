# Adding  (bin)  1.0.0.CR1/mod_cluster-1.0.0.CR1-windows-x86-ssl.zip
VERSION=1.0.8.GA
count=0
# Print a file entry in the project.xml file
# $1 file name
# $2 type binaries or sources or java
printfile()
{
  file=$1
  type=$2
  desc=$3

  os=`echo $file | awk -F- ' { print $3 } '`
  pro=`echo $file | awk -F- ' { print $4 } '`
  # quick hack for the solaris9/10
  case "${os}-${pro}" in
    solaris-sparc*)
      os=solaris9
      ;;
    solaris-x86)
      os=solaris10
      ;;
  esac
  case "$type" in
    binaries|dynamic*)
      type="$type ${os}-${pro}"
      ;;
  esac

  size=`ls -lh $file | awk ' { print $5 } '`
  date=`ls -lh $file | awk ' { print $6" "$7 }'`
  # date=`date -d "$date" +'%FT%H:%M:%S.000%z'` only F11+
  date=`date -d "$date" +'%FT%H:%M:%S.000Z'`
  count=`expr $count + 1`
  num=`echo $count | awk ' { if ($1<10) print "0"$1; else print $1 } '`
  cat << EOF
      <sv:property sv:name="desc${num}" sv:type="String">
        <sv:value>${desc}</sv:value>
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
        <sv:value>mod_cluster-${version}</sv:value>
      </sv:property>
      <sv:property sv:name="url${num}" sv:type="String">
        <sv:value>http://downloads.jboss.org/mod_cluster/${version}/${file}</sv:value>
      </sv:property>
EOF
}

# print the first part of the export stuff.
cat firstnode.xml | sed "s:@node@:downloads:"
cat itemdownload.xml

# For the java part
printfile ${VERSION}/mod-cluster-${VERSION}-bin.tar.gz "java bundles" "java mod_cluster $VERSION"

# Sources in tarball format.
printfile ${VERSION}/mod_cluster-${VERSION}-src-ssl.tar.gz "source tarball" "mod_cluster $VERSION sources"

# Sources in zip format
printfile ${VERSION}/mod_cluster-${VERSION}-src-ssl.zip "source zip" "mod_cluster $VERSION sources"

# Process bundles
for file in `ls ${VERSION}/mod_cluster-${VERSION}-*-ssl.tar.gz`
do
  case "${file}" in
    *-src-*)
      continue;
      ;;
  esac
  printfile ${file} "binaries" "mod_cluster $VERSION tar bundles"
done
for file in `ls ${VERSION}/mod_cluster-${VERSION}-*-ssl.zip`
do
  case "${file}" in
    *-src-*)
      continue;
      ;;
  esac
  printfile ${file} "binaries" "mod_cluster $VERSION zip bundles"
done

# Process so bundles
for file in `ls ${VERSION}/mod_cluster-${VERSION}-*-so.tar.gz`
do
  case "${file}" in
    src-*)
      continue;
      ;;
  esac
  printfile ${file} "dynamic libraries" "mod_cluster $VERSION so/dll files"
done

# write the end of node...
cat itemdownload2.xml
