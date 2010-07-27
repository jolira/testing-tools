/**
 *
 */
package com.jolira.testing;

import static com.jolira.testing.TestUtils.getBaseDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.BindException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

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
        final InputStreamReader _reader = new InputStreamReader(in);
        final BufferedReader reader = new BufferedReader(_reader);
        final ServletOutputStream out = response.getOutputStream();
        final Writer writer = new OutputStreamWriter(out);

        try {
            for (;;) {
                final String line = reader.readLine();

                if (line == null) {
                    break;
                }

                writer.write(line);
            }
        } finally {
            writer.close();
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

            server.addHandler(new AbstractHandler() {
                @Override
                public void handle(final String target, final HttpServletRequest request,
                        final HttpServletResponse response, final int dispatch) throws IOException, ServletException {
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
}
