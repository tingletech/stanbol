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
package org.apache.stanbol.commons.solr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.commons.solr.utils.ServiceReferenceRankingComparator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks the {@link CoreContainer} of the {@link SolrCore} referenced by the
 * parsed {@link IndexReference}. All getService** and getServiceReference**
 * methods do consider {@link Constants#SERVICE_RANKING}. <p>
 * 
 * @author Rupert Westenthaler
 *
 */
public class RegisteredSolrServerTracker extends ServiceTracker {

    /**
     * In case <code>{@link IndexReference#isPath()} == true</code> than we need
     * to track registered {@link SolrCore}s, because we do not know the
     * {@link CoreContainer} in advance. In all other cases the
     * {@link CoreContainer} is tracked. This variable avoids instanceof checks
     */
    private final boolean trackingSolrCore;
    /**
     * needed to create {@link EmbeddedSolrServer} instances
     */
    private final String coreName;
    /**
     * Creates a new Tracker for the parsed {@link IndexReference}
     * @param context the BundleContext used for tracking
     * @param reference the index reference
     * @throws InvalidSyntaxException if the {@link Filter} could not be
     * created for the parsed {@link IndexReference}.
     * @throws IllegalArgumentException if the parsed {@link IndexReference} is 
     * <code>null</code>
     */
    public RegisteredSolrServerTracker(BundleContext context, IndexReference reference) throws InvalidSyntaxException{
        super(context,
            reference != null ? 
                    reference.isPath() ? context.createFilter(reference.getIndexFilter()) : 
                        context.createFilter(reference.getIndexFilter()) :
                            null ,null);
        if(reference == null){
            throw new IllegalArgumentException("The parsed IndexReference MUST NOT be NULL!");
        }
        if(reference.isPath()){
            trackingSolrCore = true;
            coreName = null;
        } else {
            trackingSolrCore = true;
            coreName = reference.getIndex();
        }
    }
    
    @Override
    public SolrServer addingService(ServiceReference reference) {
        String coreName;
        CoreContainer server;
        if(trackingSolrCore){
            SolrCore core = (SolrCore)context.getService(reference);
            coreName = core.getName();
            CoreDescriptor descriptior = core.getCoreDescriptor();
            if(descriptior == null){ //core not registered with a container!
                context.ungetService(reference);
                return null; //ignore
            } else {
                server = descriptior.getCoreContainer();
            }
        } else {
            server = (CoreContainer)context.getService(reference);
            coreName = this.coreName;
        }
        return new EmbeddedSolrServer(server, coreName);
    }

    /**
     * Overrides to provides a Array sorted by {@link Constants#SERVICE_RANKING}
     * @see ServiceTracker#getServiceReferences()
     */
    @Override
    public ServiceReference[] getServiceReferences() {
        ServiceReference[] refs = super.getServiceReferences();
        if(refs != null && refs.length > 1){
            Arrays.sort(refs,ServiceReferenceRankingComparator.INSTANCE);
        }
        return refs;
    }
    /**
     * Overrides to provide the {@link ServiceReference} with the highest
     * {@link Constants#SERVICE_RANKING}.
     * @see ServiceTracker#getServiceReference()
     */
    @Override
    public ServiceReference getServiceReference() {
        ServiceReference[] refs = super.getServiceReferences();
        if(refs != null && refs.length > 0){
            return refs[0];
        } else {
            return null;
        }
    }
    /**
     * Overrides to provide the SolrServer with the highest 
     * {@link Constants#SERVICE_RANKING}.
     * @see ServiceTracker#getService()
     */
    @Override
    public SolrServer getService() {
        ServiceReference ref = getServiceReference();
        return ref == null ? null : (SolrServer)getService(ref);
    }
    
    /**
     * Overrides to provide a {@link SolrServer} instead of {@link Object}
     * @see ServiceTracker#getService(ServiceReference)
     */
    @Override
    public SolrServer getService(ServiceReference reference) {
        return reference == null ? null : (SolrServer)super.getService(reference);
    }
    /**
     * Overrides to provide a array of {@link SolrServer} that is sorted by
     * {@link Constants#SERVICE_RANKING}.
     * @see ServiceTracker#getServices()
     */
    @Override
    public SolrServer[] getServices() {
        ServiceReference[] refs = getServiceReferences();
        Collection<SolrServer> servers = new ArrayList<SolrServer>(refs.length);
        if(refs != null){
            for(ServiceReference ref : refs){
                SolrServer server = getService(ref);
                if(server != null){
                    servers.add(server);
                } //else null ... ignore
            }
        }
        return servers.isEmpty() ? null : servers.toArray(new SolrServer[servers.size()]);
    }
}
