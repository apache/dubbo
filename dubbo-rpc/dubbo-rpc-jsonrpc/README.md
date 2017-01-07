
引用自江苏千米科技
[![Build Status](https://travis-ci.org/QianmiOpen/dubbo-rpc-jsonrpc.svg)](https://travis-ci.org/QianmiOpen/dubbo-rpc-jsonrpc)



## Why HTTP
在互联网快速迭代的大潮下，越来越多的公司选择nodejs、django、rails这样的快速脚本框架来开发web端应用
而后端的服务用Java又是最合适的，这就产生了大量的跨语言的调用需求。  
而http、json是天然合适作为跨语言的标准，各种语言都有成熟的类库    
虽然Dubbo的异步长连接协议效率很高，但是在脚本语言中，这点效率的损失并不重要。  


## Why Not RESTful
Dubbox 在RESTful接口上已经做出了尝试，但是REST架构和dubbo原有的RPC架构是有区别的，  
区别在于REST架构需要有资源(Resources)的定义，
需要用到HTTP协议的基本操作GET、POST、PUT、DELETE对资源进行操作。  
Dubbox需要重新定义接口的属性，这对原有的Dubbo接口迁移是一个较大的负担。  
相比之下，RESTful更合适互联网系统之间的调用，而RPC更合适一个系统内的调用，  
所以我们使用了和Dubbo理念较为一致的JsonRPC


dubbo-rpc-jsonrpc
=====================


## 配置：
Define jsonrpc protocol:
```xml
 <dubbo:protocol name="jsonrpc" port="8080" server="jetty" />
```

Set default protocol:
```xml
<dubbo:provider protocol="jsonrpc" />
```

Set service protocol:
```xml
<dubbo:service protocol="jsonrpc" />
```

Multi port:
```xml
<dubbo:protocol id="jsonrpc1" name="jsonrpc" port="8080" />
<dubbo:protocol id="jsonrpc2" name="jsonrpc" port="8081" />
```
Multi protocol:
```xml
<dubbo:protocol name="dubbo" port="20880" />
<dubbo:protocol name="jsonrpc" port="8080" />
```
<!-- 使用多个协议暴露服务 -->
```xml
<dubbo:service id="helloService" interface="com.alibaba.hello.api.HelloService" version="1.0.0" protocol="dubbo,jsonrpc" />
```


Jetty Server: (default)
```xml
<dubbo:protocol ... server="jetty" />

或jetty的最新版：
<dubbo:protocol ... server="jetty9" />

```
Maven:
```xml
<dependency>
  <groupId>org.mortbay.jetty</groupId>
  <artifactId>jetty</artifactId>
  <version>6.1.26</version>
</dependency>
```

Servlet Bridge Server: (recommend)
```xml
<dubbo:protocol ... server="servlet" />

```

web.xml：
```xml
<servlet>
         <servlet-name>dubbo</servlet-name>
         <servlet-class>com.alibaba.dubbo.remoting.http.servlet.DispatcherServlet</servlet-class>
         <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
         <servlet-name>dubbo</servlet-name>
         <url-pattern>/*</url-pattern>
</servlet-mapping>
```
注意，如果使用servlet派发请求：

协议的端口```<dubbo:protocol port="8080" />```必须与servlet容器的端口相同，
协议的上下文路径```<dubbo:protocol contextpath="foo" />```必须与servlet应用的上下文路径相同。

--------------
## Example

JAVA API
```java
public interface PhoneNoCheckProvider {
    /**
     * 校验号码是否受限
     * @param operators 运营商
     * @param no 号码
     * @param userid 用户编号
     * */
    boolean isPhoneNoLimit(Operators operators, String no, String userid);
}
```
Client
```shell
curl -i -H 'content-type: application/json' -X POST -d '{"jsonrpc": "2.0", "method": "isPhoneNoLimit", "params": [ "MOBILE", "130000", "A001"],
         "id": 1 }' 'http://127.0.0.1:18080/com.ofpay.api.PhoneNoCheckProvider'
```

Python Client Example
```python
import httplib
import json

__author__ = 'caozupeng'


def raw_client(app_params):
    headers = {"Content-type": "application/json-rpc",
               "Accept": "text/json"}
    h1 = httplib.HTTPConnection('172.19.32.135', port=18080)
    h1.request("POST", '/com.ofpay.ofdc.api.phone.PhoneNoCheckProvider', json.dumps(app_params), headers)
    response = h1.getresponse()
    return response.read()


if __name__ == '__main__':
    app_params = {
        "jsonrpc": "2.0",
        "method": "isPhoneNoLimit",
        "params": ["MOBILE", "130000", "A001"],
        "id": 1
    }
    print json.loads(raw_client(app_params), encoding='utf-8')
```

## Python客户端
https://github.com/ofpay/dubbo-client-py

## Nodejs客户端
https://github.com/ofpay/dubbo-node-client

## 客户端服务端Example  
https://github.com/JoeCao/dubbo_jsonrpc_example  
使用docker运行


## 文档资料

[JSON-RPC 2.0 规范](http://www.jsonrpc.org/specification) 
 
[jsonrpc4j](https://github.com/briandilley/jsonrpc4j) 
 
[dubbo procotol](http://www.dubbo.io/Protocol+Reference-zh.htm) 
