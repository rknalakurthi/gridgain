// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.processors.cache.distributed.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.lang.utils.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.typedef.*;
import org.gridgain.grid.typedef.internal.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.GridEventType.*;
import static org.gridgain.grid.util.GridConcurrentFactory.*;

/**
 * Manages lock order within a thread.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.09012012
 */
public class GridCacheMvccManager<K, V> extends GridCacheManager<K, V> {
    /** Maxim number of removed locks. */
    private static final int MAX_REMOVED_LOCKS = 10240;

    /** Pending locks per thread. */
    private ThreadLocal<Queue<GridCacheMvccCandidate<K>>> pending;

    /** Set of removed lock versions. */
    private Collection<GridCacheVersion> rmvLocks =
        new GridBoundedConcurrentOrderedSet<GridCacheVersion>(MAX_REMOVED_LOCKS);

    /** Current local candidates. */
    private Collection<GridCacheMvccCandidate<K>> dhtLocCands = new ConcurrentSkipListSet<GridCacheMvccCandidate<K>>();

    /** Locked keys. */
    @GridToStringExclude
    private final ConcurrentMap<K, GridDistributedCacheEntry<K, V>> locked = newMap();

    /** Active futures mapped by version ID. */
    @GridToStringExclude
    private final ConcurrentMap<GridUuid, Collection<GridCacheFuture<?>>> futs = newMap();

    /** Near to DHT version mapping. */
    private final ConcurrentMap<GridCacheVersion, GridCacheVersion> near2dht = newMap();

    /** Finish futures. */
    private final Queue<FinishLockFuture> finishFuts = new GridConcurrentLinkedDeque<FinishLockFuture>();

    /** Logger. */
    @SuppressWarnings( {"FieldAccessedSynchronizedAndUnsynchronized"})
    private GridLogger exchLog;

    /** Lock callback. */
    @GridToStringExclude
    private final GridCacheMvccCallback<K, V> callback = new GridCacheMvccCallback<K, V>() {
        /** {@inheritDoc} */
        @SuppressWarnings({"unchecked"})
        @Override public void onOwnerChanged(GridCacheEntryEx<K, V> entry, GridCacheMvccCandidate<K> prev,
            GridCacheMvccCandidate<K> owner) {
            assert entry != null;
            assert owner != prev : "New and previous owner are identical instances: " + owner;
            assert owner == null || prev == null || !owner.version().equals(prev.version()) :
                "New and previous owners have identical versions [owner=" + owner + ", prev=" + prev + ']';

            if (log.isDebugEnabled())
                log.debug("Received owner changed callback [" + entry.key() + ", owner=" + owner + ", prev=" +
                    prev + ']');

            if (owner != null && (owner.local() || owner.nearLocal())) {
                Collection<? extends GridCacheFuture> futCol = futs.get(owner.version().id());

                if (futCol != null) {
                    for (GridCacheFuture fut : futCol) {
                        if (fut instanceof GridCacheMvccFuture && !fut.isDone()) {
                            GridCacheMvccFuture<K, V, Boolean> mvccFut =
                                (GridCacheMvccFuture<K, V, Boolean>)fut;

                            // Since this method is called outside of entry synchronization,
                            // we can safely invoke any method on the future.
                            // Also note that we don't remove future here if it is done.
                            // The removal is initiated from within future itself.
                            if (mvccFut.onOwnerChanged(entry, owner))
                                return;
                        }
                    }
                }
            }

            if (log.isDebugEnabled())
                log.debug("Lock future not found for owner change callback (will try transaction futures) [owner=" +
                    owner + ", prev=" + prev + ", entry=" + entry + ']');

            // If no future was found, delegate to transaction manager.
            if (cctx.tm().onOwnerChanged(entry, owner)) {
                if (log.isDebugEnabled())
                    log.debug("Found transaction for changed owner: " + owner);
            }
            else if (log.isDebugEnabled())
                log.debug("Failed to find transaction for changed owner: " + owner);

            for (FinishLockFuture f : finishFuts)
                f.recheck(entry);
        }

        /** {@inheritDoc} */
        @Override public void onLocked(GridDistributedCacheEntry<K, V> entry) {
            locked.put(entry.key(), entry);
        }

        /** {@inheritDoc} */
        @Override public void onFreed(GridDistributedCacheEntry<K, V> entry) {
            locked.remove(entry.key());
        }
    };

