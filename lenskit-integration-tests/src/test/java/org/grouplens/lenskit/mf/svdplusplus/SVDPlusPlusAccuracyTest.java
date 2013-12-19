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
package org.grouplens.lenskit.mf.svdplusplus;

import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.baseline.*;
import org.grouplens.lenskit.core.LenskitConfiguration;
import org.grouplens.lenskit.iterative.IterationCount;
import org.grouplens.lenskit.mf.funksvd.FeatureCount;
import org.grouplens.lenskit.test.CrossfoldTestSuite;
import org.grouplens.lenskit.util.table.Table;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

/**
 * Do major tests on the SVDPlusPlus recommender.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SVDPlusPlusAccuracyTest extends CrossfoldTestSuite {
    @SuppressWarnings("unchecked")
    @Override
    protected void configureAlgorithm(LenskitConfiguration config) {
        config.bind(ItemScorer.class)
              .to(SVDPlusPlusItemScorer.class);
        config.bind(BaselineScorer.class, ItemScorer.class)
              .to(UserMeanItemScorer.class);
        config.bind(UserMeanBaseline.class, ItemScorer.class)
              .to(ItemMeanRatingItemScorer.class);
        config.within(BaselineScorer.class, ItemScorer.class)
              .set(MeanDamping.class)
              .to(10);
        config.set(FeatureCount.class).to(10);
        config.set(IterationCount.class).to(50);
    }

    @Override
    protected void checkResults(Table table) {
        assertThat(table.column("MAE").average(),
                   closeTo(0.74, 0.025));
        assertThat(table.column("RMSE.ByUser").average(),
                   closeTo(0.92 , 0.05));
    }
}
