# Dubbo Demo

This directory contains some basic usages that can help users quickly experience what Dubbo is like.
If you find it interesting and want to learn more, please look into [the dubbo-samples repository](https://github.com/apache/dubbo-samples)
where you will find comprehensive usages for how to use Dubbo in different scenarios with different features.

- [Use Dubbo with Spring Boot](./dubbo-demo-spring-boot)
- [Use Dubbo with API](./dubbo-demo-api)
- [Use Dubbo with Annotation](./dubbo-demo-annotation)
- [Use Dubbo with XML](./dubbo-demo-xml)
- [Use IDL and Triple](./dubbo-demo-triple)

## How To Build

To build all demo applications from the source code, simply step into '*dubbo-demo*' directory and use maven to build:

```bash
mvn clean package
```

After build completes, a couple of fat jars are generated under '*target*' directory under each module directories, for example: '*dubbo-demo-api-provider-${project.version}.jar*' can be found under the directory '*dubbo-demo/dubbo-demo-api/dubbo-demo-api-provider/target*'.

## How To Run

### Run As Binary
Since the generated artifacts are fat jars backed by spring boot maven plugin, they can be executed directly with '*java -jar*', and since multicast is used for service registration, a necessary system property '**-Djava.net.preferIPv4Stack=true**' is required in order to registry and discover the demo service properly. 

Use '*dubbo-demo/dubbo-demo-api*' as an example, to start the provider '*dubbo-demo-api-provider*', execute the following command:

```bash
java -Djava.net.preferIPv4Stack=true -jar dubbo-demo-api-provider-${project.version}.jar
```

To run the consumer '*dubbo-demo-api-consumer*', execute the following command:

```bash
java -Djava.net.preferIPv4Stack=true -jar dubbo-demo-api-consumer-${project.version}.jar
```

### Run In IDEA
1. Import the `dubbo-demo` module or the whole project as maven project.
2. Then run `Application#main()` in the provider module, this will pin up an embedded zookeeper server as registry and register a demo service into that registry.
3. Finally run `Application#main()` in the consumer module, this will find the registered provider service automatically and start an RPC request.

