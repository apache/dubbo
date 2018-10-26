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

package org.apache.dubbo.common.serialize.protostuff.delegate;

import io.protostuff.Input;
import io.protostuff.Output;
import io.protostuff.Pipe;
import io.protostuff.WireFormat;
import io.protostuff.runtime.Delegate;

import java.io.IOException;
import java.sql.Time;

public class TimeDelegate implements Delegate<Time> {
    @Override
    public WireFormat.FieldType getFieldType() {
        return WireFormat.FieldType.FIXED64;
    }

    @Override
    public Time readFrom(Input input) throws IOException {
        return new Time(input.readFixed64());
    }

    @Override
    public void writeTo(Output output, int number, Time time, boolean repeated) throws IOException {
        output.writeFixed64(number, time.getTime(), repeated);
    }

    @Override
    public void transfer(Pipe pipe, Input input, Output output, int number, boolean repeated) throws IOException {
        output.writeFixed64(number, input.readFixed64(), repeated);
    }

    @Override
    public Class<?> typeClass() {
        return Time.class;
    }
}
