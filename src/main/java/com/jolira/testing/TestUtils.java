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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
     *            the class to use to determine the basedir.
     * @return the base directory (according to the default Maven conventions).
     */
    public static File getBaseDir(final Class<?> clazz) {
        final String basedir = System.getProperty(BASEDIR_PROP);

        if (basedir != null) {
            return new File(basedir);
        }

        final ProtectionDomain pd = clazz.getProtectionDomain();
        final CodeSource cs = pd.getCodeSource();
        final URL location = cs.getLocation();
        final String protocol = location.getProtocol();

        if (!"file".equals(protocol)) {
            throw new Error("coude source for class " + clazz
                    + " is not a directory and cannot be used to determine the project dir: " + location);
        }

        final String classes = location.getFile();
        final File _classes = new File(classes);
        final File target = _classes.getParentFile();

        return target.getParentFile();
    }

    private static String getNormalizedFileName(final Class<?> clazz, final String file) {
        if (file.startsWith("/")) {
            return file.substring(1);
        }

        final Package pkg = clazz.getPackage();
        final String name = pkg.getName();
        final String path = name.replace('.', '/');

        return path + '/' + file;
    }

    /**
     * Finds a resource given a name. First tries {@link Class#getResourceAsStream(String)}. If this call does not find
     * the resource, the base directory is searched using {@link #getBaseDir(Class)}. The resources is first searched as
     * ${basedir}/src/main/resources/${file} and then as ${basedir}/src/test/resources/${file}.
     * 
     * @param clazz
     *            the class
     * @param file
     *            the file
     * @return the resource or {literal null), if not found
     * @throws FileNotFoundException
     */
    public static InputStream getResourceAsStream(final Class<?> clazz, final String file) throws FileNotFoundException {
        final InputStream in = clazz.getResourceAsStream(file);

        if (in != null) {
            return in;
        }

        final File baseDir = getBaseDir(clazz);
        final String[] dirs = new String[] { "src/main/resources", "src/test/resources" };

        for (final String dir : dirs) {
            final File resources = new File(baseDir, dir);
            final String normalized = getNormalizedFileName(clazz, file);
            final File _file = new File(resources, normalized);

            if (_file.isFile()) {
                return new FileInputStream(_file);
            }
        }

        return null;
    }

    private TestUtils() {
        // nothing
    }
}
