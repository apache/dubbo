## dubbo-complier

> dubbo-complier supports generating code based on .proto files

### How to use 

#### 1.Define Proto file

greeter.proto
```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "org.apache.dubbo.demo";
option java_outer_classname = "DemoServiceProto";
option objc_class_prefix = "DEMOSRV";

package demoservice;

// The demo service definition.
service DemoService {
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}

// The request message containing the user's name.
message HelloRequest {
  string name = 1;
}

// The response message containing the greetings
message HelloReply {
  string message = 1;
}

```

#### 2.Use dubbo-maven-plugin,rather than ```protobuf-maven-plugin```

    now dubbo support his own protoc plugin base on dubbo-maven-plugin

```xml
<plugin>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-maven-plugin</artifactId>
    <version>${dubbo.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### 3.generate file

```java
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

    package org.apache.dubbo.demo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DemoServiceDubbo {
private static final AtomicBoolean registered = new AtomicBoolean();

private static Class<?> init() {
Class<?> clazz = null;
try {
clazz = Class.forName(DemoServiceDubbo.class.getName());
if (registered.compareAndSet(false, true)) {
    org.apache.dubbo.common.serialize.protobuf.support.ProtobufUtils.marshaller(
    org.apache.dubbo.demo.HelloReply.getDefaultInstance());
    org.apache.dubbo.common.serialize.protobuf.support.ProtobufUtils.marshaller(
    org.apache.dubbo.demo.HelloRequest.getDefaultInstance());
}
} catch (ClassNotFoundException e) {
// ignore
}
return clazz;
}

private DemoServiceDubbo() {}

public static final String SERVICE_NAME = "org.apache.dubbo.demo.DemoService";

/**
* Code generated for Dubbo
*/
public interface IDemoService extends org.apache.dubbo.rpc.model.DubboStub {

static Class<?> clazz = init();

    org.apache.dubbo.demo.HelloReply sayHello(org.apache.dubbo.demo.HelloRequest request);

    CompletableFuture<org.apache.dubbo.demo.HelloReply> sayHelloAsync(org.apache.dubbo.demo.HelloRequest request);


}

}

```

#### 4.others

dubbo-maven-plugin protoc mojo supported configurations

| configuration params  | isRequired | explain                                        | default                                                    | eg                                                                         |
|:----------------------|------------|------------------------------------------------|------------------------------------------------------------|----------------------------------------------------------------------------|
| dubboVersion          | true       | dubbo version ,use for find Generator          | ${dubbo.version}                                           | 3.3.0                                                                      |
| dubboGenerateType     | true       | dubbo generator type                           | dubbo3                                                     | grpc                                                                       |
| protocExecutable      | false      | protoc executable,you can use local protoc.exe |                                                            | protoc                                                                     |
| protocArtifact        | false      | download protoc from maven artifact            |                                                            | com.google.protobuf:protoc:${protoc.version}:exe:${os.detected.classifier} |
| protoSourceDir        | true       | .proto files dir                               | ${basedir}/src/main/proto                                  | ./proto                                                                    |
| outputDir             | true       | generated file output dir                      | ${project.build.directory}/generated-sources/protobuf/java | ${basedir}/src/main/java                                                   |
| protocPluginDirectory | false      | protoc plugin dir                              | ${project.build.directory}/protoc-plugins                  | ./target/protoc-plugins                                                    |


​    
