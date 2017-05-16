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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionRegistrationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import ddf.catalog.event.Subscription;

@RunWith(MockitoJUnitRunner.class)
public class CachedSubscriptionTest {
    private static final String SUBSCRIPTION_ID_OSGI = "subscription-id";

    private static final String EVENT_ENDPOINT = "event-endpoint";

    @Mock
    private BundleContext mockBundleContext;

    @Mock
    private ServiceRegistration mockRegistration;

    @Mock
    private Subscription mockSubscription;

    private CachedSubscription cachedSubscription;

    @Before
    public void setup() {
        ServiceReference mockRef = mock(ServiceReference.class);
        Bundle mockBundle = mock(Bundle.class);
        when(mockRegistration.getReference()).thenReturn(mockRef);
        when(mockRef.getBundle()).thenReturn(mockBundle);
        when(mockBundle.getBundleId()).thenReturn(10L);

        SubscriptionMetadata metadata = new SubscriptionMetadata("type",
                "filter",
                "http://localhost:1234/test");
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
        when(mockBundleContext.registerService(anyString(), anyObject(), anyObject())).thenReturn(
                mockRegistration);
        cachedSubscription.registerSubscription(mockSubscription);
        cachedSubscription.unregisterSubscription();
        verify(mockRegistration).unregister();

        boolean registrationWasSetToNull = false;
        try {
            cachedSubscription.unregisterSubscription();
        } catch (SubscriptionRegistrationException e) {
            registrationWasSetToNull = true;
        }
        assertThat(registrationWasSetToNull, is(true));
    }

    @Test
    public void testRegisterSubscription() {

    }
}
