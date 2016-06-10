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
package org.lenskit.cli.util;

import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import org.lenskit.LenskitConfiguration;
import org.lenskit.data.dao.EventDAO;
import org.grouplens.lenskit.data.source.DataSource;
import org.grouplens.lenskit.data.source.PackedDataSourceBuilder;
import org.grouplens.lenskit.data.source.TextDataSourceBuilder;
import org.grouplens.lenskit.data.text.DelimitedColumnEventFormat;
import org.grouplens.lenskit.data.text.EventFormat;
import org.grouplens.lenskit.data.text.Formats;
import org.lenskit.data.dao.ItemNameDAO;
import org.lenskit.specs.SpecUtils;
import org.lenskit.specs.data.DataSourceSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

/**
 * Helper class for managing input data.
 *
 * @since 2.1
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class InputData {
    private static final Logger logger = LoggerFactory.getLogger(InputData.class);
    private final Namespace options;
    private final ScriptEnvironment environment;

    public InputData(ScriptEnvironment env, Namespace opts) {
        environment = env;
        options = opts;
    }

    @Nullable
    public DataSource getSource() {
        TextDataSourceBuilder dsb = new TextDataSourceBuilder();

        File sourceFile = options.get("data_source");
        if (sourceFile != null) {
            ClassLoader cl = null;
            if (environment != null) {
                cl = environment.getClassLoader();
            }
            DataSourceSpec spec;
            try {
                spec = SpecUtils.load(DataSourceSpec.class, sourceFile.toPath());
            } catch (IOException e) {
                logger.error("error loading " + sourceFile, e);
                throw new RuntimeException("error loading " + sourceFile, e);
            }
            return SpecUtils.buildObject(DataSource.class, spec, cl);
        }

        String type = options.get("event_type");
        File nameFile = options.get("item_names");
        Integer header = options.get("header_lines");
        if (nameFile != null) {
            dsb.setItemNameFile(nameFile);
        }
        File ratingFile = options.get("csv_file");
        if (ratingFile != null) {
            return dsb.setFile(ratingFile)
                      .setFormat(Formats.csvRatings().setHeaderLines(header))
                      .build();
        }

        ratingFile = options.get("tsv_file");
        if (ratingFile != null) {
            return dsb.setFile(ratingFile)
                      .setFormat(Formats.delimitedRatings("\t").setHeaderLines(header))
                      .build();
        }

        ratingFile = options.get("ratings_file");
        if (ratingFile == null) {
            ratingFile = options.get("events_file");
        }
        if (ratingFile != null) {
            String delim = options.getString("delimiter");
            EventFormat fmt = DelimitedColumnEventFormat.create(type)
                                                        .setDelimiter(delim)
                                                        .setHeaderLines(header);
            return dsb.setFormat(fmt)
                      .setFile(ratingFile)
                      .build();
        }

        File packFile = options.get("pack_file");
        if (packFile != null) {
            if (nameFile != null) {
                logger.warn("item name file ignored for packed rating input");
            }
            return new PackedDataSourceBuilder(packFile).build();
        }

        return null;
    }

    @Nullable
    public EventDAO getEventDAO() throws IOException {
        DataSource src = getSource();
        return (src == null) ? null : src.getEventDAO();
    }

    @Nonnull
    public LenskitConfiguration getConfiguration() {
        DataSource src = getSource();
        LenskitConfiguration config = new LenskitConfiguration();
        if (src != null) {
            src.configure(config);
        }
        config.addRoot(ItemNameDAO.class);
        return config;
    }

    @Override
    public String toString() {
        DataSource src = getSource();
        return (src == null) ? "null" : src.toString();
    }

    public static void configureArguments(ArgumentParser parser) {
        configureArguments(parser, false);
    }

    public static void configureArguments(ArgumentParser parser, boolean required) {
        MutuallyExclusiveGroup group =
                parser.addMutuallyExclusiveGroup("input data")
                      .description("Specify the input data for the command.")
                      .required(required);
        ArgumentGroup options = parser.addArgumentGroup("input options")
                                      .description("Additional options for input data.");
        group.addArgument("--csv-file")
             .type(File.class)
             .metavar("FILE")
             .help("read from comma-separated FILE");
        group.addArgument("--tsv-file")
             .type(File.class)
             .metavar("FILE")
             .help("read from tab-separated FILE");
        group.addArgument("--ratings-file")
             .type(File.class)
             .metavar("FILE")
             .help("read from delimited text FILE");
        group.addArgument("--events-file")
             .type(File.class)
             .metavar("FILE")
             .help("read from delimited text FILE");
        options.addArgument("-d", "--delimiter")
               .setDefault(",")
               .metavar("DELIM")
               .help("input file is delimited by DELIM");
        options.addArgument("-H", "--header-lines")
               .type(Integer.class)
               .setDefault(0)
               .metavar("N")
               .help("skip N header lines at top of input file");
        options.addArgument("-t", "--event-type")
               .setDefault("rating")
               .metavar("TYPE")
               .help("read events of type TYPE from input file");
        options.addArgument("--item-names")
               .type(File.class)
               .metavar("FILE")
               .help("Read item names from CSV file FILE");
        group.addArgument("--pack-file")
             .type(File.class)
             .metavar("FILE")
             .help("read from binary packed FILE");
        group.addArgument("--data-source")
             .type(File.class)
             .metavar("FILE")
             .help("read a data source specification from FILE");
    }
}
