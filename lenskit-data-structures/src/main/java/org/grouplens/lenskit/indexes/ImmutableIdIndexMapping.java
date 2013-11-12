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
package org.grouplens.lenskit.indexes;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.grouplens.lenskit.collections.LongKeyDomain;

/**
 * Immutable index implementation.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
final class ImmutableIdIndexMapping extends IdIndexMapping {
    private static final long serialVersionUID = 1L;

    private final LongKeyDomain domain;

    public ImmutableIdIndexMapping(LongSet ids) {
        domain = LongKeyDomain.fromCollection(ids);
        assert domain.size() == domain.domainSize();
    }

    @Override
    public int tryGetIndex(long id) {
        return domain.getIndex(id);
    }

    @Override
    public long getId(int idx) {
        Preconditions.checkElementIndex(idx, domain.domainSize());
        return domain.getKey(idx);
    }

    @Override
    public int size() {
        return domain.domainSize();
    }

    @Override
    public LongList getIdList() {
        return LongLists.unmodifiable(domain.keyList());
    }
}
