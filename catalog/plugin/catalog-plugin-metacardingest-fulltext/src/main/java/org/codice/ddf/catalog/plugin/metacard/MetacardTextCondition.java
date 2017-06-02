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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.codice.ddf.catalog.common.metacard.KeyValueParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an immutable, rule-based check on a metacard for full-text containment.
 */
public class MetacardTextCondition {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardTextCondition.class);

    private final List<String> keywords;

    private final List<String> newAttributes;

    private final Map<String, String> parsedAttributes;

    private final Set<String> keywordSet;

    public MetacardTextCondition(final List<String> keywords) {
        this(keywords, new ArrayList<>(), new KeyValueParser());
    }

    public MetacardTextCondition(final List<String> keywords, final List<String> newAttributes,
            final KeyValueParser parser) {
        this.keywords = keywords;
        this.newAttributes = newAttributes;
        this.parsedAttributes = parser.parsePairsToMap(this.newAttributes);

        this.keywordSet = new HashSet<>(keywords);
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public List<String> getNewAttributes() {
        return newAttributes;
    }

    public Map<String, String> getParsedAttributes() {
        return parsedAttributes;
    }

    public boolean applies(List<String> criteria) {
        Optional<String> firstMatch = criteria.stream()
                .filter(keywordSet::contains)
                .findFirst();

        if (firstMatch.isPresent()) {
            LOGGER.info("Dirty word [{}] found, attributes will be applied", firstMatch.get());
        } else {
            LOGGER.info("No dirty words found");
        }

        return firstMatch.isPresent();
    }
}