    /** Discovery listener. */
    @GridToStringExclude private final GridLocalEventListener discoLsnr = new GridLocalEventListener() {
        @Override public void onEvent(GridEvent evt) {
            assert evt instanceof GridDiscoveryEvent;
            assert evt.type() == EVT_NODE_FAILED || evt.type() == EVT_NODE_LEFT;

            GridDiscoveryEvent discoEvt = (GridDiscoveryEvent)evt;

            if (log.isDebugEnabled())
                log.debug("Processing node left [nodeId=" + discoEvt.eventNodeId() + "]");

            for (GridDistributedCacheEntry<K, V> entry : locked.values()) {
                try {
                    entry.removeExplicitNodeLocks(discoEvt.eventNodeId());
                }
                catch (GridCacheEntryRemovedException ignore) {
                    if (log.isDebugEnabled())
                        log.debug("Attempted to remove node locks from removed entry in mvcc manager " +
                            "disco callback (will ignore): " + entry);
                }
            }

            for (Iterator<Collection<GridCacheFuture<?>>> i1 = futs.values().iterator(); i1.hasNext(); ) {
                Collection<? extends GridCacheFuture> futs = i1.next();

                for (Iterator<? extends GridCacheFuture> i2 = futs.iterator(); i2.hasNext(); ) {
                    GridCacheFuture fut = i2.next();

                    fut.onNodeLeft(discoEvt.eventNodeId());

                    if (fut.isCancelled() || fut.isDone())
                        i2.remove();
                }

                if (futs.isEmpty())
                    i1.remove();
            }
        }
    };

    /** {@inheritDoc} */
    @Override protected void start0() throws GridException {
        exchLog = cctx.logger(getClass().getName() + ".exchange");

        pending = cctx.isDht() ?
            new GridThreadLocal<Queue<GridCacheMvccCandidate<K>>>() {
                @Override protected Queue<GridCacheMvccCandidate<K>> initialValue() {
                    return new LinkedList<GridCacheMvccCandidate<K>>();
                }
            } :
            new ThreadLocal<Queue<GridCacheMvccCandidate<K>>>() {
                @Override protected Queue<GridCacheMvccCandidate<K>> initialValue() {
                    return new LinkedList<GridCacheMvccCandidate<K>>();
                }
            };
    }

    /** {@inheritDoc} */
    @Override public void onKernalStart0() throws GridException {
        cctx.gridEvents().addLocalEventListener(discoLsnr, EVT_NODE_FAILED, EVT_NODE_LEFT);
    }

    /** {@inheritDoc} */
    @Override public void onKernalStop0() {
        cctx.gridEvents().removeLocalEventListener(discoLsnr);
    }

    /**
     * @return MVCC callback.
     */
    public GridCacheMvccCallback<K, V> callback() {
        return callback;
    }

    /**
     * @param from From version.
     * @param to To version.
     */
    public void mapVersion(GridCacheVersion from, GridCacheVersion to) {
        assert from != null;
        assert to != null;

        near2dht.put(from, to);

        if (log.isDebugEnabled())
            log.debug("Added version mapping [from=" + from + ", to=" + to + ']');
    }

    /**
     * @param from Near version.
     * @return DHT version.
     */
    public GridCacheVersion mappedVersion(GridCacheVersion from) {
        assert from != null;

        GridCacheVersion to = near2dht.get(from);

        if (log.isDebugEnabled())
            log.debug("Retrieved mapped version [from=" + from + ", to=" + to + ']');

        return to;
    }

    /**
     * @param from From version.
     * @return To version.
     */
    public GridCacheVersion unmapVersion(GridCacheVersion from) {
        assert from != null;

        GridCacheVersion to = near2dht.remove(from);

        if (log.isDebugEnabled())
            log.debug("Removed mapped version [from=" + from + ", to=" + to + ']');

        return to;
    }

    /**
     * @param fut Future to check.
     * @return {@code True} if future is registered.
     */
    public boolean hasFuture(GridCacheFuture<?> fut) {
        assert fut != null;

        return future(fut.version().id(), fut.futureId()) != null;
    }

