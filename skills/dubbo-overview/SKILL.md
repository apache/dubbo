---
name: dubbo-overview
description: >
  Load when working with Apache Dubbo architecture, the Provider/Consumer/
  Registry topology, the control plane vs data plane split, the three-center
  setup (Registry Center, Config Center, Metadata Center), or
  application-level vs interface-level service discovery. Also load when
  generating any Dubbo 3 provider or consumer from scratch, configuring
  Nacos or Zookeeper as a registry, choosing between the tri and dubbo
  protocols, migrating from Dubbo 2 to Dubbo 3, or understanding why a
  Dubbo service cannot be discovered by consumers. Load this skill before
  any other dubbo-* skill.
license: Apache-2.0
---

<!---
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# What Dubbo is

Apache Dubbo is a Java RPC and microservice framework. Its architecture
divides into two layers:

**Data Plane** — Provider and Consumer JVM processes that communicate
directly via RPC protocols (Triple/HTTP2 with REST support, Dubbo/TCP).
Every remote call travels through the data plane.

**Control Plane** — Three optional centers that govern how data plane
processes discover each other, share configuration, and exchange metadata:
Registry Center, Config Center, and Metadata Center. The Dubbo Admin
console and service mesh components (Istio/xDS) also live here.

The four deployment topology nodes are: **Provider**, **Consumer**,
**Registry**, and **Monitor**. None of the three centers is mandatory in
every deployment — the minimum viable Dubbo setup is one Provider and one
Consumer with a direct connection and no registry at all.


# How Dubbo works — the three-center architecture

## Registry Center (service discovery)

Handles registration and discovery of service instances.

Supported registries: Nacos, Zookeeper, Multicast (dev only).

**Interface-level discovery (Dubbo 2 default)**
One registry entry per interface per instance.
100 interfaces × 100 instances = 10,000 registry entries.
Consumers subscribe per interface.

**Application-level discovery (Dubbo 3 default)**
One registry entry per application instance regardless of interfaces.
100 interfaces × 100 instances = 100 registry entries.
Consumers subscribe per application, then resolve interfaces via
the Metadata Center.

The scaling improvement (O(interfaces × instances) → O(instances))
is why Dubbo 3 changed the default.

## Config Center (dynamic configuration)

Stores configuration that is pushed to all instances at runtime without
restart. Supported: Nacos, Apollo, Zookeeper.

Config Center handles: timeout overrides, router rules, tag rules,
condition rules, mock rules. It is a separate concern from the Registry.
Do not confuse them: Registry handles addresses, Config handles behavior.

## Metadata Center (service interface metadata)

Stores the detailed interface metadata (methods, parameters, return types)
that application-level service discovery no longer puts in the registry.
Consumers retrieve this to know exactly what methods a provider exposes.

Supported: Nacos, Zookeeper. Optional if using interface-level
discovery. **Required for any Dubbo 2 to Dubbo 3 migration**, because
without it, consumers cannot resolve the interface-to-application mapping.


# How to use it — writing a correct Dubbo 3 provider and consumer

## Step 1: Add the dependency (Spring Boot)

```xml
<dependency>
  <groupId>org.apache.dubbo</groupId>
  <artifactId>dubbo-spring-boot-starter</artifactId>
  <version>3.3.x</version> <!-- Replace with the latest 3.3.x release -->
</dependency>

<!-- Registry: Nacos — use the managed starter instead of raw nacos-client
     to avoid version conflicts -->
<dependency>
  <groupId>org.apache.dubbo</groupId>
  <artifactId>dubbo-nacos-spring-boot-starter</artifactId>
  <version>3.3.x</version> <!-- Same version as dubbo-spring-boot-starter -->
</dependency>
```

## Step 2: Define the service interface (shared jar)

```java
// Shared between provider and consumer modules
package com.example.api;

public interface GreetingService {
    String sayHello(String name);
}
```

## Step 3: Implement the provider

```java
package com.example.provider;

import com.example.api.GreetingService;
import org.apache.dubbo.config.annotation.DubboService;

// CORRECT: use @DubboService, NOT @Service (Spring)
@DubboService
public class GreetingServiceImpl implements GreetingService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name;
    }
}
```

## Step 4: Provider application.yml

```yaml
dubbo:
  application:
    name: greeting-provider
    # register-mode defaults to 'all' (dual: interface + application level).
    # For new greenfield services, 'instance' (application-level only) is
    # recommended for better scalability:
    # register-mode: instance
  protocol:
    name: tri          # Triple (HTTP/2, gRPC-compatible) — recommended for Dubbo 3
    port: -1           # -1 = auto-assign random available port (recommended)
  registry:
    address: nacos://127.0.0.1:8848
  # Required if migrating from Dubbo 2, or if mixing Dubbo 2 and Dubbo 3 consumers
  # metadata-report:
  #   address: nacos://127.0.0.1:8848
```

