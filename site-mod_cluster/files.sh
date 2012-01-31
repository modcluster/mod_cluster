# Change the version 1.2.0.Beta4
VERSION=1.2.0.Beta4
export VERSION
mkdir -p ${VERSION}
for file in `cat files.list`
do
  HTTPFILE=`echo $file | sed "s:-1.0.0-:-${VERSION}-:"`
  FILE=${HTTPFILE}
  case $FILE in
     *hpux-parisc2*)
        BASE=mod_cluster-hp-ux-9000_800
        ;;
     *hpux-i64*)
        BASE=mod_cluster-hp-ux-ia64
        ;;
     *linux2-x86*)
        BASE=mod_cluster-linux-i686
        ;;
     *linux2-i64*)
        BASE=mod_cluster-linux-ia64
        ;;
     *linux2-x64*)
        BASE=mod_cluster-linux-x86_64
        ;;
     *solaris10-sun4v*)
        BASE=mod_cluster-solaris10-sparc
        FILE=`echo ${HTTPFILE} | sed 's:-solaris-sun4v-:-solaris10-sparc-:'`
        ;;
     *solaris64-sun4v*)
        BASE=mod_cluster-solaris10-sparc64
        FILE=`echo ${HTTPFILE} | sed 's:-solaris64-sun4v-:-solaris10-sparc64-:'`
        HTTPFILE=`echo ${HTTPFILE} | sed 's:-solaris64-sun4v-:-solaris10-sun4v-:'`
        ;;
     *solaris9-sparcv9*)
        BASE=mod_cluster-solaris-sparc
        FILE=`echo ${HTTPFILE} | sed 's:-solaris-sparcv9-:-solaris-sparc-:'`
        ;;
     *solaris64-sparcv9*)
        BASE=mod_cluster-solaris-sparc64
        FILE=`echo ${HTTPFILE} | sed 's:-solaris64-sparcv9-:-solaris-sparc64-:'`
        HTTPFILE=`echo ${HTTPFILE} | sed 's:-solaris64-sparcv9-:-solaris9-sparcv9-:'`
        ;;
     *solaris*-x86*)
        BASE=mod_cluster-solaris-x86
        ;;
     *macosx-x86*)
        BASE=mod_cluster-macosx
        ;;
     *windows*)
        BASE=mod_cluster-windows
        ;;
      *.zip)
        BASE=mod_cluster-windows
        ;;
      *.tar.gz)
        BASE=mod_cluster-linux-i686
        ;;
  esac
  echo $FILE
  echo $BASE
  (cd ${VERSION}
   wget http://hudson.qa.jboss.com/hudson/view/Mod_cluster/job/${BASE}/lastSuccessfulBuild/artifact/jbossnative/build/unix/output/$HTTPFILE || exit 1
   if [ $HTTPFILE != $FILE ]; then
     mv $HTTPFILE $FILE
   fi
  ) || exit 1
done
#http://www.jboss.org/downloading/?projectId=mod_cluster&url=http://labs.jboss.com/file-access/default/members/mod_cluster/freezone/dist/1.0.0.Alpha/mod-cluster-1.0.0-linux2-x86-ssl.tar.gz
#http://hudson.qa.jboss.com/hudson/view/Native/job/mod_cluster-solaris-x86/lastSuccessfulBuild/artifact/jbossnative/build/unix/output/rhel-httpd-2.2.8-1.el5s2-solaris-x86-ssl.tar.gz