    /**
     * Adds future.
     *
     * @param fut Future.
     * @return {@code True} if added.
     */
    @SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter"})
    public boolean addFuture(final GridCacheFuture<?> fut) {
        if (fut.isDone()) {
            fut.markNotTrackable();

            return true;
        }

        if (!fut.trackable())
            return true;

        while (true) {
            Collection<GridCacheFuture<?>> old = futs.putIfAbsent(fut.version().id(),
                new GridConcurrentLinkedDeque<GridCacheFuture<?>>() {
                    private int hash;

                    {
                        // Make sure that we add future to queue before
                        // adding queue to the map of futures.
                        add(fut);
                    }

                    @Override public int hashCode() {
                        if (hash == 0)
                            hash = System.identityHashCode(this);

                        return hash;
                    }

                    @Override public boolean equals(Object obj) {
                        return obj == this;
                    }
                });

            if (old != null) {
                boolean empty, dup = false;

                synchronized (old) {
                    empty = old.isEmpty();

                    if (!empty)
                        dup = old.contains(fut);

                    if (!empty && !dup)
                        old.add(fut);
                }

                // Future is being removed, so we force-remove here and try again.
                if (empty) {
                    if (futs.remove(fut.version().id(), old)) {
                        if (log.isDebugEnabled())
                            log.debug("Removed future list from futures map for lock version: " + fut.version());
                    }

                    continue;
                }

                if (dup) {
                    if (log.isDebugEnabled())
                        log.debug("Found duplicate future in futures map (will not add): " + fut);

                    return false;
                }
            }

            // Handle version mappings.
            if (fut instanceof GridCacheMappedVersion) {
                GridCacheVersion from = ((GridCacheMappedVersion)fut).mappedVersion();

                if (from != null)
                    mapVersion(from, fut.version());
            }

            if (log.isDebugEnabled())
                log.debug("Added future to future map: " + fut);

            break;
        }

        // Close window in case of node is gone before the future got added to
        // the map of futures.
        for (GridNode n : fut.nodes())
            if (cctx.discovery().node(n.id()) == null)
                fut.onNodeLeft(n.id());

        // Just in case if future was complete before it was added.
        if (fut.isDone())
            removeFuture(fut);

        return true;
    }

    /**
     * @param fut Future to remove.
     * @return {@code True} if removed.
     */
    @SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter"})
    public boolean removeFuture(GridCacheFuture<?> fut) {
        if (!fut.trackable())
            return true;

        Collection<GridCacheFuture<?>> cur = futs.get(fut.version().id());

        if (cur == null)
            return false;

        boolean rmv, empty;

        synchronized (cur) {
            rmv = cur.remove(fut);

            empty = cur.isEmpty();
        }

        if (rmv) {
            if (log.isDebugEnabled())
                log.debug("Removed future from future map: " + fut);
        }
        else if (log.isDebugEnabled())
            log.debug("Attempted to remove a non-registered future (has it been already removed?): " + fut);

        if (empty && futs.remove(fut.version().id(), cur))
            if (log.isDebugEnabled())
                log.debug("Removed future list from futures map for lock version: " + fut.version());

        return rmv;
    }

    /**
     * Gets future for given future ID and lock ID.
     *
     * @param ver Lock ID.
     * @param futId Future ID.
     * @return Future.
     */
    @SuppressWarnings({"unchecked"})
    @Nullable public <T> GridCacheFuture<T> future(GridUuid ver, GridUuid futId) {
        Collection<? extends GridCacheFuture> futs = this.futs.get(ver);

        if (futs != null)
            for (GridCacheFuture<?> fut : futs)
                if (fut.futureId().equals(futId)) {
                    if (log.isDebugEnabled())
                        log.debug("Found future in futures map: " + fut);

                    return (GridCacheFuture<T>)fut;
                }

        if (log.isDebugEnabled())
            log.debug("Failed to find future in futures map [ver=" + ver + ", futId=" + futId + ']');

        return null;
    }

    /**
     * Gets all futures for given lock version, possibly empty collection.
     *
     * @param ver Version.
     * @return All futures for given lock version.
     */
    public <T> Collection<? extends GridFuture<T>> futures(GridUuid ver) {
        Collection c = futs.get(ver);

        return c == null ? Collections.<GridFuture<T>>emptyList() : (Collection<GridFuture<T>>)c;
    }

    /**
     * @param ver Lock version to check.
     * @return {@code True} if lock had been removed.
     */
    public boolean isRemoved(GridCacheVersion ver) {
        return !cctx.isNear() && !cctx.isLocal() && ver != null && rmvLocks.contains(ver);
    }

