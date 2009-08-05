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

import java.util.Iterator;
import java.util.EnumSet;

/**
 * Volume information
 *
 * @author Mladen Turk
 *
 */

public class Volume extends NativeObject
{

    private static native long      enum0(long pool);
    private static native int       enum1(long handle);
    private static native int       enum2(Volume thiz, long handle);
    private static native int       enum3(Volume thiz, long handle);
    private static native int       enum4(long handle);

    private Volume()
    {
        super(0);
    }

    protected void onDestroy()
    {
        // Nothing
    }

    private void setFSType(int type)
    {
        Type = FileSystemType.valueOf(type);
    }


    private void setFlags(int flags)
    {
        Flags = FileSystemFlags.valueOf(flags);
    }

    private void setDType(int flags)
    {
        DriveType = DriveType.valueOf(flags);
    }


    /**
     * Returns the array of all Volumes
     */
    public static VolumeIterator getVolumes()
        throws OutOfMemoryError, OperatingSystemException
    {
        Volume[] array;
        long handle = enum0(0);
        try {
            int cnt = enum1(handle);
            array = new Volume[cnt];
            for (int i = 0; i < cnt; i++) {
                array[i] = new Volume();
                enum2(array[i], handle);
                int numberOfMounts = array[i].NumberOfMountPoints;
                for (int j = 0; j < numberOfMounts; j++) {
                    Volume mountPoint = new Volume();
                    int rv = enum3(mountPoint, handle);
                    if (rv != 0)
                        array[++i] = mountPoint;
                }
            }
            return new VolumeIterator(array);
        }
        finally {
            // Do not leak memory
            enum4(handle);
        }
    }

    /**
     * Name of the volume.
     */
    public String               Name;

    /**
     * Description for the volume.
     */
    public String               Description;

    /**
     * Volume Mount Point.
     * <BR/>
     * This is directory the volume is mouted on,
     * eg <code>C:\</code> on windows or
     * <code>/home</code> on unixes.
     */
    public String               MountPoint;

    /**
     * File system type.
     */
    public FileSystemType       Type;

    /**
     * Determines whether a disk drive is a removable, fixed, CD-ROM,
     * RAM disk, or network drive.
     */
    public DriveType            DriveType;

    /**
     * The number of sectors per cluster.
     */
    public int                  SectorsPerCluster;

    /**
     * The number of bytes per sector.
     */
    public int                  BytesPerSector;

    /**
     * Total number of free bytes on a disk that are available to the
     * user who is associated with the call.
     */
    public long                 FreeBytesAvailable;

    /**
     * Total number of free bytes on a disk.
     */
    public long                 TotalNumberOfBytes;

    /**
     * TotalNumberOfFreeBytes.
     */
    public long                 TotalNumberOfFreeBytes;

    /**
     * File system flags.
     */
    public EnumSet<FileSystemFlags> Flags;

    /**
     * Number of mounted volume points.
     */
    public int                  NumberOfMountPoints;
}
