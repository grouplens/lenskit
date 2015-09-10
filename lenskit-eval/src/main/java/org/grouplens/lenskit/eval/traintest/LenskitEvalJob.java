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
package org.grouplens.lenskit.eval.traintest;

import com.google.common.base.Preconditions;
import org.grouplens.grapht.Component;
import org.grouplens.grapht.Dependency;
import org.grouplens.grapht.InjectionException;
import org.grouplens.grapht.graph.DAGNode;
import org.grouplens.lenskit.RecommenderBuildException;
import org.grouplens.lenskit.core.LenskitRecommender;
import org.grouplens.lenskit.data.dao.UserEventDAO;
import org.lenskit.data.events.Event;
import org.grouplens.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.eval.algorithm.AlgorithmInstance;
import org.grouplens.lenskit.eval.data.traintest.TTDataSet;
import org.grouplens.lenskit.eval.metrics.Metric;
import org.grouplens.lenskit.inject.GraphtUtils;
import org.grouplens.lenskit.inject.NodeProcessors;
import org.grouplens.lenskit.inject.RecommenderInstantiator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
class LenskitEvalJob extends TrainTestJob {
    private DAGNode<Component, Dependency> recommenderGraph;
    @Nullable
    private final ComponentCache cache;

    private LenskitRecommender recommender;
    private UserEventDAO userEvents;

    LenskitEvalJob(TrainTestEvalTask task,
                   @Nonnull AlgorithmInstance algo,
                   @Nonnull TTDataSet ds,
                   DAGNode<Component,Dependency> graph,
                   @Nullable ComponentCache cache) {
        super(task, algo, ds);
        recommenderGraph = graph;
        this.cache = cache;
    }

    @Override
    protected void buildRecommender() throws RecommenderBuildException {
        Preconditions.checkState(recommender == null, "recommender already built");
        logger.debug("Starting recommender build");
        DAGNode<Component, Dependency> graph;
        if (cache == null) {
            logger.debug("Building directly without a cache");
            RecommenderInstantiator ri = RecommenderInstantiator.create(recommenderGraph);
            graph = ri.instantiate();
        } else {
            logger.debug("Instantiating graph with a cache");
            try {
                Set<DAGNode<Component, Dependency>> nodes = GraphtUtils.getShareableNodes(recommenderGraph);
                logger.debug("resolving {} nodes", nodes.size());
                graph = NodeProcessors.processNodes(recommenderGraph, nodes, cache);
                logger.debug("graph went from {} to {} nodes",
                             recommenderGraph.getReachableNodes().size(),
                             graph.getReachableNodes().size());
            } catch (InjectionException e) {
                logger.error("Error encountered while pre-processing algorithm components for sharing", e);
                throw new RecommenderBuildException("Pre-processing of algorithm components for sharing failed.", e);
            }
        }
        recommender = new LenskitRecommender(graph);
        // pre-fetch the test DAO
        userEvents = dataSet.getTestData().getUserEventDAO();
    }

    @Override
    protected <A> MetricWithAccumulator<A> makeMetricAccumulator(Metric<A> metric) {
        return new MetricWithAccumulator<A>(metric, metric.createContext(algorithmInfo, dataSet, recommender));
    }

    @Override
    protected TestUser getUserResults(long uid) {
        Preconditions.checkState(recommender != null, "recommender not built");
        UserHistory<Event> userData = userEvents.getEventsForUser(uid);
        return new LenskitTestUser(recommender, userData);
    }

    @Override
    protected void cleanup() {
        recommender = null;
        userEvents = null;
        // why do we clear the graph?
        recommenderGraph = null;
    }
}
