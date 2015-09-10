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
package org.lenskit.data.ratings;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.builder.Builder;
import org.lenskit.data.events.EventBuilder;

/**
 * Build a {@link Rating}.
 *
 * @since 1.3
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class RatingBuilder implements EventBuilder<Rating>, Builder<Rating> {
    private boolean hasUserId;
    private long userId;
    private boolean hasItemId;
    private long itemId;
    private boolean hasRating;
    private double rating;
    private long timestamp = -1;

    /**
     * Create an uninitialized rating builder.
     */
    public RatingBuilder() {}

    /**
     * Construct a new rating builder that is a copy of a particular rating.
     * @param r The rating to copy.
     * @return A rating builder that will initially build a copy of the specified rating.
     */
    public static RatingBuilder copy(Rating r) {
        return r.copyBuilder();
    }

    @Override
    public void reset() {
        hasUserId = hasItemId = hasRating = false;
        timestamp = -1;
    }

    /**
     * Get the user ID.
     * @return The user ID.
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Set the user ID.
     * @param uid The user ID.
     * @return The builder (for chaining).
     */
    public RatingBuilder setUserId(long uid) {
        userId = uid;
        hasUserId = true;
        return this;
    }

    /**
     * Get the item ID.
     * @return The item ID.
     */
    public long getItemId() {
        return itemId;
    }

    /**
     * Set the item ID.
     * @param iid The item ID.
     * @return The builder (for chaining).
     */
    public RatingBuilder setItemId(long iid) {
        itemId = iid;
        hasItemId = true;
        return this;
    }

    /**
     * Get the rating.
     * @return The rating value.
     */
    public double getRating() {
        return rating;
    }

    /**
     * Set the rating value.
     * <p>
     * In order to prevent computation errors from producing unintended unrate events, this method cannot be used to
     * create an unrate event.  Instead, use {@link #clearRating()}.
     * </p>
     * @param r The rating value.
     * @return The builder (for chaining).
     */
    public RatingBuilder setRating(double r) {
        if (Double.isNaN(r)) {
            throw new IllegalArgumentException("rating is not a number");
        }
        rating = r;
        hasRating = true;
        return this;
    }

    /**
     * Clear the rating value (so this builder builds unrate events).
     * @return The builder (for chaining).
     */
    public RatingBuilder clearRating() {
        hasRating = false;
        return this;
    }

    /**
     * Query whether this builder has a rating.
     * @return {@code true} if the builder has a rating, {@code false} if it will produce unrate
     * events.
     */
    public boolean hasRating() {
        return hasRating;
    }

    /**
     * Get the timestamp.
     * @return The timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Set the timestamp.
     * @param ts The timestamp.
     * @return The builder (for chaining).
     */
    public RatingBuilder setTimestamp(long ts) {
        timestamp = ts;
        return this;
    }

    @Override
    public Rating build() {
        Preconditions.checkState(hasUserId, "no user ID set");
        Preconditions.checkState(hasItemId, "no item ID set");
        if (hasRating) {
            return Rating.create(userId, itemId, rating, timestamp);
        } else {
            return Rating.createUnrate(userId, itemId, timestamp);
        }
    }
}
