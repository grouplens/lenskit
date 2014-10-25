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
package org.grouplens.lenskit.data.text;

import com.google.common.base.Preconditions;
import org.grouplens.lenskit.cursors.AbstractPollingCursor;
import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.data.event.MutableRating;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.event.Ratings;
import org.grouplens.lenskit.util.DelimitedTextCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import java.io.BufferedReader;

/**
 * Cursor that parses arbitrary delimited text.
 *
 * @compat Public
 * @since 2.2
 */
public class DelimitedTextRatingCursor extends AbstractPollingCursor<Rating> {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private final String fileName;
    private MutableRating rating;
    private DelimitedTextCursor rowCursor;

    /**
     * Construct a rating cursor from a reader.
     *
     * @param s         The reader to read.
     * @param name      The file name (for error messages).
     * @param delimiter The delimiter.
     * @deprecated Inheriting from this class is deprecated.
     */
    @Deprecated
    protected DelimitedTextRatingCursor(@WillCloseWhenClosed @Nonnull BufferedReader s,
                                        @Nullable String name,
                                        @Nonnull String delimiter) {
        fileName = name;
        rating = new MutableRating();
        rowCursor = new DelimitedTextCursor(s, delimiter);
    }

    @SuppressWarnings("deprecation")
    public static Cursor<Rating> open(@WillCloseWhenClosed @Nonnull BufferedReader s,
                                      @Nonnull String delimiter) {
        return new DelimitedTextRatingCursor(s, null, delimiter);
    }

    @Override
    public void close() {
        rowCursor.close();
        rating = null;
    }

    @Override
    public Rating poll() {
        Preconditions.checkState(rating != null, "cursor is closed");
        while (rowCursor.hasNext()) {
            String[] fields = rowCursor.next();
            if (fields.length < 3) {
                logger.error("{}:{}: invalid input, skipping line",
                             fileName, rowCursor.getLineNumber());
                continue;
            }

            rating.setUserId(Long.parseLong(fields[0].trim()));
            rating.setItemId(Long.parseLong(fields[1].trim()));
            rating.setRating(Double.parseDouble(fields[2].trim()));
            rating.setTimestamp(-1);
            if (fields.length >= 4) {
                rating.setTimestamp(Long.parseLong(fields[3].trim()));
            }

            return rating;
        }

        return null;
    }

    @Override
    public Rating copy(Rating r) {
        return Ratings.copyBuilder(r).build();
    }
}
