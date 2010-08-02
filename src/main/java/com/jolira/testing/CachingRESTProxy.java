/**
 * Copyright (c) 2010 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.testing;

import static com.jolira.testing.StaticWebContentServer.DEFAULT_MIME_TYPE;
import static com.jolira.testing.StaticWebContentServer.mimeTypeByExtension;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Parser;

/**
 * REST has a number of wonderful characteristics. One of which is that it can be cached very easily. This class allows
 * for easy caching. This class is such proxy that employs caching for backend systems. The backend is only called if
 * the entry is not found in the cache.
 * 
 * @author jfk
 * @date Aug 1, 2010 9:12:44 PM
 * @since 1.0
 */
public class CachingRESTProxy {
    private static final String ENCODING = System.getProperty("file.encoding");
    private static final String STATUS_PROPERTY = "status";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CACHE = "cache";
    private static final String SERVER = "server";
    private static final String USE_SSL = "ssl";
    private static final String HELP = "help";

    /**
     * @param query
     * @return the default content type for the query
     */
    protected final static String getDefaultContentType(final File query) {
        final String name = query.getName();
        final int dot = name.lastIndexOf('.');
        final String ext = dot == -1 ? null : name.substring(dot);

        return ext == null ? DEFAULT_MIME_TYPE : mimeTypeByExtension.get(ext);
    }

    private static File getPropertiesFile(final File query) {
        return new File(query, ".prp");
    }

    private static String getQuery(final String target, final HttpServletRequest request)
            throws UnsupportedEncodingException {
        final StringBuilder buf = new StringBuilder();

        buf.append(target);

        @SuppressWarnings("unchecked")
        final Enumeration<String> names = request.getParameterNames();

        if (names == null) {
            return buf.toString();
        }

        boolean first = true;

        while (names.hasMoreElements()) {
            final String name = names.nextElement();
            final String[] vals = request.getParameterValues(name);

            for (final String val : vals) {
                final String encoded = URLEncoder.encode(val, ENCODING);

                if (first) {
                    first = false;
                    buf.append('?');
                } else {
                    buf.append('&');
                }

                buf.append(name);
                buf.append('=');
                buf.append(encoded);
            }
        }

        return buf.toString();
    }

    private static File getResourceFile(final File query) {
        return new File(query, ".dmp");
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        final Parser parser = new GnuParser();
        final HelpFormatter formatter = new HelpFormatter();
        final Options options = new Options();

        options.addOption("s", SERVER, true, "server name and port number as server:port");
        options.addOption("c", CACHE, true, "chache directory");
        options.addOption("x", USE_SSL, false, "use ssl");
        options.addOption("?", HELP, false, "display help");

        final CommandLine cli = parser.parse(options, args);

        if (cli.hasOption(HELP)) {
            formatter.printHelp(CachingRESTProxy.class.getName(), options);
            return;
        }

        final String server = cli.getOptionValue(SERVER);
        final String cache = cli.getOptionValue(CACHE);
        final boolean ssl = cli.hasOption(USE_SSL);

        final CachingRESTProxy proxy = new CachingRESTProxy(ssl, server, new File(cache));

        proxy.start();
    }

    private final WebServerEmulator server;
    private final File cache;
    private final boolean ssl;

    private final String backend;

    /**
     * Create a new proxy.
     * 
     * @param ssl
     *            {@literal true} to indicate that the server uses SSL.
     * @param server
     *            the server name and password
     * @param cache
     *            the cache
     */
    public CachingRESTProxy(final boolean ssl, final String server, final File cache) {
        backend = server;
        this.ssl = ssl;
        this.cache = cache;
        this.server = ssl ? new SecureWebServerEmulator() {
            @Override
            protected void handle(final String target, final HttpServletRequest request,
                    final HttpServletResponse response) throws IOException, ServletException {
                CachingRESTProxy.this.handle(target, request, response);
            }
        } : new WebServerEmulator() {
            @Override
            protected void handle(final String target, final HttpServletRequest request,
                    final HttpServletResponse response) throws IOException, ServletException {
                CachingRESTProxy.this.handle(target, request, response);
            }
        };
    }

