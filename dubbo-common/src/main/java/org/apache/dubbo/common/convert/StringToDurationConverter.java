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
package org.apache.dubbo.common.convert;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.StringUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;

public class StringToDurationConverter implements StringConverter<Duration> {

    @Override
    public Duration convert(String source) {
        return isNotEmpty(source) ? DurationStyle.detectAndParse(source) : null;
    }

    @Override
    public int getPriority() {
        return NORMAL_PRIORITY + 10;
    }

    /**
     * @author Phillip Webb
     * @author Valentine Wu
     * @link {org.springframework.boot.convert.DurationStyle}
     */
    enum DurationStyle {

        /**
         * Simple formatting, for example '1s'.
         */
        SIMPLE("^([+-]?\\d+)([a-zA-Z]{0,2})$") {
            @Override
            public Duration parse(String value, ChronoUnit unit) {
                try {
                    Matcher matcher = matcher(value);
                    Assert.assertTrue(matcher.matches(), "Does not match simple duration pattern");
                    String suffix = matcher.group(2);
                    return (StringUtils.isNotBlank(suffix)
                                    ? TimeUnit.fromSuffix(suffix)
                                    : TimeUnit.fromChronoUnit(unit))
                            .parse(matcher.group(1));
                } catch (Exception ex) {
                    throw new IllegalArgumentException("'" + value + "' is not a valid simple duration", ex);
                }
            }
        },

        /**
         * ISO-8601 formatting.
         */
        ISO8601("^[+-]?[pP].*$") {
            @Override
            public Duration parse(String value, ChronoUnit unit) {
                try {
                    return Duration.parse(value);
                } catch (Exception ex) {
                    throw new IllegalArgumentException("'" + value + "' is not a valid ISO-8601 duration", ex);
                }
            }
        };

        private final Pattern pattern;

        DurationStyle(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }

        protected final boolean matches(String value) {
            return this.pattern.matcher(value).matches();
        }

        protected final Matcher matcher(String value) {
            return this.pattern.matcher(value);
        }

        /**
         * Parse the given value to a duration.
         *
         * @param value the value to parse
         * @return a duration
         */
        public Duration parse(String value) {
            return parse(value, null);
        }

        /**
         * Parse the given value to a duration.
         *
         * @param value the value to parse
         * @param unit  the duration unit to use if the value doesn't specify one ({@code null}
         *              will default to ms)
         * @return a duration
         */
        public abstract Duration parse(String value, ChronoUnit unit);

        /**
         * Detect the style then parse the value to return a duration.
         *
         * @param value the value to parse
         * @return the parsed duration
         * @throws IllegalArgumentException if the value is not a known style or cannot be
         *                                  parsed
         */
        public static Duration detectAndParse(String value) {
            return detectAndParse(value, null);
        }

        /**
         * Detect the style then parse the value to return a duration.
         *
         * @param value the value to parse
         * @param unit  the duration unit to use if the value doesn't specify one ({@code null}
         *              will default to ms)
         * @return the parsed duration
         * @throws IllegalArgumentException if the value is not a known style or cannot be
         *                                  parsed
         */
        public static Duration detectAndParse(String value, ChronoUnit unit) {
            return detect(value).parse(value, unit);
        }

        /**
         * Detect the style from the given source value.
         *
         * @param value the source value
         * @return the duration style
         * @throws IllegalArgumentException if the value is not a known style
         */
        public static DurationStyle detect(String value) {
            Assert.notNull(value, "Value must not be null");
            for (DurationStyle candidate : values()) {
                if (candidate.matches(value)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("'" + value + "' is not a valid duration");
        }

        /**
         * Time Unit that support.
         */
        enum TimeUnit {

            /**
             * Nanoseconds.
             */
            NANOS(ChronoUnit.NANOS, "ns", Duration::toNanos),

            /**
             * Microseconds.
             */
            MICROS(ChronoUnit.MICROS, "us", (duration) -> duration.toNanos() / 1000L),

            /**
             * Milliseconds.
             */
            MILLIS(ChronoUnit.MILLIS, "ms", Duration::toMillis),

            /**
             * Seconds.
             */
            SECONDS(ChronoUnit.SECONDS, "s", Duration::getSeconds),

            /**
             * Minutes.
             */
            MINUTES(ChronoUnit.MINUTES, "m", Duration::toMinutes),

            /**
             * Hours.
             */
            HOURS(ChronoUnit.HOURS, "h", Duration::toHours),

            /**
             * Days.
             */
            DAYS(ChronoUnit.DAYS, "d", Duration::toDays);

            private final ChronoUnit chronoUnit;

            private final String suffix;

            private final Function<Duration, Long> longValue;

            TimeUnit(ChronoUnit chronoUnit, String suffix, Function<Duration, Long> toUnit) {
                this.chronoUnit = chronoUnit;
                this.suffix = suffix;
                this.longValue = toUnit;
            }

            public Duration parse(String value) {
                return Duration.of(Long.parseLong(value), this.chronoUnit);
            }

            public long longValue(Duration value) {
                return this.longValue.apply(value);
            }

            public static TimeUnit fromChronoUnit(ChronoUnit chronoUnit) {
                if (chronoUnit == null) {
                    return TimeUnit.MILLIS;
                }
                for (TimeUnit candidate : values()) {
                    if (candidate.chronoUnit == chronoUnit) {
                        return candidate;
                    }
                }
                throw new IllegalArgumentException("Unknown unit " + chronoUnit);
            }

            public static TimeUnit fromSuffix(String suffix) {
                for (TimeUnit candidate : values()) {
                    if (candidate.suffix.equalsIgnoreCase(suffix)) {
                        return candidate;
                    }
                }
                throw new IllegalArgumentException("Unknown unit '" + suffix + "'");
            }
        }
    }
}
