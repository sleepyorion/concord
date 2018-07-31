package com.walmartlabs.concord.it.webhook;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import com.google.common.base.Strings;

public class ITConstants {

    public static final String SERVER_URL;
    public static final String GIT_SERVER_URL_PATTERN;
    public static final Integer GIT_WEBHOOK_MOCK_PORT;

    static {
        SERVER_URL = "http://localhost:" + env("IT_SERVER_PORT", "8001");

        String dockerAddr = env("IT_DOCKER_HOST_ADDR", "127.0.0.1");
        String gitHost = dockerAddr != null ? dockerAddr : "localhost";
        GIT_SERVER_URL_PATTERN = "ssh://git@" + gitHost + ":%d/";

        GIT_WEBHOOK_MOCK_PORT = Integer.parseInt(env("IT_GIT_WEBHOOK_MOCK_PORT", "4567"));
    }

    private static String env(String k, String def) {
        String v = System.getenv(k);
        if (Strings.isNullOrEmpty(v)) {
            return def;
        }
        return v;
    }

    private ITConstants() {
    }
}