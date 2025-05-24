/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

// import java.io.IOException; // JcrValue might need this, but current usage is string-based.
// import java.security.AccessControlContext; // REMOVED - Not used
import java.util.Arrays; // For STANDARD_DESCRIPTORS_ARRAY initialization
import java.util.Collections;
import java.util.HashSet; // For STANDARD_DESCRIPTORS set initialization
import java.util.Map; // For method signatures like getPredefinedWorkspaceNames
// import java.util.Set; // No longer needed directly, STANDARD_DESCRIPTORS uses java.util.Set
// import java.util.concurrent.ExecutionException; // REMOVED - Not used
import java.util.concurrent.Future; // For method signatures (shutdown, backup)
import java.util.concurrent.TimeUnit; // For method signatures (shutdown)
import java.util.concurrent.CompletableFuture; // For stubbed Future returns

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PropertyType; // For JcrValue creation
import javax.jcr.RepositoryException;
import javax.jcr.Session;
// JCR API standard descriptors are referenced via javax.jcr.Repository.*

import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
// import org.modeshape.common.i18n.I18n; // REMOVED - Not used
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ModeShapeEngine.State; // For getState()
// import org.modeshape.jcr.RepositoryConfiguration.FieldName; // REMOVED - Not used
import org.modeshape.jcr.api.Repository; // Implemented interface
import org.modeshape.jcr.api.RepositoryManager; // For method signatures (stubbed)
import org.modeshape.jcr.api.RestoreOptions; // For method signatures (stubbed)
// import org.modeshape.jcr.api.Workspace; // REMOVED - Not used in signatures
import org.modeshape.jcr.api.monitor.RepositoryMonitor; // For method signatures (stubbed)
import org.modeshape.jcr.api.query.Query; // For QueryLanguage inner class
import org.modeshape.jcr.api.sequencer.SequencerManager; // For method signatures (stubbed)
import org.modeshape.jcr.api.text.TextExtractorManager; // For method signatures (stubbed)
import org.modeshape.jcr.api.index.IndexManager; // For method signatures (stubbed)

import org.modeshape.jcr.value.ValueFactories; // For JcrValue creation in getDescriptorValue/s
// import org.modeshape.schematic.document.Array; // REMOVED
// import org.modeshape.schematic.document.Changes; // REMOVED
// import org.modeshape.schematic.document.Editor; // REMOVED
// import org.modeshape.schematic.document.Path; // REMOVED
// import org.modeshape.schematic.internal.document.Paths; // REMOVED

/**
 *
 */
public class JcrRepository implements org.modeshape.jcr.api.Repository {

    private static final String[] STANDARD_DESCRIPTORS_ARRAY = {
        javax.jcr.Repository.REP_NAME_DESC,
        javax.jcr.Repository.REP_VENDOR_DESC,
        javax.jcr.Repository.REP_VENDOR_URL_DESC,
        javax.jcr.Repository.REP_VERSION_DESC,
        javax.jcr.Repository.SPEC_NAME_DESC,
        javax.jcr.Repository.SPEC_VERSION_DESC,
        javax.jcr.Repository.OPTION_TRANSACTIONS_SUPPORTED_DESC,
        javax.jcr.Repository.OPTION_VERSIONING_SUPPORTED_DESC,
        javax.jcr.Repository.OPTION_OBSERVATION_SUPPORTED_DESC,
        javax.jcr.Repository.OPTION_LOCKING_SUPPORTED_DESC,
        javax.jcr.Repository.QUERY_LANGUAGES,
        javax.jcr.Repository.NODE_TYPE_MANAGEMENT_AUTOCREATED_DEFINITIONS_SUPPORTED,
        javax.jcr.Repository.NODE_TYPE_MANAGEMENT_INHERITANCE,
        javax.jcr.Repository.NODE_TYPE_MANAGEMENT_MULTIPLE_BINARY_PROPERTIES_SUPPORTED,
        javax.jcr.Repository.NODE_TYPE_MANAGEMENT_MULTIVALUED_PROPERTIES_SUPPORTED,
        javax.jcr.Repository.NODE_TYPE_MANAGEMENT_ORDERABLE_CHILD_NODES_SUPPORTED,
        javax.jcr.Repository.NODE_TYPE_MANAGEMENT_OVERRIDES_SUPPORTED,
        javax.jcr.Repository.NODE_TYPE_MANAGEMENT_PRIMARY_ITEM_NAME_SUPPORTED,
        javax.jcr.Repository.NODE_TYPE_MANAGEMENT_PROPERTY_TYPES,
        javax.jcr.Repository.NODE_TYPE_MANAGEMENT_RESIDUAL_DEFINITIONS_SUPPORTED,
        javax.jcr.Repository.NODE_TYPE_MANAGEMENT_SAME_NAME_SIBLINGS_SUPPORTED,
        javax.jcr.Repository.NODE_TYPE_MANAGEMENT_UPDATE_IN_USE_SUPPORTED,
        javax.jcr.Repository.NODE_TYPE_MANAGEMENT_VALUE_CONSTRAINTS_SUPPORTED,
        javax.jcr.Repository.QUERY_STORED_QUERIES_SUPPORTED,
        javax.jcr.Repository.QUERY_JOINS_SUPPORTED
        // Add other JCR 2.0 standard descriptors if needed.
    };
    private static final java.util.Set<String> STANDARD_DESCRIPTORS = 
        Collections.unmodifiableSet(new java.util.HashSet<>(java.util.Arrays.asList(STANDARD_DESCRIPTORS_ARRAY)));


