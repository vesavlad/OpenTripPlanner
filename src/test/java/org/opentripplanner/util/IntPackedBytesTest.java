/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.util;

import org.junit.Test;

import static java.lang.Byte.MAX_VALUE;
import static org.junit.Assert.assertEquals;
import static org.opentripplanner.util.IntPackedBytes.getByte0;
import static org.opentripplanner.util.IntPackedBytes.getByte1;
import static org.opentripplanner.util.IntPackedBytes.getByte2;
import static org.opentripplanner.util.IntPackedBytes.getByte3;
import static org.opentripplanner.util.IntPackedBytes.setByte0;
import static org.opentripplanner.util.IntPackedBytes.setByte1;
import static org.opentripplanner.util.IntPackedBytes.setByte2;
import static org.opentripplanner.util.IntPackedBytes.setByte3;

/**
 * Non-exhaustive test of the {@link IntPackedBytes} class. An exhaustive version is in Git history.
 */
public class IntPackedBytesTest {
    @Test
    public void testDownward() {
        byte a = MAX_VALUE, b, c, d = MAX_VALUE;

        do {
            c = -121;
            do {
                b = -121;
                do {
                    do {
                        final byte w, x, y, z;
                        final int packed = setByte3(
                                           setByte2(
                                           setByte1(
                                           setByte0(
                                                    -1,
                                                    a),
                                                    b),
                                                    c),
                                                    d);

                        w = getByte3(packed);
                        x = getByte2(packed);
                        y = getByte1(packed);
                        z = getByte0(packed);

                        assertEquals(a, z);
                        assertEquals(b, y);
                        assertEquals(c, x);
                        assertEquals(d, w);
                    } while (--a != MAX_VALUE);
                } while (--b != 119);
            } while (--c != 119);
        } while (--d != MAX_VALUE);
    }

    @Test
    public void testUpward() {
        byte a = 0, b, c, d = 0;

        do {
            b = 0;
            do {
                c = 0;
                do {
                    do {
                        final byte w, x, y, z;
                        final int packed = setByte0(
                                           setByte1(
                                           setByte2(
                                           setByte3(
                                                    0,
                                                    d),
                                                    c),
                                                    b),
                                                    a);

                        z = getByte0(packed);
                        y = getByte1(packed);
                        x = getByte2(packed);
                        w = getByte3(packed);

                        assertEquals(d, w);
                        assertEquals(c, x);
                        assertEquals(b, y);
                        assertEquals(a, z);
                    } while (++d != 0);
                } while (++c != 16);
            } while (++b != 16);
        } while (++a != 0);
    }
}
