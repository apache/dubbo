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
package org.apache.dubbo.remoting.http12.message;

import java.time.Duration;

/**
 * Represents a Server-Sent Event according to the HTML specification.
 * <p>
 * Server-Sent Events (SSE) is a server push technology enabling a client to receive automatic updates from a server via HTTP connection.
 * The server can send new data to the client at any time by pushing messages, without the need to reestablish the connection.
 * <p>
 * This class encapsulates the structure of a Server-Sent Event, which may include:
 * <ul>
 *   <li>An event ID</li>
 *   <li>An event type</li>
 *   <li>A retry interval</li>
 *   <li>A comment</li>
 *   <li>Data payload</li>
 * </ul>
 * <p>
 * Use the {@link #builder()} method to create instances of this class.
 *
 * @param <T> the type of data that this event contains
 * @see <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Events</a>
 */
public final class ServerSentEvent<T> {

    /**
     * The event ID that can be used for tracking or resuming event streams.
     */
    private final String id;

    /**
     * The event type or name that identifies the type of event.
     */
    private final String event;

    /**
     * The reconnection time in milliseconds that the client should wait before reconnecting
     * after a connection is closed.
     */
    private final Duration retry;

    /**
     * A comment that will be ignored by event-processing clients but can be useful for debugging.
     */
    private final String comment;

    /**
     * The data payload of this event.
     */
    private final T data;

    /**
     * Constructs a new ServerSentEvent with the specified properties.
     * <p>
     * It's recommended to use the {@link #builder()} method instead of this constructor directly.
     *
     * @param id      the event ID, can be null
     * @param event   the event type, can be null
     * @param retry   the reconnection time, can be null
     * @param comment the comment, can be null
     * @param data    the data payload, can be null
     */
    public ServerSentEvent(String id, String event, Duration retry, String comment, T data) {
        this.id = id;
        this.event = event;
        this.retry = retry;
        this.comment = comment;
        this.data = data;
    }

    /**
     * Returns the event ID.
     *
     * @return the event ID, may be null
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the event type.
     *
     * @return the event type, may be null
     */
    public String getEvent() {
        return event;
    }

    /**
     * Returns the reconnection time that clients should wait before reconnecting.
     *
     * @return the reconnection time as a Duration, may be null
     */
    public Duration getRetry() {
        return retry;
    }

    /**
     * Returns the comment associated with this event.
     *
     * @return the comment, may be null
     */
    public String getComment() {
        return comment;
    }

    /**
     * Returns the data payload of this event.
     *
     * @return the data payload, may be null
     */
    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "ServerSentEvent{id='" + id + '\'' + ", event='" + event + '\'' + ", retry=" + retry + ", comment='"
                + comment + '\'' + ", data=" + data + '}';
    }

    /**
     * Creates a new {@link Builder} instance.
     *
     * @param <T> the type of data that the event will contain
     * @return a new builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Builder for {@link ServerSentEvent}.
     *
     * @param <T> the type of data that the event will contain
     */
    public static final class Builder<T> {
        private String id;
        private String event;
        private Duration retry;
        private String comment;
        private T data;

        private Builder() {}

        /**
         * Sets the id of the event.
         *
         * @param id the id
         * @return this builder
         */
        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the event type.
         *
         * @param event the event type
         * @return this builder
         */
        public Builder<T> event(String event) {
            this.event = event;
            return this;
        }

        /**
         * Sets the retry duration.
         *
         * @param retry the retry duration
         * @return this builder
         */
        public Builder<T> retry(Duration retry) {
            this.retry = retry;
            return this;
        }

        /**
         * Sets the comment.
         *
         * @param comment the comment
         * @return this builder
         */
        public Builder<T> comment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Sets the data.
         *
         * @param data the data
         * @return this builder
         */
        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        /**
         * Builds a new {@link ServerSentEvent} with the configured properties.
         *
         * @return the built event
         */
        public ServerSentEvent<T> build() {
            return new ServerSentEvent<>(id, event, retry, comment, data);
        }
    }
}