    /**
     * The set of supported query language string constants.
     *
     * @see javax.jcr.query.QueryManager#getSupportedQueryLanguages()
     * @see javax.jcr.query.QueryManager#createQuery(String, String)
     */
    public static final class QueryLanguage {
        /**
         * The standard JCR 1.0 XPath query language.
         */
        @SuppressWarnings( "deprecation" )
        public static final String XPATH = Query.XPATH;

        /**
         * The SQL dialect that is based upon an enhanced version of the JCR-SQL query language defined by the JCR 1.0.1
         * specification.
         */
        @SuppressWarnings( "deprecation" )
        public static final String JCR_SQL = Query.SQL;

        /**
         * The SQL dialect that is based upon an enhanced version of the JCR-SQL2 query language defined by the JCR 2.0
         * specification.
         */
        public static final String JCR_SQL2 = Query.JCR_SQL2;

        /**
         * The enhanced Query Object Model language defined by the JCR 2.0 specification.
         */
        public static final String JCR_JQOM = Query.JCR_JQOM;
        /**
         * The full-text search language defined as part of the abstract query model, in Section 6.7.19 of the JCR 2.0
         * specification.
         */
        public static final String SEARCH = Query.FULL_TEXT_SEARCH;
    }

    // protected static final Set<String> MISSING_JAAS_POLICIES = new CopyOnWriteArraySet<String>(); // JAAS related
    // private static final boolean AUTO_START_REPO_UPON_LOGIN = true; // ModeShape lifecycle - REMOVED
    // private static final String INTERNAL_WORKER_USERNAME = "<modeshape-worker>"; // ModeShape internal session - REMOVED

    protected final Logger logger;
    // private final AtomicReference<RepositoryConfiguration> config = new AtomicReference<RepositoryConfiguration>(); // REMOVED
    // private final AtomicReference<String> repositoryName = new AtomicReference<String>(); // REMOVED - replaced by repositoryNameResolved
    // private final Map<String, Object> descriptors; // REMOVED - will delegate to Oak
    // private final AtomicReference<RunningState> runningState = new AtomicReference<RunningState>(); // REMOVED
    // private final AtomicReference<State> state = new AtomicReference<State>(State.NOT_RUNNING); // REMOVED - getState will return fixed state
    // private final Lock stateLock = new ReentrantLock(); // REMOVED
    // private final AtomicBoolean allowAutoStartDuringLogin = new AtomicBoolean(AUTO_START_REPO_UPON_LOGIN); // REMOVED
    // private Problems configurationProblems = null; // REMOVED

    private final javax.jcr.Repository oakRepository;
    private final String repositoryNameResolved;


    /**
     * Create a Repository instance given the underlying Oak {@link javax.jcr.Repository}.
     *
     * @param oakRepository the underlying Oak repository; may not be null
     * @throws RepositoryException if there is a problem
     */
    protected JcrRepository( javax.jcr.Repository oakRepository ) throws RepositoryException {
        this.logger = Logger.getLogger(getClass());
        if (oakRepository == null) {
            // Using a simple string as JcrI18n might be removed.
            throw new IllegalArgumentException("Oak repository instance cannot be null.");
        }
        this.oakRepository = oakRepository;
        String name = oakRepository.getDescriptor(javax.jcr.Repository.REP_NAME_DESC);
        this.repositoryNameResolved = (name != null) ? name : "OakRepository"; 
        this.logger.debug("Wrapping Oak repository: '{0}'", this.repositoryNameResolved);
    }

