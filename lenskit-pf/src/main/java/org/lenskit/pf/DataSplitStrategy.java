/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2016 LensKit Contributors.  See CONTRIBUTORS.md.
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
package org.lenskit.pf;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.grouplens.grapht.annotation.DefaultImplementation;
import org.lenskit.data.ratings.RatingMatrixEntry;
import org.lenskit.util.keys.KeyIndex;

import java.util.List;

/**
 * Strategy of splitting ratings data into training data and validation data.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
@DefaultImplementation(RandomDataSplitStrategy.class)
public interface DataSplitStrategy {

    /**
     * Get the training mapping from item indices to maps of item ratings (user indices to user ratings)
     *
     * @return The training map
     */
    Int2ObjectMap<Int2DoubleMap> getTrainingMatrix();

    /**
     * Get a list of validation ratings
     *
     * @return The validation list of RatingMatrixEntry
     */
    List<RatingMatrixEntry> getValidationRatings();

    /**
     * Get the mapping from user indices to sets of user consumed items
     *
     * @return The map of user indices to user consumed items indices
     */
    Int2ObjectMap<ImmutableSet<Integer>> getUserItemIndices();

    KeyIndex getUserIndex();

    KeyIndex getItemIndex();
}