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
package org.grouplens.lenskit.eval.traintest;

import com.google.common.base.Throwables;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import org.grouplens.lenskit.Recommender;
import org.grouplens.lenskit.collections.LongUtils;
import org.grouplens.lenskit.eval.Attributed;
import org.grouplens.lenskit.eval.data.traintest.TTDataSet;
import org.grouplens.lenskit.eval.metrics.AbstractMetric;
import org.grouplens.lenskit.util.table.TableLayout;
import org.grouplens.lenskit.util.table.TableLayoutBuilder;
import org.grouplens.lenskit.util.table.writer.CSVWriter;
import org.grouplens.lenskit.util.table.writer.TableWriter;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Predict metric that writes predictions to a file.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class OutputPredictMetric extends AbstractMetric<OutputPredictMetric.Context, Void, Void> {
    private static final Logger logger = LoggerFactory.getLogger(OutputPredictMetric.class);

    private final ExperimentOutputLayout outputLayout;
    private final TableWriter tableWriter;

    public OutputPredictMetric(ExperimentOutputLayout layout, File file) throws IOException {
        super(Void.TYPE, Void.TYPE);
        outputLayout = layout;

        TableLayout recLayout = TableLayoutBuilder.copy(layout.getCommonLayout())
                                                  .addColumn("User")
                                                  .addColumn("Item")
                                                  .addColumn("Rating")
                                                  .addColumn("Prediction")
                                                  .build();
        tableWriter = CSVWriter.open(file, recLayout);
    }

    @Override
    public Context createContext(Attributed algo, TTDataSet ds, Recommender rec) {
        return new Context(outputLayout.prefixTable(tableWriter, algo, ds));
    }

    @Override
    public Void doMeasureUser(TestUser user, Context context) {
        SparseVector ratings = user.getTestRatings();
        SparseVector predictions = user.getPredictions();
        if (predictions == null) {
            predictions = MutableSparseVector.create();
        }

        LongSortedSet items = ratings.keySet();
        if (!items.containsAll(predictions.keySet())) {
            items = LongUtils.setUnion(items, predictions.keySet());
        }

        logger.debug("outputting {} predictions for user {}", predictions.size(), user.getUserId());
        for (LongIterator iter = items.iterator(); iter.hasNext(); /* no increment */) {
            long item = iter.nextLong();
            Double rating = ratings.containsKey(item) ? ratings.get(item) : null;
            Double pred = predictions.containsKey(item) ? predictions.get(item) : null;
            try {
                context.writer.writeRow(user.getUserId(), item, rating, pred);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
        return null;
    }

    @Override
    protected Void getTypedResults(Context context) {
        return null;
    }

    @Override
    public void close() throws IOException {
        tableWriter.close();
    }

    public static class Context {
        private final TableWriter writer;

        Context(TableWriter tw) {
            writer = tw;
        }
    }

    public static class Factory extends MetricFactory<Context> {
        @Override
        public OutputPredictMetric createMetric(TrainTestEvalTask task) throws IOException {
            return new OutputPredictMetric(task.getOutputLayout(), task.getPredictOutput());
        }

        @Override
        public List<String> getColumnLabels() {
            return Collections.emptyList();
        }

        @Override
        public List<String> getUserColumnLabels() {
            return Collections.emptyList();
        }
    }
}
