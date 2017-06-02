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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Uses Lucene's {@link StandardTokenizer} to parse a given body into a list of words.
 */
public class BasicStringTokenizer {
    public List<String> toList(String body) throws IOException {
        StandardTokenizer tokenizer = new StandardTokenizer();
        tokenizer.setReader(new StringReader(body));

        CharTermAttribute charTermAttribute = tokenizer.getAttribute(CharTermAttribute.class);
        List<String> tokens = new ArrayList<>();

        tokenizer.reset();
        while (tokenizer.incrementToken()) {
            tokens.add(charTermAttribute.toString());
        }
        tokenizer.end();
        tokenizer.close();

        return tokens;
    }
}
