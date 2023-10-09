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

#### 2.Configure the protoc plugin in your maven pom file

```xml
  <build>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
                <version>1.6.1</version>
            </extension>
        </extensions>
        <plugins>
            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <version>0.6.1</version>
                <configuration>
                    <protocArtifact>com.google.protobuf:protoc:${protoc.version}:exe:${os.detected.classifier}
                    </protocArtifact>
                    <protocPlugins>
                        <protocPlugin>
                            <id>dubbo</id>
                            <groupId>org.apache.dubbo</groupId>
                            <artifactId>dubbo-compiler</artifactId>
                            <version>3.2.2-SNAPSHOT</version>
                            <mainClass>org.apache.dubbo.gen.dubbo.DubboGenerator</mainClass>
                        </protocPlugin>
                    </protocPlugins>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>test-compile</goal>
                            <goal>compile-custom</goal>
                            <goal>test-compile-custom</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
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

#### 3.others

org.xolstice.maven.plugins provided very useful configurations

such as:
```xml
 <configuration>
    <protocArtifact>com.google.protobuf:protoc:${protoc.version}:exe:${os.detected.classifier}
    </protocArtifact>
    <!--whether if clear the directory before generating code each time !-->
    <clearOutputDirectory>false</clearOutputDirectory>
    <!--set outputDirectory -->
    <outputDirectory>./src/main/java</outputDirectory>
    <!--set protoSourceRoot -->
    <protoSourceRoot>${basedir}/proto</protoSourceRoot>
    <protocPlugins>
        <protocPlugin>
            <id>dubbo</id>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-compiler</artifactId>
            <version>3.2.2-SNAPSHOT</version>
            <mainClass>org.apache.dubbo.gen.dubbo.DubboGenerator</mainClass>
        </protocPlugin>
    </protocPlugins>
</configuration>
  
```
