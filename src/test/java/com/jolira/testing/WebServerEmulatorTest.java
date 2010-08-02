/**
 * (C) 2010 jolira (http://www.jolira.com). Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.gnu.org/licenses/gpl-3.0-standalone.html Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package com.jolira.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mortbay.jetty.Server;

/**
 * @author jfk
 */
public class WebServerEmulatorTest {
    private static final String TEST_TARGET = "/test";

    /**
     * Test method for {@link WebServerEmulator#createServer(int)}.
     * 
     * @throws Exception
     */
    @Test
    public void testCreate2Servers() throws Exception {
        final WebServerEmulator svr1 = new WebServerEmulator() {
            @Override
            protected void handle(final String target, final HttpServletRequest request,
                    final HttpServletResponse response) throws IOException, ServletException {
                fail();
            }
        };
        final WebServerEmulator svr2 = new WebServerEmulator() {
            @Override
            protected void handle(final String target, final HttpServletRequest request,
                    final HttpServletResponse response) throws IOException, ServletException {
                fail();
            }
        };

        svr1.start();
        svr2.start();

        final String name1 = svr1.getName();
        final String name2 = svr2.getName();

        assertNotNull(name2);
        assertNotNull(name1);

        if (name2.compareTo(name1) <= 0) {
            fail(name1 + " is greater or equal to " + name2);
        }

        svr1.stop();
        svr2.stop();
    }

    /**
     * Test method for {@link WebServerEmulator#createServer(int)}.
     * 
     * @throws Exception
     */
    @Test(expected = IllegalStateException.class)
    public void testCreateNoConnectorServer() throws Exception {
        final WebServerEmulator svr = new WebServerEmulator() {
            @Override
            protected Server createServer(final int port) {
                return new Server();
            }

            @Override
            protected void handle(final String target, final HttpServletRequest request,
                    final HttpServletResponse response) throws IOException, ServletException {
                fail();
            }
        };

        svr.start();
        svr.getName();
    }

    /**
     * Test method for {@link WebServerEmulator#createServer(int)}.
     * 
     * @throws Exception
     */
    @Test
    public void testResponse() throws Exception {
        final WebServerEmulator svr = new WebServerEmulator() {
            @Override
            protected void handle(final String target, final HttpServletRequest request,
                    final HttpServletResponse response) throws IOException, ServletException {
                assertEquals(TEST_TARGET, target);

                respond(response, "/response.xml");
            }
        };

        svr.start();

        final String name = svr.getName();
        final URL url = new URL("http://" + name + TEST_TARGET);
        final URLConnection conn = url.openConnection();

        conn.setReadTimeout(5000);

        final InputStream in = conn.getInputStream();
        final BufferedReader rd = new BufferedReader(new InputStreamReader(in));

        try {
            final String line1 = rd.readLine();
            final String line2 = rd.readLine();

            assertEquals("<response>Hello World</response>", line1);
            assertNull(line2);
        } finally {
            rd.close();
            svr.stop();
        }
    }

    /**
     * Test method for {@link WebServerEmulator#createServer(int)}.
     * 
     * @throws Exception
     */
    // @Test
    // public void testResponse2() throws Exception {
    // final WebServerEmulator svr = new WebServerEmulator() {
    // @Override
    // protected void handle(final String target, final HttpServletRequest request,
    // final HttpServletResponse response) throws IOException, ServletException {
    // assertEquals(TEST_TARGET, target);
    //
    // respond(response, "response.xml");
    // }
    // };
    //
    // svr.start();
    //
    // final String name = svr.getName();
    // final URL url = new URL("http://" + name + TEST_TARGET);
    // final URLConnection conn = url.openConnection();
    //
    // conn.setReadTimeout(5000);
    //
    // final InputStream in = conn.getInputStream();
    // final BufferedReader rd = new BufferedReader(new InputStreamReader(in));
    //
    // try {
    // final String line1 = rd.readLine();
    // final String line2 = rd.readLine();
    //
    // assertEquals("<response>Hello World</response>", line1);
    // assertNull(line2);
    // } finally {
    // rd.close();
    // svr.stop();
    // }
    // }

    /**
     * Test method for {@link WebServerEmulator#createServer(int)}.
     * 
     * @throws Exception
     */
    @Test(expected = IllegalStateException.class)
    public void testStartServerTwice() throws Exception {
        final WebServerEmulator svr = new WebServerEmulator() {
            @Override
            protected Server createServer(final int port) {
                return new Server();
            }

            @Override
            protected void handle(final String target, final HttpServletRequest request,
                    final HttpServletResponse response) throws IOException, ServletException {
                fail();
            }
        };

        svr.start();
        svr.start();
    }
}