    /**
     * @param ver Obsolete entry version.
     * @return {@code True} if added.
     */
    public boolean addRemoved(GridCacheVersion ver) {
        if (cctx.isNear() || cctx.isLocal())
            return true;

        boolean ret = rmvLocks.add(ver);

        if (log.isDebugEnabled())
            log.debug("Added removed lock version: " + ver);

        return ret;
    }

    /**
     * This method has poor performance, so use with care. It is currently only used by {@code DGC}.
     *
     * @return Remote candidates.
     */
    public Collection<GridCacheMvccCandidate<K>> remoteCandidates() {
        Collection<GridCacheMvccCandidate<K>> rmtCands = new LinkedList<GridCacheMvccCandidate<K>>();

        for (GridDistributedCacheEntry<K, V> entry : locked.values()) {
            rmtCands.addAll(entry.remoteMvccSnapshot());
        }

        return rmtCands;
    }

    /**
     * This method has poor performance, so use with care. It is currently only used by {@code DGC}.
     *
     * @return Local candidates.
     */
    public Collection<GridCacheMvccCandidate<K>> localCandidates() {
        Collection<GridCacheMvccCandidate<K>> locCands = new LinkedList<GridCacheMvccCandidate<K>>();

        for (GridDistributedCacheEntry<K, V> entry : locked.values()) {
            try {
                locCands.addAll(entry.localCandidates());
            }
            catch (GridCacheEntryRemovedException ignore) {
                // No-op.
            }
        }

        return locCands;
    }

    /**
     * @param cand Local lock.
     * @return {@code True} if added.
     */
    public boolean addLocal(GridCacheMvccCandidate<K> cand) {
        assert cand.key() != null;
        assert cand.local();

        if (cand.dhtLocal() && dhtLocCands.add(cand)) {
            if (log.isDebugEnabled())
                log.debug("Added local candidate: " + cand);

            return true;
        }

        return false;
    }

    /**
     *
     * @param cand Local candidate to remove.
     * @return {@code True} if removed.
     */
    public boolean removeLocal(GridCacheMvccCandidate<K> cand) {
        assert cand.key() != null;
        assert cand.local();

        if (cand.dhtLocal() && dhtLocCands.remove(cand)) {
            if (log.isDebugEnabled())
                log.debug("Removed local candidate: " + cand);

            return true;
        }

        return false;
    }

    /**
     * @param keys Keys.
     * @param base Base version.
     * @return Versions that are less than {@code base} whose keys are in the {@code keys} collection.
     */
    public Collection<GridCacheVersion> localDhtPendingVersions(Collection<K> keys, GridCacheVersion base) {
        Collection<GridCacheVersion> lessPending = new GridLeanSet<GridCacheVersion>(5);

        for (GridCacheMvccCandidate<K> cand : dhtLocCands) {
            if (cand.version().isLess(base)) {
                if (keys.contains(cand.key()))
                    lessPending.add(cand.version());
            }
            else
                break;
        }

        return lessPending;
    }

    /**
     * Unlinks a lock candidate.
     *
     * @param cand Lock candidate to unlink.
     */
    private void unlink(GridCacheMvccCandidate<K> cand) {
        GridCacheMvccCandidate<K> next = cand.next();

        if (next != null) {
            GridCacheMvccCandidate<K> prev = cand.previous();

            next.previous(prev);

            if (prev != null)
                prev.next(next);
        }

        /*
         * Note that we specifically don't set links from passed in
         * candidate to null because it is possible in some race
         * cases that it will get traversed. However, it should
         * still become available for GC and should not cause
         * an issue.
         */

        if (log.isDebugEnabled())
            log.debug("Unlinked lock candidate: " + cand);
    }

    /**
     *
     * @param cand Cache lock candidate to add.
     * @return {@code True} if added as a result of this operation,
     *      {@code false} if was previously added.
     */
    public boolean addNext(GridCacheMvccCandidate<K> cand) {
        assert cand != null;
        assert !cand.reentry() : "Lock reentries should not be linked: " + cand;

        // Don't order near candidates by thread as they will be ordered on
        // DHT node. Also, if candidate is implicit, no point to order him.
        if (cctx.isNear() || cand.singleImplicit())
            return true;

        Queue<GridCacheMvccCandidate<K>> queue = pending.get();

        boolean add = true;

        GridCacheMvccCandidate<K> prev = null;

        for (Iterator<GridCacheMvccCandidate<K>> it = queue.iterator(); it.hasNext(); ) {
            GridCacheMvccCandidate<K> c = it.next();

            if (c.equals(cand))
                add = false;

            if (c.used()) {
                it.remove();

                unlink(c);

                continue;
            }

            prev = c;
        }

        if (add) {
            queue.add(cand);

            if (prev != null) {
                prev.next(cand);

                cand.previous(prev);
            }

            if (log.isDebugEnabled())
                log.debug("Linked new candidate: " + cand);
        }

        return add;
    }

