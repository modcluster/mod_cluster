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

/**
 *
 * <b>APR_STATUS_IS</b>
 * <br /><b>Warning :</b> For any particular error condition, more than one of
 *      these tests may match. This is because platform-specific error codes
 *      may not always match the semantics of the <code>POSIX</code> codes
 *      these tests (and the corresponding <code>APR</code> error codes) are
 *      named after. A notable example are the <code>APR_STATUS_IS_ENOENT</code>
 *      and <code>APR_STATUS_IS_ENOTDIR</code> tests on Win32 platforms.
 *      The programmer should always be aware of this and adjust the order of
 *      the tests accordingly.
 *
 * @author Mladen Turk
 *
 */
public final class Status {

    private static native boolean is(int err, int idx);

    public static final boolean APR_STATUS_IS_ENOSTAT(int s)    { return is(s, 1); }
    public static final boolean APR_STATUS_IS_ENOPOOL(int s)    { return is(s, 2); }
    /* empty slot: +3 */
    public static final boolean APR_STATUS_IS_EBADDATE(int s)   { return is(s, 4); }
    public static final boolean APR_STATUS_IS_EINVALSOCK(int s) { return is(s, 5); }
    public static final boolean APR_STATUS_IS_ENOPROC(int s)    { return is(s, 6); }
    public static final boolean APR_STATUS_IS_ENOTIME(int s)    { return is(s, 7); }
    public static final boolean APR_STATUS_IS_ENODIR(int s)     { return is(s, 8); }
    public static final boolean APR_STATUS_IS_ENOLOCK(int s)    { return is(s, 9); }
    public static final boolean APR_STATUS_IS_ENOPOLL(int s)    { return is(s, 10); }
    public static final boolean APR_STATUS_IS_ENOSOCKET(int s)  { return is(s, 11); }
    public static final boolean APR_STATUS_IS_ENOTHREAD(int s)  { return is(s, 12); }
    public static final boolean APR_STATUS_IS_ENOTHDKEY(int s)  { return is(s, 13); }
    public static final boolean APR_STATUS_IS_EGENERAL(int s)   { return is(s, 14); }
    public static final boolean APR_STATUS_IS_ENOSHMAVAIL(int s){ return is(s, 15); }
    public static final boolean APR_STATUS_IS_EBADIP(int s)     { return is(s, 16); }
    public static final boolean APR_STATUS_IS_EBADMASK(int s)   { return is(s, 17); }
    /* empty slot: +18 */
    public static final boolean APR_STATUS_IS_EDSOPEN(int s)    { return is(s, 19); }
    public static final boolean APR_STATUS_IS_EABSOLUTE(int s)  { return is(s, 20); }
    public static final boolean APR_STATUS_IS_ERELATIVE(int s)  { return is(s, 21); }
    public static final boolean APR_STATUS_IS_EINCOMPLETE(int s){ return is(s, 22); }
    public static final boolean APR_STATUS_IS_EABOVEROOT(int s) { return is(s, 23); }
    public static final boolean APR_STATUS_IS_EBADPATH(int s)   { return is(s, 24); }
    public static final boolean APR_STATUS_IS_EPATHWILD(int s)  { return is(s, 25); }
    public static final boolean APR_STATUS_IS_ESYMNOTFOUND(int s)      { return is(s, 26); }
    public static final boolean APR_STATUS_IS_EPROC_UNKNOWN(int s)     { return is(s, 27); }
    public static final boolean APR_STATUS_IS_ENOTENOUGHENTROPY(int s) { return is(s, 28); }

    /*
     * APR_Error
     */
    public static final boolean APR_STATUS_IS_INCHILD(int s)    { return is(s, 51); }
    public static final boolean APR_STATUS_IS_INPARENT(int s)   { return is(s, 52); }
    public static final boolean APR_STATUS_IS_DETACH(int s)     { return is(s, 53); }
    public static final boolean APR_STATUS_IS_NOTDETACH(int s)  { return is(s, 54); }
    public static final boolean APR_STATUS_IS_CHILD_DONE(int s) { return is(s, 55); }
    public static final boolean APR_STATUS_IS_CHILD_NOTDONE(int s)  { return is(s, 56); }
    public static final boolean APR_STATUS_IS_TIMEUP(int s)     { return is(s, 57); }
    public static final boolean APR_STATUS_IS_INCOMPLETE(int s) { return is(s, 58); }
    /* empty slot: +9 */
    /* empty slot: +10 */
    /* empty slot: +11 */
    public static final boolean APR_STATUS_IS_BADCH(int s)      { return is(s, 62); }
    public static final boolean APR_STATUS_IS_BADARG(int s)     { return is(s, 63); }
    public static final boolean APR_STATUS_IS_EOF(int s)        { return is(s, 64); }
    public static final boolean APR_STATUS_IS_NOTFOUND(int s)   { return is(s, 65); }
    /* empty slot: +16 */
    /* empty slot: +17 */
    /* empty slot: +18 */
    public static final boolean APR_STATUS_IS_ANONYMOUS(int s)  { return is(s, 69); }
    public static final boolean APR_STATUS_IS_FILEBASED(int s)  { return is(s, 70); }
    public static final boolean APR_STATUS_IS_KEYBASED(int s)   { return is(s, 71); }
    public static final boolean APR_STATUS_IS_EINIT(int s)      { return is(s, 72); }
    public static final boolean APR_STATUS_IS_ENOTIMPL(int s)   { return is(s, 73); }
    public static final boolean APR_STATUS_IS_EMISMATCH(int s)  { return is(s, 74); }
    public static final boolean APR_STATUS_IS_EBUSY(int s)      { return is(s, 75); }

    /* Socket errors */
    public static final boolean APR_STATUS_IS_EAGAIN(int s)     { return is(s, 90); }
    public static final boolean APR_STATUS_IS_ETIMEDOUT(int s)  { return is(s, 91); }
    public static final boolean APR_STATUS_IS_ECONNABORTED(int s) { return is(s, 92); }
    public static final boolean APR_STATUS_IS_ECONNRESET(int s)   { return is(s, 93); }
    public static final boolean APR_STATUS_IS_EINPROGRESS(int s)  { return is(s, 94); }
    public static final boolean APR_STATUS_IS_EINTR(int s)      { return is(s, 95); }
    public static final boolean APR_STATUS_IS_ENOTSOCK(int s)   { return is(s, 96); }
    public static final boolean APR_STATUS_IS_EINVAL(int s)     { return is(s, 97); }

}
