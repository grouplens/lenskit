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
package org.lenskit.gradle.traits

import org.gradle.api.Project
import org.lenskit.gradle.delegates.SpecDelegate
import org.lenskit.specs.data.DataSourceSpec
import org.lenskit.specs.data.PackedDataSourceSpec
import org.lenskit.specs.data.TextDataSourceSpec

/**
 * Support for specifying various types of data sources.
 */
trait DataSources {
    abstract Project getProject()

    /**
     * Configure a text file data source.
     * @param block The configuration block, used to configureSpec a {@link TextDataSourceSpec}.
     * @return A JSON specification of a text file data source.
     * @see TextDataSourceSpec
     * @see SpecDelegate
     */
    DataSourceSpec textFile(Closure block) {
        SpecDelegate.configureSpec(project, TextDataSourceSpec, block)
    }

    /**
     * Configure a rating CSV file data source.
     * @param fn The file to use.
     * @return A JSON specification of a text file data source.
     * @see TextDataSourceSpec
     * @see SpecDelegate
     */
    DataSourceSpec textFile(Object fn) {
        def f = project.file(fn)
        return textFile {
            file f.toPath()
        }
    }

    /**
     * Construct a data source for a pack file.
     * @param fn The name of the pack file.
     * @return The data source spec.
     */
    DataSourceSpec packFile(Object fn) {
        def f = project.file(fn)
        def spec = new PackedDataSourceSpec()
        spec.file = f.toPath()
        return spec
    }
}
