package com.walmartlabs.concord.runtime.v2.runner;

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

import com.google.inject.*;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.walmartlabs.concord.runtime.common.cfg.RunnerConfiguration;
import com.walmartlabs.concord.runtime.v2.model.ProcessConfiguration;
import com.walmartlabs.concord.runtime.v2.runner.tasks.TaskHolder;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.sdk.Constants;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO refactor as a builder?
public class InjectorFactory {

    private static final Logger log = LoggerFactory.getLogger(InjectorFactory.class);

    public static Injector createDefault(ClassLoader parentClassLoader, RunnerConfiguration runnerCfg) throws IOException {
        Path src = Paths.get(System.getProperty("user.dir"));

        Provider<ProcessConfiguration> processCfgProvider = new DefaultProcessConfigurationProvider(src);
        WorkingDirectory workDir = new WorkingDirectory(src);

        return new InjectorFactory(parentClassLoader, workDir, runnerCfg, processCfgProvider, new DefaultServicesModule())
                .create();
    }

    private final ClassLoader parentClassLoader;
    private final WorkingDirectory workDir;
    private final RunnerConfiguration runnerCfg;
    private final Module[] modules;
    private final Provider<ProcessConfiguration> processConfigurationProvider;

    public InjectorFactory(ClassLoader parentClassLoader,
                           WorkingDirectory workDir,
                           RunnerConfiguration runnerCfg,
                           Provider<ProcessConfiguration> processConfigurationProvider,
                           Module... modules) {

        this.parentClassLoader = parentClassLoader;
        this.workDir = workDir;
        this.runnerCfg = runnerCfg;
        this.modules = modules;
        this.processConfigurationProvider = processConfigurationProvider;
    }

    public Injector create() throws IOException {
        Collection<String> dependencies = runnerCfg.dependencies();
        ClassLoader dependenciesClassLoader = loadDependencies(dependencies);

        Module tasks = new AbstractModule() {
            @Override
            protected void configure() {
                TaskHolder<com.walmartlabs.concord.sdk.Task> v1Holder = new TaskHolder<>();
                bindListener(new SubClassesOf(com.walmartlabs.concord.sdk.Task.class), new TaskClassesListener<>(v1Holder));

                TaskHolder<Task> v2Holder = new TaskHolder<>();
                bindListener(new SubClassesOf(Task.class), new TaskClassesListener<>(v2Holder));

                bind(new TypeLiteral<TaskHolder<com.walmartlabs.concord.sdk.Task>>() {
                }).annotatedWith(TaskHolder.V1.class).toInstance(v1Holder);

                bind(new TypeLiteral<TaskHolder<Task>>() {
                }).annotatedWith(TaskHolder.V2.class).toInstance(v2Holder);
            }
        };

        List<Module> l = new ArrayList<>();
        l.add(new ConfigurationModule(workDir, runnerCfg, processConfigurationProvider));
        l.add(tasks);
        l.add(new TaskCallInterceptorModule());
        l.add(new SpaceModule(new URLClassSpace(parentClassLoader)));
        l.add(new SpaceModule(new URLClassSpace(dependenciesClassLoader)));
        if (modules != null) {
            l.addAll(Arrays.asList(modules));
        }

        Module m = new WireModule(l);

        return Guice.createInjector(m);
    }

    private URLClassLoader loadDependencies(Collection<String> dependencies) throws IOException {
        List<URL> urls = toURLs(dependencies);
        return new URLClassLoader(urls.toArray(new URL[0]), InjectorFactory.class.getClassLoader());
    }

    private List<URL> toURLs(Collection<String> dependencies) throws IOException {
        List<URL> urls = dependencies.stream()
                .sorted()
                .map(s -> {
                    try {
                        // assume all dependencies are resolved into file paths at this point
                        return new URL("file://" + s);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException("Invalid dependency " + s + ": " + e.getMessage());
                    }
                }).collect(Collectors.toList());

        // collect all JARs from the `${workDir}/lib/` directory

        Path lib = workDir.getValue().resolve(Constants.Files.LIBRARIES_DIR_NAME);
        if (Files.exists(lib)) {
            try (Stream<Path> s = Files.list(lib).sorted()) {
                s.forEach(f -> {
                    if (f.toString().endsWith(".jar")) {
                        try {
                            urls.add(f.toUri().toURL());
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        }

        return urls;
    }

    private static class TaskClassesListener<T> implements TypeListener {

        private final TaskHolder<T> holder;

        public TaskClassesListener(TaskHolder<T> holder) {
            this.holder = holder;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
            Class<T> klass = (Class<T>) typeLiteral.getRawType();

            Named n = klass.getAnnotation(Named.class);
            if (n == null) {
                log.warn("Task class without @Named: {}", klass);
                return;
            }

            String key = n.value();
            if ("".equals(key)) {
                log.warn("Task class without a @Named value: {}", klass);
                return;
            }

            holder.add(key, klass);
        }
    }

    private static class SubClassesOf extends AbstractMatcher<TypeLiteral<?>> {

        private final Class<?> baseClass;

        private SubClassesOf(Class<?> baseClass) {
            this.baseClass = baseClass;
        }

        @Override
        public boolean matches(TypeLiteral<?> t) {
            return baseClass.isAssignableFrom(t.getRawType());
        }
    }

    private static class ConfigurationModule extends AbstractModule {

        private final WorkingDirectory workDir;
        private final RunnerConfiguration runnerCfg;
        private final Provider<ProcessConfiguration> processCfgProvider;

        private ConfigurationModule(WorkingDirectory workDir,
                                    RunnerConfiguration runnerCfg,
                                    Provider<ProcessConfiguration> processCfgProvider) {

            this.workDir = workDir;
            this.runnerCfg = runnerCfg;
            this.processCfgProvider = processCfgProvider;
        }

        @Override
        protected void configure() {
            bind(WorkingDirectory.class).toInstance(workDir);
            bind(RunnerConfiguration.class).toInstance(runnerCfg);
            bind(ProcessConfiguration.class).toProvider(processCfgProvider);
            bind(InstanceId.class).toProvider(InstanceIdProvider.class);
        }
    }

    private static class InstanceIdProvider implements Provider<InstanceId> {

        private final UUID value;

        @Inject
        private InstanceIdProvider(ProcessConfiguration cfg) {
            this.value = cfg.instanceId();
        }

        @Override
        public InstanceId get() {
            return new InstanceId(value);
        }
    }
}
