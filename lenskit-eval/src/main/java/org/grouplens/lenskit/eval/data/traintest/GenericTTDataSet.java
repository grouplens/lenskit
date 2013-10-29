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
package org.grouplens.lenskit.eval.data.traintest;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.grouplens.lenskit.data.dao.EventDAO;
import org.grouplens.lenskit.data.pref.PreferenceDomain;
import org.grouplens.lenskit.eval.data.DataSource;
import org.grouplens.lenskit.eval.data.GenericDataSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;
import java.util.Collections;
import java.util.Map;

/**
 * A train-test data set backed by a pair of factories.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 * @since 0.8
 */
public class GenericTTDataSet implements TTDataSet {
    @Nonnull
    private final String name;
    @Nonnull
    private final DataSource trainData;
    @Nullable
    private final DataSource queryData;
    @Nonnull
    private final DataSource testData;
    @Nullable
    private final PreferenceDomain preferenceDomain;
    private final Map<String, Object> attributes;

    public GenericTTDataSet(@Nonnull String name,
                            @Nonnull DataSource train,
                            @Nullable DataSource query,
                            @Nonnull DataSource test,
                            Map<String, Object> attrs) {
        Preconditions.checkNotNull(train, "no training data");
        Preconditions.checkNotNull(test, "no test data");
        this.name = name;
        trainData = train;
        queryData = query;
        testData = test;
        preferenceDomain = trainData.getPreferenceDomain();
        if (attrs == null) {
            attributes = Collections.emptyMap();
        } else {
            attributes = Maps.newHashMap(attrs);
        }
    }

    /**
     * Create a new generic data set.
     *
     * @param name   The data set name.
     * @param train  The training DAO factory.
     * @param test   The test DAO factory.
     * @param domain The preference domain.
     */
    public GenericTTDataSet(@Nonnull String name,
                            @Nonnull Provider<EventDAO> train,
                            @Nullable Provider<EventDAO> query,
                            @Nonnull Provider<EventDAO> test,
                            @Nullable PreferenceDomain domain) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(train);
        Preconditions.checkNotNull(test);
        this.name = name;
        trainData = new GenericDataSource(name + ".train", train, domain);
        if (query != null) {
            queryData = new GenericDataSource(name + ".query", test, domain);
        } else {
            queryData = null;
        }
        testData = new GenericDataSource(name + ".test", test, domain);
        preferenceDomain = domain;
        attributes = Collections.singletonMap("DataSet", (Object) name);
    }

    @Override
    @Nonnull
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public long lastModified() {
        return Math.max(trainData.lastModified(),
                        testData.lastModified());
    }

    @Override
    public void release() {
        /* no-op */
    }

    @Override
    @Nullable
    public PreferenceDomain getPreferenceDomain() {
        return preferenceDomain;
    }

    @Override
    public EventDAO getTrainingDAO() {
        return trainData.getEventDAO();
    }

    @Override
    public EventDAO getQueryDAO() {
        return queryData.getEventDAO();
    }

    @Override
    public EventDAO getTestDAO() {
        return testData.getEventDAO();
    }

    @Override
    @Nonnull
    public DataSource getTestData() {
        return testData;
    }

    @Override
    @Nonnull
    public DataSource getTrainingData() {
        return trainData;
    }

    @Override
    public DataSource getQueryData() {
        return queryData;
    }

    @Override
    public String toString() {
        return String.format("{TTDataSet %s}", name);
    }

    /**
     * Create a new generic train-test data set builder.
     * @return The new builder.
     */
    public static GenericTTDataBuilder newBuilder() {
        return new GenericTTDataBuilder();
    }

    /**
     * Create a new generic train-test data set builder.
     * @param name The data set name.
     * @return The new builder.
     */
    public static GenericTTDataBuilder newBuilder(String name) {
        return new GenericTTDataBuilder(name);
    }

    /**
     * Create a new builder initialized with this data set's values.
     * @return A new builder initialized to make a copy of this data set definition.
     */
    public GenericTTDataBuilder copyBuilder() {
        GenericTTDataBuilder builder = newBuilder(getName());
        builder.setTest(getTestData())
               .setQuery(getQueryData())
               .setTrain(getTrainingData());
        for (Map.Entry<String,Object> attr: getAttributes().entrySet()) {
            builder.setAttribute(attr.getKey(), attr.getValue());
        }
        return builder;
    }
}
