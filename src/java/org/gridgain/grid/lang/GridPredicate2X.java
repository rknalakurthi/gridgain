// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.lang;

import org.gridgain.grid.*;
import org.gridgain.grid.typedef.*;

/**
 * Convenient predicate subclass that allows for thrown grid exception. This class
 * implements {@link #apply(Object, Object)} method that calls {@link #applyx(Object, Object)}
 * method and properly wraps {@link GridException} into {@link GridClosureException} instance.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.09012012
 * @see PX2
 */
public abstract class GridPredicate2X<E1, E2> extends GridPredicate2<E1, E2> {
    /** {@inheritDoc} */
    @Override public boolean apply(E1 e1, E2 e2) {
        try {
            return applyx(e1, e2);
        }
        catch (GridException ex) {
            throw F.wrap(ex);
        }
    }

    /**
     * Predicate body that can throw {@link GridException}.
     *
     * @param e1 First bound free variable, i.e. the element the predicate is called or closed on.
     * @param e2 Second bound free variable, i.e. the element the predicate is called or closed on.
     * @return Return value.
     * @throws GridException Thrown in case of any error condition inside of the predicate.
     */
    public abstract boolean applyx(E1 e1, E2 e2) throws GridException;
}
