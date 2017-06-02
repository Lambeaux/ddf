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
package org.codice.ddf.catalog.plugin.metacard;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.common.metacard.AttributeFactory;
import org.codice.ddf.catalog.common.metacard.KeyValueParser;
import org.codice.ddf.catalog.common.metacard.MetacardServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.plugin.PreAuthorizationPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.transform.CatalogTransformerException;

/**
 * Conditional attribute adjustments for metacards based on keywords found in the tokenized data.
 */
public class MetacardIngestFulltextPlugin implements PreAuthorizationPlugin {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MetacardIngestFulltextPlugin.class);

    private static final String METACARD_TRANSFORMER_ID = "xml";

    private static final String ENCODING = "UTF-8";

    private static final String KEYWORDS = "keywords";

    private static final String NEW_ATTRIBUTES = "newAttributes";

    private final CatalogFramework framework;

    private final KeyValueParser keyValueParser;

    private final MetacardServices metacardServices;

    private final AttributeFactory attributeFactory;

    private volatile MetacardTextCondition metacardCondition;

    private Map<String, Object> initParameters;

    public MetacardIngestFulltextPlugin(KeyValueParser keyValueParser,
            MetacardServices metacardServices, AttributeFactory attributeFactory,
            MetacardTextCondition metacardCondition, CatalogFramework framework) {
        this.framework = framework;
        this.keyValueParser = keyValueParser;
        this.metacardServices = metacardServices;
        this.attributeFactory = attributeFactory;
        this.metacardCondition = metacardCondition;

        this.initParameters = new HashMap<>();
    }

    public List<String> getKeywords() {
        return metacardCondition.getKeywords();
    }

    public List<String> getNewAttributes() {
        return metacardCondition.getNewAttributes();
    }

    public void setKeywords(List<String> keywords) {
        initParameters.put(KEYWORDS, keywords.toArray(new String[keywords.size()]));
    }

    public void setNewAttributes(List<String> newAttributes) {
        initParameters.put(NEW_ATTRIBUTES, newAttributes.toArray(new String[newAttributes.size()]));
    }

    /**
     * Method required by the component-managed strategy. Performs set-up based on initial values
     * passed to the service.
     */
    public void init() {
        updateCondition(initParameters);
    }

    /**
     * Method required by the component-managed strategy.
     */
    public void destroy() {
    }

    /**
     * Atomically update the condition so that ingest rules are not evaluated based on an invalid
     * rule.
     */
    public void updateCondition(Map<String, Object> properties) {
        if (properties != null) {
            String[] keywords = (String[]) properties.get(KEYWORDS);
            String[] newAttributes = (String[]) properties.get(NEW_ATTRIBUTES);

            metacardCondition = new MetacardTextCondition(Arrays.asList(keywords),
                    Arrays.asList(newAttributes),
                    keyValueParser);
        }
    }

    @Override
    public CreateRequest processPreCreate(CreateRequest request) throws StopProcessingException {
        List<Metacard> metacards = request.getMetacards()
                .stream()
                .map(card -> processIndividualMetacard(card, request))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new CreateRequestImpl(metacards, request.getProperties());
    }

    //region - Passthrough  methods

    @Override
    public UpdateRequest processPreUpdate(UpdateRequest input,
            Map<String, Metacard> existingMetacards) throws StopProcessingException {
        return input;
    }

    @Override
    public DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException {
        return input;
    }

    @Override
    public DeleteResponse processPostDelete(DeleteResponse input) throws StopProcessingException {
        return input;
    }

    @Override
    public QueryRequest processPreQuery(QueryRequest input) throws StopProcessingException {
        return input;
    }

    @Override
    public QueryResponse processPostQuery(QueryResponse input) throws StopProcessingException {
        return input;
    }

    @Override
    public ResourceRequest processPreResource(ResourceRequest input)
            throws StopProcessingException {
        return input;
    }

    @Override
    public ResourceResponse processPostResource(ResourceResponse input, Metacard metacard)
            throws StopProcessingException {
        return input;
    }

    //endregion

    /**
     * Unlike the network plugin, the criteria is not part of the request, but each metacard. So we
     * can only apply attributes for a single metacard at a time, not for the entire request batch.
     * We return the original metacard if something goes wrong.
     */
    private Metacard processIndividualMetacard(Metacard metacard, CreateRequest request) {
        try {
            BinaryContent binaryContent = framework.transform(metacard,
                    METACARD_TRANSFORMER_ID,
                    request.getProperties());
            String metacardXml = IOUtils.toString(binaryContent.getInputStream(), ENCODING);
            return tokenizeAndApply(metacard, metacardXml);
        } catch (CatalogTransformerException e) {
            LOGGER.error("Could not transform metacard: ", e);
            return metacard;
        } catch (IOException e) {
            LOGGER.error("IO error when reading binary content: ", e);
            return metacard;
        }
    }

    private Metacard tokenizeAndApply(Metacard metacard, String metacardXml) {
        List<String> criteria;
        try {
            BasicStringTokenizer tokenizer = new BasicStringTokenizer();
            criteria = tokenizer.toList(metacardXml);
        } catch (IOException e) {
            LOGGER.error("IO error in Lucene when tokenizing data: ", e);
            return metacard;
        }

        if (metacardCondition.applies(criteria)) {
            List<Metacard> newMetacards =
                    metacardServices.setAttributesIfAbsent(Collections.singletonList(metacard),
                            metacardCondition.getParsedAttributes(),
                            attributeFactory);
            return newMetacards.get(0);
        }

        return metacard;
    }
}
