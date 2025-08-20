# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build System and Common Commands

This project uses **Maven** as the build system with custom build scripts for faster development.

### Core Build Commands

- **Fast build**: `./build` (Unix) or `build.cmd` (Windows) - optimized build script with caching
- **Standard Maven**: `./mvnw clean install` - full clean build
- **Compile only**: `./build -p` or `./mvnw compile`
- **Run tests**: `./build -t` or `./mvnw test`
- **Code formatting**: `./build -s` or `./mvnw spotless:apply`
- **Dependency tree**: `./build -d` or `./mvnw dependency:tree`

### Module-Specific Builds

- **Single module**: `./build -m dubbo-common` - build specific module
- **Minimal build**: `./build -m` - build core modules only (dubbo-all, spring-boot-starter)

### Code Quality

- **Checkstyle**: `./mvnw validate -Pcheckstyle`
- **Spotless format**: `./mvnw spotless:apply` (requires Java 11+)
- **License check**: `./mvnw -PlicenseCheck`

## High-Level Architecture

Apache Dubbo is a microservices RPC framework with a modular architecture:

### Core Modules

**dubbo-common**: Foundation utilities, extension framework (SPI), URL abstraction, and shared components
- Extension point system with `@SPI` annotations
- URL-based configuration model
- Thread pool management and utilities

**dubbo-rpc**: RPC abstraction layer and protocol implementations
- `Invoker` - service invocation abstraction
- `Protocol` - defines how services are exported/referenced
- `Filter` - interceptor chain for request/response processing
- `ProxyFactory` - creates service proxies

**dubbo-cluster**: Load balancing, fault tolerance, and routing
- Load balancing strategies (random, round robin, least active, etc.)
- Cluster fault tolerance (fail-fast, fail-safe, fail-back)
- Router rules for traffic management

**dubbo-registry**: Service discovery implementations
- Support for Zookeeper, Nacos, Multicast
- Registry abstraction with `Registry` interface
- Service metadata management

**dubbo-remoting**: Network transport layer
- Netty-based transport implementations
- HTTP/2, HTTP/3 support
- Multiple serialization protocols

**dubbo-config**: Configuration management
- XML, annotation, and programmatic configuration
- Spring/Spring Boot integration
- Dynamic configuration center support

### Key Design Patterns

1. **URL-Driven**: All service metadata encoded in URLs
2. **SPI Extension**: Pluggable architecture using `ExtensionLoader`
3. **Layer Architecture**: Well-defined layers (Config → Proxy → Registry → Cluster → Protocol → Transport)
4. **Filter Chain**: Request/response processing pipeline

### Spring Boot Integration

- **Starters**: `dubbo-spring-boot-starter` for quick setup
- **Autoconfiguration**: Automatic bean registration and configuration
- **Actuator**: Health checks and metrics endpoints
- **Properties**: YAML/properties-based configuration

## Testing

- **Test framework**: JUnit 5 with Mockito
- **Integration tests**: Located in each module's `src/test`
- **Test utilities**: `dubbo-test-common` provides testing helpers

## Code Style

- **Formatting**: Palantir Java Format via Spotless plugin
- **Import order**: Defined in `dubbo-importorder.txt`
- **Line length**: 120 characters maximum
- **License**: Apache 2.0 header required on all files

## Common Development Tasks

### Adding New Protocol
1. Implement `Protocol` interface in `dubbo-rpc`
2. Add SPI configuration in `META-INF/dubbo/`
3. Register with `@SPI` annotation

### Adding New Load Balancer
1. Implement `LoadBalance` interface in `dubbo-cluster`
2. Add to SPI registry
3. Test with cluster configurations

### Adding New Registry
1. Extend `AbstractRegistry` in `dubbo-registry`
2. Implement registry-specific client
3. Add factory class for discovery

## Development Notes

- Use `./build -m module-name` for faster iteration on single modules
- Code formatting is enforced - run `./build -s` before commits
- The project supports JDK 8-21 with different profiles
- Spring Boot 3.x support requires JDK 17+ (activated with profile `jdk-version-ge-17`)