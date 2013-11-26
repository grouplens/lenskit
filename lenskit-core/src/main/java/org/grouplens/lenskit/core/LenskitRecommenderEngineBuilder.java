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
package org.grouplens.lenskit.core;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang3.tuple.Pair;
import org.grouplens.grapht.graph.DAGNode;
import org.grouplens.grapht.solver.*;
import org.grouplens.grapht.spi.CachedSatisfaction;
import org.grouplens.grapht.spi.ContextMatcher;
import org.grouplens.lenskit.RecommenderBuildException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Builds LensKit recommender engines from configurations.
 *
 * <p>
 * If multiple configurations are used, later configurations superseded previous configurations.
 * This allows you to add a configuration of defaults, followed by a custom configuration.  The
 * final build process takes the <emph>union</emph> of the roots of all provided configurations as
 * the roots of the configured object graph.
 * </p>
 *
 * @see LenskitConfiguration
 * @see LenskitRecommenderEngine
 * @since 2.1
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class LenskitRecommenderEngineBuilder {
    private static final Logger logger = LoggerFactory.getLogger(LenskitRecommenderEngineBuilder.class);
    private List<Pair<LenskitConfiguration,ModelDisposition>> configurations = Lists.newArrayList();

    /**
     * Add a configuration to be included in the recommender engine.  This is the equivalent of
     * calling {@link #addConfiguration(LenskitConfiguration, ModelDisposition)} with the {@link ModelDisposition#INCLUDED}.
     * @param config The configuration.
     * @return The builder (for chaining).
     */
    public LenskitRecommenderEngineBuilder addConfiguration(LenskitConfiguration config) {
        return addConfiguration(config, ModelDisposition.INCLUDED);
    }

    /**
     * Add a configuration to be used when building the engine.
     * @param config The configuration.
     * @param disp The disposition for this configuration.
     * @return The builder (for chaining).
     */
    public LenskitRecommenderEngineBuilder addConfiguration(LenskitConfiguration config, ModelDisposition disp) {
        configurations.add(Pair.of(config, disp));
        return this;
    }

    /**
     * Build the recommender engine.
     *
     * @return The built recommender engine, with {@linkplain ModelDisposition#EXCLUDED excluded}
     *         components removed.
     * @throws RecommenderBuildException
     */
    public LenskitRecommenderEngine build() throws RecommenderBuildException {
        // Build the initial graph
        logger.debug("building graph from {} configurations", configurations.size());
        RecommenderGraphBuilder rgb = new RecommenderGraphBuilder();
        for (Pair<LenskitConfiguration,ModelDisposition> cfg: configurations) {
            rgb.addBindings(cfg.getLeft().getBindings());
            rgb.addRoots(cfg.getLeft().getRoots());
        }
        RecommenderInstantiator inst;
        try {
            inst = RecommenderInstantiator.create(rgb.getSPI(), rgb.buildGraph());
        } catch (SolverException e) {
            throw new RecommenderBuildException("Cannot resolve recommender graph", e);
        }
        DAGNode<CachedSatisfaction, DesireChain> graph = inst.instantiate();

        graph = rewriteGraph(graph);

        return new LenskitRecommenderEngine(graph, rgb.getSPI());
    }

    private DAGNode<CachedSatisfaction, DesireChain> rewriteGraph(DAGNode<CachedSatisfaction, DesireChain> graph) throws RecommenderConfigurationException {
        RecommenderGraphBuilder rewriteBuilder = new RecommenderGraphBuilder();
        rewriteBuilder.setBindingTransform(ReverseBindingTransform.INSTANCE);
        boolean rewrite = false;
        for (Pair<LenskitConfiguration,ModelDisposition> cfg: configurations) {
            switch (cfg.getRight()) {
            case EXCLUDED:
                rewriteBuilder.addBindings(cfg.getLeft().getBindings());
                rewriteBuilder.addRoots(cfg.getLeft().getRoots());
                rewrite = true;
                break;
            }
        }

        if (rewrite) {
            logger.debug("rewriting graph");
            DependencySolver rewriter = rewriteBuilder.buildDependencySolver();
            try {
                graph = rewriter.rewrite(graph);
            } catch (SolverException e) {
                throw new RecommenderConfigurationException("Resolution error while rewriting graph", e);
            }
        }
        return graph;
    }

    private static enum ReverseBindingTransform implements Function<BindingFunction,BindingFunction> {
        INSTANCE;

        @Nonnull
        @Override
        public BindingFunction apply(@Nullable BindingFunction bindFunction) {
            Preconditions.checkNotNull(bindFunction, "cannot apply to null binding function");
            if (bindFunction instanceof RuleBasedBindingFunction) {
                RuleBasedBindingFunction rbf = (RuleBasedBindingFunction) bindFunction;
                ListMultimap<ContextMatcher, BindRule> bindings = rbf.getRules();
                ListMultimap<ContextMatcher, BindRule> newBindings;
                newBindings = Multimaps.transformValues(bindings, new Function<BindRule, BindRule>() {
                    @Nullable
                    @Override
                    public BindRule apply(@Nullable BindRule rule) {
                        Preconditions.checkNotNull(rule, "cannot apply to null binding function");
                        assert rule != null;
                        BindRuleBuilder builder = rule.newCopyBuilder();
                        Class<?> type = builder.getImplementation();
                        if (builder.getSatisfaction() != null) {
                            type = builder.getSatisfaction().getErasedType();
                        }
                        return builder.setSatisfaction(new PlaceholderSatisfaction(type))
                                      .build();
                    }
                });
                return new RuleBasedBindingFunction(newBindings);
            } else {
                throw new IllegalArgumentException("cannot transform bind function " + bindFunction);
            }
        }

    }
}
