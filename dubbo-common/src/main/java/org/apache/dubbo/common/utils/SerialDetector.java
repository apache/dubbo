/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.io.ObjectInputStream;

/**
 *
 */
public class SerialDetector extends ObjectInputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerialDetector.class);

    private static final Map<String, Configuration> configs = new ConcurrentHashMap<>();

    private final Configuration config;
    private final boolean profiling;

    /**
     * SerialKiller constructor, returns instance of ObjectInputStream.
     *
     * @param inputStream The original InputStream, used by your service to receive serialized objects
     * @param configFile  The location of the config file (absolute path)
     * @throws java.io.IOException   File I/O exception
     * @throws IllegalStateException Invalid configuration exception
     */
    public SerialKiller(final InputStream inputStream, final String configFile) throws IOException {
        super(inputStream);

        config = configs.computeIfAbsent(configFile, Configuration::new);
        profiling = config.isProfiling();
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass serialInput) throws IOException, ClassNotFoundException {
        config.reloadIfNeeded();

        // Enforce SerialKiller's blacklist
        for (Pattern blackPattern : config.blacklist()) {
            Matcher blackMatcher = blackPattern.matcher(serialInput.getName());

            if (blackMatcher.find()) {
                if (profiling) {
                    // Reporting mode
                    LOGGER.info(String.format("Blacklist match: '%s'", serialInput.getName()));
                } else {
                    // Blocking mode
                    LOGGER.error(String.format("Blocked by blacklist '%s'. Match found for '%s'", new Object[]{blackPattern.pattern(), serialInput.getName()}));
                    throw new InvalidClassException(serialInput.getName(), "Class blocked from deserialization (blacklist)");
                }
            }
        }

        // Enforce SerialKiller's whitelist
        boolean safeClass = false;

        for (Pattern whitePattern : config.whitelist()) {
            Matcher whiteMatcher = whitePattern.matcher(serialInput.getName());

            if (whiteMatcher.find()) {
                safeClass = true;

                if (profiling) {
                    // Reporting mode
                    LOGGER.info(String.format("Whitelist match: '%s'", serialInput.getName()));
                }

                // We have found a whitelist match, no need to continue
                break;
            }
        }

        if (!safeClass && !profiling) {
            // Blocking mode
            LOGGER.error(String.format("Blocked by whitelist. No match found for '%s'", serialInput.getName()));
            throw new InvalidClassException(serialInput.getName(), "Class blocked from deserialization (non-whitelist)");
        }

        return super.resolveClass(serialInput);
    }

    static final class Configuration {
        private final XMLConfiguration config;

        private PatternList blacklist;
        private PatternList whitelist;

        Configuration(final String configPath) {
            try {
                config = new XMLConfiguration(configPath);

                FileChangedReloadingStrategy reloadStrategy = new FileChangedReloadingStrategy();
                reloadStrategy.setRefreshDelay(config.getLong("refresh", 6000));
                config.setReloadingStrategy(reloadStrategy);
                config.addConfigurationListener(event -> init(config));

                init(config);
            } catch (ConfigurationException | PatternSyntaxException e) {
                throw new IllegalStateException("SerialKiller not properly configured: " + e.getMessage(), e);
            }
        }

        private void init(final XMLConfiguration config) {
            blacklist = new PatternList(config.getStringArray("blacklist.regexps.regexp"));
            whitelist = new PatternList(config.getStringArray("whitelist.regexps.regexp"));
        }

        void reloadIfNeeded() {
            // NOTE: Unfortunately, this will invoke synchronized blocks in Commons Configuration
            config.reload();
        }

        Iterable<Pattern> blacklist() {
            return blacklist;
        }

        Iterable<Pattern> whitelist() {
            return whitelist;
        }

        boolean isProfiling() {
            return config.getBoolean("mode.profiling", false);
        }
    }

    static final class PatternList implements Iterable<Pattern> {
        private final Pattern[] patterns;

        PatternList(final String... regExps) {

            requireNonNull(regExps, "regExps");

            this.patterns = new Pattern[regExps.length];
            for (int i = 0; i < regExps.length; i++) {
                patterns[i] = Pattern.compile(regExps[i]);
            }
        }

        @Override
        public Iterator<Pattern> iterator() {
            return new Iterator<Pattern>() {
                int index = 0;

                @Override
                public boolean hasNext() {
                    return index < patterns.length;
                }

                @Override
                public Pattern next() {
                    return patterns[index++];
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        }

        @Override
        public String toString() {
            return Arrays.toString(patterns);
        }

    }
}