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
package org.grouplens.lenskit.vectors;

import org.junit.Test;

import static org.grouplens.lenskit.util.test.ExtraMatchers.notANumber;
import static org.grouplens.lenskit.vectors.SparseVectorTestCommon.closeTo;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class VecTest {
    final Vec empty = ImmutableVec.create();
    final Vec single = ImmutableVec.create(3.5);
    final Vec v1 = ImmutableVec.create(1, 3, 5);
    final Vec v1c = ImmutableVec.create(1, 3, 5);
    final Vec v2 = ImmutableVec.create(2, 3, 4);
    final MutableVec strided = MutableVec.wrap(new double[] {0, 1, 0, 3, 0, 5, 0, 0, 0})
                                         .subVector(1, 3, 2);

    @Test
    public void testDim() {
        assertThat(empty.size(), equalTo(0));
        assertThat(single.size(), equalTo(1));
        assertThat(v2.size(), equalTo(3));
        assertThat(v1c.size(), equalTo(3));
    }

    @Test
    public void testGet() {
        assertThat(single.get(0), closeTo(3.5));
        assertThat(v1.get(0), closeTo(1));
        assertThat(v1.get(1), closeTo(3));
        assertThat(v1.get(2), closeTo(5));
        try {
            v1.get(3);
            fail("out of bounds must fail");
        } catch (IndexOutOfBoundsException e) {
            /* expected */
        }
    }

    @Test
    public void testSum() {
        assertThat(empty.sum(), closeTo(0));
        assertThat(single.sum(), closeTo(3.5));
        assertThat(v1.sum(), closeTo(9));
        assertThat(v2.sum(), closeTo(9));
    }

    @Test
    public void testNorm() {
        assertThat(empty.norm(), closeTo(0));
        assertThat(single.norm(), closeTo(3.5));
        assertThat(v1.norm(), closeTo(5.9160798));
    }

    @Test
    public void testMean() {
        assertThat(empty.mean(), notANumber());
        assertThat(single.mean(), closeTo(3.5));
        assertThat(v1.mean(), closeTo(3));
    }

    @Test
    public void testDot() {
        assertThat(v1.dot(v2), closeTo(2 + 9 + 20));
        assertThat(v1.dot(v1c), closeTo(v1.norm() * v1c.norm()));
        try {
            v1.dot(single);
            fail("dot product with different-dimensioned vector should fail");
        } catch (IllegalArgumentException e) {
            /* expected */
        }
    }

    @Test
    public void testEquals() {
        assertThat(v1.equals(v1c), equalTo(true));
        assertThat(v1.equals(v2), equalTo(false));
        assertThat(v1.equals(v1), equalTo(true));
        assertThat(v1.equals(single), equalTo(false));
        assertThat(v1.equals(empty), equalTo(false));
        assertThat(v1.equals("foo"), equalTo(false));
    }

    @Test
    public void testHashCode() {
        assertThat(v1.hashCode(), equalTo(v1c.hashCode()));
        assertThat(empty.hashCode(), not(equalTo(v1c.hashCode())));
    }

    @Test
    public void testMutate() {
        MutableVec mv = v1.mutableCopy();
        assertThat(mv.sum(), closeTo(9));
        assertThat(mv, allOf(equalTo(v1),
                             not(sameInstance(v1))));
        double v = mv.set(1, 2);
        assertThat(v, closeTo(3));
        assertThat(mv, not(equalTo(v1)));
        assertThat(mv.get(1), closeTo(2));
        assertThat(mv.sum(), closeTo(8));
    }

    @Test
    public void testImmutable() {
        MutableVec mv = v1.mutableCopy();
        mv.set(1, 2);
        Vec iv1 = mv.immutable();
        assertThat(iv1, equalTo((Vec) mv));
        assertThat(iv1, instanceOf(ImmutableVec.class));
        assertThat(iv1.immutable(), sameInstance(iv1));
    }

    @Test
    public void testAddVector() {
        MutableVec mv = MutableVec.wrap(new double[]{3, 2, 5});
        assertThat(mv.sum(), closeTo(10));
        mv.add(v1);
        assertThat(mv.get(0), closeTo(4));
        assertThat(mv.get(1), closeTo(5));
        assertThat(mv.get(2), closeTo(10));
        assertThat(mv.sum(), closeTo(19));
    }

    @Test
    public void testAddValue() {
        MutableVec mv = v1.mutableCopy();
        double ov = mv.add(1, 2);
        assertThat(ov, closeTo(3));
        assertThat(mv.get(1), closeTo(5));
    }

    @Test
    public void testScale() {
        MutableVec mv = MutableVec.wrap(new double[]{1, 2, 3});
        assertThat(mv.sum(), closeTo(6));
        mv.scale(2);
        assertThat(mv.sum(), closeTo(12));
        assertThat(mv.get(0), closeTo(2));
        assertThat(mv.get(1), closeTo(4));
        assertThat(mv.get(2), closeTo(6));
    }

    @Test
    public void testNewMutableVector() {
        MutableVec v = MutableVec.create(5);
        assertThat(v.size(), equalTo(5));
        for (int i = 0; i < 5; i++) {
            assertThat(v.get(i), closeTo(0));
        }
        assertThat(v.sum(), closeTo(0));
    }

    @Test
    public void testSetVec() {
        MutableVec mv = v1.mutableCopy();
        mv.set(v2);
        assertThat(mv, equalTo(v2));
    }

    @Test
    public void testSetVecMismatch() {
        MutableVec mv = v1.mutableCopy();
        try {
            mv.set(single);
            fail("set with mismatched vector dimension should fail");
        } catch (IllegalArgumentException e) {
            /* expected */
        }
    }

    @Test
    public void testSetArray() {
        MutableVec mv = v1.mutableCopy();
        mv.set(101, 102, 103);
        assertThat(mv.get(0), closeTo(101));
        assertThat(mv.get(1), closeTo(102));
        assertThat(mv.get(2), closeTo(103));
    }

    @Test
    public void testSetArrayMismatch() {
        MutableVec mv = v1.mutableCopy();
        try {
            mv.set(new double[1]);
            fail("set with mismatched array dimension should fail");
        } catch (IllegalArgumentException e) {
            /* expected */
        }
    }

    @Test
    public void testFreeze() {
        MutableVec mv = v1.mutableCopy();
        ImmutableVec frozen = mv.freeze();
        assertThat(frozen, equalTo(v1));
        try {
            mv.set(0, 5);
            fail("set on frozen vector should fail");
        } catch (IllegalStateException ex) {
            /* expected */
        }

        try {
            mv.set(0.5, 5.2, 0.3);
            fail("set on frozen vector should fail");
        } catch (IllegalStateException ex) {
            /* expected */
        }

        try {
            mv.add(ImmutableVec.create(0.2, 3.9, 1.7));
            fail("set on frozen vector should fail");
        } catch (IllegalStateException ex) {
            /* expected */
        }
    }

    @Test
    public void testStridedEquals() {
        assertThat(strided, equalTo(v1));
    }

    @Test
    public void testStridedAdd() {
        MutableVec mv = v2.mutableCopy();
        mv.add(strided);
        strided.add(v2);

        Vec result = ImmutableVec.create(3, 6, 9);
        assertThat(mv, equalTo(result));
        assertThat(strided, equalTo(result));
    }

    @Test
    public void testStridedSet() {
        MutableVec mv = v2.mutableCopy();
        mv.set(strided);
        strided.set(v2);

        assertThat(mv, equalTo(v1));
        assertThat(strided, equalTo(v2));
    }

    @Test
    public void testCopyStride() {
        MutableVec mv = strided.mutableCopy();
        ImmutableVec imv = strided.immutable();
        assertThat(mv, equalTo(strided));
        assertThat(mv, equalTo(v1));
        assertThat(imv, equalTo(v1));
        mv.add(v2);
        assertThat(mv.get(0), equalTo(3.0));
        assertThat(mv.get(1), equalTo(6.0));
        assertThat(mv.get(2), equalTo(9.0));
    }

    @Test
    public void testSubStride() {
        MutableVec ssv = strided.subVector(0, 2, 2);
        assertThat(ssv.size(), equalTo(2));
        assertThat(ssv.get(0), equalTo(1.0));
        assertThat(ssv.get(1), equalTo(5.0));
        try {
            ssv.get(2);
            fail("out-of-bounds get should throw");
        } catch (IndexOutOfBoundsException e) {
            /* expected */
        }
        ssv.set(1, Math.PI);
        assertThat(strided.get(2), equalTo(Math.PI));
    }
}
