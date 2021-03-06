// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.events;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.utils.*;
import org.gridgain.grid.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;

import java.util.*;

/**
 * Data grid (cache) event.
 * <p>
 * Grid events are used for notification about what happens within the grid. Note that by
 * design GridGain keeps all events generated on the local node locally and it provides
 * APIs for performing a distributed queries across multiple nodes:
 * <ul>
 *      <li>
 *          {@link Grid#remoteEvents(org.gridgain.grid.lang.GridPredicate , long, org.gridgain.grid.lang.GridPredicate[])} -
 *          querying events occurred on the nodes specified, including remote nodes.
 *      </li>
 *      <li>
 *          {@link Grid#remoteEventsAsync(org.gridgain.grid.lang.GridPredicate , long, org.gridgain.grid.lang.GridPredicate[])} -
 *          asynchronously querying events occurred on the nodes specified, including remote nodes.
 *      </li>
 *      <li>
 *          {@link Grid#localEvents(org.gridgain.grid.lang.GridPredicate[])} -
 *          querying only local events stored on this local node.
 *      </li>
 *      <li>
 *          {@link Grid#addLocalEventListener(GridLocalEventListener , int...)} -
 *          listening to local grid events (events from remote nodes not included).
 *      </li>
 * </ul>
 * User can also wait for events using the following two methods:
 * <ul>
 *      <li>{@link Grid#waitForEventAsync(org.gridgain.grid.lang.GridPredicate , int...)}</li>
 *      <li>{@link Grid#waitForEvent(long, Runnable, org.gridgain.grid.lang.GridPredicate , int...)}</li>
 * </ul>
 * <h1 class="header">Events and Performance</h1>
 * Note that by default all events in GridGain are enabled and therefore generated and stored
 * by whatever event storage SPI is configured. GridGain can and often does generate thousands events per seconds
 * under the load and therefore it creates a significant additional load on the system. If these events are
 * not needed by the application this load is unnecessary and leads to significant performance degradation.
 * <p>
 * It is <b>highly recommended</b> to enable only those events that your application logic requires
 * by using either  {@link GridConfiguration#getExcludeEventTypes()} or
 * {@link GridConfiguration#getIncludeEventTypes()} methods in GridGain configuration. Note that certain
 * events are required for GridGain's internal operations and such events will still be generated but not stored by
 * event storage SPI if they are disabled in GridGain configuration.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.09012012
 * @see GridEventType#EVT_CACHE_ENTRY_CREATED
 * @see GridEventType#EVT_CACHE_ENTRY_DESTROYED
 * @see GridEventType#EVT_CACHE_ENTRY_EVICTED
 * @see GridEventType#EVT_CACHE_OBJECT_PUT
 * @see GridEventType#EVT_CACHE_OBJECT_READ
 * @see GridEventType#EVT_CACHE_OBJECT_REMOVED
 * @see GridEventType#EVT_CACHE_OBJECT_LOCKED
 * @see GridEventType#EVT_CACHE_OBJECT_UNLOCKED
 * @see GridEventType#EVT_CACHE_OBJECT_SWAPPED
 * @see GridEventType#EVT_CACHE_OBJECT_UNSWAPPED
 * @see GridEventType#EVT_CACHE_OBJECT_EXPIRED
 */
public class GridCacheEvent extends GridEventAdapter {
    /** Cache name. */
    private String cacheName;

    /** Partition for the event. */
    private int partition;

    /** Cache entry. */
    @GridToStringInclude
    private Object key;

    /** Event ID. */
    @GridToStringInclude
    private final GridUuid xid;

    /** Lock ID. */
    @GridToStringInclude
    private final GridUuid lockId;

    /** New value. */
    @GridToStringInclude
    private final Object newVal;

    /** Old value. */
    @GridToStringInclude
    private final Object oldVal;

    /** Event node ID. */
    @GridToStringExclude
    private final UUID evtNodeId;

    /** Flag indicating whether event happened on {@code near} or {@code partitioned} cache. */
    @GridToStringInclude
    private boolean near;

    /**
     * Constructs cache event.
     *
     * @param cacheName Cache name.
     * @param nodeId Local node ID.
     * @param evtNodeId Event node ID.
     * @param msg Event message.
     * @param type Event type.
     * @param partition Partition for the event (usually the partition the key belongs to).
     * @param near Flag indicating whether event happened on {@code near} or {@code partitioned} cache.
     * @param key Cache key.
     * @param xid Transaction ID.
     * @param lockId Lock ID.
     * @param newVal New value.
     * @param oldVal Old value.
     */
    public GridCacheEvent(String cacheName, UUID nodeId, UUID evtNodeId, String msg, int type, int partition,
        boolean near, Object key, GridUuid xid, GridUuid lockId, Object newVal, Object oldVal) {
        super(nodeId, msg, type);
        this.cacheName = cacheName;
        this.evtNodeId = evtNodeId;
        this.partition = partition;
        this.near = near;
        this.key = key;
        this.xid = xid;
        this.lockId = lockId;
        this.newVal = newVal;
        this.oldVal = oldVal;
    }

    /**
     * Gets cache name.
     *
     * @return Cache name.
     */
    public String cacheName() {
        return cacheName;
    }

    /**
     * Gets partition for the event which is the partition the key belongs to.
     *
     * @return Partition for the event.
     */
    public int partition() {
        return partition;
    }

    /**
     * Gets flag indicating whether event happened on {@code near} or {@code partitioned} cache.
     *
     * @return Flag indicating whether event happened on {@code near} or {@code partitioned} cache.
     */
    public boolean isNear() {
        return near;
    }

    /**
     * Gets ID of the node which initiated cache operation.
     *
     * @return ID of the node which initiated cache operation.
     */
    public UUID eventNodeId() {
        return evtNodeId;
    }

    /**
     * Gets cache entry associated with event.
     *
     * @return Cache entry associated with event.
     */
    @SuppressWarnings({"unchecked"})
    public <K> K key() {
        return (K)key;
    }

    /**
     * ID of surrounding cache cache transaction or <tt>null</tt> if there is
     * no surrounding transaction.
     *
     * @return ID of surrounding cache transaction.
     */
    public GridUuid xid() {
        return xid;
    }

    /**
     * ID of the lock if held or <tt>null</tt> if no lock held.
     *
     * @return ID of the lock if held.
     */
    public GridUuid lockId() {
        return lockId;
    }

    /**
     * @return New value associated with event (<tt>null</tt> if event is
     *      {@link GridEventType#EVT_CACHE_OBJECT_REMOVED}.
     */
    public Object newValue() {
        return newVal;
    }

    /**
     * @return Old value associated with event.
     */
    public Object oldValue() {
        return oldVal;
    }

    /** {@inheritDoc} */
    @Override public String shortDisplay() {
        return name() + ": near=" + near + ", key=" + key + ", newVal=" + newVal + ", oldVal=" + oldVal +
            ", nodeId8=" + U.id8(nodeId());
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheEvent.class, this,
            "nodeId8", U.id8(nodeId()),
            "evtNodeId8", U.id8(evtNodeId),
            "msg", message(),
            "type", name(),
            "tstamp", timestamp());
    }
}