    /** {@inheritDoc} */
    @Override protected void printMemoryStats() {
        X.println(">>> ");
        X.println(">>> Mvcc manager memory stats [grid=" + cctx.gridName() + ", cache=" + cctx.name() + ']');
        X.println(">>>   rmvLocksSize: " + rmvLocks.size());
        X.println(">>>   dhtLocCandsSize: " + dhtLocCands.size());
        X.println(">>>   lockedSize: " + locked.size());
        X.println(">>>   futsSize: " + futs.size());
        X.println(">>>   near2dhtSize: " + near2dht.size());
        X.println(">>>   finishFutsSize: " + finishFuts.size());
    }


    /**
     * @param nodeId Node ID.
     * @return Filter.
     */
    private GridPredicate<GridCacheMvccCandidate<K>> nodeIdFilter(final UUID nodeId) {
        if (nodeId == null)
            return F.alwaysTrue();

        return new P1<GridCacheMvccCandidate<K>>() {
            @Override public boolean apply(GridCacheMvccCandidate<K> c) {
                UUID otherId = c.otherNodeId();

                return c.nodeId().equals(nodeId) || (otherId != null && otherId.equals(nodeId));
            }
        };
    }

    /**
     * @param parts Partition numbers.
     * @param topVer Topology version.
     * @return Future that signals when all locks for given partitions are released.
     */
    @SuppressWarnings({"unchecked"})
    public GridFuture<?> finishPartitions(final Collection<Integer> parts, long topVer) {
        assert topVer > 0;

        return finishLocks(new GridPredicate[] { new P1<K>() {
            @Override public boolean apply(K key) {
                return parts != null && parts.contains(cctx.partition(key));
            }
        }}, topVer);
    }

    /**
     * @param keyFilter Key filter.
     * @return Future that signals when all locks for keys corresponding to given filter are released.
     */
    public GridFuture<?> finishKeys(GridPredicate<K>[] keyFilter) {
        return finishLocks(keyFilter, -1);
    }

    /**
     * @param keyFilter Key filter.
     * @param topVer Topology version.
     * @return Future that signals when all locks for given partitions will be released.
     */
    @SuppressWarnings({"unchecked"})
    public GridFuture<?> finishLocks(@Nullable final GridPredicate<K>[] keyFilter, long topVer) {
        assert topVer != 0;

        final FinishLockFuture finishFut = new FinishLockFuture(
            F.view(locked.values(),
                new P1<GridDistributedCacheEntry<K, V>>() {
                    @Override public boolean apply(GridDistributedCacheEntry<K, V> e) {
                            return F.isAll(e.key(), keyFilter);
                    }
                }
            ), topVer);

        finishFuts.add(finishFut);

        finishFut.listenAsync(new CI1<GridFuture<?>>() {
            @Override public void apply(GridFuture<?> e) {
                finishFuts.remove(finishFut);

                // This call is required to make sure that the concurrent queue
                // clears memory occupied by internal nodes.
                finishFuts.peek();
            }
        });

        finishFuts.add(finishFut);

        finishFut.listenAsync(new CI1<GridFuture<?>>() {
            @Override public void apply(GridFuture<?> e) {
                finishFuts.remove(finishFut);

                // This call is required to make sure that the concurrent queue
                // clears memory occupied by internal nodes.
                finishFuts.peek();
            }
        });

        finishFut.recheck();

        return finishFut;
    }

    /**
     *
     */
    public void recheckPendingLocks() {
        if (exchLog.isDebugEnabled())
            exchLog.debug("Rechecking pending locks for completion.");

        for (FinishLockFuture fut : finishFuts)
            fut.recheck();
    }

    /**
     *
     */
    private class FinishLockFuture extends GridFutureAdapter<Object> {
        /** Exclude IDs. */
        @GridToStringInclude
        private final long topVer;

        /** */
        @GridToStringInclude
        private final Map<K, Collection<GridCacheMvccCandidate<K>>> pendingLocks =
            new ConcurrentHashMap<K, Collection<GridCacheMvccCandidate<K>>>();

