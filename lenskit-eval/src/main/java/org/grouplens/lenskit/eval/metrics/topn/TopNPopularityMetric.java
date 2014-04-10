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
package org.grouplens.lenskit.eval.metrics.topn;

import it.unimi.dsi.fastutil.longs.*;
import org.grouplens.lenskit.Recommender;
import org.grouplens.lenskit.data.dao.EventDAO;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.eval.Attributed;
import org.grouplens.lenskit.eval.data.traintest.TTDataSet;
import org.grouplens.lenskit.eval.metrics.AbstractMetric;
import org.grouplens.lenskit.eval.metrics.ResultColumn;
import org.grouplens.lenskit.eval.traintest.TestUser;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.util.statistics.MeanAccumulator;

import java.util.List;

/**
 * Metric that measures how popular the items in the TopN list are.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class TopNPopularityMetric extends AbstractMetric<TopNPopularityMetric.Context, TopNPopularityMetric.Result, TopNPopularityMetric.Result> {
    private final String suffix;
    private final int listSize;
    private final ItemSelector candidates;
    private final ItemSelector exclude;

    public TopNPopularityMetric(String sfx, int listSize, ItemSelector candidates, ItemSelector exclude) {
        super(Result.class, Result.class);
        suffix = sfx;
        this.listSize = listSize;
        this.candidates = candidates;
        this.exclude = exclude;
    }

    @Override
    protected String getSuffix() {
        return suffix;
    }

    /**
     * Computes the popularity of a set of ratings as the number of users who have rated an item
     * This function is robust in the face of multiple ratings on the same item by the same user.
     * @return an immutable map from movie Ids to the number of users who have rated the identified movie.
     */
    private Long2IntMap computePop(EventDAO dao) {

        Long2ObjectOpenHashMap<LongSet> watchingUsers = new Long2ObjectOpenHashMap<LongSet>();
        for (Rating r : dao.streamEvents(Rating.class).fast()) {
            long item = r.getItemId();
            long user = r.getUserId();
            if (! watchingUsers.containsKey(item)) {
                watchingUsers.put(item, new LongOpenHashSet());
            }
            watchingUsers.get(item).add(user);
        }
        
        Long2IntMap userCounts = new Long2IntOpenHashMap();
        for (long item : watchingUsers.keySet()) {
            userCounts.put(item, watchingUsers.get(item).size());
        }
        return Long2IntMaps.unmodifiable(userCounts);
    }
    
    @Override
    public Context createContext(Attributed algo, TTDataSet ds, Recommender rec) {
        Long2IntMap popularity = computePop(ds.getTrainingDAO()); 
        return new Context(popularity);
    }

    @Override
    public Result doMeasureUser(TestUser user, Context context) {
        List<ScoredId> recs;
        recs = user.getRecommendations(listSize, candidates, exclude);
        if (recs == null || recs.isEmpty()) {
            return null;
        }
        double pop = 0;
        for (ScoredId s : recs) {
            pop += context.popularity.get(s.getId()); // default value should be 0 here.
        }
        pop = pop / recs.size();

        context.mean.add(pop);
        return new Result(pop);
    }

    @Override
    protected Result getTypedResults(Context context) {
        return new Result(context.mean.getMean());
    }

    public static class Result {
        @ResultColumn("TopN.MeanPopularity")
        public final double mean;

        public Result(double mu) {
            mean = mu;
        }
    }

    public class Context {
        final Long2IntMap popularity;
        final MeanAccumulator mean = new MeanAccumulator();

        public Context(Long2IntMap popularity) {
            this.popularity = popularity;
        }
    }

    /**
     * @author <a href="http://www.grouplens.org">GroupLens Research</a>
     */
    public static class Builder extends TopNMetricBuilder<Builder, TopNPopularityMetric> {
        private String suffix;

        /**
         * Get the column suffix for this metric.
         * @return The column suffix.
         */
        public String getSuffix() {
            return suffix;
        }

        /**
         * Set the column suffix for this metric.
         * @param l The column suffix
         * @return The builder (for chaining).
         */
        public Builder setSuffix(String l) {
            suffix = l;
            return this;
        }

        @Override
        public TopNPopularityMetric build() {
            return new TopNPopularityMetric(suffix, listSize, candidates, exclude);
        }
    }

}
