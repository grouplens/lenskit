/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2013 Regents of the University of Minnesota and contributors
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
package org.grouplens.lenskit.util;

import it.unimi.dsi.fastutil.doubles.DoubleHeapIndirectPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.grouplens.lenskit.collections.CompactableLongArrayList;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.scored.ScoredIdListBuilder;
import org.grouplens.lenskit.scored.ScoredIds;
import org.grouplens.lenskit.vectors.MutableSparseVector;

import java.util.List;

/**
 * Accumulate the top <i>N</i> scored IDs.  IDs are sorted by their associated
 * scores.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public final class TopNScoredItemAccumulator implements ScoredItemAccumulator {
    private final int count;
    private double[] scores;
    private CompactableLongArrayList items;
    private int slot;
    private int size;
    private DoubleHeapIndirectPriorityQueue heap;

    /**
     * Create a new accumulator to accumulate the top {@var n} IDs.
     *
     * @param n The number of IDs to retain.
     */
    public TopNScoredItemAccumulator(int n) {
        this.count = n;
        // arrays must have n+1 slots to hold extra item before removing smallest
        scores = new double[n + 1];
        items = new CompactableLongArrayList();
        slot = 0;
        size = 0;
        heap = new DoubleHeapIndirectPriorityQueue(scores);
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void put(long item, double score) {
        assert slot <= count;
        assert heap.size() == size;

        /*
         * Store the new item. The slot shows where the current item is, and
         * then we deal with it based on whether we're oversized.
         */
        if (slot == items.size()) {
            items.add(item);
        } else {
            items.set(slot, item);
        }
        scores[slot] = score;
        heap.enqueue(slot);

        if (size == count) {
            // already at capacity, so remove and reuse smallest item
            slot = heap.dequeue();
        } else {
            // we have free space, so increment the slot and size
            slot += 1;
            size += 1;
        }
    }

    @Override
    public List<ScoredId> finish() {
        assert size == heap.size();
        int[] indices = new int[size];
        // Copy backwards so the scored list is sorted.
        for (int i = size - 1; i >= 0; i--) {
            indices[i] = heap.dequeue();
        }
        ScoredIdListBuilder bld = ScoredIds.newListBuilder(size);
        for (int i : indices) {
            bld.add(items.get(i), scores[i]);
        }

        assert heap.isEmpty();

        size = 0;
        slot = 0;
        items.clear();

        return bld.finish();
    }

    @Override
    public MutableSparseVector finishVector() {
        if (scores == null) {
            return MutableSparseVector.create();
        }

        assert size == heap.size();
        int[] indices = new int[size];
        // Copy backwards so the scored list is sorted.
        for (int i = size - 1; i >= 0; i--) {
            indices[i] = heap.dequeue();
        }
        assert heap.isEmpty();

        long[] keys = new long[indices.length];
        double[] values = new double[indices.length];
        for (int i = 0; i < indices.length; i++) {
            keys[i] = items.get(indices[i]);
            values[i] = scores[indices[i]];
        }
        size = 0;
        slot = 0;
        items.clear();

        return MutableSparseVector.wrapUnsorted(keys, values);
    }

    @Override
    public LongSet finishSet() {
        assert size == heap.size();

        LongSet longs = new LongOpenHashSet(size);
        while (!heap.isEmpty()) {
            longs.add(items.get(heap.dequeue()));
        }

        size = 0;
        slot = 0;
        items.clear();

        return longs;
    }
}