        /**
         * Empty constructor required for {@link Externalizable}.
         */
        public FinishLockFuture() {
            assert false;

            topVer = 0;
        }

        /**
         * @param topVer Topology version.
         * @param entries Entries.
         */
        FinishLockFuture(Iterable<GridDistributedCacheEntry<K, V>> entries, long topVer) {
            super(cctx.kernalContext(), true);

            assert topVer != 0;

            this.topVer = topVer;

            for (GridCacheEntryEx<K, V> entry : entries) {
                try {
                    Collection<GridCacheMvccCandidate<K>> rmts = entry.remoteMvccSnapshot();

                    Collection<GridCacheMvccCandidate<K>> locs = null;

                    if (cctx.isDht())
                        locs = entry.localCandidates();

                    if (!F.isEmpty(rmts) || !F.isEmpty(locs)) {
                        Collection<GridCacheMvccCandidate<K>> cands =
                            new ConcurrentLinkedQueue<GridCacheMvccCandidate<K>>();

                        if (rmts != null)
                            cands.addAll(F.view(rmts, excludeFilter()));

                        if (locs != null)
                            cands.addAll(F.view(locs, excludeFilter()));

                        if (!F.isEmpty(cands))
                            pendingLocks.put(entry.key(), cands);
                    }
                }
                catch (GridCacheEntryRemovedException ignored) {
                    if (exchLog.isDebugEnabled())
                        exchLog.debug("Got removed entry when adding it to finish lock future (will ignore): " + entry);
                }
            }

            if (exchLog.isDebugEnabled())
                exchLog.debug("Pending lock set [topVer=" + topVer + ", locks=" + pendingLocks + ']');
        }

        /**
         * @return Filter.
         */
        private GridPredicate<GridCacheMvccCandidate<K>> excludeFilter() {
            if (topVer < 0)
                return F.alwaysTrue();

            return new P1<GridCacheMvccCandidate<K>>() {
                @Override public boolean apply(GridCacheMvccCandidate<K> c) {
                    return c.nearLocal() && (c.topologyVersion() == 0 || c.topologyVersion() < topVer);
                }
            };
        }

        /**
         *
         */
        void recheck() {
            for (Iterator<K> it = pendingLocks.keySet().iterator(); it.hasNext(); ) {
                K key = it.next();

                GridCacheEntryEx<K, V> entry = cctx.cache().peekEx(key);

                if (entry == null)
                    it.remove();
                else
                    recheck(entry);
            }

            if (log.isDebugEnabled())
                log.debug("After rechecking finished future: " + this);

            if (pendingLocks.isEmpty()) {
                if (exchLog.isDebugEnabled())
                    exchLog.debug("Finish lock future is done: " + this);

                onDone();
            }
        }

        /**
         * @param entry Entry.
         */
        @SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter"})
        void recheck(@Nullable GridCacheEntryEx<K, V> entry) {
            if (entry == null)
                return;

            if (exchLog.isDebugEnabled())
                exchLog.debug("Rechecking entry for completion [entry=" + entry + ", finFut=" + this + ']');

            Collection<GridCacheMvccCandidate<K>> cands = pendingLocks.get(entry.key());

            if (cands != null) {
                synchronized (cands) {
                    for (Iterator<GridCacheMvccCandidate<K>> it = cands.iterator(); it.hasNext(); ) {
                        GridCacheMvccCandidate<K> cand = it.next();

                        // Check exclude ID again, as key could have been reassigned.
                        if (cand.removed())
                            it.remove();
                    }

                    if (cands.isEmpty())
                        pendingLocks.remove(entry.key());

                    if (pendingLocks.isEmpty()) {
                        onDone();

                        if (exchLog.isDebugEnabled())
                            exchLog.debug("Finish lock future is done: " + this);
                    }
                }
            }
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            if (!pendingLocks.isEmpty()) {
                Map<GridCacheVersion, GridCacheTxEx> txs = new HashMap<GridCacheVersion, GridCacheTxEx>(1, 1.0f);

                for (Collection<GridCacheMvccCandidate<K>> cands : pendingLocks.values())
                    for (GridCacheMvccCandidate<K> c : cands)
                        txs.put(c.version(), cctx.tm().<GridCacheTxEx>tx(c.version()));

                return S.toString(FinishLockFuture.class, this, "txs=" + txs + ", super=" + super.toString());
            }
            else
                return S.toString(FinishLockFuture.class, this, super.toString());
        }
    }
}
