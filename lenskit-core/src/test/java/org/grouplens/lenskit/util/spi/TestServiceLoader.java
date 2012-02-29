/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2011 Regents of the University of Minnesota
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
package org.grouplens.lenskit.util.spi;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ServiceLoader;

import org.junit.Test;

import com.google.common.collect.Iterators;

/**
 * Make sure that we can use {@link ServiceLoader} on the classpath.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class TestServiceLoader {

    @Test
    public void test() {
        ServiceLoader<DummyInterface> loader = ServiceLoader.load(DummyInterface.class);
        DummyInterface[] dummies = Iterators.toArray(loader.iterator(), DummyInterface.class);
        assertThat(dummies.length, greaterThanOrEqualTo(1));
        boolean found = false;
        for (DummyInterface impl: dummies) {
            if (impl instanceof DummyImpl) {
                found = true;
                assertEquals("FOOBIE BLETCH", impl.getMessage());
            }
        }
        assertTrue("could not find implementation", found);
    }

}
