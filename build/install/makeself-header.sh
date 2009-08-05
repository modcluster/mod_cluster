cat << EOF  > "$archname"
#!/bin/sh
# This script was generated using Makeself $MS_VERSION
# JBoss Httpd server

CRCsum="$CRCsum"
MD5="$MD5sum"
TMPROOT=\${TMPDIR:=/tmp}

label="$LABEL"
script="$SCRIPT"
scriptargs="$SCRIPTARGS"
targetdir="$archdirname"
filesizes="$filesizes"
keep=$KEEP

print_cmd_arg=""
if type printf > /dev/null; then
    print_cmd="printf"
elif test -x /usr/ucb/echo; then
    print_cmd="/usr/ucb/echo"
else
    print_cmd="echo"
fi

unset CDPATH

MS_Printf()
{
    \$print_cmd \$print_cmd_arg "\$1"
}

MS_Progress()
{
    while read a; do
	MS_Printf .
    done
}

MS_diskspace()
{
	(
	if test -d /usr/xpg4/bin; then
		PATH=/usr/xpg4/bin:\$PATH
	fi
	df -kP "\$1" | tail -1 | awk '{print \$4}'
	)
}

MS_dd()
{
    blocks=\`expr \$3 / 1024\`
    bytes=\`expr \$3 % 1024\`
    dd if="\$1" ibs=\$2 skip=1 obs=1024 conv=sync 2> /dev/null | \\
    { test \$blocks -gt 0 && dd ibs=1024 obs=1024 count=\$blocks ; \\
      test \$bytes  -gt 0 && dd ibs=1 obs=1024 count=\$bytes ; } 2> /dev/null
}

MS_Agree()
{
    while true
    do
        read reply leftover
        case \$reply in
            [yY] | [yY][eE][sS])
		echo "yes"
		return 0
                ;;
            [nN] | [nN][oO])
		echo "no"
		return 0
                ;;
        esac
    done
}

MS_Help()
{
    cat << EOH >&2
Makeself version $MS_VERSION
 1) Getting help or info about \$0 :
  \$0 --help   Print this message
  \$0 --info   Print embedded info : title, default target directory, embedded script ...
  \$0 --lsm    Print embedded lsm entry (or no LSM)
  \$0 --list   Print the list of files in the archive
  \$0 --check  Checks integrity of the archive
 
 2) Running \$0 :
  \$0 [options] [--] [additional arguments to embedded script]
  with following options (in that order)
  --confirm             Ask before running embedded script
  --noexec              Do not run embedded script
  --keep                Do not erase target directory after running
			the embedded script
  --nox11               Do not spawn an xterm
  --nochown             Do not give the extracted files to the current user
  --target NewDirectory Extract in NewDirectory
  --tar arg1 [arg2 ...] Access the contents of the archive through the tar command
  --                    Following arguments will be passed to the embedded script
EOH
}


MS_Eula()
{
    more << EOEULA
LICENSE AGREEMENT
JBOSS(r)

This License Agreement governs the use of the Software Packages and any
updates to the Software Packages, regardless of the delivery mechanism.
Each Software Package is a collective work under U.S. Copyright Law.
Subject to the following terms, Red Hat, Inc. ("Red Hat") grants to the
user ("Client") a license to the applicable collective work(s) pursuant
to the GNU Lesser General Public License v. 2.1 except for the following
Software Packages:

(a) JBoss Portal Forums and JBoss Transactions JTS, each of which is
licensed pursuant to the GNU General Public License v.2;

(b) JBoss Rules, which is licensed pursuant to the Apache License v.2.0;

(c) an optional download for JBoss Cache for the Berkeley DB for Java
database, which is licensed under the (open source) Sleepycat License
(if Client does not wish to use the open source version of this database,
it may purchase a license from Sleepycat Software);

and (d) the BPEL extension for JBoss jBPM, which is licensed under the
Common Public License v.1, and, pursuant to the OASIS BPEL4WS standard,
requires parties wishing to redistribute to enter various royalty-free
patent licenses.

Each of the foregoing licenses is available at
http://www.opensource.org/licenses/index.php.

1. The Software. "Software Packages" refer to the various software
modules that are created and made available for distribution by the
JBoss.org open source community at http://www.jboss.org. Each of the
Software Packages may be comprised of hundreds of software components.
The end user license agreement for each component is located in the
component's source code. With the exception of certain image files
identified in Section 2 below, the license terms for the components
permit Client to copy, modify, and redistribute the component, in both
source code and binary code forms. This agreement does not limit
Client's rights under, or grant Client rights that supersede, the
license terms of any particular component.

2. Intellectual Property Rights. The Software Packages are owned by Red
Hat and others and are protected under copyright and other laws. Title
to the Software Packages and any component, or to any copy,
modification, or merged portion shall remain with the aforementioned,
subject to the applicable license. The "JBoss" trademark, "Red Hat"
trademark, the individual Software Package trademarks, and the
"Shadowman" logo are registered trademarks of Red Hat and its affiliates
in the U.S. and other countries. This agreement permits Client to
distribute unmodified copies of the Software Packages using the Red Hat
trademarks that Red Hat has inserted in the Software Packages on the
condition that Client follows Red Hat's trademark guidelines for those
trademarks located at http://www.redhat.com/about/corporate/trademark/.
Client must abide by these trademark guidelines when distributing the
Software Packages, regardless of whether the Software Packages have been
modified. If Client modifies the Software Packages, then Client must
replace all Red Hat trademarks and logos identified at
http://www.jboss.com/company/logos, unless a separate agreement with Red
Hat is executed or other permission granted. Merely deleting the files
containing the Red Hat trademarks may corrupt the Software Packages.

3. Limited Warranty. Except as specifically stated in this Paragraph 3
or a license for a particular component, to the maximum extent permitted
under applicable law, the Software Packages and the components are
provided and licensed "as is" without warranty of any kind, expressed or
implied, including the implied warranties of merchantability,
non-infringement or fitness for a particular purpose. Red Hat warrants
that the media on which Software Packages may be furnished will be free
from defects in materials and manufacture under normal use for a period
of 30 days from the date of delivery to Client. Red Hat does not warrant
that the functions contained in the Software Packages will meet Client's
requirements or that the operation of the Software Packages will be
entirely error free or appear precisely as described in the accompanying
documentation. This warranty extends only to the party that purchases
the Services pertaining to the Software Packages from Red Hat or a Red
Hat authorized distributor.

4. Limitation of Remedies and Liability. To the maximum extent permitted
by applicable law, the remedies described below are accepted by Client
as its only remedies. Red Hat's entire liability, and Client's exclusive
remedies, shall be: If the Software media is defective, Client may
return it within 30 days of delivery along with a copy of Client's
payment receipt and Red Hat, at its option, will replace it or refund
the money paid by Client for the Software. To the maximum extent
permitted by applicable law, Red Hat or any Red Hat authorized dealer
will not be liable to Client for any incidental or consequential
damages, including lost profits or lost savings arising out of the use
or inability to use the Software, even if Red Hat or such dealer has
been advised of the possibility of such damages. In no event shall Red
Hat's liability under this agreement exceed the amount that Client paid
to Red Hat under this Agreement during the twelve months preceding the
action.

5. Export Control. As required by U.S. law, Client represents and
warrants that it:
(a) understands that the Software Packages are subject to export
controls under the U.S. Commerce Department's Export Administration
Regulations ("EAR");

(b) is not located in a prohibited destination country under the EAR or
U.S. sanctions regulations (currently Cuba, Iran, Iraq, Libya, North
Korea, Sudan and Syria);

(c) will not export, re-export, or transfer the Software Packages to any
prohibited destination, entity, or individual without the necessary
export license(s) or authorizations(s) from the U.S. Government;

(d) will not use or transfer the Software Packages for use in any
sensitive nuclear, chemical or biological weapons, or missile technology
end-uses unless authorized by the U.S. Government by regulation or
specific license;

(e) understands and agrees that if it is in the United States and
exports or transfers the Software Packages to eligible end users, it
will, as required by EAR Section 740.17(e), submit semi-annual reports
to the Commerce Department's Bureau of Industry & Security (BIS), which
include the name and address (including country) of each transferee;

and (f) understands that countries other than the United States may
restrict the import, use, or export of encryption products and that it
shall be solely responsible for compliance with any such import, use, or
export restrictions.

6. Third Party Programs. Red Hat may distribute third party software
programs with the Software Packages that are not part of the Software
Packages and which Client must install separately. These third party
programs are subject to their own license terms. The license terms
either accompany the programs or can be viewed at
http://www.redhat.com/licenses/. If Client does not agree to abide by
the applicable license terms for such programs, then Client may not
install them. If Client wishes to install the programs on more than one
system or transfer the programs to another party, then Client must
contact the licensor of the programs.

7. General. If any provision of this agreement is held to be
unenforceable, that shall not affect the enforceability of the remaining
provisions. This License Agreement shall be governed by the laws of the
State of North Carolina and of the United States, without regard to any
conflict of laws provisions, except that the United Nations Convention
on the International Sale of Goods shall not apply.

Copyright 2006 Red Hat, Inc. All rights reserved.
"JBoss" and the JBoss logo are registered trademarks of Red Hat, Inc.
All other trademarks are the property of their respective owners.

18 October 2006
EOEULA
    MS_Printf "\nDo you agree to the above license terms? [yes or no]\n"
    if test "\`MS_Agree\`" = "no"; then
	    MS_Printf "If you don't agree to the license you can't install this software\n"
	    exit 1
    fi
}

MS_Check()
{
    OLD_PATH="\$PATH"
    PATH=\${GUESS_MD5_PATH:-"\$OLD_PATH:/bin:/usr/bin:/sbin:/usr/local/ssl/bin:/usr/local/bin:/opt/openssl/bin"}
	MD5_ARG=""
    MD5_PATH=\`exec <&- 2>&-; which md5sum || type md5sum\`
    test -x "\$MD5_PATH" || MD5_PATH=\`exec <&- 2>&-; which md5 || type md5\`
	test -x "\$MD5_PATH" || MD5_PATH=\`exec <&- 2>&-; which digest || type digest\`
    PATH="\$OLD_PATH"

    MS_Printf "Verifying archive integrity..."
    offset=\`head -n $SKIP "\$1" | wc -c | tr -d " "\`
    verb=\$2
    i=1
    for s in \$filesizes
    do
		crc=\`echo \$CRCsum | cut -d" " -f\$i\`
		if test -x "\$MD5_PATH"; then
			if test \`basename \$MD5_PATH\` = digest; then
				MD5_ARG="-a md5"
			fi
			md5=\`echo \$MD5 | cut -d" " -f\$i\`
			if test \$md5 = "00000000000000000000000000000000"; then
				test x\$verb = xy && echo " \$1 does not contain an embedded MD5 checksum." >&2
			else
				md5sum=\`MS_dd "\$1" \$offset \$s | eval "\$MD5_PATH \$MD5_ARG" | cut -b-32\`;
				if test "\$md5sum" != "\$md5"; then
					echo "Error in MD5 checksums: \$md5sum is different from \$md5" >&2
					exit 2
				else
					test x\$verb = xy && MS_Printf " MD5 checksums are OK." >&2
				fi
				crc="0000000000"; verb=n
			fi
		fi
		if test \$crc = "0000000000"; then
			test x\$verb = xy && echo " \$1 does not contain a CRC checksum." >&2
		else
			sum1=\`MS_dd "\$1" \$offset \$s | CMD_ENV=xpg4 cksum | awk '{print \$1}'\`
			if test "\$sum1" = "\$crc"; then
				test x\$verb = xy && MS_Printf " CRC checksums are OK." >&2
			else
				echo "Error in checksums: \$sum1 is different from \$crc"
				exit 2;
			fi
		fi
		i=\`expr \$i + 1\`
		offset=\`expr \$offset + \$s\`
    done
    echo " All good."
}

UnTAR()
{
    tar \$1vf - 2>&1 || { echo Extraction failed. > /dev/tty; kill -15 \$$; }
}

finish=true
xterm_loop=
nox11=$NOX11
copy=$COPY
ownership=y
verbose=n

initargs="\$@"

while true
do
    case "\$1" in
    -h | --help)
	MS_Help
	exit 0
	;;
    --info)
	echo Identification: "\$label"
	echo Target directory: "\$targetdir"
	echo Uncompressed size: $USIZE KB
	echo Compression: $COMPRESS
	echo Date of packaging: $DATE
	echo Built with Makeself version $MS_VERSION on $OSTYPE
	echo Build command was: "$MS_COMMAND"
	if test x\$script != x; then
	    echo Script run after extraction:
	    echo "    " \$script \$scriptargs
	fi
	if test x"$copy" = xcopy; then
		echo "Archive will copy itself to a temporary location"
	fi
	if test x"$KEEP" = xy; then
	    echo "directory \$targetdir is permanent"
	else
	    echo "\$targetdir will be removed after extraction"
	fi
	exit 0
	;;
    --dumpconf)
	echo LABEL=\"\$label\"
	echo SCRIPT=\"\$script\"
	echo SCRIPTARGS=\"\$scriptargs\"
	echo archdirname=\"$archdirname\"
	echo KEEP=$KEEP
	echo COMPRESS=$COMPRESS
	echo filesizes=\"\$filesizes\"
	echo CRCsum=\"\$CRCsum\"
	echo MD5sum=\"\$MD5\"
	echo OLDUSIZE=$USIZE
	echo OLDSKIP=`expr $SKIP + 1`
	exit 0
	;;
    --lsm)
cat << EOLSM
EOF
eval "$LSM_CMD"
cat << EOF  >> "$archname"
EOLSM
	exit 0
	;;
    --list)
	echo Target directory: \$targetdir
	offset=\`head -n $SKIP "\$0" | wc -c | tr -d " "\`
	for s in \$filesizes
	do
	    MS_dd "\$0" \$offset \$s | eval "$GUNZIP_CMD" | UnTAR t
	    offset=\`expr \$offset + \$s\`
	done
	exit 0
	;;
	--tar)
	offset=\`head -n $SKIP "\$0" | wc -c | tr -d " "\`
	arg1="\$2"
	shift 2
	for s in \$filesizes
	do
	    MS_dd "\$0" \$offset \$s | eval "$GUNZIP_CMD" | tar "\$arg1" - \$*
	    offset=\`expr \$offset + \$s\`
	done
	exit 0
	;;
    --check)
	MS_Check "\$0" y
	exit 0
	;;
    --confirm)
	verbose=y
	shift
	;;
	--noexec)
	script=""
	shift
	;;
    --keep)
	keep=y
	shift
	;;
    --target)
	keep=y
	targetdir=\${2:-.}
	shift 2
	;;
    --nox11)
	nox11=y
	shift
	;;
    --nochown)
	ownership=n
	shift
	;;
    --xwin)
	finish="echo Press Return to close this window...; read junk"
	xterm_loop=1
	shift
	;;
    --phase2)
	copy=phase2
	shift
	;;
    --)
	shift
	break ;;
    -*)
	echo Unrecognized flag : "\$1" >&2
	MS_Help
	exit 1
	;;
    *)
	break ;;
    esac
done

MS_Eula

case "\$copy" in
copy)
    tmpdir=\$TMPROOT/makeself.\$RANDOM.\`date +"%y%m%d%H%M%S"\`.\$\$
    mkdir "\$tmpdir" || {
	echo "Could not create temporary directory \$tmpdir" >&2
	exit 1
    }
    SCRIPT_COPY="\$tmpdir/makeself"
    echo "Copying to a temporary location..." >&2
    cp "\$0" "\$SCRIPT_COPY"
    chmod +x "\$SCRIPT_COPY"
    cd "\$TMPROOT"
    exec "\$SCRIPT_COPY" --phase2 -- \$initargs
    ;;
phase2)
    finish="\$finish ; rm -rf \`dirname \$0\`"
    ;;
esac

if test "\$nox11" = "n"; then
    if tty -s; then                 # Do we have a terminal?
	:
    else
        if test x"\$DISPLAY" != x -a x"\$xterm_loop" = x; then  # No, but do we have X?
            if xset q > /dev/null 2>&1; then # Check for valid DISPLAY variable
                GUESS_XTERMS="xterm rxvt dtterm eterm Eterm kvt konsole aterm"
                for a in \$GUESS_XTERMS; do
                    if type \$a >/dev/null 2>&1; then
                        XTERM=\$a
                        break
                    fi
                done
                chmod a+x \$0 || echo Please add execution rights on \$0
                if test \`echo "\$0" | cut -c1\` = "/"; then # Spawn a terminal!
                    exec \$XTERM -title "\$label" -e "\$0" --xwin "\$initargs"
                else
                    exec \$XTERM -title "\$label" -e "./\$0" --xwin "\$initargs"
                fi
            fi
        fi
    fi
fi

if test "\$targetdir" = "."; then
    tmpdir="."
else
    if test "\$keep" = y; then
	echo "Creating directory \$targetdir" >&2
	tmpdir="\$targetdir"
	dashp="-p"
    else
	tmpdir="\$TMPROOT/selfgz\$\$\$RANDOM"
	dashp=""
    fi
    mkdir \$dashp \$tmpdir || {
	echo 'Cannot create target directory' \$tmpdir >&2
	echo 'You should try option --target OtherDirectory' >&2
	eval \$finish
	exit 1
    }
fi

location="\`pwd\`"
if test x\$SETUP_NOCHECK != x1; then
    MS_Check "\$0"
fi
offset=\`head -n $SKIP "\$0" | wc -c | tr -d " "\`

if test x"\$verbose" = xy; then
	MS_Printf "About to extract $USIZE KB in \$tmpdir ... Proceed ? [Y/n] "
	read yn
	if test x"\$yn" = xn; then
		eval \$finish; exit 1
	fi
fi

MS_Printf "Uncompressing \$label"
res=3
if test "\$keep" = n; then
    trap 'echo Signal caught, cleaning up >&2; cd \$TMPROOT; /bin/rm -rf \$tmpdir; eval \$finish; exit 15' 1 2 3 15
fi

leftspace=\`MS_diskspace \$tmpdir\`
if test \$leftspace -lt $USIZE; then
    echo
    echo "Not enough space left in "\`dirname \$tmpdir\`" (\$leftspace KB) to decompress \$0 ($USIZE KB)" >&2
    if test "\$keep" = n; then
        echo "Consider setting TMPDIR to a directory with more free space."
   fi
    eval \$finish; exit 1
fi

for s in \$filesizes
do
    if MS_dd "\$0" \$offset \$s | eval "$GUNZIP_CMD" | ( cd "\$tmpdir"; UnTAR x ) | MS_Progress; then
		if test x"\$ownership" = xy; then
			(PATH=/usr/xpg4/bin:\$PATH; cd "\$tmpdir"; chown -R \`id -u\` .;  chgrp -R \`id -g\` .)
		fi
    else
		echo
		echo "Unable to decompress \$0" >&2
		eval \$finish; exit 1
    fi
    offset=\`expr \$offset + \$s\`
done
echo

cd "\$tmpdir"
res=0
if test x"\$script" != x; then
    if test x"\$verbose" = xy; then
		MS_Printf "OK to execute: \$script \$scriptargs \$* ? [Y/n] "
		read yn
		if test x"\$yn" = x -o x"\$yn" = xy -o x"\$yn" = xY; then
			eval \$script \$scriptargs \$*; res=\$?;
		fi
    else
		eval \$script \$scriptargs \$*; res=\$?
    fi
    if test \$res -ne 0; then
		test x"\$verbose" = xy && echo "The program '\$script' returned an error code (\$res)" >&2
    fi
fi
if test "\$keep" = n; then
    cd \$TMPROOT
    /bin/rm -rf \$tmpdir
fi
eval \$finish; exit \$res
EOF
