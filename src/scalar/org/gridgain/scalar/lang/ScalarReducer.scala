// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*
 * ________               ______                    ______   _______
 * __  ___/_____________ ____  /______ _________    __/__ \  __  __ \
 * _____ \ _  ___/_  __ `/__  / _  __ `/__  ___/    ____/ /  _  / / /
 * ____/ / / /__  / /_/ / _  /  / /_/ / _  /        _  __/___/ /_/ /
 * /____/  \___/  \__,_/  /_/   \__,_/  /_/         /____/_(_)____/
 *
 */
 
package org.gridgain.scalar.lang

import org.gridgain.grid.util.{GridUtils => U}
import org.gridgain.grid.lang.GridReducer
import collection._

/**
 * Peer deploy aware adapter for Java's `GridReducer`.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.09012012
 */
class ScalarReducer[E, R](private val r: Seq[E] => R) extends GridReducer[E, R] {
    assert(r != null)

    peerDeployLike(U.peerDeployAware(r))

    private val buf = new mutable.ListBuffer[E]

    /**
     * Delegates to passed in function.
     */
    def apply = r(buf.toSeq)

    /**
     * Collects given value.
     *
     * @param e Value to collect for later reduction.
     */
    def collect(e: E) = {
        buf += e

        true
    }
}