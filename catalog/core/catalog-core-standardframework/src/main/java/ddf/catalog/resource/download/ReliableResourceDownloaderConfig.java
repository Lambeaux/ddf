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
package ddf.catalog.resource.download;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import ddf.catalog.cache.impl.ResourceCacheImpl;
import ddf.catalog.event.retrievestatus.DownloadStatusContainer;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventListener;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher;

public class ReliableResourceDownloaderConfig {

    public static final int KB = 1024;

    public static final int MB = 1024 * KB;

    private static final int DEFAULT_CHUNK_SIZE = 1 * MB;

    private int maxRetryAttempts = 3;

    private int delayBetweenAttemptsMS = 10000;

    private long monitorPeriodMS = 5000;

    private int monitorInitialDelayMS = 1000;

    private boolean cacheEnabled = false;

    private boolean cacheWhenCanceled = false;

    private ResourceCacheImpl resourceCache;

    private DownloadsStatusEventPublisher eventPublisher;

    private DownloadsStatusEventListener eventListener;

    private DownloadStatusContainer downloadStatusContainer;

    private ExecutorService executor;

    private int chunkSize = DEFAULT_CHUNK_SIZE;

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getMonitorInitialDelayMS() {
        return monitorInitialDelayMS;
    }

    public void setMonitorInitialDelayMS(int monitorInitialDelayMS) {
        this.monitorInitialDelayMS = monitorInitialDelayMS;
    }

    public DownloadsStatusEventListener getEventListener() {
        return eventListener;
    }

    public void setEventListener(DownloadsStatusEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public DownloadsStatusEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    public void setEventPublisher(DownloadsStatusEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public ResourceCacheImpl getResourceCache() {
        return resourceCache;
    }

    public void setResourceCache(ResourceCacheImpl resourceCache) {
        this.resourceCache = resourceCache;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public int getDelayBetweenAttemptsMS() {
        return delayBetweenAttemptsMS;
    }

    public void setDelayBetweenAttemptsMS(int delayBetweenAttempts) {
        this.delayBetweenAttemptsMS = delayBetweenAttempts;
    }

    public long getMonitorPeriodMS() {
        return monitorPeriodMS;
    }

    public void setMonitorPeriodMS(long monitorPeriod) {
        this.monitorPeriodMS = monitorPeriod;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public boolean isCacheWhenCanceled() {
        return cacheWhenCanceled;
    }

    public void setCacheWhenCanceled(boolean cacheWhenCanceled) {
        this.cacheWhenCanceled = cacheWhenCanceled;
    }

    public DownloadStatusContainer getDownloadStatusContainer() {
        return downloadStatusContainer;
    }

    public void setDownloadStatusContainer(DownloadStatusContainer downloadStatusContainer) {
        this.downloadStatusContainer = downloadStatusContainer;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void shutdown() {
        this.executor.shutdown();
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        this.executor.awaitTermination(timeout, unit);
    }

    public void shutdownNow() {
        this.executor.shutdownNow();
    }

    public void submit(Runnable runnable) {
        this.executor.submit(runnable);
    }
}
