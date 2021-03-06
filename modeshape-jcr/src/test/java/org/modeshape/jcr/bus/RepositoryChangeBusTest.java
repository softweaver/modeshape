/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.jcr.bus;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.value.basic.JodaDateTime;

/**
 * Unit test for {@link RepositoryChangeBus}
 * 
 * @author Horia Chiorean
 */
public class RepositoryChangeBusTest {

    private static final String WORKSPACE1 = "ws1";
    private static final String WORKSPACE2 = "ws2";

    private RepositoryChangeBus changeBus;

    @Before
    public void beforeEach() {
        changeBus = createRepositoryChangeBus();
    }

    protected RepositoryChangeBus createRepositoryChangeBus() {
        return new RepositoryChangeBus(Executors.newCachedThreadPool(), null);
    }

    @After
    public void afterEach() {
        changeBus.shutdown();
    }

    @Test
    public void shouldNotAllowTheSameListenerTwice() throws Exception {
        TestListener listener1 = new TestListener();

        assertTrue(getChangeBus().register(listener1));
        assertFalse(getChangeBus().register(listener1));

        TestListener listener2 = new TestListener();
        assertTrue(getChangeBus().register(listener2));
        assertFalse(getChangeBus().register(listener2));

        assertFalse(getChangeBus().register(null));
    }

    @Test
    public void shouldAllowListenerRemoval() throws Exception {
        TestListener listener1 = new TestListener();

        assertTrue(getChangeBus().register(listener1));
        assertTrue(getChangeBus().unregister(listener1));

        TestListener listener2 = new TestListener();
        assertFalse(getChangeBus().unregister(listener2));
    }

    @Test
    public void shouldNotifyAllRegisteredListenersKeepingEventOrder() throws Exception {
        TestListener listener1 = new TestListener(4);
        getChangeBus().register(listener1);

        TestListener listener2 = new TestListener(4);
        getChangeBus().register(listener2);

        getChangeBus().notify(new TestChangeSet(WORKSPACE1));
        getChangeBus().notify(new TestChangeSet(WORKSPACE1));

        getChangeBus().notify(new TestChangeSet(WORKSPACE2));
        getChangeBus().notify(new TestChangeSet(WORKSPACE2));

        assertChangesDispatched(listener1);
        assertChangesDispatched(listener2);
    }

    @Test
    public void shouldOnlyDispatchEventsAfterListenerRegistration() throws Exception {
        getChangeBus().notify(new TestChangeSet(WORKSPACE1));

        TestListener listener1 = new TestListener(4);
        getChangeBus().register(listener1);

        getChangeBus().notify(new TestChangeSet(WORKSPACE1));
        getChangeBus().notify(new TestChangeSet(WORKSPACE1));

        TestListener listener2 = new TestListener(2);
        getChangeBus().register(listener2);

        getChangeBus().notify(new TestChangeSet(WORKSPACE2));
        getChangeBus().notify(new TestChangeSet(WORKSPACE2));

        assertChangesDispatched(listener1);
        assertChangesDispatched(listener2);
    }

    @Test
    public void shouldDispatchEventsIfWorkspaceNameIsMissing() throws Exception {
        TestListener listener = new TestListener(2);
        getChangeBus().register(listener);

        getChangeBus().notify(new TestChangeSet(null));
        getChangeBus().notify(new TestChangeSet(null));

        assertChangesDispatched(listener);
    }

    @Test
    public void shouldNotDispatchEventsAfterListenerRemoval() throws Exception {
        TestListener listener1 = new TestListener(3);
        getChangeBus().register(listener1);

        TestListener listener2 = new TestListener(2);
        getChangeBus().register(listener2);

        getChangeBus().notify(new TestChangeSet(WORKSPACE1));
        getChangeBus().notify(new TestChangeSet(WORKSPACE2));

        getChangeBus().unregister(listener2);

        getChangeBus().notify(new TestChangeSet(WORKSPACE2));

        assertChangesDispatched(listener1);
        assertChangesDispatched(listener2);
    }

