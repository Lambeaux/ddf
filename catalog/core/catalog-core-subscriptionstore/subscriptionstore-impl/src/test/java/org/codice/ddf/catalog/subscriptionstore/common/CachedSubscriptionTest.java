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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;

import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionRegistrationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import ddf.catalog.event.Subscription;

@RunWith(MockitoJUnitRunner.class)
public class CachedSubscriptionTest {
    private static final String TYPE = "type";

    private static final String NOT_TYPE = "not_type";

    private static final String FILTER = "filter";

    private static final String CALLBACK = "http://localhost:1234/test";

    private static final String SUBSCRIPTION_ID_OSGI = "subscription-id";

    private static final String EVENT_ENDPOINT = "event-endpoint";

    @Mock
    private BundleContext mockBundleContext;

    @Mock
    private ServiceRegistration mockRegistration;

    @Mock
    private Subscription mockSubscription;

    @Mock
    private SubscriptionFactory mockFactory;

    SubscriptionMetadata metadata;

    private CachedSubscription cachedSubscription;

    @Before
    public void setup() {
        ServiceReference mockRef = mock(ServiceReference.class);
        Bundle mockBundle = mock(Bundle.class);
        when(mockRegistration.getReference()).thenReturn(mockRef);
        when(mockRef.getBundle()).thenReturn(mockBundle);
        when(mockBundle.getBundleId()).thenReturn(10L);

        when(mockBundleContext.registerService(anyString(), anyObject(), anyObject())).thenReturn(
                mockRegistration);

        when(mockFactory.getType()).thenReturn(TYPE);
        when(mockFactory.createSubscription(anyObject())).thenReturn(mockSubscription);

        metadata = new SubscriptionMetadata(TYPE, FILTER, CALLBACK);
        cachedSubscription = new CachedSubscription(metadata) {
            BundleContext getBundleContext() {
                return mockBundleContext;
            }
        };
    }

    @Test(expected = SubscriptionRegistrationException.class)
    public void testUnregisterThrowsException() {
        cachedSubscription.unregisterSubscription();
    }

    @Test
    public void testUnregister() {
        cachedSubscription.registerSubscription(mockSubscription);
        cachedSubscription.unregisterSubscription();
        verify(mockRegistration).unregister();

        // Need to ensure the exception happened on the 2nd call to unregister
        boolean registrationWasSetToNull = false;
        try {
            cachedSubscription.unregisterSubscription();
        } catch (SubscriptionRegistrationException e) {
            registrationWasSetToNull = true;
        }
        assertThat(registrationWasSetToNull, is(true));
    }

    @Test(expected = SubscriptionRegistrationException.class)
    public void testRegisterSubscriptionThrowsException() {
        when(mockBundleContext.registerService(anyString(), anyObject(), anyObject())).thenReturn(
                null);
        cachedSubscription.registerSubscription(mockSubscription);
    }

    @Test
    public void testRegisterSubscription() {
        cachedSubscription.registerSubscription(mockSubscription);
        validateRegistration();
    }

    @Test(expected = SubscriptionRegistrationException.class)
    public void testRegisterWithFactoryThrowsException() {
        when(mockFactory.getType()).thenReturn(NOT_TYPE);
        cachedSubscription.registerSubscription(mockFactory);
    }

    @Test(expected = SubscriptionRegistrationException.class)
    public void testRegisterWithFactoryReturningNull() {
        when(mockFactory.createSubscription(anyObject())).thenReturn(null);
        cachedSubscription.registerSubscription(mockFactory);
    }

    @Test
    public void testRegisterWithFactory() {
        cachedSubscription.registerSubscription(mockFactory);
        validateRegistration();
    }

    private void validateRegistration() {
        ArgumentCaptor<Dictionary> argCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(mockBundleContext).registerService(eq(Subscription.class.getName()),
                eq(mockSubscription),
                argCaptor.capture());
        Dictionary<String, String> props = argCaptor.getValue();
        assertThat(props.size(), is(2));
        assertThat(metadata.getId(), is(props.get(SUBSCRIPTION_ID_OSGI)));
        assertThat(metadata.getCallbackAddress(), is(props.get(EVENT_ENDPOINT)));
        assertThat(cachedSubscription.getSubscription()
                .get(), is(mockSubscription));
    }
}
