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

import java.io.OutputStream;
import java.io.IOException;
import java.io.EOFException;

/** File
 *
 * @author Mladen Turk
 *
 */

public class FileOutputStream extends OutputStream
{

    private FileOutputStream()
    {
        // Disable creation
    }

    private File file = null;

    protected FileOutputStream(File f)
    {
        file = f;
    }


    /**
     * Writes the specified byte to this output stream. The general
     * contract for write is that one byte is written to the output
     * stream. The byte to be written is the eight low-order bits
     * of the argument b. The 24 high-order bits of b are ignored.
     * @param b The byte
     */
    public void write(int b)
        throws IOException
    {
        int rv = file.write(b);
        if (rv == Error.APR_EOF)
            throw new EOFException();
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this output stream.
     * The general contract for <code>write(b, off, len)</code> is
     * that some of the bytes in the array b are written to the
     * output stream in order; element b[off] is the first byte
     * written and b[off+len-1] is the last byte written by this operation.
     * @param b The data.
     * @param off  The start offset in the data.
     * @param len The number of bytes to write.
     */
    public void write(byte[] b, int off, int len)
        throws IOException
    {
        int rv = file.write(b, off, len);
        if (rv == Error.APR_EOF)
            throw new EOFException();
    }

    /**
     * Writes <code>byte.length</code> bytes from the specified byte array
     * to this output stream.
     * The general contract for <code>write(b)</code> is
     * that some of the bytes in the array b are written to the
     * output stream in order; element b[0] is the first byte
     * written and b[b.length-1] is the last byte written by this operation.
     * @param b The data.
     */
    public void write(byte[] b)
        throws IOException
    {
        int rv = file.write(b, 0, b.length);
        if (rv == Error.APR_EOF)
            throw new EOFException();
    }

    public void reset()
        throws IOException
    {
        // TODO: Throw an exception depending on
        // the seek result.
        file.seek(FileSeek.SET, 0);
    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written out. The general contract of flush is that calling
     * it is an indication that, if any bytes previously written have been
     * buffered by the implementation of the output stream, such bytes
     * should immediately be written to their intended destination.
     * <BR/>
     * If the intended destination of this stream is an abstraction
     * provided by the underlying operating system, for example a file,
     * then flushing the stream guarantees only that bytes previously
     * written to the stream are passed to the operating system for
     * writing; it does not guarantee that they are actually written
     * to a physical device such as a disk drive.
     */
    public void flush()
        throws IOException
    {
        file.flush();
    }

    /**
     * Closes this file output stream and releases any system resources
     * associated with this stream. This file output stream may no longer
     * be used for writing data.
     */
    public void close()
        throws IOException
    {
        // Nothing
    }
}