    // void setConfigurationProblems( Problems configurationProblems ) { // REMOVED
    // }

    // RepositoryConfiguration repositoryConfiguration() { // REMOVED
    // }

    /**
     * Get the state of this JCR repository instance.
     *
     * @return the state; never null
     */
    public State getState() {
        // If we have an Oak repository instance, we assume it's running.
        // ModeShape's complex lifecycle (starting, stopped, etc.) is not managed here.
        return State.RUNNING;
    }

    /**
     * Get the name of this JCR repository instance.
     *
     * @return the name; never null
     */
    @Override
    public String getName() {
        return this.repositoryNameResolved;
    }

    @Override
    public int getActiveSessionsCount() {
        // This information might not be directly available from a plain javax.jcr.Repository.
        // Oak's specific Repository implementation might offer this, but we're sticking to the JCR API.
        logger.warn("getActiveSessionsCount() is not supported in the current Oak-backed implementation and will return 0.");
        return 0; 
    }

    /**
     * Get the component that can be used to obtain statistics for this repository.
     * <p>
     * Note that this provides un-checked access to the statistics, unlike {@link RepositoryManager#getRepositoryMonitor()} in the
     * public API which only exposes the statistics if the session's user has administrative privileges.
     * </p>
     *
     * @return the statistics component; never null
     * @throws IllegalStateException if the repository is not {@link #getState() running}
     * @see Workspace#getRepositoryManager()
     * @see RepositoryManager#getRepositoryMonitor()
     */
    public RepositoryStatistics getRepositoryStatistics() {
        // return statistics();
        logger.warn("ModeShape-specific RepositoryStatistics not available in Oak-backed version.");
        return null; 
    }

    /**
     * Starts this repository instance (if not already started) and returns all the possible startup problems & warnings which did
     * not prevent the repository from starting up.
     * <p>
     * The are 2 general categories of issues that can be logged as problems:
     * <ul>
     * <li>configuration warnings - any warnings raised by the structure of the repository configuration file</li>
     * <li>startup warnings/error - any warnings/errors raised by various repository components which didn't prevent them from
     * starting up, but could mean they are only partly initialized.</li>
     * </ul>
     * </p>
     *
     * @return a {@link Problems} instance which may contains errors and warnings raised by various components; may be empty if
     *         nothing unusual happened during start but never {@code null}
     * @throws Exception if there is a problem with underlying resource setup
     */
    public org.modeshape.common.collection.Problems getStartupProblems() throws Exception {
        // The concept of startup problems as ModeShape defined it (related to its own engine)
        // is not directly applicable here as the Oak repo is already started.
        logger.warn("getStartupProblems() is not directly applicable in Oak-backed version. Returning empty problems.");
        return new SimpleProblems();
    }

    /**
     * Start this repository instance.
     *
     * @throws Exception if there is a problem with underlying resource setup
     */
    void start() throws Exception {
        // The Oak repository is assumed to be started by the JcrRepositoryFactory.
        logger.info("start() called on Oak-backed JcrRepository. Lifecycle is managed externally.");
    }

    /**
     * Terminate all active sessions.
     *
     * @return a future representing the asynchronous session termination process.
     */
    Future<Boolean> shutdown() {
        // The Oak repository's lifecycle is managed externally.
        logger.warn("shutdown() called on Oak-backed JcrRepository. Lifecycle is managed externally.");
        // Return a completed future indicating success as this class doesn't manage the lifecycle.
        return CompletableFuture.completedFuture(true); 
    }

    /**
     * Apply the supplied changes to this repository's configuration, and if running change the services to reflect the updated
     * configuration. Note that this method assumes the proposed changes have already been validated; see
     * {@link RepositoryConfiguration#validate(Changes)}.
     *
     * @param changes the changes for the configuration
     * @throws Exception if there is a problem with underlying resources
     * @see ModeShapeEngine#update(String, Changes)
     */
    void apply( Changes changes ) throws Exception {
        throw new UnsupportedOperationException("Repository configuration changes via apply(Changes) are not supported in this Oak-backed version.");
    }

