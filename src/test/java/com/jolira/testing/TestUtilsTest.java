/**
 * (C) 2010 jolira (http://www.jolira.com). Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.gnu.org/licenses/gpl-3.0-standalone.html Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package com.jolira.testing;

import static com.jolira.testing.TestUtils.BASEDIR_PROP;
import static com.jolira.testing.TestUtils.getBaseDir;

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * @author jfk
 */
public class TestUtilsTest extends TestCase {
    private static final File TEST_BASEDIR_VAL = new File("/tmp/test");

    /**
     * Test method for {@link TestUtils#getBaseDir(Class)}.
     */
    public void testGetBasedir() {
        final Properties properties = System.getProperties();
        final String org = properties.getProperty(BASEDIR_PROP);

        try {
            properties.remove(BASEDIR_PROP);
            final File baseDir = getBaseDir(TestUtilsTest.class);

            assertNotNull(baseDir);

            properties.setProperty(BASEDIR_PROP, TEST_BASEDIR_VAL.getAbsolutePath());

            final File baseDir2 = getBaseDir(TestUtilsTest.class);

            assertEquals(TEST_BASEDIR_VAL, baseDir2);
        } finally {
            if (org != null) {
                properties.setProperty(BASEDIR_PROP, org);
            } else {
                properties.remove(BASEDIR_PROP);
            }
        }
    }
}
