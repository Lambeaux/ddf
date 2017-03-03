/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.platform.ignite;

import org.apache.ignite.IgniteException;
import org.apache.ignite.lifecycle.LifecycleBean;
import org.apache.ignite.lifecycle.LifecycleEventType;

/**
 * Provides hooks for before and after Ignite nodes start or stop.
 */
public class CustomLifecycleBean implements LifecycleBean {
    @Override
    public void onLifecycleEvent(LifecycleEventType evt) throws IgniteException {
        switch (evt) {
        case BEFORE_NODE_START:
        case AFTER_NODE_START:
        case BEFORE_NODE_STOP:
        case AFTER_NODE_STOP:
        default:
            break;
        }
    }
}
