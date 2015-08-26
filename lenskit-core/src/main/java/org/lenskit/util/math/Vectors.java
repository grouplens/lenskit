/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2014 LensKit Contributors.  See CONTRIBUTORS.md.
 * Work on LensKit has been funded by the National Science Foundation under
 * grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.lenskit.util.math;

import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.longs.Long2DoubleFunction;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleSortedMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.Iterator;

/**
 * Utility methods for vector arithmetic.
 */
public final class Vectors {
    private Vectors() {}

    /**
     * Get an iterator over the entries of the map. If possible, this is a {@linkplain Long2DoubleMap.FastEntrySet fast iterator}.
     *
     * @param map The map.
     * @return An iterator over the map's entries.
     */
    public static Iterator<Long2DoubleMap.Entry> fastEntryIterator(Long2DoubleMap map) {
        ObjectSet<Long2DoubleMap.Entry> entries = map.long2DoubleEntrySet();
        if (entries instanceof Long2DoubleMap.FastEntrySet) {
            return ((Long2DoubleMap.FastEntrySet) entries).fastIterator();
        } else {
            return entries.iterator();
        }
    }

    /**
     * Compute the sum of the elements of a map.
     * @param v The vector
     * @return The sum of the values of {@code v}.
     */
    public static double sum(Long2DoubleMap v) {
        double sum = 0;
        DoubleIterator iter = v.values().iterator();
        while (iter.hasNext()) {
            sum += iter.nextDouble();
        }
        return sum;
    }

    /**
     * Compute the sum of the squares of elements of a map.
     * @param v The vector
     * @return The sum of the squares of the values of {@code v}.
     */
    public static double sumOfSquares(Long2DoubleMap v) {
        double sum = 0;
        DoubleIterator iter = v.values().iterator();
        while (iter.hasNext()) {
            double d = iter.nextDouble();
            sum += d * d;
        }
        return sum;
    }

    /**
     * Compute the Euclidean norm of the values of the map. This is the square root of the sum of squares.
     * @param v The vector.
     * @return The Euclidean norm of the vector.
     */
    public static double euclideanNorm(Long2DoubleMap v) {
        return Math.sqrt(sumOfSquares(v));
    }

    /**
     * Compute the dot product of two maps. This method assumes any value missing in one map is 0, so it is the dot
     * product of the values of common keys.
     * @param v1 The first vector.
     * @param v2 The second vector.
     * @return The sum of the products of corresponding values in the two vectors.
     */
    public static double dotProduct(Long2DoubleMap v1, Long2DoubleMap v2) {
        double result = 0;

        if (v1 instanceof Long2DoubleSortedMap && v2 instanceof Long2DoubleSortedMap) {
            Iterator<Long2DoubleMap.Entry> it1 = fastEntryIterator(v1);
            Iterator<Long2DoubleMap.Entry> it2 = fastEntryIterator(v2);
            Long2DoubleMap.Entry e1 = it1.hasNext() ? it1.next() : null;
            Long2DoubleMap.Entry e2 = it2.hasNext() ? it2.next() : null;
            while (e1 != null && e2 != null) {
                long k1 = e1.getLongKey();
                long k2 = e2.getLongKey();
                if (k1 == k2) {
                    result += e1.getDoubleValue() * e2.getDoubleValue();
                    e1 = it1.hasNext() ? it1.next() : null;
                    e2 = it2.hasNext() ? it2.next() : null;
                } else if (k1 < k2) {
                    e1 = it1.hasNext() ? it1.next() : null;
                } else { // k2 < k1
                    e2 = it2.hasNext() ? it2.next() : null;
                }
            }
        } else {
            Long2DoubleFunction v2d = adaptDefaultValue(v2, 0.0);
            Iterator<Long2DoubleMap.Entry> iter = fastEntryIterator(v1);
            while (iter.hasNext()) {
                Long2DoubleMap.Entry e = iter.next();
                long k = e.getLongKey();
                result += e.getDoubleValue() * v2d.get(k); // since default is 0
            }
        }

        return result;
    }

    /**
     * Wrap a long-to-double function in another w/ the a different default return value.
     * @param f The function to wrap.
     * @param dft The new
     * @return A function with a different default value.
     */
    @SuppressWarnings("FloatingPointEquality")
    static Long2DoubleFunction adaptDefaultValue(final Long2DoubleFunction f, final double dft) {
        if (f.defaultReturnValue() == dft) {
            return f;
        }

        return new DftAdaptingL2DFunction(f, dft);
    }

    private static class DftAdaptingL2DFunction implements Long2DoubleFunction {
        private final Long2DoubleFunction delegate;
        private final double dft;

        public DftAdaptingL2DFunction(Long2DoubleFunction f, double dft) {
            this.delegate = f;
            this.dft = dft;
        }

        @Override
        public double get(long l) {
            if (delegate.containsKey(l)) {
                return delegate.get(l);
            } else {
                return dft;
            }
        }

        @Override
        public boolean containsKey(long l) {
            return delegate.containsKey(l);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public double put(long key, double value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double remove(long key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void defaultReturnValue(double rv) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double defaultReturnValue() {
            return dft;
        }

        @Override
        public Double put(Long key, Double value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Double get(Object key) {
            return delegate.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public Double remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }
}
