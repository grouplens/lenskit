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
package org.lenskit.eval.temporal;

import com.google.common.base.Preconditions;
import org.grouplens.lenskit.util.io.CompressionMode;
import org.lenskit.LenskitConfiguration;
import org.lenskit.LenskitRecommenderEngine;
import org.lenskit.ModelDisposition;
import org.lenskit.api.Recommender;
import org.lenskit.api.RecommenderBuildException;
import org.lenskit.api.Result;
import org.lenskit.data.dao.SortOrder;
import org.lenskit.data.packed.BinaryRatingDAO;
import org.lenskit.eval.traintest.AlgorithmInstance;
import org.lenskit.util.io.ObjectStreams;
import org.lenskit.data.ratings.Rating;
import org.lenskit.util.table.TableLayout;
import org.lenskit.util.table.TableLayoutBuilder;
import org.lenskit.util.table.writer.CSVWriter;
import org.lenskit.util.table.writer.TableWriter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.sqrt;

public class TemporalEvaluator {
    private BinaryRatingDAO dataSource;
    private AlgorithmInstance algorithm;
    private File predictOutputFile;
    private Long rebuildPeriod;

    /**
     * Adds an algorithmInfo
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
     * @param name   Name of algorithm instance
     * @param config Lenskit configuration
     */
    public TemporalEvaluator setAlgorithm(String name, LenskitConfiguration config) {
        algorithm = new AlgorithmInstance(name, config);
        return this;
    }


    /**
     * @param dao The datasource to be added to the command.
     * @return Itself to allow for  method chaining.
     */
    public TemporalEvaluator setDataSource(BinaryRatingDAO dao) {
        dataSource = dao;
        return this;
    }

    /**
     * @param file The file contains the ratings input data
     * @return itself
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

    /**
     * *
     *
     * @param file  The file set as the output of the command
     * @param cMode Compression Mode
     * @return Itself for  method chaining
     */

    //TODO Set compression mode in file
    public TemporalEvaluator setPredictOutputFile(File file, CompressionMode cMode) {
        predictOutputFile = file;
        return this;
    }

    /**
     * @param time The time to rebuild
     * @param unit Unit of time set
     * @return Itself for  method chaining
     */
    public TemporalEvaluator setRebuildPeriod(Long time, TimeUnit unit) {
        rebuildPeriod = unit.toSeconds(time);
        return this;
    }

    /**
     * @param seconds default rebuild period in seconds
     * @return Itself for  method chaining
     */
    public TemporalEvaluator setRebuildPeriod(Long seconds) {
        rebuildPeriod = seconds;
        return this;
    }

    /**
     * @return Returns prediction output file
     */
    public File getPredictOutputFile() {
        return predictOutputFile;
    }

    /**
     * @return Returns rebuild period
     */
    public Long getRebuildPeriod() {
        return rebuildPeriod;
    }

    /**
     * During the evaluation, it will replay the ratings, try to predict each one, and
     * write the prediction, TARMSE and the rating to the output file
     *
     * @return Itself for  method chaining
     */

    public void execute() throws IOException, RecommenderBuildException {
        Preconditions.checkState(algorithm != null, "no algorithm specified");
        Preconditions.checkState(dataSource != null, "no input data specified");
        Preconditions.checkState(predictOutputFile != null, "no output file specified");
        //Builds file layout
        TableLayoutBuilder tlb = new TableLayoutBuilder();
        //file headers
        tlb.addColumn("User")
           .addColumn("Item")
           .addColumn("Rating")
           .addColumn("Timestamp")
           .addColumn("Prediction")
           .addColumn("TARMSE");

        TableLayout tl = tlb.build();

        try (TableWriter tableWriter = CSVWriter.open(predictOutputFile, tl, CompressionMode.AUTO)) {
            List<Rating> ratings = ObjectStreams.makeList(dataSource.streamEvents(Rating.class, SortOrder.TIMESTAMP));
            BinaryRatingDAO limitedDao = dataSource.createWindowedView(0);

            LenskitRecommenderEngine lre = null;
            Recommender recommender = null;
            double sse = 0;
            int n = 0;
            long buildTime = 0L;

            for (Rating r : ratings) {
                if (r.getTimestamp() > 0 && limitedDao.getLimitTimestamp() < r.getTimestamp()) {
                    limitedDao = dataSource.createWindowedView(r.getTimestamp());
                    LenskitConfiguration config = new LenskitConfiguration();
                    config.addComponent(limitedDao);

                    if ((r.getTimestamp() - buildTime >= rebuildPeriod) || lre == null) {
                        buildTime = r.getTimestamp();
                        lre = LenskitRecommenderEngine.newBuilder()
                                                      .addConfiguration(algorithm.getConfigurations().get(0))
                                                      .addConfiguration(config, ModelDisposition.EXCLUDED)
                                                      .build();
                    }
                    recommender = lre.createRecommender(config);
                }
                //gets prediction score
                Result predictionResult = recommender.getRatingPredictor().predict(r.getUserId(), r.getItemId());
                double predict = Double.NaN;
                if (predictionResult != null) {
                    predict = predictionResult.getScore();
                }
                //calculates time averaged RMSE
                if (!Double.isNaN(predict)) {
                    double err = predict - r.getValue();
                    sse += err * err;
                    n++;
                }
                double rmse = 0.0;
                if (n > 0) {
                    rmse = sqrt(sse / n);
                }
                //writes the Prediction Score and TARMSE on file
                tableWriter.writeRow(r.getUserId(), r.getItemId(), r.getValue(), r.getTimestamp(), predict, rmse);
            }
        }
    }
}

