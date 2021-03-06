// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.query;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.lang.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Local query manager.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.09012012
 */
public class GridCacheLocalQueryManager<K, V> extends GridCacheQueryManager<K, V> {
    /** {@inheritDoc} */
    @Override protected boolean onPageReady(
        boolean loc,
        GridCacheQueryInfo<K, V> qryInfo,
        Collection<?> data,
        boolean finished, Throwable e) {
        GridCacheQueryFutureAdapter fut = qryInfo.localQueryFuture();

        assert fut != null;

        if (e != null)
            fut.onPage(null, null, e, true);
        else
            fut.onPage(null, data, null, finished);

        return true;
    }

    /** {@inheritDoc} */
    @Override public void start0() throws GridException {
        super.start0();

        assert cctx.config().getCacheMode() == LOCAL;
    }

    /** {@inheritDoc} */
    @Override public <R> GridCacheQueryFuture<R> queryLocal(GridCacheQueryBaseAdapter<K, V> qry, boolean single,
        boolean rmtRdcOnly, @Nullable GridInClosure2<UUID, Collection<R>> pageLsnr) {
        if (log.isDebugEnabled())
            log.debug("Executing query on local node: " + qry);

        assert cctx.config().getCacheMode() == LOCAL;

        GridCacheLocalQueryFuture<K, V, R> fut =
            new GridCacheLocalQueryFuture<K, V, R>(cctx, qry, true, single, rmtRdcOnly, pageLsnr);

        try {
            validateQuery(qry);
        }
        catch (GridException e) {
            fut.onDone(e);
        }

        return fut;
    }

    /** {@inheritDoc} */
    @Override public <R> GridCacheQueryFuture<R> queryDistributed(GridCacheQueryBaseAdapter<K, V> qry,
        Collection<GridRichNode> nodes, boolean single, boolean rmtOnly,
        @Nullable GridInClosure2<UUID, Collection<R>> pageLsnr) {
        assert cctx.config().getCacheMode() == LOCAL;

        throw new GridRuntimeException("Distributed queries are not available for local cache " +
            "(use 'GridCacheQuery.execute(grid.localNode())' instead) [cacheName=" + cctx.name() + ']');
    }
}
