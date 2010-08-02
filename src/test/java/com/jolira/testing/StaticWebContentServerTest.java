/**
 * Copyright (c) 2010 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.testing;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Test;

/**
 * @author jfk
 * @date Jul 26, 2010 11:56:46 PM
 * @since 1.0
 */
public class StaticWebContentServerTest {
    static void read(final String hostName, final int port, final String file) throws MalformedURLException,
            IOException {
        final URL url = new URL("http", hostName, port, file);
        final URLConnection connection = url.openConnection();
        final InputStream in = connection.getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
            final String line = reader.readLine();

            assertEquals("<html><head><title>Test!</title></head><body><h1>Test!</h1></body></html>", line);
        } finally {
            reader.close();
        }
    }

    /**
     * Test method for
     * {@link com.jolira.testing.StaticWebContentServer#handle(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     * .
     * 
     * @throws Exception
     */
    @Test
    public void testHandle() throws Exception {
        final File base = TestUtils.getBaseDir(StaticWebContentServerTest.class);
        final File dir1 = new File(base, "src/test/resources/dir1");
        final StaticWebContentServer server = new StaticWebContentServer();

        server.addMapping("/", dir1);
        server.addMapping("special", new File(dir1, "index.html"));
        server.start();

        try {
            final String hostName = server.getHostName();
            final int port = server.getPort();

            read(hostName, port, "/index.html");
            read(hostName, port, "/special");
        } finally {
            server.stop();
        }
    }

    /**
     * Test method for
     * {@link com.jolira.testing.StaticWebContentServer#handle(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     * .
     * 
     * @throws Exception
     */
    @Test(expected = FileNotFoundException.class)
    public void testNotFound() throws Exception {
        final StaticWebContentServer server = new StaticWebContentServer();

        server.start();

        try {
            final String hostName = server.getHostName();
            final int port = server.getPort();

            read(hostName, port, "/index.html");
            read(hostName, port, "/special");
        } finally {
            server.stop();
        }
    }

    /**
     * Test method for
     * {@link com.jolira.testing.StaticWebContentServer#handle(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     * .
     * 
     * @throws Exception
     */
    @Test
    public void testUnknownMimeType() throws Exception {
        final File base = TestUtils.getBaseDir(StaticWebContentServerTest.class);
        final File file1 = new File(base, "src/test/resources/dir1/example.grr");
        final File file2 = new File(base, "src/test/resources/dir1/example");
        final StaticWebContentServer server = new StaticWebContentServer();

        server.addMapping("/special1", file1);
        server.addMapping("special2", file2);
        server.start();

        try {
            final String hostName = server.getHostName();
            final int port = server.getPort();

            read(hostName, port, "/special1");
            read(hostName, port, "/special2");
        } finally {
            server.stop();
        }
    }
}
