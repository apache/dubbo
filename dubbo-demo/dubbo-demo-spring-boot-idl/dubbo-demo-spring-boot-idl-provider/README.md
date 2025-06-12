## 1. startup

1. run the maven plugin `dubbo:compile`, generate protobuf java file.
2. run `ProviderApplication`

## 2. http request

### 2.1 sample request

```shell
curl -v -d '{"name":"dubbo"}' -H 'Content-Type: application/json' http://127.0.0.1:50051/org.apache.dubbo.demo.hello.GreeterService/sayHello
```

### 2.2 request async

```shell
curl -v -d '{"name":"dubbo async"}' -H 'Content-Type: application/json' http://127.0.0.1:50051/org.apache.dubbo.demo.hello.GreeterService/sayHelloAsync
```

### 2.3 server stream

```shell
curl -v -d '{"name":"dubbo"}' -H 'Content-Type: application/json' http://127.0.0.1:50051/org.apache.dubbo.demo.hello.GreeterService/sayHelloStream
```
