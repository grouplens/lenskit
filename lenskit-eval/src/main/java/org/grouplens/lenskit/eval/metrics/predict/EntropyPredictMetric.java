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
package org.grouplens.lenskit.eval.metrics.predict;

import org.apache.commons.lang3.tuple.Pair;
import org.grouplens.lenskit.Recommender;
import org.grouplens.lenskit.data.pref.PreferenceDomain;
import org.grouplens.lenskit.eval.Attributed;
import org.grouplens.lenskit.eval.data.traintest.TTDataSet;
import org.grouplens.lenskit.eval.metrics.AbstractMetric;
import org.grouplens.lenskit.eval.metrics.ResultColumn;
import org.grouplens.lenskit.eval.traintest.TestUser;
import org.lenskit.transform.quantize.PreferenceDomainQuantizer;
import org.lenskit.transform.quantize.Quantizer;
import org.grouplens.lenskit.util.statistics.MutualInformationAccumulator;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.grouplens.lenskit.vectors.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluate a recommender's prediction accuracy by computing the mutual
 * information between the ratings and the prediction. This tells us the amount
 * of information our predictions can tell the user about our ratings.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class EntropyPredictMetric extends AbstractMetric<EntropyPredictMetric.Context, EntropyPredictMetric.EntropyResult, EntropyPredictMetric.EntropyResult> {
    private static final Logger logger = LoggerFactory.getLogger(EntropyPredictMetric.class);

    public EntropyPredictMetric() {
        super(EntropyResult.class, EntropyResult.class);
    }

    @Override
    public Context createContext(Attributed algorithm, TTDataSet dataSet, Recommender rec) {
        return new Context(dataSet.getTrainingData().getPreferenceDomain());
    }

    @Override
    public EntropyResult doMeasureUser(TestUser user, Context context) {
        SparseVector ratings = user.getTestRatings();
        SparseVector predictions = user.getPredictions();
        if (predictions == null) {
            return null;
        }

        Quantizer q = context.quantizer;

        // TODO Re-use accumulators
        MutualInformationAccumulator accum = new MutualInformationAccumulator(q.getCount());

        for (Pair<VectorEntry,VectorEntry> e: Vectors.fastIntersect(ratings, predictions)) {
            accum.count(q.index(e.getLeft().getValue()),
                        q.index(e.getRight().getValue()));
        }

        if (accum.getCount() > 0) {
            double ratingEntropy = accum.getV1Entropy();
            double predEntropy = accum.getV2Entropy();
            double info = accum.getMutualInformation();
            context.addUser(info, ratingEntropy, predEntropy);
            return new EntropyResult(info, ratingEntropy, predEntropy);
        } else {
            return null;
        }
    }

    @Override
    protected EntropyResult getTypedResults(Context context) {
        if (context.nusers <= 0) {
            return null;
        } else {
            return new EntropyResult(context.informationSum / context.nusers,
                                     context.ratingEntropySum / context.nusers,
                                     context.predictionEntropySum / context.nusers);
        }
    }

    public static class EntropyResult {
        @ResultColumn("MutualInformation")
        public final double mutualInformation;
        @ResultColumn("RatingEntropy")
        public final double ratingEntropy;
        @ResultColumn("PredictionEntropy")
        public final double predictionEntropy;

        public EntropyResult(double mi, double re, double pe){
            mutualInformation = mi;
            ratingEntropy = re;
            predictionEntropy = pe;
        }
    }

    public class Context {
        private Quantizer quantizer;

        private double informationSum = 0.0;
        private double ratingEntropySum = 0.0;
        private double predictionEntropySum = 0.0;
        private int nusers = 0;

        private Context(PreferenceDomain preferenceDomain) {
            quantizer = new PreferenceDomainQuantizer(preferenceDomain);
        }

        private void addUser(double info, double rent, double pent) {
            informationSum += info;
            ratingEntropySum += rent;
            predictionEntropySum += pent;
            nusers += 1;
        }
    }
}
