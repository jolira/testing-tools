/**
 * (C) 2010 jolira (http://www.jolira.com). Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.gnu.org/licenses/gpl-3.0-standalone.html Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package com.jolira.testing;

import java.net.URL;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SslSocketConnector;

/**
 * Creates a server emulator that supports secure (https) communication.
 * 
 * @author jfk
 */
public abstract class SecureWebServerEmulator extends WebServerEmulator {
    static final String DEFAULT_TRUST_PASSWORD = "jolira";
    static final String DEFAULT_KEY_PASSWORD = "jolira";
    static final String DEFAULT_KEYSTORE = "/keystore";

    private final String keystoreURL;
    private final String trustPassword;
    private final String keyPassword;

    /**
     * Creates a new emulator using the built-in keystore. See
     * {@link "http://www.exampledepot.com/egs/javax.net.ssl/trustall.html"} for how to disable certificate validation
     * for your tests so you can use this approach.
     * 
     * @see "http://www.exampledepot.com/egs/javax.net.ssl/trustall.html"
     */
    public SecureWebServerEmulator() {
        final URL url = SecureWebServerEmulator.class
                .getResource(DEFAULT_KEYSTORE);

        keystoreURL = url.toExternalForm();
        trustPassword = DEFAULT_TRUST_PASSWORD;
        keyPassword = DEFAULT_KEY_PASSWORD;
    }

    /**
     * Creates a server emulator with a user-provided keystore. See
     * {@link "http://docs.codehaus.org/display/JETTY/How+to+configure+SSL"} for creating a keystore.
     * 
     * @param keystoreURL
     *            the location of the keystore
     * @param trustPassword
     *            the password for the keystore
     * @param keyPassword
     *            the password for the key
     * @see "http://docs.codehaus.org/display/JETTY/How+to+configure+SSL"
     */
    public SecureWebServerEmulator(final String keystoreURL,
            final String trustPassword, final String keyPassword) {
        this.keystoreURL = keystoreURL;
        this.trustPassword = trustPassword;
        this.keyPassword = keyPassword;
    }

    @Override
    protected Server createServer(final int _port) {
        final SslSocketConnector connector = new SslSocketConnector();
        final Server svr = new Server();

        connector.setKeystore(keystoreURL);
        connector.setTrustPassword(trustPassword);
        connector.setKeyPassword(keyPassword);
        connector.setPort(_port);
        svr.addConnector(connector);

        return svr;
    }
}
