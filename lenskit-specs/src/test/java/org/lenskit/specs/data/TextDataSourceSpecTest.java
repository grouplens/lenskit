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
package org.lenskit.specs.data;

import org.junit.Test;
import org.lenskit.specs.SpecUtils;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class TextDataSourceSpecTest {
    @Test
    public void testBasicTextDataSource() {
        TextDataSourceSpec spec = new TextDataSourceSpec();
        spec.setName("foo");
        spec.setFile(Paths.get("wombat.csv"));

        assertThat(spec.getName(), equalTo("foo"));
        assertThat(spec.getFile(), equalTo(Paths.get("wombat.csv")));
        assertThat(spec.getDomain(), nullValue());
    }

    @Test
    public void testTextDataSourceRoundTrip() {
        TextDataSourceSpec spec = new TextDataSourceSpec();
        spec.setName("foo");
        spec.setFile(Paths.get("wombat.csv"));

        String json = SpecUtils.stringify(spec);

        DataSourceSpec s2 = SpecUtils.parse(DataSourceSpec.class, json);

        assertThat(s2, instanceOf(TextDataSourceSpec.class));

        TextDataSourceSpec tds2 = (TextDataSourceSpec) s2;

        assertThat(tds2.getName(), equalTo("foo"));
        assertThat(tds2.getFile(), equalTo(Paths.get("wombat.csv")));
        assertThat(tds2.getDomain(), nullValue());

        assertThat(tds2, equalTo(spec));
    }
}
