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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;

import javax.cache.Cache;
import javax.cache.CacheException;

import org.codice.ddf.catalog.subscriptionstore.common.CachedSubscription;
import org.codice.ddf.catalog.subscriptionstore.common.SubscriptionMetadata;
import org.codice.ddf.catalog.subscriptionstore.internal.MarshalledSubscription;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionIdentifier;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionStoreException;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionType;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;

import com.google.common.collect.ImmutableList;

import ddf.catalog.event.Subscription;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionContainerImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private static BundleContext mockBundleContext;

    @Mock
    private static Cache<String, CachedSubscription> mockCache;

    private static Map<String, SubscriptionFactory> factories = new HashMap<>();

    @Mock
    private static SubscriptionFactory mockFactoryA;

    @Mock
    private static SubscriptionFactory mockFactoryB;

    private SubscriptionContainerImpl container;

    @Before
    public void setup() {
        when(mockFactoryA.getTypeName()).thenReturn("A");
        when(mockFactoryB.getTypeName()).thenReturn("B");

        container = new ContainerUnderTest();

        factories.put("A", mockFactoryA);
        factories.put("B", mockFactoryB);
    }

    @After
    public void cleanup() {
        factories.clear();
    }

    //region UTIL

    @Test
    public void testInit() {
        FactoryUpdateDataGroup group = new FactoryUpdateDataGroup();
        Spliterator spliterator = group.getSpliterator();
        when(mockCache.spliterator()).thenReturn(spliterator);

        container.init();

        verify(mockCache).loadAll(anySet(), eq(false), anyObject());
        verify(group.registeredSub, never()).registerSubscription(any(SubscriptionFactory.class));
        verify(group.typeMismatchSub, never()).registerSubscription(any(SubscriptionFactory.class));
        verify(group.validSubA).registerSubscription(eq(mockFactoryA));
        verify(group.validSubB).registerSubscription(eq(mockFactoryB));
    }

    @Test
    public void testBindFactory() {
        FactoryUpdateDataGroup group = new FactoryUpdateDataGroup();
        Spliterator spliterator = group.getSpliterator();
        when(mockCache.spliterator()).thenReturn(spliterator);

        SubscriptionFactory factory = mock(SubscriptionFactory.class);
        when(factory.getTypeName()).thenReturn("Z");

        container.bindFactory(factory);

        verify(group.registeredSub, never()).registerSubscription(any(SubscriptionFactory.class));
        verify(group.typeMismatchSub, never()).registerSubscription(any(SubscriptionFactory.class));
        verify(group.validSubZ).registerSubscription(eq(factory));

        assertThat(factories.containsKey("Z"), is(true));
        assertThat(factories.get("Z"), is(factory));
    }

    @Test
    public void testBindFactoryNullDoesNoOp() {
        Spliterator spliterator = mock(Spliterator.class);
        when(mockCache.spliterator()).thenReturn(spliterator);

        container.bindFactory(null);

        verifyZeroInteractions(spliterator);
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testBindFactoryDuplicateThrowsException() {
        Spliterator spliterator = mock(Spliterator.class);
        when(mockCache.spliterator()).thenReturn(spliterator);

        SubscriptionFactory factory = mock(SubscriptionFactory.class);
        when(factory.getTypeName()).thenReturn("Z");
        factories.put("Z", factory);

        container.bindFactory(factory);

        verifyZeroInteractions(spliterator);
    }

    @Test
    public void testUnbindFactory() {
        SubscriptionFactory factory = mock(SubscriptionFactory.class);
        when(factory.getTypeName()).thenReturn("Z");
        factories.put("Z", factory);

        container.unbindFactory(factory);
        assertThat(factories.get("Z"), is(nullValue()));
    }

    @Test
    public void testUnbindFactoryNullDoesNoOp() {
        Map<String, SubscriptionFactory> mockFactoryMap = mock(Map.class);
        container = new ContainerUnderTest() {
            @Override
            Map<String, SubscriptionFactory> createFactoryMap() {
                return mockFactoryMap;
            }
        };
        container.unbindFactory(null);
        verifyZeroInteractions(mockFactoryMap);
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testUnbindNonexistentFactoryThrowsException() {
        SubscriptionFactory factory = mock(SubscriptionFactory.class);
        when(factory.getTypeName()).thenReturn("Z");
        container.unbindFactory(factory);
    }

    //endregion

    //region GET

    @Test
    public void testGetSubscription() {
        SubscriptionIdentifier identifier = mock(SubscriptionIdentifier.class);
        Subscription subscription = mock(Subscription.class);
        when(identifier.getId()).thenReturn("id");
        when(identifier.getTypeName()).thenReturn("type");

        CachedSubscription cachedSub = mock(CachedSubscription.class);
        when(cachedSub.isNotType("type")).thenReturn(false);
        when(cachedSub.isNotRegistered()).thenReturn(false);
        when(cachedSub.getSubscription()).thenReturn(Optional.of(subscription));

        when(mockCache.get("id")).thenReturn(cachedSub);
        assertThat(container.get(identifier), is(subscription));
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testGetSubscriptionWhenOptionalIsEmpty() {
        SubscriptionIdentifier identifier = mock(SubscriptionIdentifier.class);
        Subscription subscription = mock(Subscription.class);
        when(identifier.getId()).thenReturn("id");
        when(identifier.getTypeName()).thenReturn("type");

        CachedSubscription cachedSub = mock(CachedSubscription.class);
        when(cachedSub.isNotType("type")).thenReturn(false);
        when(cachedSub.isNotRegistered()).thenReturn(false);
        when(cachedSub.getSubscription()).thenReturn(Optional.empty());

        when(mockCache.get("id")).thenReturn(cachedSub);
        container.get(identifier);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSubscriptionUsingNullIdentifier() {
        container.get(null);
    }

    @Test
    public void testGetNullSubscription() {
        SubscriptionIdentifier identifier = mock(SubscriptionIdentifier.class);
        when(identifier.getId()).thenReturn("id");
        when(identifier.getTypeName()).thenReturn("type");

        when(mockCache.get("id")).thenReturn(null);
        assertThat(container.get(identifier), is(nullValue()));
    }

    @Test
    public void testGetWrongTypeOfSubscription() {
        SubscriptionIdentifier identifier = mock(SubscriptionIdentifier.class);
        when(identifier.getId()).thenReturn("id");
        when(identifier.getTypeName()).thenReturn("type");

        CachedSubscription sub = mock(CachedSubscription.class);
        when(sub.isNotType("type")).thenReturn(true);

        when(mockCache.get("id")).thenReturn(sub);
        assertThat(container.get(identifier), is(nullValue()));
    }

    @Test
    public void testGetUnregisteredSubscription() {
        SubscriptionIdentifier identifier = mock(SubscriptionIdentifier.class);
        when(identifier.getId()).thenReturn("id");
        when(identifier.getTypeName()).thenReturn("type");

        CachedSubscription sub = mock(CachedSubscription.class);
        when(sub.isNotType("type")).thenReturn(false);
        when(sub.isNotRegistered()).thenReturn(true);

        when(mockCache.get("id")).thenReturn(sub);
        assertThat(container.get(identifier), is(nullValue()));
    }

    //endregion

    //region INSERT

    @Test
    public void testInsertSubscription() {
        Subscription subscription = mock(Subscription.class);
        MarshalledSubscription marshalledSubscription = mock(MarshalledSubscription.class);
        SubscriptionType type = mock(SubscriptionType.class);

        when(marshalledSubscription.getFilter()).thenReturn("filter");
        when(marshalledSubscription.getCallbackAddress()).thenReturn("http://localhost8993/test");
        when(type.getTypeName()).thenReturn("type");

        container.insert(subscription, marshalledSubscription, type);

        ArgumentCaptor<CachedSubscription> argCaptor =
                ArgumentCaptor.forClass(CachedSubscription.class);
        verify(mockCache).put(anyString(), argCaptor.capture());

        CachedSubscription arg = argCaptor.getValue();
        if (arg.getSubscription()
                .isPresent()) {
            assertThat(arg.getSubscription()
                    .get(), is(subscription));
        } else {
            fail("Subscription was null on the cache object. ");
        }
    }

    @Test(expected = SubscriptionStoreException.class)
    public void testInsertWhenPersistorThrowsException() {
        Subscription subscription = mock(Subscription.class);
        MarshalledSubscription marshalledSubscription = mock(MarshalledSubscription.class);
        SubscriptionType type = mock(SubscriptionType.class);

        doThrow(CacheException.class).when(mockCache)
                .put(anyString(), any(CachedSubscription.class));

        when(marshalledSubscription.getFilter()).thenReturn("filter");
        when(marshalledSubscription.getCallbackAddress()).thenReturn("http://localhost8993/test");
        when(type.getTypeName()).thenReturn("type");

        container.insert(subscription, marshalledSubscription, type);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertSubscriptionUsingNullSubscription() {
        MarshalledSubscription marshalledSubscription = mock(MarshalledSubscription.class);
        SubscriptionType type = mock(SubscriptionType.class);
        container.insert(null, marshalledSubscription, type);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertSubscriptionUsingNullSerializedSubscription() {
        Subscription subscription = mock(Subscription.class);
        SubscriptionType type = mock(SubscriptionType.class);
        container.insert(subscription, null, type);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertSubscriptionUsingNullSubscriptionType() {
        Subscription subscription = mock(Subscription.class);
        MarshalledSubscription marshalledSubscription = mock(MarshalledSubscription.class);
        container.insert(subscription, marshalledSubscription, null);
    }

    //endregion

    //region UPDATE
    @Test
    public void testUpdate() {

    }
    //endregion

    //region DELETE
    @Ignore
    @Test
    public void testDelete() {
        SubscriptionIdentifier identifier = mock(SubscriptionIdentifier.class);
        Subscription subscription = mock(Subscription.class);
        when(identifier.getId()).thenReturn("id");
        when(identifier.getTypeName()).thenReturn("type");

        CachedSubscription cachedSub = mock(CachedSubscription.class);
    }

    @Ignore
    @Test(expected = SubscriptionStoreException.class)
    public void testDeleteThrowsException() {

    }

    @Ignore
    @Test(expected = SubscriptionStoreException.class)
    public void testDeleteWhenOptionalIsEmpty() {

    }
    //endregion

    private static class ContainerUnderTest extends SubscriptionContainerImpl {

        public ContainerUnderTest() {
            super(null, null);
        }

        @Override
        Cache<String, CachedSubscription> createCache(SubscriptionCacheLoader cacheLoader,
                SubscriptionCacheWriter cacheWriter) {
            return mockCache;
        }

        @Override
        Map<String, SubscriptionFactory> createFactoryMap() {
            return factories;
        }

        @Override
        CachedSubscription createCachedSubscription(SubscriptionMetadata metadata) {
            return new CachedSubscription(metadata) {
                @Override
                protected BundleContext getBundleContext() {
                    return mockBundleContext;
                }
            };
        }
    }

    private static class FactoryUpdateDataGroup {

        CachedSubscription registeredSub;

        CachedSubscription typeMismatchSub;

        CachedSubscription validSubA;

        CachedSubscription validSubB;

        CachedSubscription validSubZ;

        public FactoryUpdateDataGroup() {
            registeredSub = makeMockForType("donotcare", "id-dnc", false);
            typeMismatchSub = makeMockForType("C", "id-C");
            validSubA = makeMockForType("A", "id-A");
            validSubB = makeMockForType("B", "id-B");
            validSubZ = makeMockForType("Z", "id-Z");

            when(validSubZ.isType(anyString())).thenReturn(true);
        }

        Spliterator getSpliterator() {
            return ImmutableList.of(wrap(registeredSub),
                    wrap(typeMismatchSub),
                    wrap(validSubA),
                    wrap(validSubB),
                    wrap(validSubZ))
                    .spliterator();
        }

        private CachedSubscription makeMockForType(String type, String id) {
            return makeMockForType(type, id, true);
        }

        private CachedSubscription makeMockForType(String type, String id,
                boolean isNotRegistered) {
            CachedSubscription sub = mock(CachedSubscription.class, RETURNS_DEEP_STUBS);
            when(sub.isNotRegistered()).thenReturn(isNotRegistered);
            when(sub.getMetadata()
                    .getTypeName()).thenReturn(type);
            when(sub.getMetadata()
                    .getId()).thenReturn(id);
            return sub;
        }

        private CacheEntryTestImpl wrap(CachedSubscription sub) {
            return new CacheEntryTestImpl(sub);
        }
    }
}
