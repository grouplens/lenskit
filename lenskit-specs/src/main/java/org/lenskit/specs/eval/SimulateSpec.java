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
package org.lenskit.specs.eval;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableSet;
import org.lenskit.specs.AbstractSpec;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Set;

/**
 * A spec for configuring an algorithm simulation.
 */
public class SimulateSpec extends AbstractSpec {
    private Path inputFile;
    private Path outputFile;
    private Path extendedOutputFile;
    private int listSize;
    private long rebuildPeriod;
    private AlgorithmSpec algorithm;

    public Path getInputFile() {
        return inputFile;
    }

    public void setInputFile(Path inputFile) {
        this.inputFile = inputFile;
    }

    @Nullable
    public Path getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(Path outputFile) {
        this.outputFile = outputFile;
    }

    @Nullable
    public Path getExtendedOutputFile() {
        return extendedOutputFile;
    }

    public void setExtendedOutputFile(Path extendedOutputFile) {
        this.extendedOutputFile = extendedOutputFile;
    }

    public int getListSize() {
        return listSize;
    }

    public void setListSize(int listSize) {
        this.listSize = listSize;
    }

    public long getRebuildPeriod() {
        return rebuildPeriod;
    }

    public void setRebuildPeriod(long rebuildPeriod) {
        this.rebuildPeriod = rebuildPeriod;
    }

    public AlgorithmSpec getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(AlgorithmSpec algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Get the output files for this task.
     * @return The task's output files.
     */
    @JsonIgnore
    public Set<Path> getOutputFiles() {
        ImmutableSet.Builder<Path> bld = ImmutableSet.builder();
        if (outputFile != null) {
            bld.add(outputFile);
        }
        if (extendedOutputFile != null) {
            bld.add(extendedOutputFile);
        }
        return bld.build();
    }
}
