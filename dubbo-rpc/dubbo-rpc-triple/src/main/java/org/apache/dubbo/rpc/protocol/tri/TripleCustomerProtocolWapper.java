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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.CollectionUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TripleCustomerProtocolWapper {

    static int makeTag(int fieldNumber, int wireType) {
        return fieldNumber << 3 | wireType;
    }

    public static byte[] varIntEncode(int val) {
        byte[] data = new byte[varIntComputeLength(val)];
        for (int i = 0; i < data.length - 1; i++) {
            data[i] = (byte) ((val & 0x7F) | 0x80);
            val = val >>> 7;
        }
        data[data.length - 1] = (byte) (val);
        return data;
    }

    public static int varIntComputeLength(int val) {
        if (val == 0) {
            return 1;
        }
        int length = 0;
        while (val != 0) {
            val = val >>> 7;
            length++;
        }
        return length;
    }


    public static int readRawVarint32(ByteBuffer byteBuffer) {
        int val = 0;
        int currentPosition = byteBuffer.position();
        int varIntLength = 1;
        byte currentByte = byteBuffer.get();
        while ((currentByte & 0XF0) >> 7 == 1) {
            varIntLength++;
            currentByte = byteBuffer.get();
        }

        for (int index = currentPosition + varIntLength - 1; index >= currentPosition; index--) {
            val = val << 7;
            val = val | (byteBuffer.get(index) & 0x7F);
        }
        byteBuffer.position(currentPosition + varIntLength);
        return val;
    }

    public static int extractFieldNumFromTag(int tag) {
        return tag >> 3;
    }

    public static int extractWireTypeFromTag(int tag) {
        return tag & 0X07;
    }

    public static final class TripleResponseWrapper {
        private String serializeType;

        private byte[] data;

        private String type;

        public String getSerializeType() {
            return serializeType;
        }

        public byte[] getData() {
            return data;
        }

        public String getType() {
            return type;
        }

        public static TripleResponseWrapper parseFrom(byte[] data) {
            TripleResponseWrapper tripleResponseWrapper = new TripleResponseWrapper();
            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            while (byteBuffer.position() < byteBuffer.limit()) {
                int tag = readRawVarint32(byteBuffer);
                int fieldNum = extractFieldNumFromTag(tag);
                int wireType = extractWireTypeFromTag(tag);
                if (wireType != 2) {
                    throw new RuntimeException(String.format("unexpect wireType, expect %d realType %d", 2, wireType));
                }
                if (fieldNum == 1) {
                    int serializeTypeLength = readRawVarint32(byteBuffer);
                    byte[] serializeTypeBytes = new byte[serializeTypeLength];
                    byteBuffer.get(serializeTypeBytes, 0, serializeTypeLength);
                    tripleResponseWrapper.serializeType = new String(serializeTypeBytes);
                } else if (fieldNum == 2) {
                    int dataLength = readRawVarint32(byteBuffer);
                    byte[] dataBytes = new byte[dataLength];
                    byteBuffer.get(dataBytes, 0, dataLength);
                    tripleResponseWrapper.data = dataBytes;
                } else if (fieldNum == 3) {
                    int typeLength = readRawVarint32(byteBuffer);
                    byte[] typeBytes = new byte[typeLength];
                    byteBuffer.get(typeBytes, 0, typeLength);
                    tripleResponseWrapper.type = new String(typeBytes);
                } else {
                    throw new RuntimeException("fieldNum should in (1,2,3)");
                }
            }
            return tripleResponseWrapper;
        }

        public byte[] toByteArray() {
            int totalSize = 0;

            int serializeTypeTag = makeTag(1, 2);
            byte[] serializeTypeTagBytes = varIntEncode(serializeTypeTag);
            byte[] serializeTypeBytes = serializeType.getBytes(StandardCharsets.UTF_8);
            byte[] serializeTypeLengthVarIntEncodeBytes = varIntEncode(serializeTypeBytes.length);
            totalSize += serializeTypeTagBytes.length
                + serializeTypeLengthVarIntEncodeBytes.length
                + serializeTypeBytes.length;

            int dataTag = makeTag(2, 2);
            if (data != null) {
                totalSize += varIntComputeLength(dataTag)
                    + varIntComputeLength(data.length)
                    + data.length;
            }

            int typeTag = makeTag(3, 2);
            byte[] typeTagBytes = varIntEncode(typeTag);
            byte[] typeBytes = type.getBytes(StandardCharsets.UTF_8);
            byte[] typeLengthVarIntEncodeBytes = varIntEncode(typeBytes.length);
            totalSize += typeTagBytes.length
                + typeLengthVarIntEncodeBytes.length
                + typeBytes.length;

            ByteBuffer byteBuffer = ByteBuffer.allocate(totalSize);
            byteBuffer
                .put(serializeTypeTagBytes)
                .put(serializeTypeLengthVarIntEncodeBytes)
                .put(serializeTypeBytes);
            if (data != null) {
                byteBuffer
                    .put(varIntEncode(dataTag))
                    .put(varIntEncode(data.length))
                    .put(data);
            }
            byteBuffer
                .put(typeTagBytes)
                .put(typeLengthVarIntEncodeBytes)
                .put(typeBytes);
            return byteBuffer.array();
        }

        public static final class Builder {
            private String serializeType;

            private byte[] data;

            private String type;

            public Builder setSerializeType(String serializeType) {
                this.serializeType = serializeType;
                return this;
            }

            public Builder setData(byte[] data) {
                this.data = data;
                return this;
            }

            public Builder setType(String type) {
                this.type = type;
                return this;
            }

            public static Builder newBuilder() {
                return new Builder();
            }

            public TripleResponseWrapper build() {
                Assert.notNull(serializeType, "serializeType can not be null");
                Assert.notNull(type, "type can not be null");
                TripleResponseWrapper tripleResponseWrapper = new TripleResponseWrapper();
                tripleResponseWrapper.data = this.data;
                tripleResponseWrapper.serializeType = this.serializeType;
                tripleResponseWrapper.type = this.type;
                return tripleResponseWrapper;
            }
        }
    }


    public static final class TripleRequestWrapper {

        private String serializeType;

        private List<byte[]> args;

        private List<String> argTypes;

        public String getSerializeType() {
            return serializeType;
        }

        public List<byte[]> getArgs() {
            return args;
        }

        public List<String> getArgTypes() {
            return argTypes;
        }

        public TripleRequestWrapper() {
        }

        public static TripleRequestWrapper parseFrom(byte[] data) {
            TripleRequestWrapper tripleRequestWrapper = new TripleRequestWrapper();
            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            tripleRequestWrapper.args = new ArrayList<>();
            tripleRequestWrapper.argTypes = new ArrayList<>();
            while (byteBuffer.position() < byteBuffer.limit()) {
                int tag = readRawVarint32(byteBuffer);
                int fieldNum = extractFieldNumFromTag(tag);
                int wireType = extractWireTypeFromTag(tag);
                if (wireType != 2) {
                    throw new RuntimeException(String.format("unexpect wireType, expect %d realType %d", 2, wireType));
                }
                if (fieldNum == 1) {
                    int serializeTypeLength = readRawVarint32(byteBuffer);
                    byte[] serializeTypeBytes = new byte[serializeTypeLength];
                    byteBuffer.get(serializeTypeBytes, 0, serializeTypeLength);
                    tripleRequestWrapper.serializeType = new String(serializeTypeBytes);
                } else if (fieldNum == 2) {
                    int argLength = readRawVarint32(byteBuffer);
                    byte[] argBytes = new byte[argLength];
                    byteBuffer.get(argBytes, 0, argLength);
                    tripleRequestWrapper.args.add(argBytes);
                } else if (fieldNum == 3) {
                    int argTypeLength = readRawVarint32(byteBuffer);
                    byte[] argTypeBytes = new byte[argTypeLength];
                    byteBuffer.get(argTypeBytes, 0, argTypeLength);
                    tripleRequestWrapper.argTypes.add(new String(argTypeBytes));
                } else {
                    throw new RuntimeException("fieldNum should in (1,2,3)");
                }
            }
            return tripleRequestWrapper;
        }

        public byte[] toByteArray() {

            int totalSize = 0;
            int serializeTypeTag = makeTag(1, 2);
            byte[] serializeTypeTagBytes = varIntEncode(serializeTypeTag);
            byte[] serializeTypeBytes = serializeType.getBytes(StandardCharsets.UTF_8);
            byte[] serializeTypeLengthVarIntEncodeBytes = varIntEncode(serializeTypeBytes.length);
            totalSize += serializeTypeTagBytes.length
                + serializeTypeLengthVarIntEncodeBytes.length
                + serializeTypeBytes.length;

            int argTypeTag = makeTag(3, 2);
            if (CollectionUtils.isNotEmpty(argTypes)) {
                totalSize += varIntComputeLength(argTypeTag) * argTypes.size();
                for (String argType : argTypes) {
                    byte[] argTypeBytes = argType.getBytes(StandardCharsets.UTF_8);
                    totalSize += argTypeBytes.length + varIntComputeLength(argTypeBytes.length);
                }
            }

            int argTag = makeTag(2, 2);
            if (CollectionUtils.isNotEmpty(args)) {
                totalSize += varIntComputeLength(argTag) * args.size();
                for (byte[] arg : args) {
                    totalSize += arg.length + varIntComputeLength(arg.length);
                }
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(totalSize);
            byteBuffer
                .put(serializeTypeTagBytes)
                .put(serializeTypeLengthVarIntEncodeBytes)
                .put(serializeTypeBytes);

            if (CollectionUtils.isNotEmpty(args)) {
                byte[] argTagBytes = varIntEncode(argTag);
                for (byte[] arg : args) {
                    byteBuffer
                        .put(argTagBytes)
                        .put(varIntEncode(arg.length))
                        .put(arg);
                }
            }

            if (CollectionUtils.isNotEmpty(argTypes)) {
                byte[] argTypeTagBytes = varIntEncode(argTypeTag);
                for (String argType : argTypes) {
                    byte[] argTypeBytes = argType.getBytes(StandardCharsets.UTF_8);
                    byteBuffer
                        .put(argTypeTagBytes)
                        .put(varIntEncode(argTypeBytes.length))
                        .put(argTypeBytes);
                }
            }
            return byteBuffer.array();
        }


        public static final class Builder {

            private String serializeType;

            private final List<byte[]> args = new ArrayList<>();

            private final List<String> argTypes = new ArrayList<>();

            public Builder setSerializeType(String serializeType) {
                this.serializeType = serializeType;
                return this;
            }

            public Builder addArgTypes(String argsType) {
                Assert.notEmptyString(argsType, "argsType cannot be empty.");
                argTypes.add(argsType);
                return this;
            }

            public Builder addArgs(byte[] arg) {
                args.add(arg);
                return this;
            }

            public static Builder newBuilder() {
                return new Builder();
            }

            public TripleRequestWrapper build() {
                Assert.notNull(serializeType, "serializeType can not be null");
                TripleRequestWrapper tripleRequestWrapper = new TripleRequestWrapper();
                tripleRequestWrapper.args = this.args;
                tripleRequestWrapper.argTypes = this.argTypes;
                tripleRequestWrapper.serializeType = this.serializeType;
                return tripleRequestWrapper;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TripleRequestWrapper)) {
                return false;
            }
            TripleRequestWrapper that = (TripleRequestWrapper) o;
            return Objects.equals(serializeType, that.serializeType)
                && Objects.equals(args, that.args)
                && Objects.equals(argTypes, that.argTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serializeType, args, argTypes);
        }
    }
}
