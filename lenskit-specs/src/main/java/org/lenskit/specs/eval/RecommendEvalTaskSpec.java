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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Top-N evaluation specification.
 */
public class RecommendEvalTaskSpec extends EvalTaskSpec {
    private Path outputFile;
    private String labelPrefix;
    private int listSize = -1;
    private String candidateItems;
    private String excludeItems;

    /**
     * Get the prefix applied to column labels.
     * @return The column label prefix.
     */
    public String getLabelPrefix() {
        return labelPrefix;
    }

    /**
     * Set the prefix applied to column labels.  If provided, it will be prepended to column labels from this task,
     * along with a ".".
     * @param prefix The label prefix.
     */
    public void setLabelPrefix(String prefix) {
        labelPrefix = prefix;
    }

    /**
     * Get the recommendation output file.
     * @return The recommendation output file.
     */
    public Path getOutputFile() {
        return outputFile;
    }

    /**
     * Set the recommendation output file.
     * @param outputFile The recommendation output file.
     */
    public void setOutputFile(Path outputFile) {
        this.outputFile = outputFile;
    }

    @JsonIgnore
    @Override
    public Set<Path> getOutputFiles() {
        Set<Path> files = new HashSet<>();
        if (outputFile != null) {
            files.add(outputFile);
        }
        return files;
    }

    /**
     * Get the number of items to recommend per user.
     * @return The number of items to recommend per user.
     */
    public int getListSize() {
        return listSize;
    }

    /**
     * Set the number of items to recommend per user.
     * @param n The number of items to recommend per user.
     */
    public void setListSize(int n) {
        listSize = n;
    }

    /**
     * Get the candidate item selector.
     * @return The selector expression for candidate items.
     */
    public String getCandidateItems() {
        return candidateItems;
    }

    /**
     * Set the candidate item selector.
     * @param candidates The selector expression for candidate items.  Can be `null` to use the default.
     */
    public void setCandidateItems(String candidates) {
        candidateItems = candidates;
    }

    /**
     * Get the candidate item selector.
     * @return The selector expression for exclude items.
     */
    public String getExcludeItems() {
        return excludeItems;
    }

    /**
     * Get the exclude item selector.
     * @param excludeItems The selector expression for exclude items.
     */
    public void setExcludeItems(String excludeItems) {
        this.excludeItems = excludeItems;
    }
}