    // All ModeShape-specific internal state and lifecycle management methods are REMOVED or commented out.
    // protected final RunningState doStart() throws Exception { ... } // REMOVED
    // protected final boolean doShutdown(boolean rollback) { ... } // REMOVED
    // public Transactions transactions() { ... } // REMOVED
    // protected final DocumentStore documentStore() { ... } // REMOVED
    // protected final String repositoryName() { ... } // REMOVED (Use this.repositoryNameResolved)
    // protected final RepositoryCache repositoryCache() { ... } // REMOVED
    // protected final RepositoryStatistics statistics() { ... } // REMOVED
    // protected final RepositoryNodeTypeManager nodeTypeManager() { ... } // REMOVED
    // protected final RepositoryQueryManager queryManager() { ... } // REMOVED
    // protected final RepositoryLockManager lockManager() { ... } // REMOVED
    // protected final NamespaceRegistry persistentRegistry() { ... } // REMOVED
    // protected final String systemWorkspaceName() { ... } // REMOVED
    // protected final String systemWorkspaceKey() { ... } // REMOVED
    // protected final ChangeBus changeBus() { ... } // REMOVED
    // protected final String repositoryKey() { ... } // REMOVED
    // protected final JcrRepository.RunningState runningState() { ... } // REMOVED
    // protected final boolean hasWorkspace( String workspaceName ) { ... } // REMOVED
    // protected final NodeCache workspaceCache( String workspaceName ) { ... } // REMOVED
    // final SessionCache createSystemSession( ExecutionContext context, boolean readOnly ) { ... } // REMOVED
    // protected final TransactionManager transactionManager() { ... } // REMOVED
    
    protected final void prepareToRestore() throws RepositoryException {
        throw new UnsupportedOperationException("prepareToRestore not supported in Oak-backed version");
    }

    // protected final String journalId() { ... } // REMOVED
    // protected final ChangeJournal journal() { ... } // REMOVED
    
    protected final boolean mimeTypeDetectionEnabled() {
        // This functionality would depend on how/if Oak is configured for it.
        logger.warn("mimeTypeDetectionEnabled() may not reflect actual Oak configuration.");
        return true; // Defaulting to true, assuming Oak might do this.
    }

    protected final void completeRestore(RestoreOptions options) throws Exception {
        throw new UnsupportedOperationException("completeRestore not supported in Oak-backed version");
    }

    /**
     * Get the immutable configuration for this repository.
     *
     * @return the configuration; never null
     */
    public RepositoryConfiguration getConfiguration() {
        throw new UnsupportedOperationException("ModeShape RepositoryConfiguration not available in Oak-backed version.");
    }

    @Override
    public String getDescriptor( String key ) {
        return this.oakRepository.getDescriptor(key);
    }

    @Override
    public JcrValue getDescriptorValue( String key ) {
        // This needs adaptation or removal, as JcrValue is ModeShape specific.
        // For now, try to adapt or throw UnsupportedOperationException.
        String value = this.oakRepository.getDescriptor(key);
        if (value == null) return null;
        try {
            // This is a simplification. A proper implementation would need to know the actual type
            // of the descriptor in Oak and map it correctly to a JcrValue.
            // ModeShape's JcrValue is specific, and direct mapping from Oak's string descriptor might be lossy.
            // For now, attempting conversion to String JcrValue as a best effort.
            return new JcrValue(new ExecutionContext().getValueFactories(), PropertyType.STRING, value);
        } catch (Exception e) {
            logger.warn("Could not create JcrValue for descriptor key '" + key + "' with value '" + value + "'. " +
                        "This descriptor might not be representable as a JcrValue or ExecutionContext/ValueFactories are not available.", e);
            // As per instructions, returning null if conversion is not robust.
            // Consider throwing UnsupportedOperationException if this behavior is problematic.
            return null; 
        }
    }

