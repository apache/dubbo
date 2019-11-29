# Dubbo customized version

## Get Started, how to use
1. Add maven dependency to your project
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
                <version>0.5.1</version>
                <configuration>
                    <protocArtifact>com.google.protobuf:protoc:3.7.1:exe:${os.detected.classifier}</protocArtifact>
                    <pluginId>grpc-java</pluginId>
                    <pluginArtifact>org.apache.dubbo:protoc-gen-dubbo-java:${proto_dubbo_plugin_version}:exe:${os.detected.classifier}</pluginArtifact>
                    <outputDirectory>build/generated/source/proto/main/java</outputDirectory>
                    <clearOutputDirectory>false</clearOutputDirectory>
                    <!-- supports 'dubbo' and 'grpc' -->
                    <pluginParameter>dubbo</pluginParameter>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>compile-custom</goal>
                            <goal>test-compile</goal>
                            <goal>test-compile-custom</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>add-source</goal>
                        </goals>
                        <configuration>
                            <sources>
                                <source>build/generated/source/proto/main/java</source>
                            </sources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

2. Decide which protocol to use: Dubbo or gRPC
   * Dubbo, ` <pluginParameter>dubbo</pluginParameter>`
   * gRPC, ` <pluginParameter>grpc</pluginParameter>`

3. Define service using IDL
```text
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

4. Build
mvn clean compile

## Customized

1. Dubbo Interface
```java
public interface IGreeter {

    default public io.grpc.examples.helloworld.HelloReply sayHello(io.grpc.examples.helloworld.HelloRequest request) {
       throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }
    
    default public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.helloworld.HelloReply> sayHelloAsync(
        io.grpc.examples.helloworld.HelloRequest request) {
       throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
    }
    
    public void sayHello(io.grpc.examples.helloworld.HelloRequest request,
        io.grpc.stub.StreamObserver<io.grpc.examples.helloworld.HelloReply> responseObserver);

}
```

2. Dubbo Stub
```java
public static DubboGreeterStub getDubboStub(io.grpc.Channel channel) {
  return new DubboGreeterStub(channel);
}

public static class DubboGreeterStub implements IGreeter {

    private GreeterBlockingStub blockingStub;
    private GreeterFutureStub futureStub;
    private GreeterStub stub;
    
    public DubboGreeterStub(io.grpc.Channel channel) {
       blockingStub = GreeterGrpc.newBlockingStub(channel);
       futureStub = GreeterGrpc.newFutureStub(channel);
       stub = GreeterGrpc.newStub(channel);
    }
    
    public io.grpc.examples.helloworld.HelloReply sayHello(io.grpc.examples.helloworld.HelloRequest request) {
        return blockingStub.sayHello(request);
    }
    
    public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.helloworld.HelloReply> sayHelloAsync(
        io.grpc.examples.helloworld.HelloRequest request) {
        return futureStub.sayHello(requesthttps://github.com/apache/dubbo-samples.git);
    }
    
    public void sayHello(io.grpc.examples.helloworld.HelloRequest request,
        io.grpc.stub.StreamObserver<io.grpc.examples.helloworld.HelloReply> responseObserver){
        stub.sayHello(request, responseObserver);
    }

}

```

3. XxxImplBase implements DubboInterface
```java
public static abstract class GreeterImplBase implements io.grpc.BindableService, IGreeter {

  @java.lang.Override
  public final io.grpc.examples.helloworld.HelloReply sayHello(io.grpc.examples.helloworld.HelloRequest request) {
     throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
  }

  @java.lang.Override
  public final com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.helloworld.HelloReply> sayHelloAsync(
      io.grpc.examples.helloworld.HelloRequest request) {
     throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
  }

  public void sayHello(io.grpc.examples.helloworld.HelloRequest request,
      io.grpc.stub.StreamObserver<io.grpc.examples.helloworld.HelloReply> responseObserver) {
      asyncUnimplementedUnaryCall(getSayHelloMethod(), responseObserver);
  }
  
  ...
}
```

## Build locally

To compile the plugin:
```
$ ./gradlew java_pluginExecutable
```

To publish to local repository
```
$ ./gradlew publishToMavenLocal
```

## Publish to maven repository

Add gradle.properties
```properties
repositoryUser=user
repositoryPasword=pwd
```

Then, run
```
$ ../gradlew publishMavenPublicationToDubboRepository
```
Notice current groupId is `com.alibaba`.


Check [here](https://github.com/grpc/grpc-java/blob/master/compiler/README.md) for basic requirements and usage of protoc plugin.
