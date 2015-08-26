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
package org.grouplens.lenskit.eval.temporal;

import com.google.common.base.Preconditions;
import org.grouplens.lenskit.Recommender;
import org.grouplens.lenskit.RecommenderBuildException;
import org.grouplens.lenskit.core.LenskitConfiguration;
import org.grouplens.lenskit.core.LenskitRecommenderEngine;
import org.grouplens.lenskit.core.ModelDisposition;
import org.grouplens.lenskit.cursors.Cursors;
import org.grouplens.lenskit.data.dao.SortOrder;
import org.grouplens.lenskit.data.dao.packed.BinaryRatingDAO;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.eval.algorithm.AlgorithmInstance;
import org.grouplens.lenskit.util.io.CompressionMode;
import org.grouplens.lenskit.util.table.TableLayout;
import org.grouplens.lenskit.util.table.TableLayoutBuilder;
import org.grouplens.lenskit.util.table.writer.CSVWriter;
import org.grouplens.lenskit.util.table.writer.TableWriter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class TemporalEvaluator {
    private BinaryRatingDAO dataSource;
    private AlgorithmInstance algorithm;
    private File predictOutputFile;
    private Long rebuildPeriod;
    private TableWriter tableWriter;

    /**
     * Adds an algorithmInfo
     * <p>
     * If any exception is thrown while the command is called it is rethrown as a runtime error.
     *
     * @param algo The algorithmInfo added
     * @return Itself to allow  chaining
     */
    public TemporalEvaluator setAlgorithm(AlgorithmInstance algo) {
        algorithm = algo;
        return this;
    }

    /**
     * An algorithm instance constructed with a name and Lenskit configuration
     *
     * @param name
     * @param config Lenskit configuration
     */
    public TemporalEvaluator setAlgorithm(String name, LenskitConfiguration config) {
        algorithm = new AlgorithmInstance(name, config);
        return this;
    }


    /*
     * Adds a single {@code TTDataSet}
     * @param data The dataset to be added to the command.
     * @return Itself to allow for  method chaining.
     */
    public TemporalEvaluator setDataSource(BinaryRatingDAO dao) {
        dataSource = dao;
        return this;
    }

    /**
     * @param file
     * @return
     */
    public TemporalEvaluator setDataSource(File file) throws IOException {
        dataSource = BinaryRatingDAO.open(file);
        return this;
    }

    /**
     * @param file The file set as the output of the command
     * @return Itself for  method chaining
     */
    public TemporalEvaluator setPredictOutputFile(File file) {
        predictOutputFile = file;
        return this;
    }

    public TemporalEvaluator setPredictOutputFile(File file, CompressionMode cMode) {
        predictOutputFile = file;
        return this;
    }

    /**
     * @param time
     * @param unit
     * @return
     */
    public TemporalEvaluator setRebuildPeriod(Long time, TimeUnit unit) {
        rebuildPeriod = unit.toSeconds(time);
        return this;
    }

    /**
     * @param seconds
     * @return
     */
    public TemporalEvaluator setRebuildPeriod(Long seconds) {
        rebuildPeriod = seconds;
        return this;
    }

    /**
     * Returns prediction output file
     *
     * @return
     */
    public File getPredictOutputFile() {
        return predictOutputFile;
    }

    /**
     * Returns rebuild period
     *
     * @return
     */
    public Long getRebuildPeriod() {
        return rebuildPeriod;
    }

    /**
     * when the evaluation is run, it will replay the ratings, try to predict each one, and
     * write the prediction and the rating to the output file (using a TableWriter)
     *
     * @return
     */

    public TemporalEvaluator execute() throws IOException, RecommenderBuildException {
        Preconditions.checkState(algorithm != null, "no algorithm specified");
        Preconditions.checkState(dataSource != null, "no input data specified");
        Preconditions.checkState(predictOutputFile != null, "no output file specified");

        TableLayoutBuilder tlb = new TableLayoutBuilder();
        tlb.addColumn("User")
                .addColumn("Item")
                .addColumn("Rating")
                .addColumn("Timestamp")
                .addColumn("Prediction");

        TableLayout tl = tlb.build();
        tableWriter = CSVWriter.open(predictOutputFile, tl, CompressionMode.AUTO);

        List<Rating> ratings = Cursors.makeList(dataSource.streamEvents(Rating.class, SortOrder.TIMESTAMP));
        BinaryRatingDAO limitedDao = dataSource.createWindowedView(0);
        LenskitRecommenderEngine lre;
        Recommender recommender;

        try {
            for (Rating r : ratings) {
                if (r.getTimestamp() > 0 && limitedDao.getLimitTimestamp() < r.getTimestamp()) {
                    limitedDao = dataSource.createWindowedView(r.getTimestamp());
                }
                LenskitConfiguration config = new LenskitConfiguration();
                config.addComponent(limitedDao);
                lre = LenskitRecommenderEngine.newBuilder()
                        .addConfiguration(algorithm.getConfig())
                        .addConfiguration(config, ModelDisposition.EXCLUDED)
                        .build();
                recommender = lre.createRecommender(config);
                double prediction = recommender.getRatingPredictor().predict(r.getUserId(), r.getItemId());
                tableWriter.writeRow(r.getUserId(), r.getItemId(), r.getValue(), r.getTimestamp(), prediction);
            }
        } finally {
            tableWriter.close();
        }
        return this;
    }
}