    @Override
    public JcrValue[] getDescriptorValues( String key ) {
        // Oak's getDescriptor always returns a single String or null.
        // To mimic multi-value, we'd need to know which descriptors ARE multi-valued from Oak's perspective.
        // JCR 2.0 spec for Repository.QUERY_LANGUAGES implies it can be multi-valued.
        if (javax.jcr.Repository.QUERY_LANGUAGES.equals(key)) {
            try {
                ValueFactories factories = new ExecutionContext().getValueFactories();
                // This is a guess at what Oak might support. A more robust solution
                // would query Oak's capabilities if possible.
                 return new JcrValue[] {valueFor(factories, QueryLanguage.XPATH), 
                                       valueFor(factories, QueryLanguage.JCR_SQL2),
                                       valueFor(factories, QueryLanguage.JCR_JQOM)};
            } catch (Exception e) {
                logger.warn("Could not create JcrValue array for descriptor " + key, e);
                return null;
            }
        }
        String value = this.oakRepository.getDescriptor(key);
        if (value == null) return null;
        try {
            return new JcrValue[] { new JcrValue(new ExecutionContext().getValueFactories(), PropertyType.STRING, value) };
        } catch (Exception e) {
            logger.warn("Could not create JcrValue array for descriptor " + key, e);
            return null;
        }
    }

    @Override
    public boolean isSingleValueDescriptor( String key ) {
        // In JCR API, getDescriptor returns String (implying single value for that key if it exists).
        // getDescriptorValues is for multi-valued descriptors.
        // Oak's JcrRepositoryBase.getDescriptorValues handles some keys as multi-valued.
        // For simplicity, we'll assume most are single unless explicitly known.
        if (javax.jcr.Repository.QUERY_LANGUAGES.equals(key)) {
            return false; 
        }
        // For other keys, assume they are single if getDescriptor returns non-null
        return this.oakRepository.getDescriptor(key) != null;
    }

    @Override
    public boolean isStandardDescriptor( String key ) {
        // Check against the standard JCR descriptor keys.
        return STANDARD_DESCRIPTORS.contains(key);
    }

    @Override
    public String[] getDescriptorKeys() {
        return this.oakRepository.getDescriptorKeys();
    }

    @Override
    public Session login() throws RepositoryException {
        logger.debug("Login to Oak repository '{0}' with no credentials and default workspace", getName());
        return this.oakRepository.login();
    }

    @Override
    public Session login( Credentials credentials ) throws RepositoryException {
        logger.debug("Login to Oak repository '{0}' with credentials and default workspace", getName());
        return this.oakRepository.login(credentials);
    }

    @Override
    public Session login( String workspaceName ) throws RepositoryException {
        logger.debug("Login to Oak repository '{0}' with no credentials and workspace '{1}'", getName(), workspaceName);
        return this.oakRepository.login(workspaceName);
    }

    /**
     * @see javax.jcr.Repository#login(javax.jcr.Credentials, java.lang.String)
     */
    @Override
    public Session login( final Credentials credentials, String workspaceName ) throws RepositoryException {
        logger.debug("Login to Oak repository '{0}' with credentials and workspace '{1}'", getName(), workspaceName);
        return this.oakRepository.login(credentials, workspaceName);
    }

    // private String validateWorkspaceName( RunningState runningState,
    //                                       String workspaceName ) throws RepositoryException {
    //     // This was tied to ModeShape's RunningState and default/system workspace logic
    //     // Oak will handle workspace validation.
    //     if (workspaceName == null) {
    //         // Oak's login(credentials, null) handles default workspace
    //         return null; 
    //     }
    //     // Any other validation specific to ModeShape (like system workspace) is removed.
    //     return workspaceName;
    // }

    // protected static class ConfigurationChange implements Editor.Observer { ... } // REMOVED
    // All fields and methods of ConfigurationChange are REMOVED.

    // @SuppressWarnings( "deprecation" )
    // private void initializeDescriptors() { ... } // REMOVED
    
    // private static JcrValue valueFor( ValueFactories valueFactories, // REMOVED
    //                                   int type,
    //                                   Object value ) {
    //     // return new JcrValue(valueFactories, type, value); // REMOVED
    // }

    private static JcrValue valueFor( ValueFactories valueFactories,
                                      String value ) {
        // This one is used by getDescriptorValues for QUERY_LANGUAGES
        return new JcrValue(valueFactories, PropertyType.STRING, value);
    }

    // private static JcrValue valueFor( ValueFactories valueFactories, // REMOVED
    //                                   boolean value ) {
    //     // return valueFor(valueFactories, PropertyType.BOOLEAN, value); // REMOVED
    // }
    
    // protected void refreshWorkspaces() { ... } // REMOVED
    // private void repositoryNameChanged() { ... } // REMOVED

    // @Immutable
    // protected class RunningState { ... } // REMOVED

    // protected class JcrRepositoryEnvironment implements RepositoryEnvironment { ... } // REMOVED

