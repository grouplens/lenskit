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
package org.grouplens.lenskit.util;

import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;

/**
 * Various type utilities used in LensKit.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class TypeUtils {
    /**
     * Build the set of types implemented by the objects' classes. This includes
     * all supertypes which are themselves subclasses of {@var parent}.  The
     * resulting set is the set of all subclasses of {@var parent} such that
     * there exists some object in {@var objects} assignable to one of them.
     *
     * @param objects A collection of objects.  This iterable may be fast (returning a modified
     *                version of the same object).
     * @param parent  The parent type of interest.
     * @return The set of types applicable to objects in {@var objects}.
     */
    public static <T> Set<Class<? extends T>>
    findTypes(Iterable<? extends T> objects, Class<T> parent) {
        Set<Class<? extends T>> objTypes =
                Sets.newHashSet(transform(objects, extractClass(parent)));

        Set<Class<? extends T>> allTypes = new HashSet<Class<? extends T>>();
        for (Class<? extends T> t : objTypes) {
            addAll(allTypes, transform(filter(typeClosure(t), isSubclass(parent)),
                                       asSubclass(parent)));
        }
        return allTypes;
    }

    /**
     * Return the supertype closure of a type (the type and all its transitive
     * supertypes).
     *
     * @param type The type.
     * @return All supertypes of the type, including the type itself.
     */
    public static Set<Class<?>> typeClosure(Class<?> type) {
        if (type == null) {
            return Collections.emptySet();
        }

        Set<Class<?>> supertypes = new HashSet<Class<?>>();
        supertypes.add(type);
        supertypes.addAll(typeClosure(type.getSuperclass()));
        for (Class<?> iface : type.getInterfaces()) {
            supertypes.addAll(typeClosure(iface));
        }

        return supertypes;
    }

    public static Predicate<Class<?>> isSubclass(final Class<?> cls) {
        return new Predicate<Class<?>>() {

            @Override
            public boolean apply(Class<?> input) {
                return cls.isAssignableFrom(input);
            }
        };
    }

    /**
     * Function that casts classes to specified types.  This function does not accept nulls.
     *
     * @param supertype A class known to be a valid supertype for any argument.
     */
    public static <T> Function<Class<?>, Class<? extends T>> asSubclass(final Class<T> supertype) {
        return new Function<Class<?>, Class<? extends T>>() {
            @Override
            @SuppressFBWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
            public Class<? extends T> apply(Class<?> input) {
                if (input == null) {
                    throw new NullPointerException("null class");
                } else {
                    return input.asSubclass(supertype);
                }
            }
        };
    }

    /**
     * Function that gets the class for its argument.
     * @param supertype A class known to be a valid supertype for any argument.
     * @param acceptNull Whether nulls are accepted &amp; passed through. If {@code false}, the function
     *                   will never return {@code null}.
     */
    public static <T> Function<T, Class<? extends T>> extractClass(final Class<T> supertype, final boolean acceptNull) {
        return new Function<T, Class<? extends T>>() {
            @Override
            public Class<? extends T> apply(@Nullable T input) {
                if (input != null) {
                    return input.getClass().asSubclass(supertype);
                } else if (acceptNull) {
                    return null;
                } else {
                    throw new NullPointerException();
                }
            }
        };
    }

    /**
     * Function that gets the class for its argument.  This function does not accept nulls.
     *
     * @param supertype A class known to be a valid supertype for any argument.
     */
    public static <T> Function<T, Class<? extends T>> extractClass(final Class<T> supertype) {
        return extractClass(supertype, false);
    }

    /**
     * Function that gets the class for its argument.
     */
    public static Function<Object, Class<?>> extractClass() {
        return extractClass(Object.class);
    }

    /**
     * Function that gets the class for its argument.
     */
    public static Function<Object, Class<?>> extractClass(boolean acceptNull) {
        return extractClass(Object.class, acceptNull);
    }

    /**
     * A predicate that accepts classes which are subtypes of (assignable to) the parent class.
     * @param parent The parent class.
     * @return A predicate that returns {@code true} when applied to a subtype of {@code parent}.
     *         That is, it implements {@code paret.isAssignableFrom(type)}.
     */
    public static Predicate<Class<?>> subtypePredicate(final Class<?> parent) {
        return new Predicate<Class<?>>() {
            @Override
            public boolean apply(@Nullable Class<?> input) {
                return parent.isAssignableFrom(input);
            }
        };
    }
}
