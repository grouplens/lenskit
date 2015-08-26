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
package org.lenskit.util.keys;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import org.lenskit.util.collections.LongUtils;

import javax.annotation.concurrent.Immutable;
import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * An immutable long-to-double map backed by a sorted key array.
 */
@Immutable
public class Long2DoubleSortedArrayMap extends AbstractLong2DoubleSortedMap {
    private static final long serialVersionUID = 1L;

    private final LongKeyIndex keys;
    private final double[] values;

    Long2DoubleSortedArrayMap(LongKeyIndex ks, double[] vs) {
        Preconditions.checkArgument(vs.length >= ks.getUpperBound(),
                                    "index and value sizes mismatched");
        keys = ks;
        values = vs;
    }

    public Long2DoubleSortedArrayMap(Map<Long,Double> data) {
        Long2DoubleFunction vf = LongUtils.asLong2DoubleFunction(data);
        keys = LongKeyIndex.fromCollection(data.keySet());
        int size = keys.size();
        values = new double[size];
        for (int i = 0; i < size; i++) {
            values[i] = vf.get(keys.getKey(i));
        }
    }

    /**
     * Create a new long-to-double map backed by a key index and a corresponding value array.
     * @param keys The keys.
     * @param vs The values (the array is used as-is, it is *not* copied).
     * @return The array map.
     */
    public static Long2DoubleSortedArrayMap wrap(LongKeyIndex keys, double[] vs) {
        return new Long2DoubleSortedArrayMap(keys, vs);
    }

    @Override
    public FastSortedEntrySet long2DoubleEntrySet() {
        return new EntrySet();
    }

    @Override
    public LongSortedSet keySet() {
        return keys.keySet();
    }

    @Override
    public LongComparator comparator() {
        return null; // natural ordering
    }

    private Long2DoubleSortedMap createSubMap(int lb, int ub) {
        return new Long2DoubleSortedArrayMap(keys.subIndex(lb, ub), values);
    }

    @Override
    public Long2DoubleSortedMap subMap(long from, long to) {
        int startIdx = keys.findLowerBound(from); // include 'from'
        int endIdx = keys.findLowerBound(to); // lower bound so we don't include 'to'
        return createSubMap(startIdx, endIdx);
    }

    @Override
    public Long2DoubleSortedMap headMap(long l) {
        int endIdx = keys.findLowerBound(l); // lower bound so we don't include 'l'
        return createSubMap(keys.getLowerBound(), endIdx);
    }

    @Override
    public Long2DoubleSortedMap tailMap(long l) {
        int startIdx = keys.findLowerBound(l); // include 'l'
        return createSubMap(startIdx, keys.getUpperBound());
    }

    @Override
    public long firstLongKey() {
        return keySet().firstLong();
    }

    @Override
    public long lastLongKey() {
        return keySet().lastLong();
    }

    @Override
    public double get(long l) {
        int idx = keys.getIndex(l);
        if (idx >= 0) {
            return values[idx];
        } else {
            return defaultReturnValue();
        }
    }

    @Override
    public boolean containsKey(long k) {
        return keys.containsKey(k);
    }

    @Override
    public int size() {
        return keySet().size();
    }

    private Entry entry(int idx) {
        return new BasicEntry(keys.getKey(idx), values[idx]);
    }

    private class EntrySet extends AbstractObjectSortedSet<Entry> implements FastSortedEntrySet {
        @Override
        public ObjectBidirectionalIterator<Entry> iterator(Entry entry) {
            return new EntryIter(entry.getLongKey());
        }

        @Override
        public ObjectBidirectionalIterator<Entry> iterator() {
            return new EntryIter();
        }

        @Override
        public ObjectBidirectionalIterator<Entry> fastIterator(Entry entry) {
            return new EntryIter(entry.getLongKey());
        }

        @Override
        public ObjectIterator<Entry> fastIterator() {
            return new FastEntryIter();
        }

        @Override
        public Comparator<? super Entry> comparator() {
            return null;
        }

        @Override
        public ObjectSortedSet<Entry> subSet(Entry from, Entry to) {
            return subMap(from.getLongKey(), to.getLongKey()).long2DoubleEntrySet();
        }

        @Override
        public ObjectSortedSet<Entry> headSet(Entry entry) {
            return headMap(entry.getLongKey()).long2DoubleEntrySet();
        }

        @Override
        public ObjectSortedSet<Entry> tailSet(Entry entry) {
            return tailMap(entry.getLongKey()).long2DoubleEntrySet();
        }

        @Override
        public Entry first() {
            if (isEmpty()) {
                throw new NoSuchElementException();
            }
            return entry(keys.getLowerBound());
        }

        @Override
        public Entry last() {
            if (isEmpty()) {
                throw new NoSuchElementException();
            }
            return entry(keys.getUpperBound() - 1);
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry) o;
                long key = e instanceof Entry ? ((Entry) e).getLongKey() : (Long) e.getKey();
                int idx = keys.getIndex(key);
                if (idx >= 0) {
                    return e.getValue().equals(values[idx]);
                }
            }
            return false;
        }

        @Override
        public int size() {
            return keySet().size();
        }
    }

    private class EntryIter extends AbstractObjectBidirectionalIterator<Entry> {
        IntBidirectionalIterator iter;

        public EntryIter() {
            iter = IntIterators.fromTo(keys.getLowerBound(), keys.getUpperBound());
        }

        public EntryIter(long k) {
            this();
            // skip past the item
            int idx = keys.findUpperBound(k);
            iter.skip(idx);
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public Entry next() {
            return entry(iter.nextInt());
        }

        @Override
        public Entry previous() {
            return entry(iter.previousInt());
        }

        @Override
        public boolean hasPrevious() {
            return iter.hasPrevious();
        }
    }

    private class FastEntryIter extends AbstractObjectBidirectionalIterator<Entry> {
        IntBidirectionalIterator iter = IntIterators.fromTo(keys.getLowerBound(), keys.getUpperBound());
        IndirectEntry entry = new IndirectEntry(0);

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public Entry next() {
            entry.index = iter.nextInt();
            return entry;
        }

        @Override
        public Entry previous() {
            entry.index = iter.nextInt();
            return entry;
        }

        @Override
        public boolean hasPrevious() {
            return iter.hasPrevious();
        }
    }

    private class IndirectEntry implements Entry {
        int index;

        public IndirectEntry(int idx) {
            index = idx;
        }

        @Override
        public long getLongKey() {
            return keys.getKey(index);
        }

        @Override
        public double setValue(double v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double getDoubleValue() {
            return values[index];
        }

        @Override
        public Long getKey() {
            return getLongKey();
        }

        @Override
        public Double getValue() {
            return getDoubleValue();
        }

        @Override
        public Double setValue(Double value) {
            throw new UnsupportedOperationException();
        }
    }
}
