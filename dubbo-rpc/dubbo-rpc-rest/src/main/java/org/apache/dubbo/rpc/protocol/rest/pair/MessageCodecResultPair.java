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
package org.apache.dubbo.rpc.protocol.rest.pair;

import org.apache.dubbo.metadata.rest.media.MediaType;

/**
 *  for http message codec result
 */
public class MessageCodecResultPair {
    /**
     *  has coded
     */
    boolean coded;

    /**
     *  codec type
     */
    MediaType mediaType;


    public MessageCodecResultPair(boolean coded, MediaType mediaType) {
        this.coded = coded;
        this.mediaType = mediaType;
    }


    public boolean isCoded() {
        return coded;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public static MessageCodecResultPair pair(boolean coded, MediaType mediaType) {
        return new MessageCodecResultPair(coded, mediaType);
    }
}
