/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2014 Regents of the University of Minnesota and contributors
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


import org.grouplens.lenskit.eval.metrics.predict.*
import org.lenskit.api.ItemScorer
import org.lenskit.baseline.ItemMeanRatingItemScorer
import org.lenskit.baseline.UserMeanBaseline
import org.lenskit.baseline.UserMeanItemScorer

trainTest {
    dataset crossfold("ML100K") {
        source csvfile(config['ml100k.ratings']) {
            delimiter "\t"
        }
        partitions 5
        holdout 5
        train 'train.%d.pack'
        test 'test.%d.pack'
        writeTimestamps false
    }

    componentCacheDirectory "cache"
    cacheAllComponents true

    algorithm("Baseline") {
        bind ItemScorer to UserMeanItemScorer
        bind (UserMeanBaseline, ItemScorer) to ItemMeanRatingItemScorer
    }

    metric CoveragePredictMetric
    metric RMSEPredictMetric
    metric MAEPredictMetric
    metric NDCGPredictMetric
    metric HLUtilityPredictMetric
    metric precisionRecall {
        listSize 10
    }
    metric topNRecallPrecision {
        prefix "Deprecated"
        listSize 10
    }
    metric predictions {
        file 'predictions.csv'
    }
    metric recommendations {
        file 'recommendations.csv.gz'
    }

    output 'results.csv'
    userOutput 'users.csv'
    predictOutput 'deprecated-predictions.csv'
    recommendOutput 'deprecated-recommendations.csv.gz'
}
