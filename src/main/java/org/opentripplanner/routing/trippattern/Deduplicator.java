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

package org.opentripplanner.routing.trippattern;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;

/**
 * Does the same thing as String.intern, but for several different types.
 * Java's String.intern uses perm gen space and is broken anyway.
 */
public class Deduplicator implements Serializable {
    private static final Optional<String> ABSENT = Optional.absent();
    private static final Optional<Boolean> FALSE = Optional.of(false);
    private static final Optional<Boolean> TRUE = Optional.of(true);
    private static final long serialVersionUID = 20141020L;

    private final Map<IntArray, IntArray> canonicalIntArrays = Maps.newHashMap();
    private final Map<String, String> canonicalStrings = Maps.newHashMap();
    private final Map<String, Optional<String>> canonicalOptionalStrings = Maps.newHashMap();
    private final Map<BitSet, BitSet> canonicalBitSets = Maps.newHashMap();
    private final Map<StringArray, StringArray> canonicalStringArrays = Maps.newHashMap();
    private final Map<Integer, Optional<Integer>> canonicalOptionalIntegers = Maps.newHashMap();
    private final Map<Double, Optional<Double>> canonicalOptionalDoubles = Maps.newHashMap();

    /** Free up any memory used by the deduplicator. */
    public synchronized void reset() {
        canonicalIntArrays.clear();
        canonicalStrings.clear();
        canonicalOptionalStrings.clear();
        canonicalBitSets.clear();
        canonicalStringArrays.clear();
        canonicalOptionalIntegers.clear();
        canonicalOptionalDoubles.clear();
    }

    /** Used to deduplicate time and stop sequence arrays. The same times may occur in many trips. */
    public synchronized int[] deduplicateIntArray(int[] original) {
        if (original == null) return null;
        IntArray intArray = new IntArray(original);
        IntArray canonical = canonicalIntArrays.get(intArray);
        if (canonical == null) {
            canonical = intArray;
            canonicalIntArrays.put(canonical, canonical);
        }
        return canonical.array;
    }

    public synchronized String deduplicateString(String original) {
        if (original == null) return null;
        String canonical = canonicalStrings.get(original);
        if (canonical == null) {
            canonical = new String(original.toCharArray()); // Trim String if necessary (older JDKs)
            canonicalStrings.put(canonical, canonical);
        }
        return canonical;
    }

    public synchronized Optional<String> deduplicateOptionalString(String original) {
        if (original == null) return ABSENT;
        Optional<String> canonical = canonicalOptionalStrings.get(original);
        if (canonical == null) {
            canonical = Optional.of(deduplicateString(original));
            canonicalOptionalStrings.put(canonical.get(), canonical);
        }
        return canonical;
    }

    public synchronized BitSet deduplicateBitSet(BitSet original) {
        if (original == null) return null;
        BitSet canonical = canonicalBitSets.get(original);
        if (canonical == null) {
            canonical = original;
            canonicalBitSets.put(canonical, canonical);
        }
        return canonical;
    }

    public synchronized String[] deduplicateStringArray(String[] original) {
        if (original == null) return null;
        StringArray canonical = canonicalStringArrays.get(new StringArray(original, false));
        if (canonical == null) {
            canonical = new StringArray(original, true);
            canonicalStringArrays.put(canonical, canonical);
        }
        return canonical.array;
    }

    public synchronized Optional<Integer> deduplicateOptionalInteger(int original) {
        Optional<Integer> canonical = canonicalOptionalIntegers.get(original);
        if (canonical == null) {
            canonical = Optional.of(original);
            canonicalOptionalIntegers.put(canonical.get(), canonical);
        }
        return canonical;
    }

    public synchronized Optional<Double> deduplicateOptionalDouble(double original) {
        Optional<Double> canonical = canonicalOptionalDoubles.get(original);
        if (canonical == null) {
            canonical = Optional.of(original);
            canonicalOptionalDoubles.put(canonical.get(), canonical);
        }
        return canonical;
    }

    public synchronized Optional<Boolean> deduplicateOptionalBoolean(boolean original) {
        return original ? TRUE : FALSE;
    }

    /** A wrapper for a primitive int array. This is insane but necessary in Java. */
    private class IntArray implements Serializable {
        private static final long serialVersionUID = 20140524L;
        final int[] array;
        IntArray(int[] array) {
            this.array = array;
        }
        @Override
        public boolean equals (Object other) {
            if (other instanceof IntArray) {
                return Arrays.equals(array, ((IntArray) other).array);
            } else return false;
        }
        @Override
        public int hashCode() {
            return Arrays.hashCode(array);
        }
    }

    /** A wrapper for a String array. Optionally, the individual Strings may be deduplicated too. */
    private class StringArray implements Serializable {
        private static final long serialVersionUID = 20140524L;
        final String[] array;
        StringArray(String[] array, boolean deduplicateStrings) {
            if (deduplicateStrings) {
                this.array = new String[array.length];
                for (int i = 0; i < array.length; i++) {
                    this.array[i] = deduplicateString(array[i]);
                }
            } else this.array = array;
        }
        @Override
        public boolean equals (Object other) {
            if (other instanceof StringArray) {
                return Arrays.equals(array, ((StringArray) other).array);
            } else return false;
        }
        @Override
        public int hashCode() {
            return Arrays.hashCode(array);
        }
    }
}
