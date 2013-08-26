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
package org.apache.stanbol.enhancer.engines.lucenefstlinking.cache;

import org.apache.lucene.document.Document;
import org.apache.solr.search.FastLRUCache;
import org.apache.solr.search.SolrCache;

/**
 * Implementation of the {@link EntityCache} interface by using the
 * {@link SolrCache} API.
 * 
 * @author Rupert Westenthaler
 *
 */
public class SolrEntityCache implements EntityCache {

    private final SolrCache<Integer,Document> cache;
    private final Object version;
    private boolean closed;
    
    public SolrEntityCache(Object version, SolrCache<Integer,Document> cache) {
        this.cache = cache;
        this.version = version;
    }
    
    @Override
    public Object getVersion() {
        return version;
    }

    @Override
    public Document get(Integer docId) {
        return !closed ? cache.get(docId) : null;
    }

    @Override
    public void cache(Integer docId, Document doc) {
        if(!closed){
            cache.put(docId, doc);
        }
    }

    @Override
    public int size() {
        return cache.size();
    }
    @Override
    public String printStatistics() {
        return cache.getStatistics().toString();
    }
    
    @Override
    public String toString() {
        return cache.getDescription();
    }
    
    void close(){
        closed = true;
        cache.clear();
        cache.close();
    }
}
