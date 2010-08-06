/**
 * Copyright (c) 2010 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.testing;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    static void read(final String hostName, final int port, final String file) throws MalformedURLException,
            IOException {
        final URL url = new URL("http", hostName, port, file);
        final URLConnection connection = url.openConnection();

        connection.setReadTimeout(5000);

        final InputStream in = connection.getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
            final String line = reader.readLine();

            assertEquals("<html><head><title>Test!</title></head><body><h1>Test!</h1></body></html>", line);
        } finally {
            reader.close();
        }

        final Map<String, List<String>> headers = connection.getHeaderFields();
        final List<String> cookies = headers.get(CachingRESTProxy.SET_COOKIE);
        final int cookieCount = cookies.size();

        assertEquals(4, cookieCount);
        assertEquals("a=b", cookies.get(0));
        assertEquals("c=d", cookies.get(1));
        assertEquals("e=f;Path=/jolira/test;Domain=jolira.com;Secure", cookies.get(2));
        assertEquals("g=h", cookies.get(3));
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

        backend = new StaticWebContentServer() {
            @Override
            protected void handle(final String target, final HttpServletRequest request,
                    final HttpServletResponse response) throws IOException, ServletException {
                response.addCookie(new Cookie("a", "b"));
                response.addCookie(new Cookie("c", "d"));

                final Cookie cookie3 = new Cookie("e", "f");

                cookie3.setSecure(true);
                cookie3.setComment("test");
                cookie3.setPath("/jolira/test");
                cookie3.setDomain("jolira.com");
                cookie3.setMaxAge(3600 * 48);

                response.addCookie(cookie3);
                response.addCookie(new Cookie("g", "h"));
                System.out.println("COOKIES!!!!!!!!!!!!");

                super.handle(target, request, response);
            }

        };

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
        final CachingRESTProxy proxy1 = new CachingRESTProxy(false, backendName, cacheDir);

        try {
            proxy1.start();

            final String hostName = proxy1.getHostName();
            final int port = proxy1.getPort();

            read(hostName, port, "/index.html");
            read(hostName, port, "/index.html");
        } finally {
            proxy1.stop();
        }

        final CachingRESTProxy proxy2 = new CachingRESTProxy(false, null, cacheDir);

        try {
            proxy2.start();

            final String hostName = proxy2.getHostName();
            final int port = proxy2.getPort();

            read(hostName, port, "/index.html");
        } finally {
            proxy2.stop();
        }
    }
}
