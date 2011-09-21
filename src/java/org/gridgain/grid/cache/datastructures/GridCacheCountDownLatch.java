// Copyright (C) GridGain Systems, Inc. Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.cache.datastructures;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;

import java.util.concurrent.*;

/**
 * This interface provides a rich API for working with distributed count down latch.
 * <p>
 * Note that distributed count down latch is only available in <b>Enterprise Edition</b>.
 * <p>
 * <h1 class="header">Functionality</h1>
 * Distributed count down latch provides functionality similar to {@code java.util.CountDownLatch}.
 * Note that you cannot remove count down latch having count greater that zero. It should be
 * counted down to zero first.
 * <h1 class="header">Creating Distributed Count Down Latch</h1>
 * Instance of cache count down latch can be created by calling the following method:
 * {@link GridCache#countDownLatch(String, int, boolean)}.
 *
 * @author 2005-2011 Copyright (C) GridGain Systems, Inc.
 * @version 3.5.0c.20092011
 * @see GridCache#countDownLatch(String, int, boolean)
 * @see GridCache#countDownLatch(String)
 * @see GridCache#removeCountDownLatch(String)
 */
public interface GridCacheCountDownLatch extends GridMetadataAware {
    /**
     * Gets name of the latch.
     *
     * @return Name of the latch.
     */
    public String name();

    /**
     * Gets current count value of the latch.
     *
     * @return Current count.
     */
    public int count();

    /**
     * Gets initial count value of the latch.
     *
     * @return Initial count.
     */
    public int initialCount();

    /**
     * Gets {@code autoDelete} flag. If this flag is {@code true} latch is removed
     * from cache when it has been counted down to 0.
     *
     * @return Value of {@code autoDelete} flag.
     */
    public boolean autoDelete();

    /**
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless current thread is interrupted.
     * <p>
     * If the current count of the latch is zero then this method returns immediately.
     * <p>
     * If the current count is greater than zero then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of two things happen:
     * <ul>
     *     <li>The count reaches zero due to invocations of the
     *      {@link #countDown} method on any node; or
     *      <li>Some other thread interrupts the current thread.
     * </ul>
     * <p>
     * If the current thread:
     * <ul>
     *      <li>has its interrupted status set on entry to this method; or
     *      <li>is interrupted while waiting,
     * </ul>
     * then {@link GridInterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws GridException If operation failed.
     * @throws GridInterruptedException if the current thread is interrupted
     *      while waiting
     */
    public void await() throws GridException;

    /**
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is interrupted, or the specified waiting time elapses.
     * <p>
     * If the current count is zero then this method returns immediately
     * with the value {@code true}.
     * <p>
     * If the current count is greater than zero then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of three things happen:
     * <ul>
     *      <li>The count reaches zero due to invocations of the
     *      {@link #countDown} method on any node; or
     *      <li>Some other thread interrupts the current thread; or
     *      <li>The specified waiting time elapses.
     * </ul>
     * <p>
     * If the count reaches zero then the method returns with the
     * value {@code true}.
     * <p>
     * If the current thread:
     * <ul>
     *      <li>has its interrupted status set on entry to this method; or
     *      <li>is interrupted while waiting,
     * </ul>
     * then {@link GridInterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * <p>
     * If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * @param timeout The maximum time to wait in milliseconds.
     * @return {@code True} if the count reached zero and {@code false}
     *      if the waiting time elapsed before the count reached zero.
     * @throws GridInterruptedException If the current thread is interrupted
     *      while waiting.
     * @throws GridException If operation failed.
     */
    public boolean await(long timeout) throws GridException;

