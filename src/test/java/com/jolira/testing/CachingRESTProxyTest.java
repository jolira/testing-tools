/**
 * Copyright (c) 2010 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.testing;

import static com.jolira.testing.StaticWebContentServerTest.read;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author jfk
 * @date Aug 1, 2010 11:52:31 PM
 * @since 1.0
 */
public class CachingRESTProxyTest {
    private static StaticWebContentServer backend;

    /**
     * @return the cache dir
     * @throws IOException
     */
    protected static final File getCacheDir() throws IOException {
        final File tmp = File.createTempFile("jo-", "-lira");

        tmp.delete();

        return tmp;
    }

    /**
     * Start a fake backend server
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void startBackend() throws Exception {
        final File base = TestUtils.getBaseDir(CachingRESTProxyTest.class);
        final File dir1 = new File(base, "src/test/resources/dir1");

        backend = new StaticWebContentServer();

        backend.addMapping("/", dir1);
        backend.start();
    }

    /**
     * Start a fake backend server
     * 
     * @throws Exception
     */
    @AfterClass
    public static void stopBackend() throws Exception {
        backend.stop();
        backend = null;
    }

    /**
     * Test method for {@link com.jolira.testing.CachingRESTProxy#main(java.lang.String[])}.
     * 
     * @throws Exception
     */
    @Test
    public void testHelp() throws Exception {
        final String[] args = new String[] { "-?" };

        CachingRESTProxy.main(args);
    }

    /**
     * Test method for {@link com.jolira.testing.CachingRESTProxy#main(java.lang.String[])}.
     * 
     * @throws Exception
     */
    @Test
    public void testMain() throws Exception {
        final File cacheDir = getCacheDir();
        final String[] args = new String[] { "--server", backend.getName(), "--cache=", cacheDir.getAbsolutePath() };

        CachingRESTProxy.main(args);
    }

    /**
     * Test method for {@link com.jolira.testing.CachingRESTProxy#main(java.lang.String[])}.
     * 
     * @throws Exception
     */
    @Test
    public void testProxy() throws Exception {
        final File cacheDir = getCacheDir();
        final String backendName = backend.getName();
        final CachingRESTProxy proxy = new CachingRESTProxy(false, backendName, cacheDir);

        try {
            proxy.start();

            final String hostName = proxy.getHostName();
            final int port = proxy.getPort();

            read(hostName, port, "/index.html");
            read(hostName, port, "/index.html");
        } finally {
            proxy.stop();
        }
    }
}
