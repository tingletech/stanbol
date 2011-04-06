package org.apache.stanbol.ontologymanager.ontonet.api;

import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyIndex;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScopeFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryLoader;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyManager;


/**
 * A KReS Ontology Network Manager.
 * 
 * @author andrea.nuzzolese
 * 
 */
public interface ONManager {

	/**
	 * Returns the default object that automatically indexes ontologies as they
	 * are loaded within scopes.
	 * 
	 * @return the default ontology index.
	 */
    OntologyIndex getOntologyIndex();

	/**
	 * Returns the ontology scope factory that was created along with the
	 * manager context.
	 * 
	 * @return the default ontology scope factory
	 */
    OntologyScopeFactory getOntologyScopeFactory();

	/**
	 * Returns the ontology space factory that was created along with the
	 * manager context.
	 * 
	 * @return the default ontology space factory.
	 */
    OntologySpaceFactory getOntologySpaceFactory();

	/**
	 * Returns the default ontology storage system for this KReS instance.
	 * 
	 * @return the default ontology store.
	 */
	public PersistenceStore getOntologyStore();

	/**
	 * Returns an OWL Ontology Manager that is never cleared of its ontologies,
	 * so it can be used for caching ontologies without having to reload them
	 * using other managers. It is sufficient to catch
	 * {@link OWLOntologyAlreadyExistsException}s and obtain the ontology with
	 * that same ID from this manager.
	 * 
	 * @return the OWL Ontology Manager used for caching ontologies.
	 */
    OWLOntologyManager getOwlCacheManager();

	/**
	 * Returns a factory object that can be used for obtaining OWL API objects.
	 * 
	 * @return the default OWL data factory
	 */
    OWLDataFactory getOwlFactory();

	/**
	 * Returns the default ontology registry loader.
	 * 
	 * @return the default ontology registry loader.
	 */
    RegistryLoader getRegistryLoader();

	/**
	 * Returns the unique ontology scope registry for this context.
	 * 
	 * @return the ontology scope registry.
	 */
    ScopeRegistry getScopeRegistry();

	/**
	 * Returns the unique KReS session manager for this context.
	 * 
	 * @return the KreS session manager.
	 */
    SessionManager getSessionManager();

	/**
	 * Returns the list of IRIs that identify scopes that should be activated on
	 * startup, <i>if they exist</i>.
	 * 
	 * @return the list of scope IDs to activate.
	 */
    String[] getUrisToActivate();
	
	/**
	 * Returns the String that represent the namespace used by KReS for its ontologies
	 * 
	 * @return the namespace of KReS.
	 */
    String getKReSNamespace();
}
