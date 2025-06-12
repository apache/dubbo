## startup

1. run the maven plugin `dubbo:compile`, generate protobuf java file.
2. run `ProviderApplication`

## sample http request

```shell
curl -v -d '{"name":"dubbo"}' -H 'Content-Type: application/json' http://127.0.0.1:50051/org.apache.dubbo.demo.hello.GreeterService/sayHello
```

response:

```
{
  "message": "Hello dubbo"
}
```

## http2 stream request

### 1. sample request

```shell
curl -v -d '{"name":"dubbo X"}' -H 'Content-Type: application/json' http://127.0.0.1:50051/org.apache.dubbo.demo.stream.StreamService/sayHello
```

response

```
{
  "message": "dubbo X"
}
```

### 2. server stream response

```shell
curl -v -d '{"name":"dubbo X"}' -H 'Content-Type: application/json' http://127.0.0.1:50051/org.apache.dubbo.demo.stream.StreamService/sayServerStream
```

response, receive this one by one

```
data:{"message":"2025-06-12 15:28:49"}

data:{"message":"2025-06-12 15:28:51"}

data:{"message":"2025-06-12 15:28:53"}

data:{"message":"2025-06-12 15:28:55"}

data:{"message":"2025-06-12 15:28:57"}

data:{"message":"2025-06-12 15:28:59"}

data:{"message":"2025-06-12 15:29:01"}

data:{"message":"2025-06-12 15:29:03"}

data:{"message":"2025-06-12 15:29:05"}

data:{"message":"2025-06-12 15:29:07"}
```

### 3. client stream request

must exec with `--http2`

```shell
curl -v --http2 -d '{"name":"dubbo X"}' -H 'Content-Type: application/json' http://127.0.0.1:50051/org.apache.dubbo.demo.stream.StreamService/sayClientStream
```

response

```
data:{"message":"the full data: \ndata -\u003e dubbo X\n"}
```

### 4. client and server stream

must exec with `--http2`


```shell
curl -v --http2 -d '{"name":"dubbo X"}' -H 'Content-Type: application/json' http://127.0.0.1:50051/org.apache.dubbo.demo.stream.StreamService/sayBIStream
```

response

```
data:{"message":"receive: dubbo X"}
```
