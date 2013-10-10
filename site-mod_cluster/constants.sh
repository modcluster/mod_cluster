# Constants

# Change the version...
VERSION=1.2.6.Final
export VERSION

# Should I overwrite already downloaded files?
OVERWRITE=false

JENKINS_JOB_URL="http://hudson.qa.jboss.com/hudson/view/mod_cluster/view/mod_cluster/job/"
JENKINS_ARCHIVE_PATH="lastSuccessfulBuild/artifact/jbossnative/build/unix/output/"

OUTPUT_XML_FILE="website.mod_cluster.downloads.${VERSION}.xml"

# Warning: There is a reason for having linux-x86_64 and mod_cluster-parent-${VERSION}-bin.tar.gz as the
# very first elements. This makes sure the java bundle appears as the first thing on the download page.
# The order of all other elements is of no consequence but for the fact that it affects the
# order of appearance on the download page...
BUILDS="\
    linux-x86_64 \
    linux-i686 \
    macosx \
    solaris10-sparc \
    solaris10-sparc64 \
    solaris10-x86 \
    windows \
    hp-ux-9000_800 \
    hp-ux-ia64 \
"

LINUX_X86_64="\
    mod_cluster-parent-${VERSION}-bin.tar.gz \
    mod_cluster-${VERSION}-linux2-x64-so.tar.gz \
    mod_cluster-${VERSION}-linux2-x64-ssl.tar.gz \
    mod_cluster-${VERSION}-linux2-x64.tar.gz \
    mod_cluster-${VERSION}-src-ssl.tar.gz \
    mod_cluster-${VERSION}-src.tar.gz \
"
LINUX_I686="\
    mod_cluster-${VERSION}-linux2-x86-so.tar.gz \
    mod_cluster-${VERSION}-linux2-x86-ssl.tar.gz \
    mod_cluster-${VERSION}-linux2-x86.tar.gz \
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
HP_UX_9000_800="\
    mod_cluster-${VERSION}-hpux-parisc2-so.tar.gz \
    mod_cluster-${VERSION}-hpux-parisc2-ssl.tar.gz \
    mod_cluster-${VERSION}-hpux-parisc2.tar.gz \
    mod_cluster-${VERSION}-src-ssl.tar.gz \
    mod_cluster-${VERSION}-src.tar.gz \
"
HP_UX_IA64="\
    mod_cluster-${VERSION}-hpux-i64-so.tar.gz \
    mod_cluster-${VERSION}-hpux-i64-ssl.tar.gz \
    mod_cluster-${VERSION}-hpux-i64.tar.gz \
    mod_cluster-${VERSION}-src-ssl.tar.gz \
    mod_cluster-${VERSION}-src.tar.gz \
"

function die() {
  die_ret_val=$?
  echo ${1}
  exit ${die_ret_val}
}
