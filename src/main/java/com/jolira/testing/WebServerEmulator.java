/**
 *
 */
package com.jolira.testing;

import static com.jolira.testing.TestUtils.getBaseDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * Manages a little server, which can be launched at the beginning of unit tests and stopped at the end of it, such as
 * in
 * 
 * <pre>
 * public class ExampleTest {
 *     private static final String TEST_TARGET = "/test";
 *     private static WebServerEmulator server;
 * 
 *     @AfterClass
 *     public static void shutdown() throws Exception {
 *         server.stop();
 * 
 *         server = null;
 *     }
 * 
 *     @BeforeClass
 *     public static void startup() throws Exception {
 *         server = new WebServerEmulator() {
 *             @Override
 *             protected void handle(final String target,
 *                     final HttpServletRequest request,
 *                     final HttpServletResponse response) throws IOException,
 *                     ServletException {
 *                 assertEquals(TEST_TARGET, target);
 * 
 *                 respond(response, "/response.xml");
 *             }
 *         };
 * 
 *         server.start();
 *     }
 * </pre>
 * 
 * The server will return the content set using {@link #respond(String, HttpServletResponse, String)}. The hostname:port
 * combination required for accessing this server is returned when calling {@link #getName()}.
 * 
 * @author jfk
 */
public abstract class WebServerEmulator {
    private static final String LOCALHOST = "localhost";
    private static final int PORT = 16000;

    private Server server = null;

    /**
     * Create the server using a given port. To be overridden by subclasses that need to create different types of
     * servers (such as ones using HTTPS connectors).
     * 
     * @param _port
     * @return the newly created server
     */
    protected Server createServer(final int _port) {
        return new Server(_port);
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WebServerEmulator other = (WebServerEmulator) obj;
        if (server == null) {
            if (other.server != null) {
                return false;
            }
        } else if (!server.equals(other.server)) {
            return false;
        }
        return true;
    }

    /**
     * @return {@literal "localhost"}
     */
    public String getHostName() {
        return LOCALHOST;
    }

    /**
     * @return name and port to be used to access the server
     */
    public String getName() {
        final int port = getPort();

        return LOCALHOST + ':' + port;
    }

    /**
     * @return the port the server listens to
     */
    public int getPort() {
        final Connector[] connectors = server.getConnectors();

        if (connectors == null) {
            throw new IllegalStateException("server must contain at least one connector");
        }

        return connectors[0].getPort();
    }

    private InputStream getResourceAsStream(final String resource) throws FileNotFoundException {
        final InputStream in = WebServerEmulator.class.getResourceAsStream(resource);

        if (in != null) {
            return in;
        }

        final Class<? extends WebServerEmulator> clazz = this.getClass();
        final File basedir = getBaseDir(clazz);
        final File resources = new File(basedir, "src/test/resources");
        final File _resource = new File(resources, resource);

        return new FileInputStream(_resource);
    }

    /**
     * Handle an incoming request. This method has to be implemented by subclasses. These implementation should perform
     * validation on the incoming request and use {@link #respond(HttpServletResponse, InputStream)},
     * {@link #respond(HttpServletResponse, String)}, and {@link #respond(String, HttpServletResponse, String)} to
     * return data to the caller.
     * 
     * @param target
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    protected abstract void handle(final String target, final HttpServletRequest request,
            final HttpServletResponse response) throws IOException, ServletException;

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (server == null ? 0 : server.hashCode());
        return result;
    }

    /**
     * Respond with the content of a particular input stream. This method copied the contents of the stream into the
     * response object.
     * 
     * @param response
     *            the response object to be used
     * @param in
     *            the string to be copied to the response
     * @throws IOException
     *             bad things happened
     */
    protected void respond(final HttpServletResponse response, final InputStream in) throws IOException {
        final ServletOutputStream out = response.getOutputStream();

        try {
            final byte[] buffer = new byte[65535];

            for (;;) {
                final int read = in.read(buffer);

                if (read == -1) {
                    break;
                }

                out.write(buffer, 0, read);
            }
        } finally {
            out.close();
        }
    }

    /**
     * Respond with an xml file.
     * 
     * @param response
     *            the http response
     * @param resource
     *            the file to return
     * @throws IOException
     *             something went wrong
     */
    public void respond(final HttpServletResponse response, final String resource) throws IOException {
        respond("text/xml", response, resource);
    }

    /**
     * Respond with a static file.
     * 
     * @param mimeType
     *            the file type
     * @param response
     *            the http response
     * @param resource
     *            the file to return
     * @throws IOException
     *             something went wrong
     */
    public void respond(final String mimeType, final HttpServletResponse response, final File resource)
            throws IOException {
        response.setContentType(mimeType);

        final InputStream in = new FileInputStream(resource);

        try {
            respond(response, in);
        } finally {
            in.close();
        }
    }

    /**
     * Respond with a static file.
     * 
     * @param mimeType
     *            the file type
     * @param response
     *            the http response
     * @param resource
     *            the file to return
     * @throws IOException
     *             something went wrong
     */
    public void respond(final String mimeType, final HttpServletResponse response, final String resource)
            throws IOException {
        response.setContentType(mimeType);

        final InputStream in = getResourceAsStream(resource);

        try {
            respond(response, in);
        } finally {
            in.close();
        }
    }

    /**
     * Start the server.
     * 
     * @return the port
     * @throws Exception
     *             the startup failed
     */
    public int start() throws Exception {
        if (server != null && server.isStarted()) {
            throw new IllegalStateException();
        }

        for (int _port = PORT;; _port++) {
            server = createServer(_port);

            server.setHandler(new AbstractHandler() {
                @Override
                public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
                        final HttpServletResponse response) throws IOException, ServletException {
                    WebServerEmulator.this.handle(target, request, response);
                }
            });

            try {
                server.start();
            } catch (final BindException e) {
                continue;
            }

            return _port;
        }
    }

    /**
     * Stop the server.
     * 
     * @throws Exception
     *             shut the server down
     */
    public void stop() throws Exception {
        server.stop();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append("WebServerEmulator [server=");
        builder.append(server);
        builder.append("]");

        return builder.toString();
    }
}
