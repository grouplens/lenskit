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
package org.grouplens.lenskit.data.pref;

import org.junit.Test;
import org.lenskit.specs.SpecUtils;
import org.lenskit.specs.data.PrefDomainSpec;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class PreferenceDomainTest {
    @Test
    public void testParseContinuous() {
        PreferenceDomain d = PreferenceDomain.fromString("[1.0,3.0]");
        assertThat(d.getMinimum(), closeTo(1.0, 1.0e-6));
        assertThat(d.getMaximum(), closeTo(3.0, 1.0e-6));
        assertFalse(d.hasPrecision());
        assertThat(d.getPrecision(), equalTo(0.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInverted() {
        PreferenceDomain.fromString("[2.5, -1]");
    }

    @Test
    public void testParseDiscrete() {
        PreferenceDomain d = PreferenceDomain.fromString("[1.0,5.0]/0.5");
        assertThat(d.getMinimum(), closeTo(1.0, 1.0e-6));
        assertThat(d.getMaximum(), closeTo(5.0, 1.0e-6));
        assertTrue(d.hasPrecision());
        assertThat(d.getPrecision(), equalTo(0.5));
    }

    @Test
    public void testParseInt() {
        PreferenceDomain d = PreferenceDomain.fromString("[ 1 , 5 ] / 1");
        assertThat(d.getMinimum(), closeTo(1.0, 1.0e-6));
        assertThat(d.getMaximum(), closeTo(5.0, 1.0e-6));
        assertTrue(d.hasPrecision());
        assertThat(d.getPrecision(), equalTo(1.0));
    }

    @Test
    public void testBuildFromSpec() {
        PrefDomainSpec spec = new PrefDomainSpec();
        spec.setMinimum(1.0);
        spec.setMaximum(10.0);
        spec.setPrecision(0.2);

        PreferenceDomain dom = SpecUtils.buildObject(PreferenceDomain.class, spec);
        assertThat(dom, notNullValue());

        assertThat(dom.getMinimum(), equalTo(1.0));
        assertThat(dom.getMaximum(), equalTo(10.0));
        assertThat(dom.getPrecision(), equalTo(0.2));
    }
}
