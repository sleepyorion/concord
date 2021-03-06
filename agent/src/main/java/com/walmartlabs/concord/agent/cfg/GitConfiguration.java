package com.walmartlabs.concord.agent.cfg;

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

import com.typesafe.config.Config;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.time.Duration;

import static com.walmartlabs.concord.agent.cfg.Utils.getStringOrDefault;

@Named
@Singleton
public class GitConfiguration {

    private final String token;
    private final boolean shallowClone;
    private final Duration fetchTimeout;
    private final int httpLowSpeedLimit;
    private final int httpLowSpeedTime;
    private final int sshTimeout;
    private final int sshTimeoutRetryCount;

    @Inject
    public GitConfiguration(Config cfg) {
        this.token = getStringOrDefault(cfg, "git.oauth", () -> null);
        this.shallowClone = cfg.getBoolean("git.shallowClone");
        this.fetchTimeout = cfg.getDuration("git.fetchTimeout");
        this.httpLowSpeedLimit = cfg.getInt("git.httpLowSpeedLimit");
        this.httpLowSpeedTime = cfg.getInt("git.httpLowSpeedTime");
        this.sshTimeout = cfg.getInt("git.sshTimeout");
        this.sshTimeoutRetryCount = cfg.getInt("git.sshTimeoutRetryCount");
    }

    public String getToken() {
        return token;
    }

    public boolean isShallowClone() {
        return shallowClone;
    }

    public Duration getFetchTimeout() {
        return fetchTimeout;
    }

    public int getHttpLowSpeedLimit() {
        return httpLowSpeedLimit;
    }

    public int getHttpLowSpeedTime() {
        return httpLowSpeedTime;
    }

    public int getSshTimeout() {
        return sshTimeout;
    }

    public int getSshTimeoutRetryCount() {
        return sshTimeoutRetryCount;
    }
}
