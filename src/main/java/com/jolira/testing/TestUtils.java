/**
 * (C) 2010 jolira (http://www.jolira.com). Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.gnu.org/licenses/gpl-3.0-standalone.html Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.jolira.testing;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * Various routines for testing.
 * 
 * @author jfk
 */
public class TestUtils {
    static final String BASEDIR_PROP = "basedir";

    /**
     * Return the Maven-style base directory for a given class. This method first checks the {@link System} property
     * {@literal basedir} and returns its value, if specified. If this {@link System} property was not specified, this
     * operation calculates the {@link CodeSource} location of the {@code class} parameter and returns the directory
     * that is two levels up from this location (as Maven keeps classes in {@literal basedir/target/classes}.
     * 
     * @param clazz
     * @return
     */
    public static String getBaseDir(final Class<?> clazz) {
        final String basedir = System.getProperty(BASEDIR_PROP);

        if (basedir != null) {
            return basedir;
        }

        final ProtectionDomain pd = clazz.getProtectionDomain();
        final CodeSource cs = pd.getCodeSource();
        final URL location = cs.getLocation();
        final String classes = location.getFile();
        final File _classes = new File(classes);
        final File target = _classes.getParentFile();
        final File _basedir = target.getParentFile();

        return _basedir.getAbsolutePath();
    }

    private TestUtils() {
        // nothing
    }
}
