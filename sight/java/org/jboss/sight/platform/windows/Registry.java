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

package org.jboss.sight.platform.windows;
import org.jboss.sight.OS;
import org.jboss.sight.OperatingSystemException;
import org.jboss.sight.UnsupportedOperatingSystemException;
import java.util.EnumSet;

/**
 * Windows Registy support
 *
 * @author Mladen Turk
 */

public class Registry {

    /* Native object pointer */
    private long NHKEY;


    private static native long create0(int root, String name, int sam)
        throws OperatingSystemException, OutOfMemoryError;
    private static native long open0(int root, String name, int sam)
        throws OperatingSystemException, OutOfMemoryError;
    private static native int close0(long key);
    private static native int getType(long key, String name)
        throws OperatingSystemException;
    private static native int getValueI(long key, String name)
        throws OperatingSystemException;
    private static native long getValueJ(long key, String name)
        throws OperatingSystemException;
    private static native int getSize(long key, String name);
    private static native String getValueS(long key, String name)
        throws OperatingSystemException, OutOfMemoryError;
    private static native String[] getValueA(long key, String name)
        throws OperatingSystemException, OutOfMemoryError;
    private static native byte[] getValueB(long key, String name)
        throws OperatingSystemException, OutOfMemoryError;
    private static native int setValueI(long key, String name, int val);
    private static native int setValueJ(long key, String name, long val);
    private static native int setValueS(long key, String name, String val);
    private static native int setValueE(long key, String name, String val);
    private static native int setValueA(long key, String name, String[] val);
    private static native int setValueB(long key, String name, byte[] val, int off, int len);
    private static native String[] enumKeys(long key)
        throws OperatingSystemException, OutOfMemoryError;
    private static native String[] enumValues(long key)
        throws OperatingSystemException, OutOfMemoryError;
    private static native int deleteValue(long key, String name);
    private static native int deleteKey0(int root, String name,
                                         boolean onlyIfEmpty);

    private int     sam  = 0;
    private int     root = 0;
    private String  name;

    /**
     * Object finalize callback.
     */
    protected final void finalize()
    {
        close0(NHKEY);
        NHKEY = 0;
    }

    /**
     * Create empty Registry class.
     * Created class can then be used for calling
     * create or open methods.
     */
    public Registry()
        throws UnsupportedOperatingSystemException
    {
        if (!OS.IS_WINDOWS)
            throw new UnsupportedOperatingSystemException();
        NHKEY = 0;
    }

    /**
     * Create or opens the Registry Key.
     * @param name Registry Subkey to create or open
     * @param root Root key, one of HKEY_*
     * @param sam Access mask that specifies the access rights for the key.
     */
    public Registry(HKEY root, String name, EnumSet<KeyAccessRights> sam)
        throws UnsupportedOperatingSystemException,
               OperatingSystemException, OutOfMemoryError
    {
        if (!OS.IS_WINDOWS)
            throw new UnsupportedOperatingSystemException();
        this.sam  = KeyAccessRights.bitmapOf(sam);
        this.root = root.valueOf();
        this.name = name;
        NHKEY = create0(this.root, this.name, this.sam);
    }

    /**
     * Create or opens the Registry Sub Key
     * @param root Parent Registry key
     * @param subKey The sub key to open or create.
     */
    public Registry(Registry root, String subKey)
        throws UnsupportedOperatingSystemException,
               OperatingSystemException, OutOfMemoryError
    {
        if (!OS.IS_WINDOWS)
            throw new UnsupportedOperatingSystemException();
        this.root = root.root;
        this.name = root.name + "\\" + subKey;
        this.sam  = root.sam;
        NHKEY = create0(this.root, this.name, this.sam);
    }

    /**
     * Create or opens the Registry Key.
     * @param name Registry Subkey to create or open
     * @param root Root key, one of HKEY_*
     * @param sam Access mask that specifies the access rights for the key.
     */
    public void create(HKEY root, String name, EnumSet<KeyAccessRights> sam)
        throws OperatingSystemException, OutOfMemoryError
    {
        if (NHKEY != 0)
            close0(NHKEY);
        this.sam  = KeyAccessRights.bitmapOf(sam);
        this.root = root.valueOf();
        this.name = name;
        NHKEY = create0(this.root, this.name, this.sam);
    }

    /**
     * Opens the specified Registry Key.
     * @param name Registry Subkey to open
     * @param root Root key, one of HKEY_*
     * @param sam Access mask that specifies the access rights for the key.
     */
    public void open(HKEY root, String name,  EnumSet<KeyAccessRights> sam)
        throws OperatingSystemException, OutOfMemoryError
    {
        if (NHKEY != 0)
            close0(NHKEY);
        this.sam  = KeyAccessRights.bitmapOf(sam);
        this.root = root.valueOf();
        this.name = name;
        NHKEY = open0(this.root, name, this.sam);
    }

    /**
     * Close the specified Registry key.
     */
    public void close()
    {
        close0(NHKEY);
        NHKEY  = 0;
    }