    // private final class InternalSecurityContext implements SecurityContext { ... } // REMOVED

    /**
     * Determine the initial delay before the garbage collection process(es) should be run, based upon the supplied initial
     * expression. Note that the initial expression specifies the hours and minutes in local time, whereas this method should
     * return the delay in milliseconds after the current time.
     *
     * @param initialTimeExpression the expression of the form "<code>hh:mm</code>"; never null
     * @return the number of milliseconds after now that the process(es) should be started
     */
    // protected long determineInitialDelay( String initialTimeExpression ) { ... } // REMOVED

    /**
     * The garbage collection tasks should get cancelled before the repository is shut down, but just in case we'll use a weak
     * reference to hold onto the JcrRepository instance and we'll also check that the repository is running before we actually do
     * any work.
     */
    // protected static abstract class BackgroundRepositoryTask implements Runnable { ... } // REMOVED
    // protected static class BinaryValueGarbageCollectionTask extends BackgroundRepositoryTask { ... } // REMOVED
    // protected static class LockGarbageCollectionTask extends BackgroundRepositoryTask { ... } // REMOVED
    // protected static class OptimizationTask extends BackgroundRepositoryTask { ... } // REMOVED
    // protected static class JournalingGCTask extends BackgroundRepositoryTask { ... } // REMOVED
    
    // Implementation of org.modeshape.jcr.api.Repository methods not in javax.jcr.Repository
    
    @Override
    public RepositoryManager getRepositoryManager() {
        logger.warn("getRepositoryManager() is not supported in the current Oak-backed implementation.");
        throw new UnsupportedOperationException("RepositoryManager is not available in this configuration.");
    }

    @Override
    public RepositoryMonitor getMonitor() {
        logger.warn("getMonitor() is not supported in the current Oak-backed implementation.");
        throw new UnsupportedOperationException("RepositoryMonitor is not available in this configuration.");
    }
    
    @Override
    public org.modeshape.common.collection.Problems getProblems() {
        logger.warn("getProblems() is not supported in the current Oak-backed implementation.");
        return new SimpleProblems(); // Return empty problems
    }
    
    @Override
    public Future<Boolean> shutdown(long timeout, TimeUnit unit) {
        logger.warn("shutdown(long, TimeUnit) is not supported in the current Oak-backed implementation.");
        return CompletableFuture.completedFuture(true);
    }
    
    @Override
    public SequencerManager getSequencerManager() {
        logger.warn("getSequencerManager() is not supported in the current Oak-backed implementation.");
        throw new UnsupportedOperationException("SequencerManager is not available in this configuration.");
    }
    
    @Override
    public TextExtractorManager getTextExtractorManager() {
        logger.warn("getTextExtractorManager() is not supported in the current Oak-backed implementation.");
        throw new UnsupportedOperationException("TextExtractorManager is not available in this configuration.");
    }
    
    @Override
    public IndexManager getIndexManager() {
        logger.warn("getIndexManager() is not supported in the current Oak-backed implementation.");
        throw new UnsupportedOperationException("IndexManager is not available in this configuration.");
    }

    @Override
    public Future<?> backup(java.io.File backupLocation, String nameOfRepositoryBeingBackedUp) throws RepositoryException {
        logger.warn("backup(...) is not supported in the current Oak-backed implementation.");
        throw new UnsupportedOperationException("Backup functionality is not available in this configuration.");
    }

    @Override
    public Map<String,String> restore(java.io.File backupLocation, RestoreOptions options) throws RepositoryException {
        logger.warn("restore(...) is not supported in the current Oak-backed implementation.");
        throw new UnsupportedOperationException("Restore functionality is not available in this configuration.");
    }

    @Override
    public Map<String,java.util.Set<String>> getPredefinedWorkspaceNames() {
        logger.warn("getPredefinedWorkspaceNames() is not supported in the current Oak-backed implementation.");
        return Collections.emptyMap();
    }

    @Override
    public java.util.Set<String> getAccessibleWorkspaceNames() {
        logger.warn("getAccessibleWorkspaceNames() is not supported in the current Oak-backed implementation.");
        // The previous attempt to implement this by creating a session is removed to simplify.
        // As per general guidelines for this refactoring, non-JCR standard API methods are stubbed or made unsupported.
        throw new UnsupportedOperationException("getAccessibleWorkspaceNames is not available in this configuration.");
    }
}
