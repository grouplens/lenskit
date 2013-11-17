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
package org.grouplens.lenskit.mf.funksvd;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.baseline.BaselineScorer;
import org.grouplens.lenskit.basic.AbstractItemScorer;
import org.grouplens.lenskit.collections.LongUtils;
import org.grouplens.lenskit.data.dao.UserEventDAO;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.event.Ratings;
import org.grouplens.lenskit.data.history.History;
import org.grouplens.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.iterative.TrainingLoopController;
import org.grouplens.lenskit.transform.clamp.ClampingFunction;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Do recommendations and predictions based on SVD matrix factorization.
 *
 * Recommendation is done based on folding-in.  The strategy is do a fold-in
 * operation as described in
 * <a href="http://www.grouplens.org/node/212">Sarwar et al., 2002</a> with the
 * user's ratings.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class FunkSVDItemScorer extends AbstractItemScorer {

    protected final FunkSVDModel model;
    private UserEventDAO dao;
    private final ItemScorer baselineScorer;
    private final int featureCount;
    private final ClampingFunction clamp;

    @Nullable
    private final FunkSVDUpdateRule rule;

    /**
     * Construct the item scorer.
     *
     * @param dao      The DAO.
     * @param model    The model.
     * @param baseline The baseline scorer.  Be very careful when configuring a different baseline
     *                 at runtime than at model-build time; such a configuration is unlikely to
     *                 perform well.
     * @param rule     The update rule, or {@code null} (the default) to only use the user features
     *                 from the model. If provided, this update rule is used to update a user's
     *                 feature values based on their profile when scores are requested.
     */
    @Inject
    public FunkSVDItemScorer(UserEventDAO dao, FunkSVDModel model,
                             @BaselineScorer ItemScorer baseline,
                             @Nullable @RuntimeUpdate FunkSVDUpdateRule rule) {
        // FIXME Unify requirement on update rule and DAO
        this.dao = dao;
        this.model = model;
        baselineScorer = baseline;
        this.rule = rule;

        featureCount = model.getFeatureCount();
        clamp = model.getClampingFunction();
    }

    @Nullable
    public FunkSVDUpdateRule getUpdateRule() {
        return rule;
    }

    /**
     * Predict for a user using their preference array and history vector.
     *
     * @param user   The user's ID
     * @param uprefs The user's preference array from the model.
     * @param output The output vector, whose key domain is the items to predict for. It must
     *               be initialized to the user's baseline predictions.
     */
    private void predict(long user, double[] uprefs, MutableSparseVector output) {
        for (VectorEntry e : output.fast()) {
            final long item = e.getKey();
            final int iidx = model.getItemIndex().getIndex(item);

            if (iidx < 0) {
                continue;
            }

            double score = e.getValue();
            for (int f = 0; f < featureCount; f++) {
                score += uprefs[f] * model.getItemFeatures()[f][iidx];
                score = clamp.apply(user, item, score);
            }
            output.set(e, score);
        }
    }

    /**
     * Get estimates for all a user's ratings and the target items.
     *
     * @param user    The user ID.
     * @param ratings The user's ratings.
     * @param items   The target items.
     * @return Baseline predictions for all items either in the target set or the set of
     *         rated items.
     */
    private MutableSparseVector initialEstimates(long user, SparseVector ratings, LongSortedSet items) {
        LongSet allItems = LongUtils.setUnion(items, ratings.keySet());
        MutableSparseVector estimates = MutableSparseVector.create(allItems);
        baselineScorer.score(user, estimates);
        return estimates;
    }

    @Override
    public void score(long user, @Nonnull MutableSparseVector scores) {
        int uidx = model.getUserIndex().getIndex(user);
        UserHistory<Rating> history = dao.getEventsForUser(user, Rating.class);
        if (history == null) {
            history = History.forUser(user);
        }
        SparseVector ratings = Ratings.userRatingVector(history);

        MutableSparseVector estimates = initialEstimates(user, ratings, scores.keyDomain());
        // propagate estimates to the output scores
        scores.set(estimates);
        if (uidx < 0 && ratings.isEmpty()) {
            // no real work to do, stop with baseline predictions
            return;
        }

        double[] uprefs;
        if (uidx < 0) {
            uprefs = new double[model.getFeatureCount()];
            for (int i = 0; i < model.getFeatureCount(); i++) {
                uprefs[i] = model.getFeatureInfo(i).getUserAverage();
            }
        } else {
            uprefs = new double[featureCount];
            for (int i = 0; i < featureCount; i++) {
                uprefs[i] = model.getUserFeatures()[i][uidx];
            }
        }

        if (!ratings.isEmpty() && rule != null) {
            for (int f = 0; f < featureCount; f++) {
                trainUserFeature(user, uprefs, ratings, estimates, f);
            }
        }

        // scores are the estimates, uprefs are trained up.
        predict(user, uprefs, scores);
    }

    private void trainUserFeature(long user, double[] uprefs, SparseVector ratings,
                                  MutableSparseVector estimates, int feature) {
        assert rule != null;

        double rmse = Double.MAX_VALUE;
        TrainingLoopController controller = rule.getTrainingLoopController();
        while (controller.keepTraining(rmse)) {
            rmse = doFeatureIteration(user, uprefs, ratings, estimates, feature);
        }

        // After training this feature, we need to update each rating's cached
        // value to accommodate it.
        for (VectorEntry itemId : ratings.fast()) {
            final long iid = itemId.getKey();
            double est = estimates.get(iid);
            double offset = uprefs[feature] * model.getItemFeature(iid, feature);
            est = clamp.apply(user, iid, est + offset);
            estimates.set(iid, est);
        }
    }

    private double doFeatureIteration(long user, double[] uprefs,
                                      SparseVector ratings, MutableSparseVector estimates,
                                      int feature) {
        assert rule != null;
        FunkSVDUpdater updater = rule.createUpdater();
        double sse = 0;
        int n = 0;
        for (VectorEntry e: ratings.fast()) {
            final long iid = e.getKey();
            final int iidx = model.getItemIndex().getIndex(iid);

            // Step 1: Compute the trailing value for this item-feature pair
            double trailingValue = 0.0;
            for (int f = feature + 1; f < featureCount; f++) {
                trailingValue += uprefs[f] * model.getItemFeatures()[f][iidx];
            }

            updater.prepare(user, iid, trailingValue, estimates.get(iid), e.getValue(),
                            uprefs[feature], model.getItemFeatures()[feature][iidx]);

            // Step 4: update user preferences
            uprefs[feature] += updater.getUserFeatureUpdate();
        }
        return updater.getRMSE();
    }
}
