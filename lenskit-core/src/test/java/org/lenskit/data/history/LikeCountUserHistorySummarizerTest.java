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
package org.lenskit.data.history;

import org.grouplens.lenskit.data.history.LikeCountUserHistorySummarizer;
import org.grouplens.lenskit.data.history.UserHistorySummarizer;
import org.lenskit.data.events.Like;
import org.lenskit.data.events.LikeBatch;
import org.lenskit.data.history.History;
import org.lenskit.data.ratings.Rating;
import org.grouplens.lenskit.vectors.SparseVector;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class LikeCountUserHistorySummarizerTest {
    UserHistorySummarizer sum;

    @Before
    public void createSummarizer() {
        sum = new LikeCountUserHistorySummarizer();
    }

    @Test
    public void testSummarizeEmpty() {
        SparseVector vec = sum.summarize(History.forUser(42));
        assertThat(vec.size(), equalTo(0));
    }

    @Test
    public void testSummarizeNoLike() {
        SparseVector vec = sum.summarize(History.forUser(42, Rating.create(42, 39, 2.5)));
        assertThat(vec.size(), equalTo(0));
    }

    @Test
    public void testOneLike() {
        SparseVector vec = sum.summarize(History.forUser(42, Like.create(42, 39)));
        assertThat(vec.size(), equalTo(1));
        assertThat(vec.get(39), equalTo(1.0));
    }

    @Test
    public void testTwoLikes() {
        SparseVector vec = sum.summarize(History.forUser(42,
                                                         Like.create(42, 39),
                                                         Like.create(42, 67)));
        assertThat(vec.size(), equalTo(2));
        assertThat(vec.get(39), equalTo(1.0));
        assertThat(vec.get(67), equalTo(1.0));
    }

    @Test
    public void testRepeatedLikes() {
        SparseVector vec = sum.summarize(History.forUser(42,
                                                         Like.create(42, 39),
                                                         Like.create(42, 67),
                                                         Like.create(42, 39)));
        assertThat(vec.size(), equalTo(2));
        assertThat(vec.get(39), equalTo(2.0));
        assertThat(vec.get(67), equalTo(1.0));
    }

    @Test
    public void testLikeBatch() {
        SparseVector vec = sum.summarize(History.forUser(42,
                                                         Like.create(42, 39),
                                                         LikeBatch.create(42, 67, 402),
                                                         Like.create(42, 39)));
        assertThat(vec.size(), equalTo(2));
        assertThat(vec.get(39), equalTo(2.0));
        assertThat(vec.get(67), equalTo(402.0));
    }
}
