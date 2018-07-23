### dubbo-compatible

Hi, all

From 2.7.x, `Dubbo` has renamed package to `org.apache.dubbo`, so `dubbo-compatible` module is provided.

For compatibility with older versions, we provider the following most popular APIs(classes/interfaces):

* com.alibaba.dubbo.rpc.Filter / Invocation / Invoker / Result / RpcContext / RpcException
* com.alibaba.dubbo.config.annotation.Reference / Service
* com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo
* com.alibaba.dubbo.common.Constants / URL
* com.alibaba.dubbo.common.extension.ExtensionFactory
* com.alibaba.dubbo.common.serialize.Serialization / ObjectInput / ObjectOutput
* com.alibaba.dubbo.cache.CacheFactory / Cache
* com.alibaba.dubbo.rpc.service.EchoService / GenericService

The above APIs work fine with some unit tests in the test root. 

Except these APIs, others provided in `dubbo-compatible` are just bridge APIs without any unit tests, they may work with wrong. If you have any demand for them, you could: 

* Implement your own extensions with new APIs. (RECOMMENDED) 
* Follow `com.alibaba.dubbo.rpc.Filter` to implement bridge APIs, and then contribute to community. 
* Open issue on github.

By the way, We will remove this module some day, so it's recommended that implementing your extensions with new APIs at the right time. 

Now we need your help: Any other popular APIs are missing?

For compatible module, any suggestions are welcome. Thanks.