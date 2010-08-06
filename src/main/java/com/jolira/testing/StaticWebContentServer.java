package com.jolira.testing;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A very simple server that serves static resources from a location in the file system relative to one or more
 * specified files.
 * 
 * @author jfk
 * @date Jul 26, 2010 9:27:06 PM
 */
public class StaticWebContentServer extends WebServerEmulator {
    static class Mapping {
        final String prefix;
        final File path;

        Mapping(final String prefix, final File path) {
            this.prefix = prefix;
            this.path = path;
        }
    }

    private final Collection<Mapping> mapped = new TreeSet<Mapping>(new Comparator<Mapping>() {
        @Override
        public int compare(final Mapping m1, final Mapping m2) {
            final int l1 = m1.prefix.length();
            final int l2 = m2.prefix.length();
            final int diff = l2 - l1;

            if (diff != 0) {
                return diff;
            }

            return m1.prefix.compareTo(m2.prefix);
        }
    });

    static final Map<String, String> mimeTypeByExtension = new HashMap<String, String>();
    static final String DEFAULT_MIME_TYPE = "unknown/unknown";

    static {
        mimeTypeByExtension.put(".js", "text/javascript");
        mimeTypeByExtension.put(".html", "text/html");
        mimeTypeByExtension.put(".htm", "text/html");
        mimeTypeByExtension.put(".css", "text/css");
        mimeTypeByExtension.put(".xml", "text/xml");
        mimeTypeByExtension.put(".gif", "image/gif");
        mimeTypeByExtension.put(".jpg", "image/jpeg");
        mimeTypeByExtension.put(".png", "image/png");
    }

    /**
     * Add a new prefix mapping to a file.
     * 
     * @param prefix
     *            the prefix, such a /images
     * @param path
     *            the directory where to find the images
     */
    public void addMapping(final String prefix, final File path) {
        final String _prefix = prefix.startsWith("/") ? prefix : "/" + prefix;
        final Mapping mapping = new Mapping(_prefix, path);

        synchronized (mapped) {
            mapped.add(mapping);
        }
    }

    private synchronized File findMapping(final String target) {
        final Mapping mapping = findMappingEnry(target);

        if (mapping == null) {
            return null;
        }

        final int len = mapping.prefix.length();
        final String path = target.substring(len);

        return new File(mapping.path, path);
    }

    private Mapping findMappingEnry(final String target) {
        synchronized (mapped) {
            for (final Mapping mapping : mapped) {
                if (target.startsWith(mapping.prefix)) {
                    return mapping;
                }
            }
        }

        return null;
    }

    private String getMimeType(final File mapping) {
        final String path = mapping.getPath();
        final int idx = path.lastIndexOf('.');

        if (idx == -1) {
            return DEFAULT_MIME_TYPE;
        }

        final String mimeType = mimeTypeByExtension.get(path.substring(idx));

        return mimeType == null ? DEFAULT_MIME_TYPE : mimeType;
    }

    @Override
    protected void handle(final String target, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        final File mapping = findMapping(target);

        if (mapping == null) {
            handleNotFound(target, request, response);
            return;
        }

        final String mimeType = getMimeType(mapping);

        respond(mimeType, response, mapping);
    }

    /**
     * Sends a 404 error. Override in subclasses if different behavior is requried.
     * 
     * @param target
     *            the target
     * @param request
     *            the request
     * @param response
     *            the response
     * @throws IOException
     *             bad thing happened
     */
    protected void handleNotFound(final String target, final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        response.sendError(SC_NOT_FOUND, "target " + target + " not supported");
    }
}
