// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.lang.utils.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.typedef.*;
import org.gridgain.grid.typedef.internal.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.tostring.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;
import static org.gridgain.grid.cache.GridCacheTxState.*;

/**
 * Managed transaction adapter.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.09012012
 */
public abstract class GridCacheTxAdapter<K, V> extends GridMetadataAwareAdapter
    implements GridCacheTxEx<K, V>, Externalizable {
    /** Static logger to avoid re-creation. */
    private static final AtomicReference<GridLogger> logRef = new AtomicReference<GridLogger>();

    /** Transaction ID. */
    @GridToStringInclude
    protected GridCacheVersion xidVer;

    /** Implicit flag. */
    @GridToStringInclude
    protected boolean implicit;

    /** Implicit with one key flag. */
    @GridToStringInclude
    protected boolean implicitSingle;

    /** Local flag. */
    @GridToStringInclude
    protected boolean local;

    /** Thread ID. */
    @GridToStringInclude
    protected long threadId;

    /** Thread name. */
    @GridToStringInclude
    protected String threadName; // TODO: remove.

    /** Transaction start time. */
    @GridToStringInclude
    protected long startTime = System.currentTimeMillis();

    /** Node ID. */
    @GridToStringInclude
    protected UUID nodeId;

    /** Transaction counter value at the start of transaction. */
    @GridToStringInclude
    protected GridCacheVersion startVer;

    /** Cache registry. */
    @GridToStringExclude
    protected GridCacheContext<K, V> cctx;

    /** Logger. */
    @SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})
    protected GridLogger log;

    /**
     * End version (a.k.a. <tt>'tnc'</tt> or <tt>'transaction number counter'</tt>)
     * assigned to this transaction at the end of write phase.
     */
    @GridToStringInclude
    protected volatile GridCacheVersion endVer;

    /** Isolation. */
    @GridToStringInclude
    protected GridCacheTxIsolation isolation = READ_COMMITTED;

    /** Concurrency. */
    @GridToStringInclude
    protected GridCacheTxConcurrency concurrency = PESSIMISTIC;

    /** Transaction timeout. */
    @GridToStringInclude
    protected long timeout;

    /** Invalidate flag. */
    protected volatile boolean invalidate;

    /** Invalidation flag for system invalidations (not user-based ones). */
    private boolean sysInvalidate;

    /** */
    protected volatile boolean swapEnabled;

    /** */
    protected volatile boolean storeEnabled;

    /** Internal flag. */
    protected volatile boolean internal;

    /** Commit version. */
    private AtomicReference<GridCacheVersion> commitVer = new AtomicReference<GridCacheVersion>(null);

    /** Done marker. */
    protected final AtomicBoolean isDone = new AtomicBoolean(false);

    /** */
    private AtomicBoolean finalizing = new AtomicBoolean();

    /** */
    private Set<Integer> invalidParts = new GridLeanSet<Integer>();

    /**
     * Transaction state. Note that state is not protected, as we want to
     * always use {@link #state()} and {@link #state(GridCacheTxState)}
     * methods.
     */
    @GridToStringInclude
    private volatile GridCacheTxState state = ACTIVE;

    /** Timed out flag. */
    private volatile boolean timedOut;

    /** */
    private AtomicReference<GridFutureAdapter<GridCacheTx>> finFut =
        new AtomicReference<GridFutureAdapter<GridCacheTx>>();

    private AtomicLong topVer = new AtomicLong(-1);

    /** Mutex. */
    private final Lock lock = new ReentrantLock();

    /** Lock condition. */
    private final Condition cond = lock.newCondition();

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    protected GridCacheTxAdapter() {
        // No-op.
    }

    /**
     * @param cctx Cache registry.
     * @param xidVer Transaction ID.
     * @param implicit Implicit flag.
     * @param implicitSingle Implicit with one key flag.
     * @param local Local flag.
     * @param concurrency Concurrency.
     * @param isolation Isolation.
     * @param timeout Timeout.
     * @param invalidate Invalidation policy.
     * @param swapEnabled Whether to use swap storage.
     * @param storeEnabled Whether to use read/write through.
     */
    protected GridCacheTxAdapter(
        GridCacheContext<K, V> cctx,
        GridCacheVersion xidVer,
        boolean implicit,
        boolean implicitSingle,
        boolean local,
        GridCacheTxConcurrency concurrency,
        GridCacheTxIsolation isolation,
        long timeout,
        boolean invalidate,
        boolean swapEnabled,
        boolean storeEnabled) {
        assert xidVer != null;
        assert cctx != null;

        this.cctx = cctx;
        this.xidVer = xidVer;
        this.implicit = implicit;
        this.implicitSingle = implicitSingle;
        this.local = local;
        this.concurrency = concurrency;
        this.isolation = isolation;
        this.timeout = timeout;
        this.invalidate = invalidate;
        this.swapEnabled = swapEnabled;
        this.storeEnabled = storeEnabled;

        startVer = cctx.versions().last();

        nodeId = cctx.discovery().localNode().id();

        Thread t = Thread.currentThread();

        threadId = t.getId();
        threadName = t.getName();

        log = U.logger(cctx.kernalContext(), logRef, this);
    }

    /**
     * @param cctx Cache registry.
     * @param nodeId Node ID.
     * @param xidVer Transaction ID.
     * @param startVer Start version mark.
     * @param threadId Thread ID.
     * @param concurrency Concurrency.
     * @param isolation Isolation.
     * @param timeout Timeout.
     * @param invalidate Invalidation policy.
     * @param swapEnabled Swap enabled flag.
     * @param storeEnabled Store enabled (read/write through) flag.
     */
    protected GridCacheTxAdapter(
        GridCacheContext<K, V> cctx,
        UUID nodeId,
        GridCacheVersion xidVer,
        GridCacheVersion startVer,
        long threadId,
        GridCacheTxConcurrency concurrency,
        GridCacheTxIsolation isolation,
        long timeout,
        boolean invalidate,
        boolean swapEnabled,
        boolean storeEnabled
    ) {
        this.cctx = cctx;
        this.nodeId = nodeId;
        this.threadId = threadId;
        this.xidVer = xidVer;
        this.startVer = startVer;
        this.concurrency = concurrency;
        this.isolation = isolation;
        this.timeout = timeout;
        this.invalidate = invalidate;
        this.swapEnabled = swapEnabled;
        this.storeEnabled = storeEnabled;

        implicit = false;
        implicitSingle = false;
        local = false;

        threadName = Thread.currentThread().getName();

        log = U.logger(cctx.kernalContext(), logRef, this);
    }

    /**
     * Acquires lock.
     */
    @SuppressWarnings({"LockAcquiredButNotSafelyReleased"})
    protected final void lock() {
        lock.lock();
    }

    /**
     * Releases lock.
     */
    protected final void unlock() {
        lock.unlock();
    }

    /**
     * Signals all waiters.
     */
    protected final void signalAll() {
        cond.signalAll();
    }

    /**
     * Waits for signal.
     *
     * @throws InterruptedException If interrupted.
     */
    protected final void awaitSignal() throws InterruptedException {
        cond.await();
    }

    /**
     * Waits for signal.
     *
     * @param ms Time to wait.
     * @return {@code True} if signal occurred.
     * @throws InterruptedException If interrupted.
     */
    protected final boolean awaitSignal(long ms) throws InterruptedException {
        return cond.await(ms, TimeUnit.MILLISECONDS);
    }

    /** {@inheritDoc} */
    @Override public UUID otherNodeId() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public Collection<UUID> nodeIds() {
        assert false;

        return null;
    }

    @Override public long topologyVersion() {
        return topVer.get();
    }

    /** {@inheritDoc} */
    @Override public long topologyVersion(long topVer) {
        this.topVer.compareAndSet(-1, topVer);

        return this.topVer.get();
    }

    /**
     * @return {@code True} if marked.
     */
    @Override public boolean markFinalizing() {
        if (finalizing.compareAndSet(false, true)) {
            if (log.isDebugEnabled())
                log.debug("Marked transaction as finalized: " + this);

            return true;
        }

        if (log.isDebugEnabled())
            log.debug("Transaction was not marked finalized: " + this);

        return false;
    }

    /**
     * @return {@code True} if transaction has at least one key enlisted.
     */
    public abstract boolean isStarted();

    /**
     * @return Logger.
     */
    protected GridLogger log() {
        return log;
    }

    /** {@inheritDoc} */
    @Override public boolean near() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean implicit() {
        return implicit;
    }

    /** {@inheritDoc} */
    @Override public boolean implicitSingle() {
        return implicitSingle;
    }

    /** {@inheritDoc} */
    @Override public boolean local() {
        return local;
    }

    /** {@inheritDoc} */
    @Override public final boolean user() {
        return !implicit() && local() && !dht() && !internal();
    }

    /** {@inheritDoc} */
    @Override public boolean dht() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean enforceSerializable() {
        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean syncCommit() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean syncRollback() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public GridUuid xid() {
        return xidVer.id();
    }

    /** {@inheritDoc} */
    @Override public Set<Integer> invalidPartitions() {
        return invalidParts;
    }

    /** {@inheritDoc} */
    @Override public void addInvalidPartition(int part) {
        invalidParts.add(part);

        if (log.isDebugEnabled())
            log.debug("Added invalid partition for transaction [part=" + part + ", tx=" + this + ']');
    }

    /** {@inheritDoc} */
    @Override public long startTime() {
        return startTime;
    }

    /**
     * Gets remaining allowed transaction time.
     *
     * @return Remaining transaction time.
     */
    @Override public long remainingTime() {
        if (timeout() <= 0)
            return -1;

        long timeLeft = timeout() - (System.currentTimeMillis() - startTime());

        if (timeLeft < 0)
            return 0;

        return timeLeft;
    }

    /**
     * @return Lock timeout.
     */
    protected long lockTimeout() {
        long timeout = remainingTime();

        return timeout < 0 ? 0 : timeout == 0 ? -1 : timeout;
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion xidVersion() {
        return xidVer;
    }

    /** {@inheritDoc} */
    @Override public long threadId() {
        return threadId;
    }

    /** {@inheritDoc} */
    @Override public UUID nodeId() {
        return nodeId;
    }

    /** {@inheritDoc} */
    @Override public GridCacheTxIsolation isolation() {
        return isolation;
    }

    /** {@inheritDoc} */
    @Override public GridCacheTxConcurrency concurrency() {
        return concurrency;
    }

    /** {@inheritDoc} */
    @Override public long timeout() {
        return timeout;
    }

    /** {@inheritDoc} */
    @Override public long timeout(long timeout) {
        if (isStarted())
            throw new IllegalStateException("Cannot change timeout after transaction has started: " + this);

        long old = this.timeout;

        this.timeout = timeout;

        return old;
    }

    /** {@inheritDoc} */
    @Override public boolean ownsLock(GridCacheEntryEx<K, V> entry) throws GridCacheEntryRemovedException {
        GridCacheTxEntry<K, V> txEntry = entry(entry.key());

        GridCacheVersion explicit = txEntry == null ? null : txEntry.explicitVersion();

        return local() && !cctx.isDht() ?
            entry.lockedByThread(threadId()) || (explicit != null && entry.lockedBy(explicit)) :
            // If candidate is not there, then lock was explicit.
            // Otherwise, check if entry is owned by version.
            !entry.hasLockCandidate(xidVersion()) || entry.lockedBy(xidVersion());
    }

    /** {@inheritDoc} */
    @Override public boolean ownsLockUnsafe(GridCacheEntryEx<K, V> entry) {
        GridCacheTxEntry<K, V> txEntry = entry(entry.key());

        GridCacheVersion explicit = txEntry == null ? null : txEntry.explicitVersion();

        return local() && !cctx.isDht() ?
            entry.lockedByThreadUnsafe(threadId()) || (explicit != null && entry.lockedByUnsafe(explicit)) :
            // If candidate is not there, then lock was explicit.
            // Otherwise, check if entry is owned by version.
            !entry.hasLockCandidateUnsafe(xidVersion()) || entry.lockedByUnsafe(xidVersion());
    }

    /** {@inheritDoc} */
    @Override public GridCacheTxState state() {
        return state;
    }

    /** {@inheritDoc} */
    @Override public boolean setRollbackOnly() {
        return state(MARKED_ROLLBACK);
    }

    /**
     * @return {@code True} if rollback only flag is set.
     */
    @Override public boolean isRollbackOnly() {
        return state == MARKED_ROLLBACK || state == ROLLING_BACK || state == ROLLED_BACK;
    }

    /** {@inheritDoc} */
    @Override public boolean done() {
        return isDone.get();
    }

    /**
     * @return Commit version.
     */
    @Override public GridCacheVersion commitVersion() {
        initCommitVersion();

        return commitVer.get();
    }

    /**
     * @param commitVer Commit version.
     * @return {@code True} if set to not null value.
     */
    @Override public boolean commitVersion(GridCacheVersion commitVer) {
        return commitVer != null && this.commitVer.compareAndSet(null, commitVer);
    }

    /**
     *
     */
    public void initCommitVersion() {
        if (ec()) {
            if (commitVer.get() == null)
                commitVer.compareAndSet(null, cctx.versions().next());
        }
        else {
            commitVer.compareAndSet(null, xidVer);
        }
    }


    /**
     *
     */
    @Override public void end() throws GridException {
        GridCacheTxState state = state();

        if (state != ROLLING_BACK && state != ROLLED_BACK && state != COMMITTING && state != COMMITTED)
            rollback();

        awaitCompletion();
    }

    /** {@inheritDoc} */
    @Override public boolean needsCompletedVersions() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public void completedVersions(GridCacheVersion base, Collection<GridCacheVersion> committed,
        Collection<GridCacheVersion> txs) {
        /* No-op. */
    }

    /**
     * Awaits transaction completion.
     *
     * @throws GridException If waiting failed.
     */
    protected void awaitCompletion() throws GridException {
        lock();

        try {
            while (!done())
                awaitSignal();
        }
        catch (InterruptedException e) {
            if (!done())
                throw new GridException("Got interrupted while waiting for transaction to complete: " + this, e);
        }
        finally {
            unlock();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean internal() {
        return internal;
    }

    /**
     * @param key Key.
     * @return {@code True} if key is internal.
     */
    protected boolean checkInternal(K key) {
        if (key instanceof GridCacheInternal) {
            internal = true;

            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean ec() {
        return concurrency == EVENTUALLY_CONSISTENT;
    }

    /** {@inheritDoc} */
    @Override public boolean optimistic() {
        return concurrency == OPTIMISTIC;
    }

    /** {@inheritDoc} */
    @Override public boolean pessimistic() {
        return concurrency == PESSIMISTIC;
    }

    /** {@inheritDoc} */
    @Override public boolean serializable() {
        return isolation == SERIALIZABLE;
    }

    /** {@inheritDoc} */
    @Override public boolean repeatableRead() {
        return isolation == REPEATABLE_READ;
    }

    /** {@inheritDoc} */
    @Override public boolean readCommitted() {
        return isolation == READ_COMMITTED;
    }

    /** {@inheritDoc} */
    @Override public boolean state(GridCacheTxState state) {
        return state(state, false);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<GridCacheTx> finishFuture() {
        GridFutureAdapter<GridCacheTx> fut = finFut.get();

        if (fut == null) {
            if (!finFut.compareAndSet(null, fut = new GridFutureAdapter<GridCacheTx>()))
                fut = finFut.get();
        }

        assert fut != null;

        if (isDone.get())
            fut.onDone(this);

        return fut;
    }

    /**
     *
     * @param state State to set.
     * @param timedOut Timeout flag.
     * @return {@code True} if state changed.
     */
    @SuppressWarnings({"NotifyWithoutCorrespondingWait", "NonPrivateFieldAccessedInSynchronizedContext", "TooBroadScope"})
    private boolean state(GridCacheTxState state, boolean timedOut) {
        boolean valid = false;

        GridCacheTxState prev;

        boolean notify = false;

        lock();

        try {
            prev = this.state;

            switch (state) {
                case ACTIVE: {
                    valid = false;

                    break;
                } // Active is initial state and cannot be transitioned to.
                case PREPARING: {
                    valid = prev == ACTIVE;

                    break;
                }
                case PREPARED: {
                    valid = prev == PREPARING;

                    break;
                }
                case COMMITTING: {
                    valid = prev == PREPARED;

                    break;
                }

                case UNKNOWN: {
                    if (isDone.compareAndSet(false, true))
                        notify = true;

                    valid = prev == ROLLING_BACK || prev == COMMITTING;

                    break;
                }

                case COMMITTED: {
                    if (isDone.compareAndSet(false, true))
                        notify = true;

                    valid = prev == COMMITTING;

                    break;
                }

                case ROLLED_BACK: {
                    if (isDone.compareAndSet(false, true))
                        notify = true;

                    valid = prev == ROLLING_BACK;

                    break;
                }

                case MARKED_ROLLBACK: {
                    valid = prev == ACTIVE || prev == PREPARING || prev == PREPARED || prev == COMMITTING;

                    break;
                }

                case ROLLING_BACK: {
                    valid =
                        prev == ACTIVE || prev == MARKED_ROLLBACK || prev == PREPARING ||
                            prev == PREPARED || prev == COMMITTING;

                    break;
                }
            }

            if (valid) {
                this.state = state;
                this.timedOut = timedOut;

                if (log.isDebugEnabled())
                    log.debug("Changed transaction state [prev=" + prev + ", new=" + this.state + ", tx=" + this + ']');

                // Notify of state change.
                signalAll();
            }
            else {
                if (log.isDebugEnabled())
                    log.debug("Invalid transaction state transition [invalid=" + state + ", cur=" + this.state +
                        ", tx=" + this + ']');
            }
        }
        finally {
            unlock();
        }

        if (notify) {
            GridFutureAdapter<GridCacheTx> fut = finFut.get();

            if (fut != null)
                fut.onDone(this);
        }

        if (valid) {
            // Seal transactions maps.
            if (state != ACTIVE)
                seal();

            cctx.tm().onTxStateChange(prev, state, this);
        }

        return valid;
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion startVersion() {
        return startVer;
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion endVersion() {
        return endVer;
    }

    /** {@inheritDoc} */
    @Override public void endVersion(GridCacheVersion endVer) {
        this.endVer = endVer;
    }

    /** {@inheritDoc} */
    @Override public GridUuid timeoutId() {
        return xidVer.id();
    }

    /** {@inheritDoc} */
    @Override public long endTime() {
        long endTime = timeout == 0 ? Long.MAX_VALUE : startTime + timeout;

        return endTime > 0 ? endTime : endTime < 0 ? Long.MAX_VALUE : endTime;
    }

    /** {@inheritDoc} */
    @Override public void onTimeout() {
        state(MARKED_ROLLBACK, true);
    }

    /** {@inheritDoc} */
    @Override public boolean timedOut() {
        return timedOut;
    }

    /** {@inheritDoc} */
    @Override public void invalidate(boolean invalidate) {
        if (isStarted() && !dht())
            throw new IllegalStateException("Cannot change invalidation flag after transaction has started: " + this);

        this.invalidate = invalidate;
    }

    /** {@inheritDoc} */
    @Override public boolean isInvalidate() {
        return invalidate;
    }

    /** {@inheritDoc} */
    @Override public boolean isSystemInvalidate() {
        return sysInvalidate;
    }

    /** {@inheritDoc} */
    @Override public void systemInvalidate(boolean sysInvalidate) {
        this.sysInvalidate = sysInvalidate;
    }

    /**
     * @param e Transaction entry.
     * @param primaryOnly Flag to include backups into check or not.
     * @return {@code True} if entry is locally mapped as a primary or back up node.
     */
    protected boolean isNearLocallyMapped(GridCacheTxEntry<K, V> e, boolean primaryOnly) {
        if (!near())
            return false;

        // Try to take either entry-recorded primary node ID,
        // or transaction node ID from near-local transactions.
        UUID nodeId = e.nodeId() == null ? local() ? this.nodeId :  null : e.nodeId();

        if (nodeId != null && nodeId.equals(cctx.nodeId()))
            return true;

        GridCacheEntryEx<K, V> cached = e.cached();

        int part = cached != null ? cached.partition() : cctx.partition(e.key());

        Collection<GridRichNode> affNodes = cctx.affinity(part, CU.allNodes(cctx));

        if (primaryOnly) {
            GridRichNode primary = F.first(affNodes);

            assert primary != null : "Primary node is null for affinity nodes: " + affNodes;

            return primary.isLocal();
        }
        else
            return F.contains(affNodes, cctx.localNode());
    }

    /**
     * @param e Entry to evict if it qualifies for eviction.
     * @param primaryOnly Flag to try to evict only on primary node.
     * @return {@code True} if attempt was made to evict the entry.
     * @throws GridException If failed.
     */
    protected boolean evictNearEntry(GridCacheTxEntry<K, V> e, boolean primaryOnly) throws GridException {
        assert e != null;

        if (isNearLocallyMapped(e, primaryOnly)) {
            GridCacheEntryEx<K, V> cached = e.cached();

            if (log.isDebugEnabled())
                log.debug("Evicting dht-local entry from near cache [entry=" + cached + ", tx=" + this + ']');

            if (cached != null && cached.markObsolete(xidVer, true))
                return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        writeExternalMeta(out);

        out.writeObject(xidVer);
        out.writeBoolean(invalidate);
        out.writeLong(timeout);
        out.writeLong(threadId);
        out.writeLong(startTime);

        U.writeUuid(out, nodeId);

        out.write(isolation.ordinal());
        out.write(concurrency.ordinal());
        out.write(state().ordinal());
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        readExternalMeta(in);

        xidVer = (GridCacheVersion)in.readObject();
        invalidate = in.readBoolean();
        timeout = in.readLong();
        threadId = in.readLong();
        startTime = in.readLong();

        nodeId = U.readUuid(in);

        isolation = GridCacheTxIsolation.fromOrdinal(in.read());
        concurrency = GridCacheTxConcurrency.fromOrdinal(in.read());

        state = GridCacheTxState.fromOrdinal(in.read());
    }

    /**
     * Reconstructs object on demarshalling.
     *
     * @return Reconstructed object.
     * @throws ObjectStreamException Thrown in case of demarshalling error.
     */
    protected Object readResolve() throws ObjectStreamException {
        return new TxShadow(
            xidVer.id(),
            nodeId,
            threadId,
            startTime,
            isolation,
            concurrency,
            invalidate,
            implicit,
            timeout,
            state(),
            isRollbackOnly()
        );
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        return o == this || (o instanceof GridCacheTxAdapter && xidVer.equals(((GridCacheTxAdapter)o).xidVer));
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return xidVer.hashCode();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return GridToStringBuilder.toString(GridCacheTxAdapter.class, this, "duration",
            (System.currentTimeMillis() - startTime) + "ms");
    }

    /**
     * Transaction shadow class to be used for deserialization.
     */
    private static class TxShadow extends GridMetadataAwareAdapter implements GridCacheTx {
        /** Xid. */
        private final GridUuid xid;

        /** Node ID. */
        private final UUID nodeId;

        /** Thread ID. */
        private final long threadId;

        /** Start time. */
        private final long startTime;

        /** Transaction isolation. */
        private final GridCacheTxIsolation isolation;

        /** Concurrency. */
        private final GridCacheTxConcurrency concurrency;

        /** Invalidate flag. */
        private final boolean invalidate;

        /** Timeout. */
        private final long timeout;

        /** State. */
        private final GridCacheTxState state;

        /** Rollback only flag. */
        private final boolean rollbackOnly;

        /** Implicit flag. */
        private final boolean implicit;

        /**
         * @param xid Xid.
         * @param nodeId Node ID.
         * @param threadId Thread ID.
         * @param startTime Start time.
         * @param isolation Isolation.
         * @param concurrency Concurrency.
         * @param invalidate Invalidate flag.
         * @param implicit Implicit flag.
         * @param timeout Transaction timeout.
         * @param state Transaction state.
         * @param rollbackOnly Rollback-only flag.
         */
        TxShadow(GridUuid xid, UUID nodeId, long threadId, long startTime, GridCacheTxIsolation isolation,
            GridCacheTxConcurrency concurrency, boolean invalidate, boolean implicit, long timeout,
            GridCacheTxState state, boolean rollbackOnly) {
            this.xid = xid;
            this.nodeId = nodeId;
            this.threadId = threadId;
            this.startTime = startTime;
            this.isolation = isolation;
            this.concurrency = concurrency;
            this.invalidate = invalidate;
            this.implicit = implicit;
            this.timeout = timeout;
            this.state = state;
            this.rollbackOnly = rollbackOnly;
        }

        /** {@inheritDoc} */
        @Override public GridUuid xid() {
            return xid;
        }

        /** {@inheritDoc} */
        @Override public UUID nodeId() {
            return nodeId;
        }

        /** {@inheritDoc} */
        @Override public long threadId() {
            return threadId;
        }

        /** {@inheritDoc} */
        @Override public long startTime() {
            return startTime;
        }

        /** {@inheritDoc} */
        @Override public GridCacheTxIsolation isolation() {
            return isolation;
        }

        /** {@inheritDoc} */
        @Override public GridCacheTxConcurrency concurrency() {
            return concurrency;
        }

        /** {@inheritDoc} */
        @Override public boolean isInvalidate() {
            return invalidate;
        }

        /** {@inheritDoc} */
        @Override public boolean implicit() {
            return implicit;
        }

        /** {@inheritDoc} */
        @Override public long timeout() {
            return timeout;
        }

        /** {@inheritDoc} */
        @Override public GridCacheTxState state() {
            return state;
        }

        /** {@inheritDoc} */
        @Override public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        /** {@inheritDoc} */
        @Override public long timeout(long timeout) {
            throw new IllegalStateException("Deserialized transaction can only be used as read-only.");
        }

        /** {@inheritDoc} */
        @Override public boolean setRollbackOnly() {
            throw new IllegalStateException("Deserialized transaction can only be used as read-only.");
        }

        /** {@inheritDoc} */
        @Override public void commit() {
            throw new IllegalStateException("Deserialized transaction can only be used as read-only.");
        }

        /** {@inheritDoc} */
        @Override public void end() {
            throw new IllegalStateException("Deserialized transaction can only be used as read-only.");
        }

        /** {@inheritDoc} */
        @Override public GridFuture<GridCacheTx> commitAsync() {
            throw new IllegalStateException("Deserialized transaction can only be used as read-only.");
        }

        /** {@inheritDoc} */
        @Override public void rollback() {
            throw new IllegalStateException("Deserialized transaction can only be used as read-only.");
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            return this == o || o instanceof GridCacheTx && xid.equals(((GridCacheTx)o).xid());
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return xid.hashCode();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(TxShadow.class, this);
        }
    }
}
