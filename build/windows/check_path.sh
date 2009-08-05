mountpoint=$CYGDRIVE_MOUNT
if test -z "$mountpoint"; then
    mountpoint=`mount -p | tail -1 | awk ' { print $1 } '`
    if test -z "$mountpoint"; then
       print "Cannot determine cygwin mount points. Exiting"
       exit 1
    fi
fi
localdir=`pwd`
case ${localdir} in
  ${mountpoint}*)
    exit 0
    ;;
  *)
    echo "cygwin is ${mountpoint} and we are in ${localdir}"
    exit 1
    ;;
esac
exit 0
