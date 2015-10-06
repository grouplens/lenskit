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

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import org.lenskit.inject.Shareable;
import org.lenskit.data.events.EventBuilder;
import org.lenskit.data.events.Like;
import org.lenskit.data.events.LikeBatchBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Event type for {@link Like}, reading plus counts without
 * timestamps (by default).
 *
 * @since 2.2
 */
@AutoService(EventTypeDefinition.class) // register this class to be locatable as an event type
@Shareable
@SuppressWarnings("rawtypes")
public class LikeBatchEventType implements EventTypeDefinition, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return "like-batch";
    }

    @Override
    public Class<? extends EventBuilder> getBuilderType() {
        return LikeBatchBuilder.class;
    }

    @Override
    public LikeBatchBuilder newBuilder() {
        return new LikeBatchBuilder();
    }

    @Override
    public Set<Field> getRequiredFields() {
        return ImmutableSet.of(Fields.user(), Fields.item());
    }

    @Override
    public List<Field> getDefaultFields() {
        return Fields.list(Fields.user(),
                           Fields.item(),
                           Fields.likeCount());
    }
}
