/*
 *  SIGHT - System information gathering hybrid tool
 *
 *  Copyright(c) 2007 Red Hat Middleware, LLC,
 *  and individual contributors as indicated by the @authors tag.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library in the file COPYING.LIB;
 *  if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 */

package org.jboss.sight;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * LibraryLoader
 *
 * @author Mladen Turk
 *
 */

public final class LibraryLoader {


    public static String getDefaultPlatformName()
    {
        String name = System.getProperty("os.name");
        String platform = "unknown";

        if (name.startsWith("Windows"))
            platform = "windows";
        else if (name.startsWith("Mac OS"))
            platform = "macosx";
        else if (name.endsWith("BSD"))
            platform = "bsd";
        else if (name.equals("Linux"))
            platform = "linux2";
        else if (name.equals("Solaris"))
            platform = "solaris";
        else if (name.equals("SunOS"))
            platform = "solaris";
        else if (name.equals("HP-UX"))
            platform = "hpux";
        else if (name.equals("AIX"))
            platform = "aix";

        return platform;
    }

    public static String getDefaultPlatformCpu()
    {
        String cpu;
        String arch = System.getProperty("os.arch");

        if (arch.endsWith("86"))
            cpu = "i686";
        else if (arch.startsWith("PA_RISC"))
            cpu = "parisc2";
        else if (arch.startsWith("IA64"))
            cpu = "ia64";
        else if (arch.startsWith("sparc"))
            cpu = "sparcv9";
        else if (arch.equals("x86_64"))
            cpu = "amd64";
        else
            cpu = arch;
        return cpu;
    }

    public static String getDefaultLibraryPath()
    {
        String name = getDefaultPlatformName();
        String arch = getDefaultPlatformCpu();

        return "SIGHT-BIN" + File.separator + name + File.separator + arch;
    }

    private LibraryLoader()
    {
        // Disallow creation
    }

    protected static void load(String rootPath)
        throws SecurityException, IOException, UnsatisfiedLinkError
    {
        int count = 0;
        String name = getDefaultPlatformName();
        String path = getDefaultLibraryPath();
        Properties props = new Properties();

        File root = new File(rootPath);
        String basePath = root.getCanonicalPath().toString();
        if (!basePath.endsWith(File.separator)) {
            basePath += File.separator;
        }
        try {
            InputStream is = LibraryLoader.class.getResourceAsStream
                ("/org/jboss/sight/Library.properties");
            props.load(is);
            is.close();
            count = Integer.parseInt(props.getProperty(name + ".count"));
        }
        catch (Throwable t) {
            throw new UnsatisfiedLinkError("Can't use Library.properties");
        }
        for (int i = 0; i < count; i++) {
            String dlibName = props.getProperty(name + "." + i);
            String fullPath = basePath + path +
                              File.separator + dlibName;
            Runtime.getRuntime().load(fullPath);
        }
    }

}
