package com.jolira.testing;

import java.io.File;

abstract class CachedResponse {
    abstract String getContentType();

    abstract File getResource();

    abstract int getStatus();
}
