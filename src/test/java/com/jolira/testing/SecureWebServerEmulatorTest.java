/**
 * (C) 2010 jolira (http://www.jolira.com). Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.gnu.org/licenses/gpl-3.0-standalone.html Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package com.jolira.testing;

import static com.jolira.testing.SecureWebServerEmulator.DEFAULT_KEYSTORE;
import static com.jolira.testing.SecureWebServerEmulator.DEFAULT_KEY_PASSWORD;
import static com.jolira.testing.SecureWebServerEmulator.DEFAULT_TRUST_PASSWORD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

/**
 * @author jfk
 */
public class SecureWebServerEmulatorTest {
    private static final String TEST_TARGET = "/secure";

    /**
     * Test method for {@link SecureWebServerEmulator#SecureWebServerEmulator()}.
     * 
     * @throws Exception
     */
    @Test
    public void testCustomerKeyStore() throws Exception {
        final URL url = SecureWebServerEmulator.class
                .getResource(DEFAULT_KEYSTORE);
        final String _url = url.toExternalForm();
        final SecureWebServerEmulator svr = new SecureWebServerEmulator(_url,
                DEFAULT_TRUST_PASSWORD, DEFAULT_KEY_PASSWORD) {
            @Override
            protected void handle(final String target,
                    final HttpServletRequest request,
                    final HttpServletResponse response) throws IOException,
                    ServletException {
                assertEquals(TEST_TARGET, target);

                respond(response, "response.xml");
            }
        };

        svr.start();

        // BEGIN: http://www.exampledepot.com/egs/javax.net.ssl/trustall.html
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public void checkClientTrusted(
                    final java.security.cert.X509Certificate[] certs,
                    final String authType) {
                // empty
            }

            public void checkServerTrusted(
                    final java.security.cert.X509Certificate[] certs,
                    final String authType) {
                // empty
            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        } };

        // Install the all-trusting trust manager
        final SSLContext sc = SSLContext.getInstance("SSL");

        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // END: http://www.exampledepot.com/egs/javax.net.ssl/trustall.html

        final String name = svr.getName();
        final URL url_ = new URL("https://" + name + TEST_TARGET);
        final URLConnection conn = url_.openConnection();
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
     * Test method for {@link SecureWebServerEmulator#SecureWebServerEmulator()}.
     * 
     * @throws Exception
     */
    @Test
    public void testSecureWebServerEmulator() throws Exception {
        final SecureWebServerEmulator svr = new SecureWebServerEmulator() {
            @Override
            protected void handle(final String target,
                    final HttpServletRequest request,
                    final HttpServletResponse response) throws IOException,
                    ServletException {
                assertEquals(TEST_TARGET, target);

                respond(response, "response.xml");
            }
        };

        svr.start();

        // BEGIN: http://www.exampledepot.com/egs/javax.net.ssl/trustall.html
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public void checkClientTrusted(
                    final java.security.cert.X509Certificate[] certs,
                    final String authType) {
                // empty
            }

            public void checkServerTrusted(
                    final java.security.cert.X509Certificate[] certs,
                    final String authType) {
                // empty
            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        } };

        // Install the all-trusting trust manager
        final SSLContext sc = SSLContext.getInstance("SSL");

        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // END: http://www.exampledepot.com/egs/javax.net.ssl/trustall.html

        final String name = svr.getName();
        final URL url = new URL("https://" + name + TEST_TARGET);
        final URLConnection conn = url.openConnection();
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
}
