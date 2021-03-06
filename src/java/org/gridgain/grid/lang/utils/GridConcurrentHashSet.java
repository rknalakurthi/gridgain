// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.lang.utils;

import org.gridgain.grid.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Concurrent set implementation.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.09012012
 */
public class GridConcurrentHashSet<E> extends GridSetWrapper<E> {
    /**
     * Creates a new, empty set with a default initial capacity,
     * load factor, and concurrencyLevel.
     */
    public GridConcurrentHashSet() {
        super(new ConcurrentHashMap<E, E>());
    }

    /**
     * Creates a new, empty set with the specified initial
     * capacity, and with default load factor and concurrencyLevel.
     *
     * @param initCap The initial capacity. The implementation
     *      performs internal sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of
     *      elements is negative.
     */
    public GridConcurrentHashSet(int initCap) {
        super(new ConcurrentHashMap<E, E>(initCap));
    }

    /**
     * Creates a new, empty set with the specified initial
     * capacity, load factor, and concurrency level.
     *
     * @param initCap The initial capacity. The implementation
     *      performs internal sizing to accommodate this many elements.
     * @param loadFactor The load factor threshold, used to control resizing.
     *      Resizing may be performed when the average number of elements per
     *      bin exceeds this threshold.
     * @param conLevel The estimated number of concurrently
     *      updating threads. The implementation performs internal sizing
     *      to try to accommodate this many threads.
     * @throws IllegalArgumentException if the initial capacity is
     *      negative or the load factor or concurrency level are
     *      non-positive.
     */
    public GridConcurrentHashSet(int initCap, float loadFactor, int conLevel) {
        super(new ConcurrentHashMap<E, E>(initCap, loadFactor, conLevel));
    }

    /**
     * Creates a new set with the same elements as the given collection. The
     * collection is created with a capacity of twice the number of mappings in
     * the given map or 11 (whichever is greater), and a default load factor
     * and concurrencyLevel.
     *
     * @param c Collection to add.
     */
    public GridConcurrentHashSet(Collection<E> c) {
        super(new ConcurrentHashMap<E, E>(c.size()));

        addAll(c);
    }

    /**
     * Note that unlike regular add operation on a set, this method will only
     * add the passed in element if it's not already present in set.
     *
     * @param e Element to add.
     * @return {@code True} if element was added.
     */
    @Override public boolean add(E e) {
        ConcurrentMap<E, Object> m = (ConcurrentMap<E, Object>)map;

        return m.putIfAbsent(e, e) == null;
    }

    /**
     * Note that unlike regular add operation on a set, this method will only
     * add the passed in element if it's not already present in set.
     *
     * @param e Element to add.
     * @return Value previously present in set or {@code null} if set didn't have this value.
     */
    @Nullable public E addx(E e) {
        ConcurrentMap<E, E> m = (ConcurrentMap<E, E>)map;

        return m.putIfAbsent(e, e);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridConcurrentHashSet.class, this, "elements", map().keySet());
    }
}
