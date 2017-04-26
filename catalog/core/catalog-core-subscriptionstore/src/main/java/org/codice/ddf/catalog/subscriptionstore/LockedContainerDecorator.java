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
package org.codice.ddf.catalog.subscriptionstore;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;

import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionContainer;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory;

import ddf.catalog.event.Subscription;

/**
 * Handles the locking semantics for {@link SubscriptionContainer}s. While they are not transactional,
 * this strategy optimizes efficiency while preventing any form of data corruption.
 */
public class LockedContainerDecorator<S extends Subscription> implements SubscriptionContainer {

    private final SubscriptionContainer<S> container;

    private final Lock read;

    private final Lock write;

    public LockedContainerDecorator(SubscriptionStore store) {
        ReadWriteLock lock = new ReentrantReadWriteLock(true);
        this.read = lock.readLock();
        this.write = lock.writeLock();

        this.container = new ThreadMonitoredSubscriptionCache(store, write);
    }

    public LockedContainerDecorator(SubscriptionContainer<S> container) {
        ReadWriteLock lock = new ReentrantReadWriteLock(true);
        this.read = lock.readLock();
        this.write = lock.writeLock();

        this.container = container;
    }

    @Override
    public void registerSubscriptionFactory(String type, SubscriptionFactory factory) {
        write.lock();
        try {
            container.registerSubscriptionFactory(type, factory);
        } finally {
            write.unlock();
        }
    }

    @Nullable
    @Override
    public Subscription get(String subscriptionId, String type) {
        read.lock();
        try {
            return container.get(subscriptionId, type);
        } finally {
            read.unlock();
        }
    }

    @Override
    public String insert(Subscription subscription, String type, String messageBody,
            String callbackUrl) {
        write.lock();
        try {
            return container.insert((S) subscription, type, messageBody, callbackUrl);
        } finally {
            write.unlock();
        }
    }

    @Override
    public void update(Subscription subscription, String type, String messageBody,
            String callbackUrl, String subscriptionId) {
        write.lock();
        try {
            container.update((S) subscription, type, messageBody, callbackUrl, subscriptionId);
        } finally {
            write.unlock();
        }
    }

    @Nullable
    @Override
    public Subscription delete(String subscriptionId, String type) {
        write.lock();
        try {
            return container.delete(subscriptionId, type);
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean contains(String subscriptionId, String type) {
        read.lock();
        try {
            return container.contains(subscriptionId, type);
        } finally {
            read.unlock();
        }
    }
}
