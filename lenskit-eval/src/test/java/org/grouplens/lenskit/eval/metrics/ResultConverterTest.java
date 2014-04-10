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
package org.grouplens.lenskit.eval.metrics;

import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class ResultConverterTest {
    @Test
    public void testEmptyType() {
        ResultConverter<String> foo = ResultConverter.create(String.class);
        assertThat(foo.getColumnLabels(), hasSize(0));
        assertThat(foo.getColumns("hello"), hasSize(0));
    }

    @Test
    public void testMethod() {
        ResultConverter<MethodResult> foo = ResultConverter.create(MethodResult.class);
        assertThat(foo.getColumnLabels(), contains("hello"));
        assertThat(foo.getColumns(new MethodResult()),
                   contains((Object) "world"));
    }

    @Test
    public void testField() {
        ResultConverter<FieldResult> foo = ResultConverter.create(FieldResult.class);
        assertThat(foo.getColumnLabels(), contains("hello"));
        assertThat(foo.getColumns(new FieldResult()),
                   contains((Object) "world"));
    }

    private static class MethodResult {
        @ResultColumn("hello")
        public String getHello() {
            return "world";
        }
    }

    private static class FieldResult {
        @ResultColumn("hello")
        public final String hello = "world";
    }
}
