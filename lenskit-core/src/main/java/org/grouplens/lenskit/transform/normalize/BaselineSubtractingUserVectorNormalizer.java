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
package org.grouplens.lenskit.transform.normalize;

import it.unimi.dsi.fastutil.longs.Long2DoubleFunction;
import org.grouplens.lenskit.core.Shareable;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.lenskit.api.ItemScorer;
import org.lenskit.baseline.BaselineScorer;
import org.lenskit.util.collections.LongUtils;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.Map;

/**
 * User vector normalizer that subtracts a user's baseline scores.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
@Shareable
public class BaselineSubtractingUserVectorNormalizer extends AbstractUserVectorNormalizer implements Serializable {
    private static final long serialVersionUID = 3L;

    protected final ItemScorer baselineScorer;

    /**
     * Create a new baseline-subtracting normalizer with the given baseline.
     *
     * @param baseline The baseline scorer to use for normalization.
     */
    @Inject
    public BaselineSubtractingUserVectorNormalizer(@BaselineScorer ItemScorer baseline) {
        baselineScorer = baseline;
    }

    @Override
    public VectorTransformation makeTransformation(long user, SparseVector ratings) {
        return new Transformation(user);
    }

    private class Transformation implements VectorTransformation {
        private final long user;

        public Transformation(long u) {
            user = u;
        }

        @Override
        public MutableSparseVector apply(MutableSparseVector vector) {
            Map<Long,Double> base = baselineScorer.score(user, vector.keySet());
            Long2DoubleFunction bf = LongUtils.asLong2DoubleFunction(base);
            for (VectorEntry e: vector.view(VectorEntry.State.SET)) {
                vector.set(e, e.getValue() - bf.get(e.getKey()));
            }
            return vector;
        }

        @Override
        public MutableSparseVector unapply(MutableSparseVector vector) {
            Map<Long,Double> base = baselineScorer.score(user, vector.keySet());
            Long2DoubleFunction bf = LongUtils.asLong2DoubleFunction(base);
            for (VectorEntry e: vector.view(VectorEntry.State.SET)) {
                vector.set(e, e.getValue() + bf.get(e.getKey()));
            }
            return vector;
        }
    }

    @Override
    public String toString() {
        return String.format("[BaselineNorm: %s]", baselineScorer);
    }
}
