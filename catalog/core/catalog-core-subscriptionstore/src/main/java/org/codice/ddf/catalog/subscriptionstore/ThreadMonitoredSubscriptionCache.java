package org.codice.ddf.catalog.subscriptionstore;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

/**
 * Temporary measure for refreshing the cache.
 */
public class ThreadMonitoredSubscriptionCache extends SubscriptionCache {

    // TODO: Might abstract this into a cache invalidation strategy later
    private final Executor cacheThread = Executors.newSingleThreadExecutor();

    private final Lock write;

    public ThreadMonitoredSubscriptionCache(SubscriptionStore subscriptionStore, Lock write) {
        super(subscriptionStore);
        this.write = write;
    }

    public void synchronizeCacheWithBackend() {
        write.lock();
        try {
            Map<String, SubscriptionMetadata> backendCollection = subscriptionStore.getSubscriptions();
            subscriptions.keySet()
                    .stream()
                    .filter(key -> !backendCollection.containsKey(key))
                    .forEach(this::deleteSubscriptionLocally);
            backendCollection.keySet()
                    .stream()
                    .filter(key -> !subscriptions.containsKey(key))
                    .map(backendCollection::get)
                    .forEach(this::createSubscriptionLocally);

            // TODO: Add processing / hashcode checking for update operations
            // Might need to rework this entire method

        } finally {
            write.unlock();
        }
    }
}