    /**
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is interrupted, or the specified waiting time elapses.
     * <p>
     * If the current count is zero then this method returns immediately
     * with the value {@code true}.
     * <p>
     * If the current count is greater than zero then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of three things happen:
     * <ul>
     *      <li>The count reaches zero due to invocations of the
     *      {@link #countDown} method on any node; or
     *      <li>Some other thread interrupts the current thread; or
     *      <li>The specified waiting time elapses.
     * </ul>
     * <p>
     * If the count reaches zero then the method returns with the
     * value {@code true}.
     * <p>
     * If the current thread:
     * <ul>
     *      <li>has its interrupted status set on entry to this method; or
     *      <li>is interrupted while waiting,
     * </ul>
     * then {@link GridInterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * <p>
     * If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     *
     * @param timeout The maximum time to wait.
     * @param unit The time unit of the {@code timeout} argument.
     * @return {@code True} if the count reached zero and {@code false}
     *      if the waiting time elapsed before the count reached zero.
     * @throws GridInterruptedException If the current thread is interrupted
     *      while waiting.
     * @throws GridException If operation failed.
     */
    public boolean await(long timeout, TimeUnit unit) throws GridException;

    /**
     * Asynchronously waits until the latch has counted down to zero.
     *
     * @return Future for the operation.
     */
    public GridFuture<?> awaitAsync();

    /**
     * Asynchronously waits until the latch has counted down to zero
     * or the specified waiting time elapses.
     *
     * @param timeout The maximum time to wait in milliseconds.
     * @return Future for the operation.
     */
    public GridFuture<Boolean> awaitAsync(long timeout);

    /**
     * Asynchronously waits until the latch has counted down to zero,
     * or the specified waiting time elapses.
     *
     *
     * @param timeout The maximum time to wait.
     * @param unit The time unit of the {@code timeout} argument.
     * @return Future for the operation.
     */
    public GridFuture<Boolean> awaitAsync(long timeout, TimeUnit unit);

    /**
     * Decrements the count of the latch, releasing all waiting threads
     * on all nodes if the count reaches zero.
     * <p>
     * If the current count is greater than zero then it is decremented.
     * If the new count is zero then all waiting threads are re-enabled for
     * thread scheduling purposes.
     * <p>
     * If the current count equals zero then nothing happens.
     *
     * @return Count after decrement.
     * @throws GridException If operation failed.
     */
    public int countDown() throws GridException;

    /**
     * Decreases the count of the latch using passed in value,
     * releasing all waiting threads on all nodes if the count reaches zero.
     * <p>
     * If the current count is greater than zero then it is decreased.
     * If the new count is zero then all waiting threads are re-enabled for
     * thread scheduling purposes.
     * <p>
     * If the current count equals zero then nothing happens.
     *
     * @param val Value to decrease counter on.
     * @return Count after decreasing.
     * @throws GridException If operation failed.
     */
    public int countDown(int val) throws GridException;

    /**
     * Counts down this latch to zero, releasing all waiting threads on all nodes.
     * <p>
     * If the current count equals zero then nothing happens.
     *
     * @throws GridException If operation failed.
     */
    public void countDownAll() throws GridException;

    /**
     * Asynchronously decrements the count of the latch, releasing all waiting threads if
     * the count reaches zero.
     * <p>
     * If the current count is greater than zero then it is decremented.
     * If the new count is zero then all waiting threads on all nodes
     * are re-enabled for thread scheduling purposes.
     * <p>
     * If the current count equals zero then nothing happens.
     *
     * @return Count after decrement.
     */
    public GridFuture<Integer> countDownAsync();

    /**
     * Asynchronously decreases the count of the latch using passed in value,
     * releasing all waiting threads on all nodes if the count reaches zero.
     * <p>
     * If the current count is greater than zero then it is decreased.
     * If the new count is zero then all waiting threads on all nodes
     * are re-enabled for thread scheduling purposes.
     * <p>
     * If the current count equals zero then nothing happens.
     *
     * @param val Value to decrease counter on.
     * @return Count after decrement.
     */
    public GridFuture<Integer> countDownAsync(int val);

    /**
     * Asynchronously counts down this latch to zero, releasing all waiting
     * threads on all nodes.
     * <p>
     * If the current count equals zero then nothing happens.
     *
     * @return Count after decrement.
     */
    public GridFuture<?> countDownAllAsync();

    /**
     * Gets {@code removed} status of the latch.
     *
     * @return {@code True} if latch was removed from cache, {@code false} otherwise.
     */
    public boolean removed();
}
