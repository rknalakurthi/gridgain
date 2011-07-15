// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.examples.junit.junit3;

import junit.framework.*;
import org.gridgain.grid.typedef.*;

/**
 * Regular JUnit3 test case used for JUnit3 example.
 *
 * @author @java.author
 * @version @java.version
 */
@SuppressWarnings({"ProhibitedExceptionThrown"})
public class TestC extends TestCase {
    /** {@inheritDoc} */
    @Override protected void setUp() throws Exception {
        X.println("Preparing for test execution: " + getName());
    }

    /** {@inheritDoc} */
    @Override protected void tearDown() throws Exception {
        X.println("Tearing down test execution: " + getName());
    }

    /**
     * Example test method.
     */
    public void testMethod1() {
        X.println("Output from TestC.testMethod1().");
    }

    /**
     * Example test method.
     */
    public void testMethod2() {
        X.println("Output from TestC.testMethod2().");
    }

    /**
     * Example test method.
     */
    public void testMethod3() {
        X.println("Output from TestC.testMethod3().");

        assertTrue("Failed assertion from TestC.testMethod3().", false);
    }

    /**
     * Example test method.
     */
    public void testMethod4() {
        X.println("Output from TestC.testMethod4().");

        throw new RuntimeException("Failed exception from TestC.testMethod4().");
    }
}
