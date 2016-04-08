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
package org.lenskit.gradle.delegates

import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.lenskit.specs.AbstractSpec

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Configurable wrapper for a specification.  It automatically creates methods to set properties, like configurable
 * Gradle objects usually have, so you can say {@code prop value} instead of {@code prop = value}.
 */
class SpecDelegate {
    // FIXME Support ObjectNode specifications
    final def Project project
    final def AbstractSpec spec

    SpecDelegate(Project prj, AbstractSpec sp) {
        project = prj
        spec = sp
    }

    @Override
    Object getProperty(String property) {
        return spec.metaClass.getProperty(spec, property)
    }

    @Override
    void setProperty(String property, Object newValue) {
        spec.metaClass.setProperty(spec, property, newValue)
    }

    Object methodMissing(String name, Object args) {
        try {
            return spec.metaClass.invokeMethod(spec, name, args)
        } catch (MissingMethodException ignored) {
            /* no method, continue with fallback code */
        }
        def argArray = args as Object[]
        if (argArray.length != 1) {
            throw missing(name, argArray, null)
        }
        def value = argArray[0]
        try {
            spec.metaClass.setProperty(spec, name, value)
        } catch (MissingPropertyException e) {
            // no such property
            throw missing(name, argArray, e)
        } catch (ClassCastException e) {
            // this means that we had the wrong type
            MetaProperty prop = spec.metaClass.hasProperty(spec, name)
            if (prop == null) {
                throw missing(name, argArray, e)
            }
            assert value != null // else how do we have a wrong class?
            def ptype = prop.type
            def vtype = value.class
            if (ptype == Path.class) {
                if (vtype == File.class) {
                    def file = value as File
                    prop.setProperty(spec, file.toPath())
                } else {
                    prop.setProperty(spec, Paths.get(value))
                }
            } else if (Closure.isAssignableFrom(vtype) && AbstractSpec.isAssignableFrom(ptype)) {
                def val = configureSpec(project, ptype, value as Closure)
                prop.setProperty(spec, val)
            } else {
                throw missing(name, argArray, e);
            }
        }
        return null
    }

    private MissingMethodException missing(String name, Object[] args, Throwable cause) {
        def ex = new MissingMethodException(name, getClass(), args)
        if (cause != null) {
            if (ex.cause != null) {
                ex.initCause(cause)
            } else {
                ex.addSuppressed(cause)
            }
        }
        return ex
    }

    /**
     * Configure a spec using a block.
     * @param spec The spec.
     * @param block The block.
     * @return The configured specification.
     */
    public static <T extends AbstractSpec> T configureSpec(Project prj, T spec, Closure block, Class... traits) {
        def dlg = new SpecDelegate(prj, spec)
        for (tr in traits) {
            dlg = dlg.withTraits(tr)
        }
        ConfigureUtil.configure(block, dlg)
        return spec
    }

    /**
     * Instantiate and configure a spec using a block.
     * @param specType The specification type.
     * @param block The configuration block.
     * @return The spec.
     */
    public static <T extends AbstractSpec> T configureSpec(Project prj, Class<? extends T> specType, Closure block, Class... traits) {
        def spec = specType.newInstance()
        return configureSpec(prj, spec, block, traits)
    }

    /**
     * Instantiate and configure a spec using a block.
     * @param specType The specification type.
     * @param block The configuration block.
     * @return The spec.
     */
    public static <T extends AbstractSpec> T configureSpec(Project prj, Class<? extends T> specType, Class<? extends SpecDelegate> dlgType, Closure block) {
        def spec = specType.newInstance()
        def dlg = dlgType.newInstance(prj, spec)
        ConfigureUtil.configure(block, dlg)
        spec
    }
}
