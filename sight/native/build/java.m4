dnl --------------------------------------------------------------------------
dnl JAVA Utilities
dnl
dnl Detection of JDK location and Java OS Platform
dnl result goes in JAVA_HOME / JAVA_OS
dnl
dnl --------------------------------------------------------------------------
AC_DEFUN(
  [JAVA_FIND_JDK],
  [
    tempval=""
    AC_MSG_CHECKING([for JDK location (please wait)])
    if test -n "${JAVA_HOME}" ; then
      JAVA_HOME_ENV="${JAVA_HOME}"
    else
      JAVA_HOME_ENV=""
    fi

    JAVA_HOME=""
    JAVA_PLATFORM=""

    AC_ARG_WITH(
      [java-home],
      [  --with-java-home=DIR     Location of JDK directory.],
      [

      # This stuff works if the command line parameter --with-java-home was
      # specified, so it takes priority rightfully.

      tempval=${withval}

      if test ! -d "${tempval}" ; then
          AC_MSG_ERROR(Not a directory: ${tempval})
      fi

      JAVA_HOME=${tempval}
      AC_MSG_RESULT(${JAVA_HOME})
    ],
    [
      # This works if the parameter was NOT specified, so it's a good time
      # to see what the enviroment says.
      # Since Sun uses JAVA_HOME a lot, we check it first and ignore the
      # JAVA_HOME, otherwise just use whatever JAVA_HOME was specified.

      if test -n "${JAVA_HOME_ENV}" ; then
        JAVA_HOME=${JAVA_HOME_ENV}
        AC_MSG_RESULT(${JAVA_HOME_ENV} from environment)
      fi
    ])

    if test ! -n "${JAVA_HOME}" ; then
      AC_MSG_RESULT(Cannot locate a valid JDK location)
      AC_MSG_ERROR(You should retry --with-java-home=DIR)
    fi
    unset tempval
  ])


AC_DEFUN(
  [JAVA_FIND_JDK_OS],
  [
    tempval=""
    JAVA_OS=""
    AC_ARG_WITH(java-os,
      [  --with-java-os[=SUBDIR]  Location of JDK os-type subdirectory.],
      [
        tempval=${withval}

        if test ! -d "${JAVA_HOME}/${tempval}" ; then
          AC_MSG_ERROR(Not a directory: ${JAVA_HOME}/${tempval})
        fi

        JAVA_OS = ${tempval}
        AC_MSG_RESULT(${JAVA_OS})
      ],
      [
        AC_MSG_CHECKING(java_os directory)
        JAVA_OS=NONE
        if test -f ${JAVA_HOME}/${JAVA_INC}/jni_md.h; then
          JAVA_OS=${JAVA_HOME}/${JAVA_INC}
        else
          for f in ${JAVA_HOME}/${JAVA_INC}/*/jni_md.h; do
            if test -f $f; then
              JAVA_OS=`dirname ${f}`
              JAVA_OS=`basename ${JAVA_OS}`
              echo " ${JAVA_OS}"
            fi
          done
        fi
      ])

    if test ! -n "${JAVA_OS}" ; then
      AC_MSG_RESULT(Cannot find jni_md.h in ${JAVA_HOME}/${OS})
      AC_MSG_ERROR(You should retry --with-java-os=SUBDIR)
    fi
    unset tempval
  ])

