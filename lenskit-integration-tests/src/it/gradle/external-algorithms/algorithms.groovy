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


import org.lenskit.api.ItemScorer
import org.lenskit.baseline.ItemMeanRatingItemScorer
import org.lenskit.inject.Transient
import org.lenskit.data.dao.EventDAO
import org.lenskit.data.dao.UserDAO
import org.lenskit.eval.traintest.QueryData
import org.grouplens.lenskit.eval.metrics.predict.*
import org.lenskit.external.ExternalProcessItemScorerBuilder

import javax.inject.Inject
import javax.inject.Provider

/**
 * Shim class to run item-mean.py to build an ItemScorer.
 */
class ExternalItemMeanScorerBuilder implements Provider<ItemScorer> {
    EventDAO eventDAO
    UserDAO userDAO

    @Inject
    public ExternalItemMeanScorerBuilder(@Transient EventDAO events,
                                         @Transient @QueryData UserDAO users) {
        eventDAO = events
        userDAO = users
    }

    @Override
    ItemScorer get() {
        def wrk = new File("external-scratch")
        wrk.mkdirs()
        def builder = new ExternalProcessItemScorerBuilder()
        // Note: don't use file names because it will interact badly with crossfolding
        return builder.setWorkingDir(wrk)
                      .setExecutable("python")
                      .addArgument("../item-mean.py")
                      .addArgument("--for-users")
                      .addUserFileArgument(userDAO)
                      .addRatingFileArgument(eventDAO)
                      .build()
    }
}

/* externalAlgorithm("FullExternal") {
    command "python", "item-mean.py", "{TRAIN_DATA}", "{TEST_DATA}", "{OUTPUT}"
    workDir config.scriptDir
} */ // FIXME support external algorithms more cleanly
algorithm("Baseline") {
    bind ItemScorer to ItemMeanRatingItemScorer
}
algorithm("External") {
    bind ItemScorer toProvider ExternalItemMeanScorerBuilder
}
