This example demonstrates how to use IDL as a language independent way to define Dubbo service, meanwhile, the RPC protocol used is Triple - an HTTP/2 based protocol fully compatible with gRPC.

More details can be found here,
* [Triple](https://dubbo.apache.org/zh/docs3-v2/java-sdk/reference-manual/protocol/triple/)
* [IDL](https://dubbo.apache.org/zh/docs3-v2/java-sdk/quick-start/idl/)

## How To Run
1. Run `mvn clean compile` first to generate stub files from `helloworld.proto`.
2. Then you can Run `Provider` and `Consumer` in turn like other normal Dubbo demos.
