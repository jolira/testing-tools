/**
 * (C) 2010 jolira (http://www.jolira.com). Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.gnu.org/licenses/gpl-3.0-standalone.html Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package com.jolira.testing;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SslSocketConnector;

/**
 * @author jfk
 */
public abstract class SecureWebServerEmulator extends WebServerEmulator {
    @Override
    protected Server createServer(final int _port) {
        final SslSocketConnector connector = new SslSocketConnector();
        final Server svr = new Server();

        connector.setPort(_port);
        svr.addConnector(connector);

        return svr;
    }
}
