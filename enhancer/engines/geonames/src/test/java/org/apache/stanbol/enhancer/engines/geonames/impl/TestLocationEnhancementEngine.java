/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.enhancer.engines.geonames.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PERSON;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PLACE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_REQUIRES;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENTITYANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.TextAnnotation;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.RdfEntityFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestLocationEnhancementEngine {

    private static final Logger log = LoggerFactory.getLogger(TestLocationEnhancementEngine.class);

    /**
     * The context for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String CONTEXT = "Dr. Patrick Marshall (1869 - November 1950) was a"
            + " geologist who lived in New Zealand and worked at the University of Otago.";

    /**
     * The person for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String PERSON = "Patrick Marshall";

    /**
     * The organisation for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String ORGANISATION = "University of Otago";

    /**
     * The place for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String PLACE = "New Zealand";


    static LocationEnhancementEngine locationEnhancementEngine = new LocationEnhancementEngine();

    @BeforeClass
    public static void setUpServices() throws IOException, ConfigurationException {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        // use the anonymous service for the unit tests
        properties.put(LocationEnhancementEngine.GEONAMES_USERNAME, 
            "\u0073\u0074\u0061\u006E\u0062\u006F\u006C");
        properties.put(LocationEnhancementEngine.GEONAMES_TOKEN, 
            "\u0073\u0074\u006E\u0062\u006C\u002E\u0075\u0074");
        MockComponentContext context = new MockComponentContext(properties);
        locationEnhancementEngine.activate(context);
    }

    @AfterClass
    public static void shutdownServices() {
        locationEnhancementEngine.deactivate(null);
    }

    public static ContentItem getContentItem(final String id,
            final String text) {
    	return new InMemoryContentItem(id, text, "text/plain");
    }

    public static void getTextAnnotation(ContentItem ci, String name, String context, UriRef type) {
        String content;
        try {
            content = IOUtils.toString(ci.getStream(),"UTF-8");
        } catch (IOException e) {
            //should never happen anyway!
            content = "";
        }
        RdfEntityFactory factory = RdfEntityFactory.createInstance(ci.getMetadata());
        TextAnnotation testAnnotation = factory.getProxy(new UriRef("urn:org.apache:stanbol.enhancer:test:text-annotation:person"), TextAnnotation.class);
        testAnnotation.setCreator(new UriRef("urn:org.apache:stanbol.enhancer:test:dummyEngine"));
        testAnnotation.setCreated(new Date());
        testAnnotation.setSelectedText(name);
        testAnnotation.setSelectionContext(context);
        testAnnotation.getDcType().add(type);
        Integer start = content.indexOf(name);
        if (start < 0) { //if not found in the content
            //set some random numbers for start/end
            start = (int) Math.random() * 100;
        }
        testAnnotation.setStart(start);
        testAnnotation.setEnd(start + name.length());
    }

    @Test
    public void testLocationEnhancementEngine() {//throws Exception{
        //create a content item
        ContentItem ci = getContentItem("urn:org.apache:stanbol.enhancer:text:content-item:person", CONTEXT);
        //add three text annotations to be consumed by this test
        getTextAnnotation(ci, PERSON, CONTEXT, DBPEDIA_PERSON);
        getTextAnnotation(ci, ORGANISATION, CONTEXT, DBPEDIA_ORGANISATION);
        getTextAnnotation(ci, PLACE, CONTEXT, DBPEDIA_PLACE);
        //perform the computation of the enhancements
        try {
            locationEnhancementEngine.computeEnhancements(ci);
        } catch (EngineException e) {
            if (e.getCause() instanceof UnknownHostException) {
                log.warn("Unable to test LocationEnhancemetEngine when offline! -> skipping this test", e.getCause());
                return;
            } else if (e.getCause() instanceof SocketTimeoutException) {
                log.warn("Seams like the geonames.org webservice is currently unavailable -> skipping this test", e.getCause());
                return;
            } else if (e.getMessage().contains("overloaded with requests")) {
                log.warn(
                        "Seams like the geonames.org webservice is currently unavailable -> skipping this test",
                        e.getCause());
                return;
            }
        }
        /*
         * Note:
         *  - Expected results depend on the geonames.org data. So if the test
         *    fails it may also mean that the data provided by geonames.org have
         *    changed
         */
        int entityAnnotationCount = checkAllEntityAnnotations(ci.getMetadata());
        //two suggestions for New Zealand and one hierarchy entry for the first
        //suggestion
        assertEquals(3, entityAnnotationCount);
    }

    /*
     * -----------------------------------------------------------------------
     * Helper Methods to check Text and EntityAnnotations
     * -----------------------------------------------------------------------
     */

    private int checkAllEntityAnnotations(MGraph g) {
        Iterator<Triple> entityAnnotationIterator = g.filter(null,
                RDF_TYPE, ENHANCER_ENTITYANNOTATION);
        int entityAnnotationCount = 0;
        while (entityAnnotationIterator.hasNext()) {
            UriRef entityAnnotation = (UriRef) entityAnnotationIterator.next().getSubject();
            // test if selected Text is added
            checkEntityAnnotation(g, entityAnnotation);
            entityAnnotationCount++;
        }
        return entityAnnotationCount;
    }

    /**
     * Checks if an entity annotation is valid
     */
    private void checkEntityAnnotation(MGraph g, UriRef entityAnnotation) {
        Iterator<Triple> relationIterator = g.filter(
                entityAnnotation, DC_RELATION, null);
        Iterator<Triple> requiresIterator = g.filter(
                entityAnnotation, DC_REQUIRES, null);
        // check if the relation or an requires annotation set
        assertTrue(relationIterator.hasNext() || requiresIterator.hasNext());
        while (relationIterator.hasNext()) {
            // test if the referred annotations are text annotations
            UriRef referredTextAnnotation = (UriRef) relationIterator.next().getObject();
            assertTrue(g.filter(referredTextAnnotation, RDF_TYPE,
                    ENHANCER_TEXTANNOTATION).hasNext());
        }

        // test if an entity is referred
        Iterator<Triple> entityReferenceIterator = g.filter(entityAnnotation,
                ENHANCER_ENTITY_REFERENCE, null);
        assertTrue(entityReferenceIterator.hasNext());
        // test if the reference is an URI
        assertTrue(entityReferenceIterator.next().getObject() instanceof UriRef);
        // test if there is only one entity referred
        assertFalse(entityReferenceIterator.hasNext());

        // finally test if the entity label is set
        Iterator<Triple> entityLabelIterator = g.filter(entityAnnotation,
                ENHANCER_ENTITY_LABEL, null);
        assertTrue(entityLabelIterator.hasNext());
    }

}
