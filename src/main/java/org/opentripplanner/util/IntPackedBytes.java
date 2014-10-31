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

public class IntPackedBytes {
    final static private int MASK = 0XFF;
    final static private int BYTE_0 = MASK << (8 * 0);
    final static private int BYTE_1 = MASK << (8 * 1);
    final static private int BYTE_2 = MASK << (8 * 2);
    final static private int BYTE_3 = MASK << (8 * 3);

    public static byte getByte0(int input) {
        return (byte) (input >>> (8 * 0));
    }

    public static byte getByte1(int input) {
        return (byte) (input >>> (8 * 1));
    }

    public static byte getByte2(int input) {
        return (byte) (input >>> (8 * 2));
    }

    public static byte getByte3(int input) {
        return (byte) (input >>> (8 * 3));
    }

    public static int setByte0(int input, byte byte0) {
        return (input & ~BYTE_0) | ((byte0 << (8 * 0)) & BYTE_0);
    }

    public static int setByte1(int input, byte byte1) {
        return (input & ~BYTE_1) | ((byte1 << (8 * 1)) & BYTE_1);
    }

    public static int setByte2(int input, byte byte2) {
        return (input & ~BYTE_2) | ((byte2 << (8 * 2)) & BYTE_2);
    }

    public static int setByte3(int input, byte byte3) {
        return (input & ~BYTE_3) | ((byte3 << (8 * 3)) & BYTE_3);
    }
}