    @Test
    public void shouldNotDispatchEventsIfShutdown() throws Exception {
        TestListener listener = new TestListener(1);
        getChangeBus().register(listener);

        getChangeBus().notify(new TestChangeSet(WORKSPACE1));

        getChangeBus().shutdown();

        getChangeBus().notify(new TestChangeSet(WORKSPACE2));

        assertChangesDispatched(listener);
    }

    protected ChangeBus getChangeBus() throws Exception {
        return changeBus;
    }

    private void assertChangesDispatched( TestListener listener ) throws InterruptedException {
        listener.await();

        List<TestChangeSet> receivedChanges = listener.getObservedChangeSet();
        Map<String, List<Long>> changeSetsPerWs = new HashMap<String, List<Long>>();

        for (TestChangeSet changeSet : receivedChanges) {
            String wsName = changeSet.getWorkspaceName();
            List<Long> receivedTimes = changeSetsPerWs.get(wsName);
            if (receivedTimes == null) {
                receivedTimes = new ArrayList<Long>();
                changeSetsPerWs.put(wsName, receivedTimes);
            }
            for (Long receivedTime : receivedTimes) {
                assertTrue(receivedTime <= changeSet.getTimestamp().getMilliseconds());
            }
            receivedTimes.add(changeSet.getTimestamp().getMilliseconds());
        }
    }

    protected static class TestChangeSet implements ChangeSet {

        private static final long serialVersionUID = 1L;

        private final String workspaceName;
        private final DateTime dateTime;

        protected TestChangeSet( String workspaceName ) {
            this.workspaceName = workspaceName;
            this.dateTime = new JodaDateTime(System.currentTimeMillis());
        }

        @Override
        public Set<NodeKey> changedNodes() {
            return Collections.emptySet();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public String getUserId() {
            return null;
        }

        @Override
        public Map<String, String> getUserData() {
            return Collections.emptyMap();
        }

        @Override
        public DateTime getTimestamp() {
            return dateTime;
        }

        @Override
        public String getProcessKey() {
            return null;
        }

        @Override
        public String getRepositoryKey() {
            return null;
        }

        @Override
        public String getWorkspaceName() {
            return workspaceName;
        }

        @Override
        public Iterator<Change> iterator() {
            return Collections.<Change>emptySet().iterator();
        }

        @Override
        public String getJournalId() {
            return null;
        }

        @Override
        public String getUUID() {
            return null;
        }

        @Override
        public boolean equals( Object o ) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestChangeSet changes = (TestChangeSet)o;

            if (!dateTime.equals(changes.dateTime)) return false;
            if (!workspaceName.equals(changes.workspaceName)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = workspaceName.hashCode();
            result = 31 * result + dateTime.hashCode();
            return result;
        }
    }

    protected static class TestListener implements ChangeSetListener {
        private final List<TestChangeSet> receivedChangeSet;
        private CountDownLatch latch;

        public TestListener() {
            this(0);
        }

        protected TestListener( int expectedNumberOfChangeSet ) {
            latch = new CountDownLatch(expectedNumberOfChangeSet);
            receivedChangeSet = new ArrayList<TestChangeSet>();
        }

        public void expectChangeSet( int expectedNumberOfChangeSet ) {
            latch = new CountDownLatch(expectedNumberOfChangeSet);
            receivedChangeSet.clear();
        }

        @Override
        public void notify( ChangeSet changeSet ) {
            if (!(changeSet instanceof TestChangeSet)) {
                throw new IllegalArgumentException("Invalid type of change set received");
            }
            receivedChangeSet.add((TestChangeSet)changeSet);
            latch.countDown();
        }

        public void await() throws InterruptedException {
            latch.await(350, TimeUnit.MILLISECONDS);
        }

        public List<TestChangeSet> getObservedChangeSet() {
            return receivedChangeSet;
        }
    }
}
