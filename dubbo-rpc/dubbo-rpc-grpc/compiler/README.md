# Dubbo customized version

## 修改内容

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

## 如何构建

To compile the plugin:
```
$ ../gradlew java_pluginExecutable
```

To publish to local repository
```
$ ../gradlew publishToMavenLocal
```

## 远程发布

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


gRPC Java Codegen Plugin for Protobuf Compiler
==============================================

This generates the Java interfaces out of the service definition from a
`.proto` file. It works with the Protobuf Compiler (``protoc``).

Normally you don't need to compile the codegen by yourself, since pre-compiled
binaries for common platforms are available on Maven Central. However, if the
pre-compiled binaries are not compatible with your system, you may want to
build your own codegen.

## System requirement

* Linux, Mac OS X with Clang, or Windows with MSYS2
* Java 7 or up
* [Protobuf](https://github.com/google/protobuf) 3.0.0-beta-3 or up

## Compiling and testing the codegen
Change to the `compiler` directory:
```
$ cd $GRPC_JAVA_ROOT/compiler
```

To compile the plugin:
```
$ ../gradlew java_pluginExecutable
```

To test the plugin with the compiler:
```
$ ../gradlew test
```
You will see a `PASS` if the test succeeds.

To compile a proto file and generate Java interfaces out of the service definitions:
```
$ protoc --plugin=protoc-gen-grpc-java=build/exe/java_plugin/protoc-gen-grpc-java \
  --grpc-java_out="$OUTPUT_FILE" --proto_path="$DIR_OF_PROTO_FILE" "$PROTO_FILE"
```
To generate Java interfaces with protobuf lite:
```
$ protoc --plugin=protoc-gen-grpc-java=build/exe/java_plugin/protoc-gen-grpc-java \
  --grpc-java_out=lite:"$OUTPUT_FILE" --proto_path="$DIR_OF_PROTO_FILE" "$PROTO_FILE"
```
To generate Java interfaces with protobuf nano:
```
$ protoc --plugin=protoc-gen-grpc-java=build/exe/java_plugin/protoc-gen-grpc-java \
  --grpc-java_out=nano:"$OUTPUT_FILE" --proto_path="$DIR_OF_PROTO_FILE" "$PROTO_FILE"
```

## Installing the codegen to Maven local repository
This will compile a codegen and put it under your ``~/.m2/repository``. This
will make it available to any build tool that pulls codegens from Maven
repostiories.
```
$ ../gradlew publishToMavenLocal
```

## Creating a release of GRPC Java
Please follow the instructions in ``RELEASING.md`` under the root directory for
details on how to create a new release.
