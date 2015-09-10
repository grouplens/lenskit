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
package org.lenskit.knn.item;

import org.grouplens.lenskit.GlobalItemRecommender;
import org.grouplens.lenskit.GlobalItemScorer;
import org.grouplens.lenskit.RecommenderBuildException;
import org.grouplens.lenskit.basic.TopNGlobalItemRecommender;
import org.grouplens.lenskit.core.LenskitConfiguration;
import org.grouplens.lenskit.data.dao.EventCollectionDAO;
import org.grouplens.lenskit.data.dao.EventDAO;
import org.lenskit.data.ratings.Rating;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.lenskit.LenskitRecommender;
import org.lenskit.LenskitRecommenderEngine;
import org.lenskit.api.ItemScorer;
import org.lenskit.basic.SimpleRatingPredictor;
import org.lenskit.basic.TopNItemRecommender;
import org.lenskit.knn.item.model.ItemItemBuildContext;
import org.lenskit.knn.item.model.ItemItemModel;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class ItemItemRecommenderBuildTest {

    private LenskitRecommenderEngine engine;

    @SuppressWarnings("deprecation")
    @Before
    public void setup() throws RecommenderBuildException {
        List<Rating> rs = new ArrayList<Rating>();
        rs.add(Rating.create(1, 5, 2));
        rs.add(Rating.create(1, 7, 4));
        rs.add(Rating.create(8, 4, 5));
        rs.add(Rating.create(8, 5, 4));
        EventDAO dao = new EventCollectionDAO(rs);

        LenskitConfiguration config = new LenskitConfiguration();
        config.bind(EventDAO.class).to(dao);
        config.bind(ItemScorer.class).to(ItemItemScorer.class);
        config.bind(GlobalItemScorer.class).to(ItemItemGlobalScorer.class);
        // this is the default
//        factory.setComponent(UserVectorNormalizer.class, VectorNormalizer.class,
//                             IdentityVectorNormalizer.class);

        engine = LenskitRecommenderEngine.build(config);
    }

    @SuppressWarnings("deprecation")
    @Test
    @Ignore("will not work until moved")
    public void testItemItemRecommenderEngineCreate() {
        LenskitRecommender rec = engine.createRecommender();

        assertThat(rec.getItemScorer(),
                   instanceOf(ItemItemScorer.class));
        assertThat(rec.getRatingPredictor(),
                   instanceOf(SimpleRatingPredictor.class));
        assertThat(rec.getItemRecommender(),
                   instanceOf(TopNItemRecommender.class));
        assertThat(rec.get(GlobalItemRecommender.class),
                   instanceOf(TopNGlobalItemRecommender.class));
        assertThat(rec.get(GlobalItemScorer.class),
                   instanceOf(ItemItemGlobalScorer.class));
    }

    @Test
    public void testContextRemoved() {
        LenskitRecommender rec = engine.createRecommender();
        assertThat(rec.get(ItemItemBuildContext.class),
                   nullValue());
    }

    @Test
    public void testConfigSeparation() {
        LenskitRecommender rec1 = null;
        LenskitRecommender rec2 = null;
        rec1 = engine.createRecommender();
        rec2 = engine.createRecommender();

        assertThat(rec1.getItemScorer(),
                   not(sameInstance(rec2.getItemScorer())));
        assertThat(rec1.get(ItemItemModel.class),
                   allOf(not(nullValue()),
                         sameInstance(rec2.get(ItemItemModel.class))));
    }
}
