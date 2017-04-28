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
package org.codice.ddf.catalog.subscriptionstore.common;

import static java.lang.String.format;
import static org.apache.commons.lang.Validate.notEmpty;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

import org.codice.ddf.catalog.subscriptionstore.internal.SerializedSubscription;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.event.Subscription;

/**
 * An immutable data object for storing subscriptions in the {@link org.codice.ddf.persistence.PersistentStore}.
 * <p>
 * {@link SubscriptionMetadata} wraps a serialized {@link Subscription} with no assumption as to the specific
 * serialization format, only the guarantee that metadata objects with the same <i>type</i> are the same format.
 * That means metadata objects with the same <i>type</i> can be serialized and deserialized the same way, using
 * the same instance of {@link org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory}.
 */
public class SubscriptionMetadata implements SubscriptionIdentifier, SerializedSubscription {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionMetadata.class);

    private static final String URN_UUID = "urn:uuid:";

    private final String id;

    private final String type;

    private final String serializedFilter;

    private final URL callbackUrl;

    public SubscriptionMetadata(String type, String serializedFilter, String callbackUrl) {
        this(type,
                serializedFilter,
                callbackUrl,
                URN_UUID + UUID.randomUUID()
                        .toString());
    }

    public SubscriptionMetadata(String type, String serializedFilter, String callbackUrl,
            String id) {
        notEmpty(type, "type cannot be null or empty");
        notEmpty(serializedFilter, "serializedFilter cannot be null or empty");
        notEmpty(callbackUrl, "callbackUrl cannot be null or empty");
        notEmpty(id, "id cannot be null or empty");

        this.type = type;
        this.serializedFilter = serializedFilter;
        this.callbackUrl = validateCallbackUrl(callbackUrl);
        this.id = id;

        LOGGER.debug("Created subscription metadata object: {} | {} | {}", id, type, callbackUrl);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getSerializedFilter() {
        return serializedFilter;
    }

    @Override
    public String getCallbackAddress() {
        return callbackUrl.toString();
    }

    private URL validateCallbackUrl(String callbackUrl) {
        URL url;
        try {
            url = URI.create(callbackUrl)
                    .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(format(
                    "Invalid subscription request: callback URL [%s] was malformed",
                    callbackUrl));
        }
        return url;
    }
}
