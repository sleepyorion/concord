package com.walmartlabs.concord.server.plugins.iamsso;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.walmartlabs.concord.server.cfg.SsoConfiguration;
import org.eclipse.jetty.util.URIUtil;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

@Named
public class RedirectHelper {

    private final String redirectHost;

    @Inject
    public RedirectHelper(SsoConfiguration cfg) {
        this.redirectHost = getBaseUrl(cfg.getRedirectUrl());
    }

    public void sendRedirect(HttpServletResponse response, String location) throws IOException {
        response.sendRedirect(response.encodeRedirectURL(normalizeLocation(location)));
    }

    private String normalizeLocation(String location) {
        if (URIUtil.hasScheme(location)) {
            return location;
        }

        return concatPath(this.redirectHost, location);
    }

    private static String concatPath(String host, String location) {
        if (host == null) {
            return location;
        }

        if (location.startsWith("/")) {
            return host + location;
        } else {
            return host + "/" + location;
        }
    }

    private static String getBaseUrl(String s) {
        if(s == null || s.trim().isEmpty()) {
            return null;
        }

        URL url;
        try {
            url = new URL(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (url.getPort() == -1) {
            return url.getProtocol() + "://" + url.getHost();
        } else {
            return url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
        }
    }
}