    /**
     * Get the Registry key type.
     * @param name The name of the value to query
     * @return Value type or negative error value
     */
    public RegistryValueType getValueType(String name)
        throws OperatingSystemException
    {
        return RegistryValueType.valueOf(getType(NHKEY, name));
    }

    /**
     * Get the Registry value for REG_DWORD
     * @param name The name of the value to query
     * @return Registry key value
     */
    public int getIntegerValue(String name)
        throws OperatingSystemException
    {
        return getValueI(NHKEY, name);
    }

    /**
     * Get the Registry value for REG_QWORD or REG_DWORD
     * @param name The name of the value to query
     * @return Registry key value
     */
    public long getLongValue(String name)
        throws OperatingSystemException
    {
        return getValueI(NHKEY, name);
    }

    /**
     * Get the Registry value length.
     * @param name The name of the value to query
     * @return Value size or negative error value
     */
    public int getValueSize(String name)
    {
        return getSize(NHKEY, name);
    }

    /**
     * Get the Registry value for REG_SZ or REG_EXPAND_SZ
     * @param name The name of the value to query
     * @return Registry key value
     */
    public String getStringValue(String name)
        throws OperatingSystemException, OutOfMemoryError
    {
        return getValueS(NHKEY, name);
    }

    /**
     * Get the Registry value for REG_MULTI_SZ
     * @param name The name of the value to query
     * @return Registry key values
     */
    public String[] getStringArrayValue(String name)
        throws OperatingSystemException, OutOfMemoryError
    {
        return getValueA(NHKEY, name);
    }

    /**
     * Get the Registry value for REG_BINARY
     * @param name The name of the value to query
     * @return Registry key value
     */
    public byte[] getByteArrayValue(String name)
        throws OperatingSystemException, OutOfMemoryError
    {
        return getValueB(NHKEY, name);
    }

    /**
     * Set the Registry value for REG_DWORD
     * @param name The name of the value to set
     * @param val The the value to set
     * @return If the function succeeds, the return value is 0
     */
    public int setValue(String name, int val)
    {
        return setValueI(NHKEY, name, val);
    }

    /**
     * Set the Registry value for REG_QWORD
     * @param name The name of the value to set
     * @param val The the value to set
     * @return If the function succeeds, the return value is 0
     */
    public int setValue(String name, long val)
    {
        return setValueJ(NHKEY, name, val);
    }

    /**
     * Set the Registry value for REG_SZ
     * @param name The name of the value to set
     * @param val The the value to set
     * @return If the function succeeds, the return value is 0
     */
    public int setValue(String name, String val)
    {
        return setValueS(NHKEY, name, val);
    }

    /**
     * Set the Registry value for REG_EXPAND_SZ
     * @param name The name of the value to set
     * @param val The the value to set
     * @return If the function succeeds, the return value is 0
     */
    public int setValueEpanded(String name, String val)
    {
        return setValueE(NHKEY, name, val);
    }

    /**
     * Set the Registry value for REG_MULTI_SZ
     * @param name The name of the value to set
     * @param val The the value to set
     * @return If the function succeeds, the return value is 0
     */
    public int setValue(String name, String[] val)
    {
        return setValueA(NHKEY, name, val);
    }

    /**
     * Set the Registry value for REG_BINARY
     * @param name The name of the value to set
     * @param val The the value to set
     * @return If the function succeeds, the return value is 0
     */
    public int setValue(String name, byte[] val)
    {
        return setValueB(NHKEY, name, val, 0, val.length);
    }

    /**
     * Set the Registry value for REG_BINARY
     * @param name The name of the value to set
     * @param val The the value to set
     * @param off The start offset in the data.
     * @param len The number of bytes to write.
     * @return If the function succeeds, the return value is 0
     */
    public int setValue(String name, byte[] val, int off, int len)
    {
        return setValueB(NHKEY, name, val, off, len);
    }

    /**
     * Enumerate the Registry subkeys
     * @return Array of all subkey names
     */
    public String[] enumKeys()
        throws OperatingSystemException, OutOfMemoryError
    {
        return enumKeys(NHKEY);
    }

    /**
     * Enumerate the Registry values
     * @return Array of all value names
     */
    public String[] enumValues()
        throws OperatingSystemException, OutOfMemoryError
    {
        return enumValues(NHKEY);
    }

    /**
     * Delete the Registry value
     * @param name The name of the value to delete
     * @return If the function succeeds, the return value is 0
     */
    public int deleteValue(String name)
    {
        return deleteValue(NHKEY, name);
    }

    /**
     * Delete the Registry subkey
     * @param root Root key, one of HKEY_*
     * @param name Subkey to delete
     * @param onlyIfEmpty If true will not delete a key if
     *                    it contains any subkeys or values
     * @return If the function succeeds, the return value is 0
     */
    public static int deleteKey(HKEY root, String name,
                                boolean onlyIfEmpty)
        throws UnsupportedOperatingSystemException
    {
        if (!OS.IS_WINDOWS)
            throw new UnsupportedOperatingSystemException();
        return deleteKey0(root.valueOf(), name, onlyIfEmpty);
    }

}
