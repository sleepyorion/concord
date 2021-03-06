package com.walmartlabs.concord.runtime.v2.runner;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc.
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

import com.walmartlabs.concord.runtime.v2.model.ProcessConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Map;

@Named
// TODO make an interface, extract as a "service"
public class DefaultTaskVariables {

    private final Map<String, Map<String, Object>> variables;

    @Inject
    public DefaultTaskVariables(ProcessConfiguration cfg) {
        this.variables = Collections.unmodifiableMap(cfg.defaultTaskVariables());
    }

    public Map<String, Object> get(String taskName) {
        return variables.get(taskName);
    }
}
