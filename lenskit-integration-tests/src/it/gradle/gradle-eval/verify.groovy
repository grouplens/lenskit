/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2014 Regents of the University of Minnesota and contributors
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
import static org.grouplens.lenskit.util.test.ExtraMatchers.existingFile
import static org.grouplens.lenskit.util.test.ExtraMatchers.hasLineCount
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.equalTo

File resultsFile = new File("results.csv")
File userFile = new File("users.csv")
File predictFile = new File("predictions.csv")
File recommendFile = new File("recommendations.csv.gz")

assertThat("output file existence",
           resultsFile, allOf(existingFile(),
                              hasLineCount(equalTo(6))));
assertThat("output file existence",
           userFile, existingFile());
assertThat("output file existence",
           predictFile, existingFile());
assertThat("output file existence",
           recommendFile, existingFile());

assertThat("output file existence",
           new File("deprecated-predictions.csv"), existingFile());
assertThat("output file existence",
           new File("deprecated-recommendations.csv.gz"), existingFile());

assertThat(new File('train.pack/part01.train.pack'),
           existingFile())
