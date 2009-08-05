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

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;

/** File
 *
 * @author Mladen Turk
 *
 */

public class FileInputStream extends InputStream
{

    private FileInputStream()
    {
        // Disable creation
    }

    private File file = null;

    protected FileInputStream(File f)
    {
        file = f;
    }

    /**
     * Read a character from the specified file.
     * @return The readed character or -1 in case of
     *         {@link Error#APR_EOF Error.APR_EOF}.
     */
    public int read()
        throws IOException
    {
        return file.read();
    }

    /**
     * Reads up to <code>len</code> bytes of data from the input stream into
     * an array of bytes. An attempt is made to read as many as len bytes,
     * but a smaller number may be read. The number of bytes actually read
     * is returned as an integer.
     * @param b The buffer into which the data is read.
     * @param off The start offset in array b at which the data is written.
     * @param len Tthe maximum number of bytes to read.
     * @return The total number of bytes read into the buffer, or -1 in
     *         case of {@link Error#APR_EOF Error.APR_EOF}.
     */
    public int read(byte[] b, int off, int len)
        throws IOException
    {
        return file.read(b, off, len);
    }

    /**
     * Reads up to <code>byte.length</code> bytes of data from the input
     * stream into an array of bytes. An attempt is made to read as many
     * as <code>byte.length</code> bytes,
     * but a smaller number may be read. The number of bytes actually read
     * is returned as an integer.
     * <BR/>
     * This method simply performs the call
     * <code>read(b, 0, b.length)</code> and returns the result.
     * @param b The buffer into which the data is read.
     * @return The total number of bytes read into the buffer, or -1 in
     *         case of {@link Error#APR_EOF Error.APR_EOF}.
     */
    public int read(byte[] b)
        throws IOException
    {
        return file.read(b, 0, b.length);
    }

    /**
     * Repositions the input stream to the beginning of the File
     */
    public void reset()
        throws IOException
    {
        file.seek(FileSeek.SET, 0);
    }

    /**
     * Skips over and discards n bytes of data from this input stream.
     * The skip method may, for a variety of reasons, end up skipping
     * over some smaller number of bytes, possibly 0. This may result
     * from any of a number of conditions; reaching end of file before
     * n bytes have been skipped is only one possibility. The actual
     * number of bytes skipped is returned. If n is negative, no bytes
     * are skipped
     * @param n The number of bytes to be skipped.
     * @return The actual number of bytes skipped.
     */
    public long skip(long n)
          throws IOException
    {
        try {
            long c = file.seek(FileSeek.CUR, 0);
            long p = file.seek(FileSeek.CUR, n);
            return (p - c);
        }
        catch (Exception e) {
            // Toss any exception
        }
        return -1;
    }

    /**
     * Closes this file input stream and releases any system resources
     * associated with this stream. This file input stream may no longer
     * be used for reding data.
     */
    public void close()
        throws IOException
    {
        // Nothing
    }

}
