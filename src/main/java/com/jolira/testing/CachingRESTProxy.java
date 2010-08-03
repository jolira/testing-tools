/**
 * Copyright (c) 2010 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.testing;

import static com.jolira.testing.StaticWebContentServer.DEFAULT_MIME_TYPE;
import static com.jolira.testing.StaticWebContentServer.mimeTypeByExtension;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
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
    static abstract class CachedResponse {
        abstract String getContentType();

        abstract Map<String, String> getHeaders();

        abstract File getResource();

        abstract int getStatus();
    }

    private static final String SET_COOKIE = "Set-Cookie";
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

        final Map<String, String[]> params = getSortedParameters(request);
        final Set<Entry<String, String[]>> entries = params.entrySet();
        boolean first = true;

        for (final Entry<String, String[]> entry : entries) {
            final String name = entry.getKey();
            final String[] vals = entry.getValue();

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

    private static Map<String, String[]> getSortedParameters(final HttpServletRequest request) {
        final Map<String, String[]> params = new TreeMap<String, String[]>();
        @SuppressWarnings("unchecked")
        final Enumeration<String> names = request.getParameterNames();

        if (names == null) {
            return params;
        }

        while (names.hasMoreElements()) {
            final String name = names.nextElement();
            final String[] vals = request.getParameterValues(name);

            params.put(name, vals);
        }

        return params;
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        final Parser parser = new GnuParser();
        final HelpFormatter formatter = new HelpFormatter();
        final Options options = new Options();

        options.addOption("c", CACHE, true, "chache directory (mandatory!)");
        options.addOption("s", SERVER, true, "server name and port number as server:port");
        options.addOption("x", USE_SSL, false, "use ssl");
        options.addOption("?", HELP, false, "display help");

        final CommandLine cli = parser.parse(options, args);
        final String server = cli.getOptionValue(SERVER);
        final String cache = cli.getOptionValue(CACHE);
        final boolean ssl = cli.hasOption(USE_SSL);

        if (cli.hasOption(HELP) || cache == null) {
            formatter.printHelp(CachingRESTProxy.class.getName(), options);
            return;
        }

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
        final boolean simple = code == SC_OK && equalsContentType(defaultContentType, contentType);
        final File resourceFile = simple ? queryDir : getResourceFile(queryDir);
        final String cookies = connection.getHeaderField(SET_COOKIE);

        copy(in, resourceFile);

        if (simple) {
            return;
        }

        final Properties prps = new Properties();

        if (contentType != null) {
            prps.put(CONTENT_TYPE, contentType);
        }

        if (cookies != null) {
            prps.put(SET_COOKIE, cookies);
        }

        prps.put(STATUS_PROPERTY, Integer.toString(code));

        final File propertiesFile = getPropertiesFile(queryDir);
        final OutputStream out = new FileOutputStream(propertiesFile);

        try {
            prps.store(out, "gerated by " + CachingRESTProxy.class);
        } finally {
            out.close();
        }
    }

    private boolean cacheResponse(final String query, final File queryDir, final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        if (backend == null) {
            return false;
        }

        final String protocol = ssl ? "https" : "http";
        final String _url = protocol + "://" + backend + query;
        final URL url = new URL(_url);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        final Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            final StringBuilder cookieVal = new StringBuilder();

            for (final Cookie cookie : cookies) {
                final String value = cookie.getValue();
                final String name = cookie.getName();

                cookieVal.append(name);
                cookieVal.append('=');
                cookieVal.append(value);
                cookieVal.append(';');
            }

            connection.setRequestProperty("Cookie", cookieVal.toString());
        }

        final InputStream in = connection.getInputStream();

        try {
            cacheResponse(queryDir, connection, in);
        } finally {
            in.close();
        }

        return true;
    }

    private void copy(final InputStream in, final File resource) throws IOException {
        final File dir = resource.getParentFile();

        if (!dir.exists()) {
            dir.mkdirs();
        }

        final OutputStream out = new BufferedOutputStream(new FileOutputStream(resource));

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

    private boolean equalsContentType(final String type1, final String type2) {
        if (type1 == null) {
            return type2 == null;
        }

        final String _type1 = getBaseContentType(type1);
        final String _type2 = getBaseContentType(type2);

        return _type1.equals(_type2);
    }

    private String getBaseContentType(final String type) {
        final int idx = type.indexOf(';');

        if (idx == -1) {
            return type;
        }

        return type.substring(0, idx);
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
            Map<String, String> getHeaders() {
                final String cookies = prps.getProperty(SET_COOKIE);
                final Map<String, String> headers = new HashMap<String, String>();

                headers.put(SET_COOKIE, cookies);

                return headers;
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
            Map<String, String> getHeaders() {
                return null;
            }

            @Override
            File getResource() {
                return query;
            }

            @Override
            int getStatus() {
                return SC_OK;
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
            if (!cacheResponse(query, queryDir, request, response)) {
                response.setStatus(SC_NOT_FOUND);
                return;
            }
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
        final Map<String, String> headers = cached.getHeaders();

        if (headers != null) {
            final Collection<Entry<String, String>> entries = headers.entrySet();

            for (final Entry<String, String> entry : entries) {
                final String name = entry.getKey();
                final String value = entry.getValue();

                response.setHeader(name, value);
            }
        }
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
