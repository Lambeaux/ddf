/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.forms.model;

import ddf.catalog.data.Metacard;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.opengis.filter.v_2_0.FilterType;
import org.codice.ddf.catalog.ui.forms.data.AttributeGroupMetacard;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacard;
import org.codice.ddf.catalog.ui.forms.filter.FilterProcessingException;
import org.codice.ddf.catalog.ui.forms.filter.FilterReader;
import org.codice.ddf.catalog.ui.forms.filter.FilterWriter;
import org.codice.ddf.catalog.ui.forms.filter.VisitableJsonElementImpl;
import org.codice.ddf.catalog.ui.forms.filter.VisitableXmlElementImpl;
import org.codice.ddf.catalog.ui.forms.model.pojo.FieldFilter;
import org.codice.ddf.catalog.ui.forms.model.pojo.FormTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transform metacards into the data model expected by the frontend.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class TemplateTransformer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TemplateTransformer.class);

  public static boolean invalidFormTemplate(Metacard metacard) {
    return new TemplateTransformer().toFormTemplate(metacard) == null;
  }

  @Nullable
  public Metacard toQueryTemplateMetacard(Map<String, Object> formTemplate) {
    Map<String, Object> filterJson = (Map) formTemplate.get("filterTemplate");
    String title = (String) formTemplate.get("title");
    String description = (String) formTemplate.get("description");
    TransformVisitor<JAXBElement> visitor = new TransformVisitor<>(new XmlModelBuilder());
    try {
      VisitableJsonElementImpl.create(new FilterNodeMapImpl(filterJson)).accept(visitor);
      JAXBElement filter = visitor.getResult();
      if (!filter.getDeclaredType().equals(FilterType.class)) {
        LOGGER.error(
            "Error occurred during filter processing, root type should be a {} but was {}",
            FilterType.class.getName(),
            filter.getDeclaredType().getName());
        return null;
      }
      FilterWriter writer = new FilterWriter();
      String filterXml = writer.marshal(filter, true);
      QueryTemplateMetacard metacard = new QueryTemplateMetacard(title, description);
      metacard.setCreatedDate(new Date());
      metacard.setFormsFilter(filterXml);
      return metacard;
    } catch (JAXBException e) {
      LOGGER.error("XML generation failed for query template metacard's filter", e);
    } catch (FilterProcessingException e) {
      LOGGER.error("Could not use filter JSON for template - {}", e.getMessage());
    } catch (UnsupportedOperationException e) {
      LOGGER.error(
          "Could not use filter JSON because it contains unsupported operations - {}",
          e.getMessage());
    }
    return null;
  }

  /** Convert a query template metacard into the JSON representation of FormTemplate. */
  @Nullable
  public FormTemplate toFormTemplate(Metacard metacard) {
    if (!QueryTemplateMetacard.isQueryTemplateMetacard(metacard)) {
      LOGGER.debug("Metacard {} was not a query template metacard", metacard.getId());
      return null;
    }
    QueryTemplateMetacard wrapped = new QueryTemplateMetacard(metacard);
    TransformVisitor<FilterNode> visitor = new TransformVisitor<>(new JsonModelBuilder());
    try {
      FilterReader reader = new FilterReader();
      JAXBElement<FilterType> root =
          reader.unmarshalFilter(
              new ByteArrayInputStream(wrapped.getFormsFilter().getBytes("UTF-8")));
      VisitableXmlElementImpl.create(root).accept(visitor);
      return new FormTemplate(wrapped, visitor.getResult());
    } catch (JAXBException | UnsupportedEncodingException e) {
      LOGGER.error(
          "XML parsing failed for query template metacard's filter, with metacard id "
              + metacard.getId(),
          e);
    } catch (FilterProcessingException e) {
      LOGGER.error(
          "Could not use filter XML for template - {} [metacard id = {}]",
          e.getMessage(),
          metacard.getId());
    } catch (UnsupportedOperationException e) {
      LOGGER.error(
          "Could not use filter XML because it contains unsupported operations - {} [metacard id = {}]",
          e.getMessage(),
          metacard.getId());
    }
    return null;
  }

  @Nullable
  public Metacard toAttributeGroupMetacard(FieldFilter fieldFilter) {
    AttributeGroupMetacard metacard =
        new AttributeGroupMetacard(fieldFilter.getTitle(), fieldFilter.getDescription());
    metacard.setCreatedDate(fieldFilter.getCreated());
    metacard.setGroupDescriptors(fieldFilter.getDescriptors());
    return metacard;
  }

  /** Convert an attribute group metacard into the JSON representation of FieldFilter. */
  @Nullable
  public FieldFilter toFieldFilter(Metacard metacard) {
    if (!AttributeGroupMetacard.isAttributeGroupMetacard(metacard)) {
      LOGGER.debug("Metacard {} was not a result template metacard", metacard);
      return null;
    }
    AttributeGroupMetacard wrapped = new AttributeGroupMetacard(metacard);
    return new FieldFilter(wrapped, wrapped.getGroupDescriptors());
  }
}