## Step 5: Implement the consumer

```java
package com.example.consumer;

import com.example.api.GreetingService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Service
public class GreetingClient {

    // CORRECT: use @DubboReference, NOT @Autowired
    @DubboReference
    private GreetingService greetingService;

    public String greet(String name) {
        return greetingService.sayHello(name);
    }
}
```

## Step 6: Consumer application.yml

```yaml
dubbo:
  application:
    name: greeting-consumer
  registry:
    address: nacos://127.0.0.1:8848
```

## Step 7: Enable Dubbo in Spring Boot

```java
@SpringBootApplication
@EnableDubbo(scanBasePackages = {"com.example.provider"})  // scans for @DubboService
public class ProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
```


# How to extend it — SPI extension points

Dubbo's SPI is not Java's standard ServiceLoader. It supports adaptive
extensions, activated extensions, and dependency injection between
extensions. All built-in components are SPI-replaceable.

## Key SPI interfaces (most commonly extended)

| Interface | Purpose | Default |
|---|---|---|
| `org.apache.dubbo.rpc.Protocol` | RPC protocol implementation | tri, dubbo |
| `org.apache.dubbo.rpc.Filter` | Intercept every RPC call | ~15 built-in |
| `org.apache.dubbo.rpc.cluster.LoadBalance` | Pick one provider from list | random |
| `org.apache.dubbo.rpc.cluster.Cluster` | Fault tolerance strategy | failover |
| `org.apache.dubbo.common.serialize.Serialization` | Wire format | hessian2, fastjson2 |
| `org.apache.dubbo.registry.RegistryFactory` | Custom registry | nacos, zk |

## How to register a custom Filter (the most common extension)

```java
package com.example;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

// @Activate tells Dubbo to auto-activate this filter on the provider side
@Activate(group = {PROVIDER})
public class TimingFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation)
            throws RpcException {
        long start = System.currentTimeMillis();
        try {
            return invoker.invoke(invocation);  // always call next in chain
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println(invocation.getMethodName() + " took " + elapsed + "ms");
        }
    }
}
```

Register it by creating the file:
`resources/META-INF/dubbo/org.apache.dubbo.rpc.Filter`

```
timingFilter=com.example.TimingFilter
```

That is all. Dubbo's SPI loader picks it up automatically. No Spring bean
declaration needed. Use `META-INF/dubbo/` for app-specific extensions and
`META-INF/dubbo/internal/` for framework-level ones.


# Common mistakes AI tools make without this skill

**1. Using `@Service` instead of `@DubboService` on the provider**
`@Service` is Spring's annotation and does nothing for Dubbo registration.
The correct annotation is `org.apache.dubbo.config.annotation.DubboService`.

**2. Using `@Autowired` instead of `@DubboReference` on the consumer**
`@Autowired` will look for a Spring bean. Dubbo remote proxies are injected
with `org.apache.dubbo.config.annotation.DubboReference`.

**3. Omitting `@EnableDubbo` or `dubbo.scan.base-packages` for providers**
Without `@EnableDubbo(scanBasePackages = ...)` or the property
`dubbo.scan.base-packages`, Dubbo will not scan for `@DubboService`
annotations. For consumers, `@DubboReference` injection works via
Spring Boot auto-configuration (`DubboAutoConfiguration`) even without
`@EnableDubbo`, but adding it is still recommended for consistency.

**4. Defaulting to `dubbo` protocol instead of `tri` in Dubbo 3**
Dubbo 3 recommends the `tri` (Triple) protocol. It is HTTP/2 based,
gRPC-compatible, and works through standard API gateways. New services
should use `tri` unless they need backward TCP compatibility.

**5. Omitting MetadataCenter during Dubbo 2→3 migration**
When Dubbo 3 providers register at the application level, Dubbo 2
consumers cannot discover them without the Metadata Center bridging the
interface-to-application mapping. Without it, consumers throw
`No provider available`.

**6. Confusing Registry Center with Config Center**
Registry = where services are. Config = how services behave.
A service can use Nacos as both, but they are configured separately under
`dubbo.registry` and `dubbo.config-center`. Setting only `dubbo.registry`
does not enable dynamic configuration.

**7. Setting `register-mode: interface` in Dubbo 3 for new services**
The Dubbo 3 default is `all` (dual registration at both interface and
application level). For new greenfield services, prefer `instance`
(application-level only) for better scalability. Only use `interface`
mode when you need compatibility with existing Dubbo 2 consumers that
have not been upgraded.

**8. QoS port conflict when running multiple Dubbo apps locally**
Dubbo starts a QoS (Quality of Service) diagnostic server on port 22222
by default. When running two Dubbo apps on the same machine, the second
app will fail with `Address already in use`. Disable or offset it:
`dubbo.application.qos-port=22223` or `dubbo.application.qos-enable=false`.
