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
package org.grouplens.lenskit.mf.svdfeature;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class RadiusBasisFunctionKernel implements KernelFunction {
    public RadiusBasisFunctionKernel() {}

    public double getValue(double[] left, double[] right) {
        double[] sub = new double[left.length];
        ArrayHelper.copy(sub, left);
        ArrayHelper.subtraction(sub, right);
        double normv = ArrayHelper.innerProduct(sub, sub);
        return Math.exp(-normv);
    }

    public double[] getGradient(double[] left, double[] right, boolean side) {
        double[] grad = new double[left.length];
        double[] sub = new double[left.length];
        ArrayHelper.copy(sub, left);
        ArrayHelper.subtraction(sub, right);
        double normv = ArrayHelper.innerProduct(sub, sub);
        double val = Math.exp(-normv);
        ArrayHelper.copy(grad, sub);
        if (side == true) {
            ArrayHelper.scale(grad, -2 * val);
        } else {
            ArrayHelper.scale(grad, 2 * val);
        }
        return grad;
    }
}
