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

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteFuture;

/**
 * Simple proof-of-concept wrapper for the Ignite distributed datagrid.
 */
public class IgniteWrapper implements AutoCloseable {

    private final Ignite ignite;

    public IgniteWrapper() {
        this.ignite = Ignition.start();
    }

    @Override
    public void close() throws Exception {
        ignite.close();
    }

    public void computeExample() {
        IgniteCompute compute = ignite.compute();
        String result = compute.call(() -> "Hello World");
    }

    public void computeAsyncExample() {
        IgniteCompute computeAsync = ignite.compute()
                .withAsync();
        computeAsync.call(() -> "Hello World");
        IgniteFuture<String> result = computeAsync.future();
        result.listen(future -> doNothing(future.get()));
    }

    public void gridExample() {
        IgniteCache<String, String> cache = ignite.cache("CACHE_NAME");
        String exampleValue = cache.getAndPut("ISBN", "8652332");
    }

    public void gridAsyncExample() {
        // Docs have fallen out of date - template args should not need to be Obj, Obj
        IgniteCache<Object, Object> cacheAsync = ignite.cache("CACHE_NAME")
                .withAsync();
        cacheAsync.getAndPut("ISBN", "8652332");
        IgniteFuture<String> result = cacheAsync.future();
        result.listen(future -> doNothing(future.get()));
    }

    public void configure() {
        IgniteConfiguration config = new IgniteConfiguration();
        config.setLifecycleBeans(new CustomLifecycleBean());
    }

    private void doNothing(String arg) {

    }
}
