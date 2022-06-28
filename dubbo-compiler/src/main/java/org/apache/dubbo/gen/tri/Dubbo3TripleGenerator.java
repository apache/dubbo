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
package org.apache.dubbo.gen.tri;

import org.apache.dubbo.gen.AbstractGenerator;

import com.salesforce.jprotoc.ProtocPlugin;

public class Dubbo3TripleGenerator extends AbstractGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            ProtocPlugin.generate(new Dubbo3TripleGenerator());
        } else {
            ProtocPlugin.debug(new Dubbo3TripleGenerator(), args[0]);
        }
    }

    @Override
    protected String getClassPrefix() {
        return "Dubbo";
    }

    @Override
    protected String getClassSuffix() {
        return "Triple";
    }

    @Override
    protected String getTemplateFileName() {
        return "Dubbo3TripleStub.mustache";
    }

    @Override
    protected String getInterfaceTemplateFileName() {
        return "Dubbo3TripleInterfaceStub.mustache";
    }


    @Override
    protected String getSingleTemplateFileName() {
        throw new IllegalStateException("Do not support single template!");
    }

    @Override
    protected boolean enableMultipleTemplateFiles() {
        return true;
    }
}
