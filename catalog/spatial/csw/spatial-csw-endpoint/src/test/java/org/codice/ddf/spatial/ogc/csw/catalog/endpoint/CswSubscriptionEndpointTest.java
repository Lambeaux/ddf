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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.subscriptionstore.internal.MarshalledSubscription;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionContainer;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionIdentifier;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event.CswSubscription;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event.CswSubscriptionFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import ddf.catalog.data.Metacard;
import ddf.catalog.event.EventProcessor;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.security.SecurityConstants;
import net.opengis.cat.csw.v_2_0_2.AbstractQueryType;
import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.SearchResultsType;

@RunWith(MockitoJUnitRunner.class)
public class CswSubscriptionEndpointTest {
    private static final ch.qos.logback.classic.Logger CSW_LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CswEndpoint.class);

    private static final String RESPONSE_HANDLER_URL = "https://somehost:12345/test";

    private static final String VALID_TYPES = "csw:Record,csw:Record";

    private static final String METACARD_SCHEMA = "urn:catalog:metacard";

    private static final String SUBSCRIPTION_ID = "urn:uuid:1234";

    @Mock
    private EventProcessor mockEventProcessor;

    @Mock
    private TransformerManager mockMimeTypeManager;

    @Mock
    private TransformerManager mockSchemaManager;

    @Mock
    private TransformerManager mockInputManager;

    @Mock
    private Validator mockValidator;

    @Mock
    private CswSubscriptionFactory mockCswSubFactory;

    @Mock
    private SubscriptionContainer mockContainer;

    @Mock
    private SubscriptionIdentifier mockIdentifier;

    @Mock
    private CswQueryFactory queryFactory;

    @Mock
    private QueryRequest query;

    private CswSubscriptionEndpoint cswSubscriptionEndpoint;

    private CswSubscription subscription;

    private GetRecordsRequest defaultRequest;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        File systemKeystoreFile = temporaryFolder.newFile("serverKeystore.jks");
        FileOutputStream systemKeyOutStream = new FileOutputStream(systemKeystoreFile);
        InputStream systemKeyStream = CswSubscriptionEndpointTest.class.getResourceAsStream(
                "/serverKeystore.jks");
        IOUtils.copy(systemKeyStream, systemKeyOutStream);

        File systemTruststoreFile = temporaryFolder.newFile("serverTruststore.jks");
        FileOutputStream systemTrustOutStream = new FileOutputStream(systemTruststoreFile);
        InputStream systemTrustStream = CswSubscriptionEndpointTest.class.getResourceAsStream(
                "/serverTruststore.jks");
        IOUtils.copy(systemTrustStream, systemTrustOutStream);

        System.setProperty(SecurityConstants.KEYSTORE_TYPE, "jks");
        System.setProperty(SecurityConstants.TRUSTSTORE_TYPE, "jks");
        System.setProperty("ddf.home", "");
        System.setProperty(SecurityConstants.KEYSTORE_PATH, systemKeystoreFile.getAbsolutePath());
        System.setProperty(SecurityConstants.TRUSTSTORE_PATH,
                systemTruststoreFile.getAbsolutePath());
        String password = "changeit";
        System.setProperty(SecurityConstants.KEYSTORE_PASSWORD, password);
        System.setProperty(SecurityConstants.TRUSTSTORE_PASSWORD, password);

        when(mockIdentifier.getId()).thenReturn(SUBSCRIPTION_ID);
        when(queryFactory.getQuery(any(GetRecordsType.class))).thenReturn(query);

        defaultRequest = createDefaultGetRecordsRequest();
        subscription = new CswSubscription(defaultRequest.get202RecordsType(),
                query,
                mockMimeTypeManager);

        cswSubscriptionEndpoint = new CswSubscriptionEndpoint(mockEventProcessor,
                mockMimeTypeManager,
                mockSchemaManager,
                mockInputManager,
                mockValidator,
                mockCswSubFactory,
                mockContainer);
    }

    @Test
    public void testDeleteRecordsSubscription() throws Exception {
        ArgumentCaptor<SubscriptionIdentifier> idCaptor = ArgumentCaptor.forClass(
                SubscriptionIdentifier.class);
        when(mockContainer.contains(any(SubscriptionIdentifier.class))).thenReturn(true);
        when(mockContainer.delete(idCaptor.capture())).thenReturn(subscription);
        Response response = cswSubscriptionEndpoint.deleteRecordsSubscription(SUBSCRIPTION_ID);
        assertThat(idCaptor.getValue()
                .getId(), is(SUBSCRIPTION_ID));
        assertThat(Response.Status.OK.getStatusCode(),
                is(response.getStatusInfo()
                        .getStatusCode()));
    }

    @Test
    public void testDeleteRecordsSubscriptionNoSubscription() throws Exception {
        when(mockContainer.contains(any(SubscriptionIdentifier.class))).thenReturn(false);
        String requestId = "requestId";
        Response response = cswSubscriptionEndpoint.deleteRecordsSubscription(requestId);
        assertThat(Response.Status.NOT_FOUND.getStatusCode(),
                is(response.getStatusInfo()
                        .getStatusCode()));
    }

    @Test
    public void testGetRecordsSubscriptionNoSubscription() throws Exception {
        when(mockContainer.get(any(SubscriptionIdentifier.class))).thenReturn(null);
        String requestId = "requestId";
        Response response = cswSubscriptionEndpoint.getRecordsSubscription(requestId);
        assertThat(Response.Status.NOT_FOUND.getStatusCode(),
                is(response.getStatusInfo()
                        .getStatusCode()));
    }

    @Test
    public void testGetRecordsSubscription() throws Exception {
        ArgumentCaptor<SubscriptionIdentifier> idCaptor = ArgumentCaptor.forClass(
                SubscriptionIdentifier.class);
        when(mockContainer.get(idCaptor.capture())).thenReturn(subscription);
        Response response = cswSubscriptionEndpoint.getRecordsSubscription(SUBSCRIPTION_ID);
        assertThat(Response.Status.OK.getStatusCode(),
                is(response.getStatusInfo()
                        .getStatusCode()));
        assertThat("Expected match: ID given to container with ID given to endpoint",
                idCaptor.getValue()
                        .getId(),
                is(SUBSCRIPTION_ID));
        AcknowledgementType getAck = (AcknowledgementType) response.getEntity();
        assertThat(defaultRequest.get202RecordsType(),
                is(((JAXBElement<GetRecordsType>) getAck.getEchoedRequest()
                        .getAny()).getValue()));
    }

    @Test
    public void testUpdateRecordsSubscription() throws Exception {
        when(mockContainer.contains(any(SubscriptionIdentifier.class))).thenReturn(true);
        Response response = cswSubscriptionEndpoint.updateRecordsSubscription(SUBSCRIPTION_ID,
                defaultRequest.get202RecordsType());
        ArgumentCaptor<MarshalledSubscription> marshalledCaptor = ArgumentCaptor.forClass(
                MarshalledSubscription.class);
        ArgumentCaptor<SubscriptionIdentifier> idCaptor = ArgumentCaptor.forClass(
                SubscriptionIdentifier.class);
        verify(mockContainer).update(any(CswSubscription.class),
                marshalledCaptor.capture(),
                idCaptor.capture());
        assertThat(Response.Status.OK.getStatusCode(),
                is(response.getStatusInfo()
                        .getStatusCode()));
        assertThat("Expected match: ID given to container with ID given to endpoint",
                idCaptor.getValue()
                        .getId(),
                is(SUBSCRIPTION_ID));
        assertThat("Expected callback address to match the response handler URL. ",
                marshalledCaptor.getValue()
                        .getCallbackAddress(),
                is(RESPONSE_HANDLER_URL));
    }

    @Test
    public void testUpdateRecordDoesNotExist() throws Exception {
        when(mockContainer.contains(any(SubscriptionIdentifier.class))).thenReturn(false);
        Response response = cswSubscriptionEndpoint.updateRecordsSubscription(SUBSCRIPTION_ID,
                defaultRequest.get202RecordsType());
        assertThat(Response.Status.NOT_FOUND.getStatusCode(),
                is(response.getStatusInfo()
                        .getStatusCode()));
    }

    @Test
    public void testCreateRecordsSubscriptionGET() throws Exception {
        GetRecordsRequest getRecordsRequest = createDefaultGetRecordsRequest();
        getRecordsRequest.setVersion("");
        when(mockContainer.insert(any(CswSubscription.class),
                any(MarshalledSubscription.class),
                any(SubscriptionIdentifier.class))).thenReturn(mockIdentifier);
        Response response = cswSubscriptionEndpoint.createRecordsSubscription(getRecordsRequest);
        AcknowledgementType createAck = (AcknowledgementType) response.getEntity();
        assertThat(createAck, notNullValue());
    }

    @Test
    public void testCreateRecordsSubscriptionPOST() throws Exception {
        when(mockContainer.insert(any(CswSubscription.class),
                any(MarshalledSubscription.class),
                any(SubscriptionIdentifier.class))).thenReturn(mockIdentifier);
        Response response =
                cswSubscriptionEndpoint.createRecordsSubscription(defaultRequest.get202RecordsType());
        AcknowledgementType createAck = (AcknowledgementType) response.getEntity();
        assertThat(createAck, notNullValue());
        assertThat(createAck.getRequestId(), notNullValue());
    }

    @Test(expected = CswException.class)
    public void testCreateRecordsSubscriptionPOSTwithoutResponseHandler() throws Exception {
        GetRecordsRequest getRecordsRequest = createDefaultGetRecordsRequest();
        getRecordsRequest.setResponseHandler(null);
        cswSubscriptionEndpoint.createRecordsSubscription(getRecordsRequest.get202RecordsType());
    }

    @Test(expected = CswException.class)
    public void testCreateRecordsSubscriptionGETNullRequest() throws CswException {
        cswSubscriptionEndpoint.createRecordsSubscription((GetRecordsRequest) null);
    }

    @Test(expected = CswException.class)
    public void testCreateRecordsSubscriptionPOSTNullRequest() throws CswException {
        cswSubscriptionEndpoint.createRecordsSubscription((GetRecordsType) null);
    }

    @Test(expected = CswException.class)
    public void testCreateRecordsSubscriptionPOSTBadResponseHandler() throws CswException {
        when(mockContainer.insert(any(CswSubscription.class),
                any(MarshalledSubscription.class),
                any(SubscriptionIdentifier.class))).thenReturn(mockIdentifier);
        GetRecordsRequest getRecordsRequest = createDefaultGetRecordsRequest();
        getRecordsRequest.setResponseHandler("[]@!$&'()*+,;=");
        cswSubscriptionEndpoint.createRecordsSubscription(getRecordsRequest.get202RecordsType());
    }

    @Test(expected = CswException.class)
    public void testUnknownQueryType() throws CswException {
        GetRecordsType realRecordsType = defaultRequest.get202RecordsType();
        GetRecordsType mockRecordsType = mock(GetRecordsType.class, RETURNS_DEEP_STUBS);

        when(mockRecordsType.getOutputFormat()).thenReturn(realRecordsType.getOutputFormat());
        when(mockRecordsType.getOutputSchema()).thenReturn(realRecordsType.getOutputSchema());
        when(mockRecordsType.getAbstractQuery()
                .getValue()).then((Answer<Object>) invocation -> new NotQueryType());
        cswSubscriptionEndpoint.createRecordsSubscription(mockRecordsType);
    }

    @Test(expected = CswException.class)
    public void testConstraintsWithFilterAndCql() throws CswException {
        GetRecordsType realRecordsType = defaultRequest.get202RecordsType();
        GetRecordsType mockRecordsType = mock(GetRecordsType.class, RETURNS_DEEP_STUBS);

        when(mockRecordsType.getOutputFormat()).thenReturn(realRecordsType.getOutputFormat());
        when(mockRecordsType.getOutputSchema()).thenReturn(realRecordsType.getOutputSchema());

        QueryType mockQueryType = mock(QueryType.class, RETURNS_DEEP_STUBS);
        when(mockRecordsType.getAbstractQuery()
                .getValue()).then((Answer<AbstractQueryType>) invocation -> realRecordsType.getAbstractQuery()
                .getValue())
                .then((Answer<QueryType>) invocation -> mockQueryType);
        when(mockQueryType.getConstraint()
                .isSetFilter()).thenReturn(true);
        when(mockQueryType.getConstraint()
                .isSetCqlText()).thenReturn(true);

        cswSubscriptionEndpoint.createRecordsSubscription(mockRecordsType);
    }

    @Test
    public void testDeletedSubscription() throws Exception {
        when(mockContainer.delete(anyObject())).thenReturn(null);
        assertThat(cswSubscriptionEndpoint.deleteSubscription(SUBSCRIPTION_ID), is(false));
        when(mockContainer.delete(anyObject())).thenReturn(subscription);
        assertThat(cswSubscriptionEndpoint.deleteSubscription(SUBSCRIPTION_ID), is(true));
    }

    @Test
    public void testCreateEvent() throws Exception {
        cswSubscriptionEndpoint.createEvent(getRecordsResponse(1));
        verify(mockEventProcessor).notifyCreated(any(Metacard.class));
    }

    @Test
    public void testUpdateEvent() throws Exception {
        cswSubscriptionEndpoint.updateEvent(getRecordsResponse(2));
        verify(mockEventProcessor).notifyUpdated(any(Metacard.class), any(Metacard.class));

    }

    @Test
    public void testDeleteEvent() throws Exception {
        cswSubscriptionEndpoint.deleteEvent(getRecordsResponse(1));
        verify(mockEventProcessor).notifyDeleted(any(Metacard.class));
    }

    @Test(expected = CswException.class)
    public void testCreateEventInvalidSchema() throws Exception {
        GetRecordsResponseType getRecordsResponse = new GetRecordsResponseType();
        SearchResultsType searchResults = new SearchResultsType();
        searchResults.setRecordSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        getRecordsResponse.setSearchResults(searchResults);
        cswSubscriptionEndpoint.createEvent(getRecordsResponse);
    }

    @Test(expected = CswException.class)
    public void testUpdateEventInvalidSchema() throws Exception {
        GetRecordsResponseType getRecordsResponse = new GetRecordsResponseType();
        SearchResultsType searchResults = new SearchResultsType();
        searchResults.setRecordSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        getRecordsResponse.setSearchResults(searchResults);
        cswSubscriptionEndpoint.updateEvent(getRecordsResponse);
    }

    @Test(expected = CswException.class)
    public void testDeleteEventInvalidSchema() throws Exception {
        GetRecordsResponseType getRecordsResponse = new GetRecordsResponseType();
        SearchResultsType searchResults = new SearchResultsType();
        searchResults.setRecordSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        getRecordsResponse.setSearchResults(searchResults);
        cswSubscriptionEndpoint.deleteEvent(getRecordsResponse);
    }

    private GetRecordsRequest createDefaultGetRecordsRequest() {
        GetRecordsRequest grr = new GetRecordsRequest();
        grr.setRequestId(SUBSCRIPTION_ID);
        grr.setResponseHandler(RESPONSE_HANDLER_URL);
        grr.setService(CswConstants.CSW);
        grr.setVersion(CswConstants.VERSION_2_0_2);
        grr.setRequest(CswConstants.GET_RECORDS);
        grr.setNamespace(CswConstants.XMLNS_DEFINITION_PREFIX + CswConstants.CSW_NAMESPACE_PREFIX
                + CswConstants.EQUALS + CswConstants.CSW_OUTPUT_SCHEMA
                + CswConstants.XMLNS_DEFINITION_POSTFIX + CswConstants.COMMA

                + CswConstants.XMLNS_DEFINITION_PREFIX + CswConstants.OGC_NAMESPACE_PREFIX
                + CswConstants.EQUALS + CswConstants.OGC_SCHEMA
                + CswConstants.XMLNS_DEFINITION_POSTFIX + CswConstants.COMMA

                + CswConstants.XMLNS_DEFINITION_PREFIX + CswConstants.GML_NAMESPACE_PREFIX
                + CswConstants.EQUALS + CswConstants.GML_SCHEMA
                + CswConstants.XMLNS_DEFINITION_POSTFIX + CswConstants.COMMA);

        grr.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        grr.setOutputFormat(CswConstants.OUTPUT_FORMAT_XML);
        grr.setTypeNames(VALID_TYPES);
        return grr;
    }

    private GetRecordsResponseType getRecordsResponse(int metacardCount)
            throws IOException, CatalogTransformerException {
        InputTransformer inputTransformer = mock(InputTransformer.class);
        when(mockInputManager.getTransformerBySchema(METACARD_SCHEMA)).thenReturn(inputTransformer);
        Metacard metacard = mock(Metacard.class);
        when(inputTransformer.transform(any(InputStream.class))).thenReturn(metacard);

        GetRecordsResponseType getRecordsResponse = new GetRecordsResponseType();
        SearchResultsType searchResults = new SearchResultsType();
        searchResults.setRecordSchema(METACARD_SCHEMA);
        getRecordsResponse.setSearchResults(searchResults);
        List<Object> any = new ArrayList<>();
        Node node = mock(Node.class);
        for (int i = 0; i < metacardCount; i++) {
            any.add(node);
        }
        searchResults.setAny(any);
        return getRecordsResponse;
    }

    private static class NotQueryType extends AbstractQueryType {
        public NotQueryType() {
        }

        @Override
        public Object createNewInstance() {
            return null;
        }
    }
}