    private void cacheResponse(final File queryDir, final HttpURLConnection connection, final InputStream in)
            throws IOException, FileNotFoundException {
        final String contentType = connection.getHeaderField(CONTENT_TYPE);
        final int code = connection.getResponseCode();
        final String defaultContentType = getDefaultContentType(queryDir);
        final boolean simple = code == HttpServletResponse.SC_OK && defaultContentType.equals(contentType);
        final File resourceFile = simple ? queryDir : getResourceFile(queryDir);

        copy(in, resourceFile);

        if (simple) {
            return;
        }

        final Properties prps = new Properties();

        prps.put(CONTENT_TYPE, contentType);
        prps.put(STATUS_PROPERTY, Integer.toString(code));

        final File propertiesFile = getPropertiesFile(queryDir);
        final OutputStream out = new FileOutputStream(propertiesFile);

        try {
            prps.store(out, "gerated by " + CachingRESTProxy.class);
        } finally {
            out.close();
        }
    }

    private void cacheResponse(final String query, final File queryDir) throws IOException {
        if (!queryDir.isDirectory()) {
            queryDir.mkdirs();
        }

        final String protocol = ssl ? "https" : "http";
        final String _url = protocol + "://" + backend + query;
        final URL url = new URL(_url);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        final InputStream in = connection.getInputStream();

        try {
            cacheResponse(queryDir, connection, in);
        } finally {
            in.close();
        }
    }

    private void copy(final InputStream in, final File resource) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        final PrintWriter writer = new PrintWriter(resource);

        try {
            for (;;) {
                final String line = reader.readLine();

                if (line == null) {
                    break;
                }

                writer.println(line);
            }
        } finally {
            writer.close();
        }
    }

    private CachedResponse getCached(final File query) throws IOException {
        if (!query.exists()) {
            return null;
        }

        if (query.isFile()) {
            return getSimpleResponse(query);
        }

        final Properties prps = new Properties();
        final File propertiesFile = getPropertiesFile(query);
        final File resourceFile = getResourceFile(query);
        final InputStream in = new FileInputStream(propertiesFile);

        try {
            prps.load(in);
        } finally {
            in.close();
        }

        return new CachedResponse() {
            @Override
            String getContentType() {
                return prps.getProperty(CONTENT_TYPE);
            }

            @Override
            File getResource() {
                return resourceFile;
            }

            @Override
            int getStatus() {
                final String status = prps.getProperty(STATUS_PROPERTY);

                return Integer.parseInt(status);
            }
        };
    }

    private File getDirectory(final String query) {
        File file = cache;
        final StringTokenizer izer = new StringTokenizer(query, "/");

        while (izer.hasMoreTokens()) {
            final String token = izer.nextToken();

            file = new File(file, token);
        }

        return file;
    }

    /**
     * @return the host name
     * @see WebServerEmulator#getHostName()
     */
    public String getHostName() {
        return server.getHostName();
    }

    /**
     * @return the server name
     * @see WebServerEmulator#getName()
     */
    public String getName() {
        return server.getName();
    }

    /**
     * @return the listen port
     * @see WebServerEmulator#getPort()
     */
    public int getPort() {
        return server.getPort();
    }

    private CachedResponse getSimpleResponse(final File query) {
        return new CachedResponse() {
            @Override
            String getContentType() {
                return getDefaultContentType(query);
            }

            @Override
            File getResource() {
                return query;
            }

            @Override
            int getStatus() {
                return HttpServletResponse.SC_OK;
            }
        };
    }

    /**
     * @param target
     * @param request
     * @param response
     * @throws IOException
     */
    protected void handle(final String target, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        final String query = getQuery(target, request);
        final File queryDir = getDirectory(query);

        while (!handleCachedResponse(queryDir, response)) {
            cacheResponse(query, queryDir);
        }
    }

    private boolean handleCachedResponse(final File query, final HttpServletResponse response) throws IOException {
        final CachedResponse cached = getCached(query);

        if (cached == null) {
            return false;
        }

        final int status = cached.getStatus();
        final String mimeType = cached.getContentType();
        final File resource = cached.getResource();

        response.setStatus(status);
        respond(mimeType, response, resource);

        return true;
    }

    /**
     * @param mimeType
     * @param response
     * @param resource
     * @throws IOException
     * @see WebServerEmulator#respond(java.lang.String, javax.servlet.http.HttpServletResponse, java.io.File)
     */
    protected void respond(final String mimeType, final HttpServletResponse response, final File resource)
            throws IOException {
        server.respond(mimeType, response, resource);
    }

    /**
     * @return the port number
     * @throws Exception
     * @see WebServerEmulator#start()
     */
    public int start() throws Exception {
        return server.start();
    }

    /**
     * @throws Exception
     * @see WebServerEmulator#stop()
     */
    public void stop() throws Exception {
        server.stop();
    }
}
