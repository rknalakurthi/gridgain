// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.lang;

import org.gridgain.grid.typedef.*;

/**
 * Defines a convenient {@code side-effect only} closure, i.e. the closure that has {@code void} return type.
 * Since Java 6 doesn't provide a language construct for first-class function the closures are
 * implemented as interfaces.
 * <h2 class="header">Type Alias</h2>
 * To provide for more terse code you can use a typedef {@link org.gridgain.grid.typedef.CI2}
 * class or various factory methods in {@link GridFunc} class. Note, however, that since typedefs
 * in Java rely on inheritance you should not use these type aliases in signatures.
 * <h2 class="header">Thread Safety</h2>
 * Note that this interface does not impose or assume any specific thread-safety by its
 * implementations. Each implementation can elect what type of thread-safety it provides,
 * if any.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.09012012
 * @param <E1> Type of the first free variable, i.e. the element the closure is called or closed on.
 * @param <E2> Type of the second free variable, i.e. the element the closure is called or closed on.
 * @see C2
 * @see GridFunc
 */
public abstract class GridInClosure2<E1, E2> extends GridLambdaAdapter {
    /**
     * Closure body.
     *
     * @param e1 First bound free variable, i.e. the element the closure is called or closed on.
     * @param e2 Second bound free variable, i.e. the element the closure is called or closed on.
     */
    public abstract void apply(E1 e1, E2 e2);

    /**
     * Curries this closure with given value. When result closure is called it will
     * be executed with given value.
     *
     * @param e1 Value to curry with.
     * @return Curried or partially applied closure with given value.
     */
    public GridInClosure<E2> curry(final E1 e1) {
        return withMeta(new CI1<E2>() {
            {
                peerDeployLike(GridInClosure2.this);
            }

            @Override public void apply(E2 e2) {
                GridInClosure2.this.apply(e1, e2);
            }
        });
    }

    /**
     * Curries this closure with given values. When result closure is called it will
     * be executed with given values.
     *
     * @param e1 Value to curry with.
     * @param e2 Value to curry with.
     * @return Curried or partially applied closure with given values.
     */
    public GridAbsClosure curry(final E1 e1, final E2 e2) {
        return withMeta(new CA() {
            {
                peerDeployLike(GridInClosure2.this);
            }

            @Override public void apply() {
                GridInClosure2.this.apply(e1, e2);
            }
        });
    }

    /**
     * Gets closure that ignores its third argument and executes the same way as this
     * in-closure with first and second arguments.
     *
     * @param <E3> Type of 3d argument that is ignored.
     * @return Closure that ignores its third argument and executes the same way as this
     *      in-closure with first and second arguments.
     */
    public <E3> GridInClosure3<E1, E2, E3> uncurry3() {
        GridInClosure3<E1, E2, E3> c = new CI3<E1, E2, E3>() {
            @Override public void apply(E1 e1, E2 e2, E3 e3) {
                GridInClosure2.this.apply(e1, e2);
            }
        };

        c.peerDeployLike(this);

        return withMeta(c);
    }
}
