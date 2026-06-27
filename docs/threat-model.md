# Threat Model — Apache Dubbo

## §1 Header

| Field | Value |
|-------|-------|
| **Project** | Apache Dubbo |
| **Version/commit** | 3.3 branch (commit to be pinned at release) |
| **Date** | 2026-06-05 |
| **Author(s)** | AI-assisted, maintainer-interviewed |
| **Status** | Reviewed — maintainer interview completed (Wave 1 + Wave 2) |

**Version binding.** This threat model is versioned alongside the project. A vulnerability report against Dubbo *N* is triaged against this model as it stood at *N*, not at HEAD.

**Reporting cross-reference.**

- Findings that violate §8 (claimed properties) should be reported to [security@dubbo.apache.org](mailto:security@dubbo.apache.org) per `SECURITY.md`.
- Findings that fall under §3 (out of scope) or §9 (disclaimed properties) will be closed citing this document.

**Provenance legend.**

| Tag | Meaning |
|-----|---------|
| *(documented)* | Stated in Dubbo's own docs (README, source comments, official website, SECURITY.md). Source cited inline. |
| *(maintainer)* | Stated by a maintainer in response to a question from this process. |
| *(inferred)* | Reasoned from code structure, absence of a feature, or general domain knowledge — not yet confirmed. Must have a matching entry in §14. |

**Draft confidence.**

| Tag | Count |
|-----|-------|
| *(documented)* | 30 |
| *(maintainer)* | 13 |
| *(inferred)* | 5 |

**One-paragraph description.** Apache Dubbo is a high-performance, extensible RPC and microservices framework for Java. It provides service discovery, load balancing, traffic management, and observability for distributed systems. Applications use Dubbo's API (Spring Boot starters or programmatic configuration) to export and consume remote services over protocols such as Triple (HTTP/2, gRPC-compatible), Dubbo TCP, and REST. Service metadata is managed through pluggable registries (ZooKeeper, Nacos, etc.) and configuration centers. Dubbo assumes an internal-network deployment model throughout *(maintainer)*.

---

## §2 Scope and intended use

### Primary intended use cases

- **In-process Java library** for building RPC-based microservices. A developer adds Dubbo as a dependency, configures service interfaces, and the framework handles remote invocation, serialization, service discovery, and load balancing *(documented: README.md)*.
- **Service-to-service communication** between Java applications (providers and consumers) within a trusted internal network. The entire framework is designed around this assumption *(maintainer — "整个Dubbo都默认在内网环境工作")*.
- **Polyglot communication** via Triple protocol (gRPC-compatible) with Go, Python, Rust, and other language implementations *(documented: README.md)*.

### Deployment contexts

- In-process Java library (not a standalone server or daemon).
- Typically deployed inside Spring Boot or plain Java applications.
- JDK 8–21 supported (3.3.x line) *(documented: README.md)*.
- Designed for internal data-center / VPC deployment; not designed for direct internet exposure.

### Caller expectations

| Role | Trust level | Description |
|------|-------------|-------------|
| **Provider developer** | Trusted | Configures and exports services; sets auth, SSL, serialization policy |
| **Consumer developer** | Trusted | Configures and invokes remote services |
| **Operator** | Trusted | Manages registry, config center, QoS access |
| **RPC Client (network)** | **Untrusted** | Any network client reaching the provider's RPC port |
| **Registry** | **Trusted** | Dubbo "can only fully trust the data [registries] push" *(documented: official security docs)*. Confirmed: compromised registry = total cluster compromise *(maintainer)*. |
| **Config Center** | **Trusted** | Pushes configuration that may contain credentials |

### Component-family table

| Family | Representative entry point | Touches network | In this model |
|--------|---------------------------|-----------------|---------------|
| **RPC Protocol (Triple)** | `TripleProtocol.export()`, `TripleProtocol.refer()` — port 50051 | Yes (HTTP/2) | Yes |
| **RPC Protocol (Dubbo TCP)** | `DubboProtocol.export()`, `DubboProtocol.refer()` — port 20880 | Yes (TCP) | Yes |
| **RPC Protocol (InJVM)** | `InjvmProtocol` | No (in-process) | Yes |
| **Serialization** | SPI: `Serialization` interface (Hessian2, Protobuf, Java, Fastjson2, etc.) | No (encoding layer) | Yes |
| **Registry** | `RegistryFactory` → ZooKeeper/Nacos/Multicast | Yes (registry protocol) | Yes |
| **Config Center** | `ConfigCenterFactory` | Yes (config protocol) | Yes |
| **Cluster / Routing** | `Cluster` SPI, `Router` SPI | No (in-process routing) | Yes |
| **QoS (Management)** | `QosProtocolWrapper` — port 22222 | Yes (TCP/HTTP) | Yes |
| **Authentication** | `dubbo-auth` plugin: `ConsumerSignFilter`, `ProviderAuthFilter` | No (filter chain) | Yes |
| **TLS / Certificate** | `SslConfig`, `DubboCertManager` | Yes (CA connection) | Yes |
| **Spring Security** | `dubbo-spring-security` / `dubbo-spring6-security` plugins | No (context propagation) | Yes |
| **dubbo-demo** | `dubbo-demo-api/`, `dubbo-demo-spring-boot/` | Varies | **No** — demo/test only |
| **dubbo-test** | `dubbo-test/` | Varies | **No** — test infrastructure |
| **dubbo-compatible** | Dubbo 2.x compatibility layer | No | **No** — scheduled for removal *(maintainer)* |

---

## §3 Out of scope (explicit non-goals)

### Use cases not supported

- **Direct internet exposure.** Dubbo is designed for internal networks. Exposing RPC ports (20880, 50051) or QoS (22222) to the internet without a reverse proxy, API gateway, or service mesh is not a supported use case *(documented: security docs — "deploy in trusted internal network"; maintainer)*.
- **Standalone authorization engine.** Dubbo has no built-in RBAC or ABAC. Authorization requires Istio integration *(documented: security docs — authorization is "equivalent to Istio documentation")*.
- **Certificate management and distribution.** Users must provide their own PKI. Dubbo recommends Istio for this *(documented: security docs)*.
- **Build-time or supply-chain security.** Dependency pinning, artifact signing, and reproducible builds are out of scope for this threat model.
- **Dubbo Admin.** Dubbo Admin is a separate project outside the Dubbo security boundary *(maintainer)*.

### Threats not defended against

- A **compromised registry** that pushes malicious provider addresses *(documented + maintainer confirmed)*.
- A **malicious provider** that returns crafted deserialized objects to exploit the consumer *(maintainer confirmed)*.
- **Insider attacks** from operators or developers with access to the registry, config center, or QoS port.
- **Denial of service** from unbounded resource consumption. Dubbo delegates rate limiting to external tools like Sentinel *(maintainer)*.

### Code that ships but is not covered

| Path | Reason |
|------|--------|
| `dubbo-demo/` | Demo/test only; explicitly for "debugging and smoke test purposes" *(documented: dubbo-demo README)* |
| `dubbo-test/` | Test infrastructure |
| `dubbo-compatible/` | Legacy compatibility; scheduled for removal *(maintainer)* |
| `*.class` files at repo root (e.g., `SimpleAI.class`) | Not part of the project |

---

## §4 Trust boundaries and data flow

### Trust boundary locations

```
┌─────────────────────────────────────────────────────────────────┐
│                     Trusted Internal Network                    │
│                                                                 │
│  ┌──────────┐    RPC (Triple/Dubbo)    ┌──────────┐            │
│  │ Consumer │ ──────────────────────▶  │ Provider │            │
│  └────┬─────┘                          └────┬─────┘            │
│       │                                     │                   │
│       │ Subscribe/Register                  │ Register          │
│       ▼                                     ▼                   │
│  ┌──────────┐                         ┌──────────┐             │
│  │ Registry │ (ZK/Nacos)              │ Config   │             │
│  │          │                         │ Center   │             │
│  └──────────┘                         └──────────┘             │
│                                                                 │
│  ┌──────────┐                                                   │
│  │   QoS    │  port 22222 (telnet/HTTP)                        │
│  └──────────┘                                                   │
└─────────────────────────────────────────────────────────────────┘

Trust boundaries ( ─ ─ ─ ─ ):

1. Consumer ──Network──▶ Provider    (RPC data plane, unauthenticated by default)
2. App      ──Network──▶ Registry    (Control plane, trusted)
3. App      ──Network──▶ Config Center (Control plane, trusted)
4. Any Host ──Network──▶ QoS Port    (Management plane, localhost-restricted by default)
5. Consumer ◀──Deserialization──◀ Provider (Return value trust)
```

### Data flow and trust transitions

1. **Provider registration**: Provider sends its URL (containing IP, port, serialization config, credentials) to the registry. **Trust transition**: Provider → Network → Registry. The registry is trusted to store and distribute this data faithfully.

2. **Consumer subscription**: Consumer queries the registry for provider URLs. **Trust transition**: Registry → Network → Consumer. Consumer trusts registry data *(documented + maintainer confirmed)*.

3. **RPC invocation**: Consumer serializes method arguments and sends them to the provider's port (20880 or 50051). **Serialization type is determined by provider-priority negotiation**: the Provider declares supported serialization formats in its URL; if multiple formats are declared, negotiation occurs with Provider priority *(maintainer)*. Provider deserializes and invokes. **Trust transition**: Consumer → Network → Provider. Without TLS, this is cleartext. Without auth, unauthenticated.

4. **RPC response**: Provider returns the result (serialized). Consumer deserializes. **Trust transition**: Provider → Network → Consumer. Consumer implicitly trusts the provider's return value *(maintainer confirmed)*.

5. **QoS command**: Client connects to port 22222 via TCP telnet or HTTP, sends command text. **Trust transition**: Network → QoS Server. When `acceptForeignIp=false` (default), only loopback connections are accepted. Default `anonymousAccessPermissionLevel` is `PUBLIC` *(maintainer confirmed — historical default)*.

### Reachability preconditions per component

| Component | Reachability precondition for a finding |
|-----------|----------------------------------------|
| RPC Provider | Attacker can reach the provider's RPC port (20880 or 50051) |
| RPC Consumer | Attacker controls a provider's return value OR the registry pushes a malicious provider address |
| Registry | Attacker has write access to the registry (ZK/Nacos) |
| QoS | Attacker can reach port 22222 (localhost or remote if `acceptForeignIp=true`) |
| Serialization | Attacker controls serialized data on the wire (either direction) |
| Config Center | Attacker has write access to the config center |

---

## §5 Assumptions about the environment

### Operating system and runtime

- **JDK 8–21** on Linux, macOS, Windows *(documented: README.md)*.
- JVM memory model; no native code (pure Java).
- Netty4 for network I/O (NIO/epoll).

### Concurrency

- Thread-per-connection model in Netty; Dubbo dispatches RPC calls to thread pools.
- `ServiceConfig` and `ReferenceConfig` are not thread-safe for concurrent reconfiguration *(inferred)*.

### Network

- **Internal network assumed throughout.** Dubbo's defaults (no TLS, no auth, no serialization strict mode) assume a trusted network. This is a foundational design assumption, not a temporary limitation *(documented: security docs; maintainer — "整个Dubbo都默认在内网环境工作")*.
- DNS is trusted for hostname resolution *(inferred)*.

### What Dubbo does *not* do to its host

- Does not open privileged ports (< 1024) by default.
- Does not write to system directories.
- Does not install signal handlers.
- Does not spawn external processes (user code might).

---

## §5a Build-time and configuration variants

### Security-relevant configuration knobs

| Knob | Default | Effect on security model | Discouraged? |
|------|---------|-------------------------|--------------|
| `ssl-enabled` | `false` | Without SSL, all RPC traffic is cleartext | No, but opt-in |
| `auth` | `false` | No authentication on RPC requests | No, but opt-in |
| `serialize.check.status` | `WARN` | Logs but allows unlisted class deserialization; `STRICT` would block | Will become `STRICT` eventually *(maintainer — "eventually")* |
| `serialize.check.serializable` | `true` | Enforces `Serializable` interface on deserialized classes | No |
| `qos.enable` | `true` | QoS server starts on port 22222 | No |
| `qos.acceptForeignIp` | `false` | QoS only accepts loopback connections | No |
| `qos.anonymousAccessPermissionLevel` | `PUBLIC` | Anonymous users get PUBLIC-level command access *(maintainer confirmed — historical default)* | **Review recommended** |
| `authenticator` SPI | `basic` | Authentication mechanism selection | No |
| Serialization SPI | `hessian2` | Default serializer is Hessian2 | No |
| `dubbo.cert.manager.caCertPath` | (empty) | Falls back to `InsecureTrustManagerFactory` if empty *(documented: code — "will use insecure connection")* | **Yes, code warns** |
| `dubbo.cert.manager.oidcTokenPath` | (empty) | Connects to CA without auth token | **Yes, code warns** |

### The insecure-default case

Dubbo's default configuration provides **no authentication, no encryption, and permissive deserialization**. This is by design for the assumed internal-network deployment. Every security property in §8 is conditional on opt-in configuration.

---

## §6 Assumptions about inputs

### Input sources

| Source | Example | Trusted? |
|--------|---------|----------|
| RPC request body (consumer → provider) | Serialized method arguments | **No** — attacker can reach the provider port |
| RPC response body (provider → consumer) | Serialized return value | **Partially** — trusted if provider is trusted; compromised provider can attack consumer *(maintainer confirmed)* |
| Registry data | Provider URLs | **Yes** *(documented + maintainer confirmed)* |
| Config center data | Application configuration | **Yes** |
| QoS command input | Telnet/HTTP text | **No** — any client reaching port 22222 |
| Serialization type | Determined by Provider-priority negotiation *(maintainer)* | **Provider-controlled** — Provider declares supported formats; Consumer negotiates within Provider's declared set |
| Dubbo URL parameters | Configuration strings | **Yes** — set by trusted developer/operator |

### Per-parameter trust table — RPC Provider (Dubbo TCP / Triple)

| Entry point | Parameter | Attacker-controllable? | Caller must enforce |
|-------------|-----------|----------------------|-------------------|
| Dubbo TCP request | Serialization type | **Partial** — bounded by Provider's declared supported formats *(maintainer)* | Provider should declare only intended serializers |
| Dubbo TCP request | Request body (serialized args) | **Yes** | `serialize.check.status=STRICT` + allowlist |
| Dubbo TCP request | Service/interface name | **Yes** | Provider must expose only intended interfaces |
| Dubbo TCP request | Method name | **Yes** | Provider must validate method exists |
| Dubbo TCP request | Attachments (metadata) | **Yes** | Provider must not trust attachment values blindly |
| Triple HTTP/2 request | Content-Type | **Yes** | Provider must validate content-type matches expected serializer |
| Triple HTTP/2 request | HTTP headers | **Yes** | Provider must validate auth headers |
| Triple HTTP/2 request | gRPC trailers | **Yes** | Provider must validate |
| RPC response | Return value (serialized) | **Partial** (provider-controlled) | Consumer must use `STRICT` serialization mode |

### Per-parameter trust table — QoS

| Entry point | Parameter | Attacker-controllable? | Caller must enforce |
|-------------|-----------|----------------------|-------------------|
| QoS telnet/HTTP | Command name | **Yes** | Permission level check |
| QoS `invoke` | Service/method selector | **Yes** | Must be PRIVATE permission |
| QoS `invoke` | JSON argument string | **Yes** | `PojoUtils.realize()` must be safe |
| QoS HTTP | Source IP | **Partial** (spoofable) | `acceptForeignIp` check |

### Size, shape, and rate assumptions

- **No built-in rate limiting** at the framework level. Rate limiting is delegated to external tools like Sentinel *(maintainer)*.
- **No request body size limit** enforced by Dubbo; Netty's default max message size applies (varies by configuration) *(inferred)*.
- **No limit on the number of service interfaces or methods** a provider exposes.
- Hessian2 deserialization can create deeply nested objects; no recursion depth limit enforced by Dubbo *(inferred)*.

---

## §7 Adversary model

### Assumed attacker (in scope)

**Network-level attacker** who can:
- Observe and modify traffic on the network between consumer and provider (MITM).
- Send crafted RPC requests to any provider's RPC port.
- Send crafted responses if they control a provider instance.
- Connect to the QoS port if it is accessible.

**Capabilities:**
- Can observe timing *(inferred: no constant-time guarantees)*.
- Can inject/modify packets on the wire (if TLS is not used).
- Can influence serialized data content (controls request body).
- Can request a serialization format, but **Provider has priority** in negotiation — the attacker cannot force a serialization format the Provider has not declared *(maintainer)*.
- Cannot break TLS/mTLS (assumes proper PKI).
- Cannot write to the registry (registry is trusted).
- Cannot write to the config center (config center is trusted).

### Explicitly out of scope

| Actor | Reason |
|-------|--------|
| Compromised registry | Dubbo explicitly trusts the registry *(documented + maintainer)* |
| Compromised config center | Configuration is trusted |
| Malicious Dubbo developer (insider) | Not a threat the framework can mitigate |
| Attacker with physical host access | OS-level compromise is out of scope |
| Compromised CI/CD pipeline | Supply-chain security is out of scope |

### Distributed-systems actor: authenticated-but-Byzantine provider

Dubbo's consumer implicitly trusts the provider *(maintainer confirmed)*. A **legitimately registered provider that turns malicious** (or serves compromised code) can:
- Return crafted serialized objects to exploit the consumer's deserialization *(maintainer confirmed)*.
- Slow-read or refuse connections to cause consumer timeouts.
- Return incorrect results without detection (no result integrity verification).

This actor is **in scope** but Dubbo provides **no defense** against it by default.

---

## §8 Security properties the project provides

### P1: Serialization class checking (conditional)

**Property:** When `serialize.check.status=STRICT`, Dubbo blocks deserialization of classes not on the allowlist, mitigating deserialization-based RCE. *(documented: code — `DefaultSerializeClassChecker`, official security docs)*

**Conditions:** Requires explicit opt-in (`STRICT` mode); only applies to serializers integrated with `SerializeSecurityManager` (Hessian2, Fastjson2). Java native serialization is NOT covered.

**Violation symptom:** Remote code execution via gadget chain deserialization.

**Severity tier:** Security-critical.

**Provenance:** *(documented: `DefaultSerializeClassChecker.java`, official security docs on serialization)*

### P2: Request authentication via HMAC (conditional)

**Property:** When `auth=true` and `authenticator=accesskey`, Dubbo authenticates RPC requests using HMAC-SHA256 signatures over `{serviceKey}#{method}#{secretKey}#{timestamp}`. *(documented: `AccessKeyAuthenticator.java`)*

**Conditions:** Requires explicit opt-in; credentials stored in URL parameters. Replay protection is **not provided** — the timestamp is signed but not validated for recency on the server side *(maintainer — timestamp validation is no longer planned)*.

**Violation symptom:** Unauthorized RPC invocations if credentials are compromised; replay of previously-signed requests.

**Severity tier:** Security-critical.

**Provenance:** *(documented: `AccessKeyAuthenticator.java`; maintainer on replay protection)*

### P3: Request authentication via Basic Auth (conditional)

**Property:** When `auth=true` and `authenticator=basic`, Dubbo authenticates using `username:password` (Base64-encoded). *(documented: `BasicAuthenticator.java`)*

**Conditions:** Requires explicit opt-in; credentials in URL parameters; Base64 is not encryption — requires TLS to be secure.

**Violation symptom:** Unauthorized RPC invocations.

**Severity tier:** Security-critical.

**Provenance:** *(documented: `BasicAuthenticator.java`)*

### P4: Transport encryption via TLS (conditional)

**Property:** When `ssl-enabled=true`, Dubbo encrypts RPC traffic using TLS via Netty's `SslHandler`. Supports mTLS. `DubboCertManager` is production-ready *(maintainer)*. *(documented: `SslConfig.java`, official security docs)*

**Conditions:** Requires explicit opt-in; certificates must be provided by the user; `DubboCertManager` falls back to `InsecureTrustManagerFactory` if `caCertPath` is empty *(documented: code warning)*.

**Violation symptom:** Eavesdropping, MITM, credential theft.

**Severity tier:** Security-critical.

**Provenance:** *(documented: `SslConfig.java`, `DubboCertManager.java`; maintainer on production readiness)*

### P5: QoS localhost restriction (default)

**Property:** When `qos.acceptForeignIp=false` (default), QoS only accepts connections from loopback addresses. Default `anonymousAccessPermissionLevel` is `PUBLIC` *(maintainer confirmed)*. *(documented: `QosProtocolWrapper.java`)*

**Conditions:** Default behavior; can be overridden by configuration.

**Violation symptom:** Remote invocation of QoS commands (including `invoke` for arbitrary method execution).

**Severity tier:** Security-critical.

**Provenance:** *(documented: `QosProtocolWrapper.java`; maintainer on PUBLIC default)*

### P6: Deserialization blocklist (defense-in-depth)

**Property:** Dubbo maintains a blocklist of known gadget chain classes (`security/serialize.blockedlist`) that are rejected even in `WARN` mode. *(documented: `DefaultSerializeClassChecker.java`, blocklist file)*

**Conditions:** Only as effective as the blocklist is complete; bypass has occurred historically.

**Violation symptom:** RCE via new or unblocked gadget chains.

**Severity tier:** Security-critical.

**Provenance:** *(documented: blocklist file with 195 entries)*

### P7: Provider-priority serialization negotiation

**Property:** The serialization format used for RPC is determined by Provider-priority negotiation. The Provider declares supported serialization formats; the Consumer negotiates within that set. A network attacker cannot force a serialization format the Provider has not declared. *(maintainer)*

**Conditions:** Effective when the Provider declares a limited set of serializers (e.g., Hessian2 only). If the Provider declares all installed serializers, the protection is reduced.

**Violation symptom:** Deserialization attacks via a permissive serializer if Provider declares too many formats.

**Severity tier:** Security-critical.

**Provenance:** *(maintainer)*

### P8: QoS invoke as a managed feature (conditional)

**Property:** The QoS `invoke` command is an officially supported feature *(maintainer)*. It is protected by the QoS permission system: `invoke` requires PRIVATE permission, which is only granted to localhost connections by default (when `acceptForeignIp=false`).

**Conditions:** Requires `qos.enable=true` (default). Safety depends on correct QoS access configuration. The `invoke` command uses `PojoUtils.realize()` for argument conversion, which bypasses the `SerializeSecurityManager` framework.

**Violation symptom:** Arbitrary method invocation on registered services if PRIVATE permission is incorrectly granted to remote users.

**Severity tier:** Security-critical.

**Provenance:** *(maintainer)*

---

## §9 Security properties the project does *not* provide

### NP1: Authentication by default

Dubbo ships with `auth=false`. Without explicit configuration, any client that can reach the provider port can invoke any service method. *(documented: default value, official docs)*

### NP2: Encryption by default

Dubbo ships with `ssl-enabled=false`. All RPC traffic (including authentication credentials) is transmitted in cleartext. *(documented: default value)*

### NP3: Strict deserialization by default

Dubbo ships with `serialize.check.status=WARN`. Unknown classes are logged but allowed. The project intends to change the default to `STRICT` eventually *(maintainer)*, but as of 3.3.x, permissive deserialization is the default.

### NP4: Result integrity verification

Dubbo does not cryptographically verify the integrity of RPC responses. A provider (or MITM if TLS is off) can return arbitrary data without detection. *(maintainer confirmed)*

### NP5: Built-in authorization

Dubbo has no standalone authorization engine. Service-level access control requires Istio integration. *(documented: official security docs — "equivalent to Istio documentation")*

### NP6: Timestamp validation / replay protection for HMAC auth

The `AccessKeyAuthenticator` includes a timestamp in the signature but the provider does not validate that the timestamp is recent. Replay protection is **not planned** *(maintainer)*. This is by design: valid signatures can be replayed indefinitely.

### NP7: Consumer protection from malicious providers

A consumer implicitly trusts serialized return values from providers *(maintainer confirmed)*. There is no mechanism for a consumer to verify provider identity beyond registry data.

### NP8: Rate limiting or resource bounding

Dubbo does not enforce request rate limits, connection limits, or payload size limits at the framework level. Rate limiting is delegated to external tools like Sentinel *(maintainer)*.

### False-friend properties

| What it looks like | What it actually is | Why it's not a security property |
|---------------------|---------------------|----------------------------------|
| Hessian2 is "safe by default" (Dubbo 3.2+) | Uses a class allowlist in STRICT mode | Default is WARN, not STRICT; allowlist may not cover all application classes |
| `AccessKeyAuthenticator` with timestamp | Looks like replay protection | Timestamp is signed but not validated for recency; replay is possible *(maintainer)* |
| QoS `acceptForeignIp=false` | Looks like access control | Default `anonymousAccessPermissionLevel=PUBLIC` grants PUBLIC-level access to localhost *(maintainer)* |
| `serialize.check.serializable=true` | Looks like deserialization safety | Only checks `Serializable` interface; most gadget chains implement it |
| `DubboCertManager` TLS | Looks like secure PKI | Falls back to `InsecureTrustManagerFactory` if CA cert not provided |

---

## §10 Downstream responsibilities

For the assumptions in §5–§7 to hold, the downstream integrator **must**:

1. **Enable TLS** (`ssl-enabled=true`) with proper certificates. Without TLS, all data and credentials are cleartext.
2. **Enable authentication** (`auth=true`) with `accesskey` authenticator. Basic Auth over cleartext is equivalent to no auth.
3. **Set `serialize.check.status=STRICT`** and maintain an application-specific allowlist.
4. **Declare only intended serialization formats** on the Provider. The Provider-priority negotiation protects only as well as the Provider's declaration is restrictive.
5. **Secure the registry** — enable ZooKeeper ACLs or Nacos authentication; keep the registry off public networks.
6. **Secure the config center** — authenticate connections; treat config data as trusted input.
7. **Configure QoS appropriately** — default `anonymousAccessPermissionLevel=PUBLIC` grants PUBLIC access to localhost. For production, set to `NONE` or disable QoS (`qos.enable=false`) if the `invoke` command is not needed.
8. **Do not expose RPC ports (20880, 50051) to the internet.** Use a reverse proxy, API gateway, or service mesh.
9. **Validate provider identity** — in environments with untrusted providers, use mTLS with client certificate verification.
10. **Review serialization configuration** — disable Java native serialization if not needed; prefer Protobuf for Triple protocol.
11. **Use Sentinel or equivalent** for rate limiting and circuit breaking — Dubbo does not provide these at the framework level.

---

## §11 Known misuse patterns

### M1: Exposing RPC ports to the internet

**What it looks like:** Deploying Dubbo providers with RPC ports reachable from the internet (e.g., via `0.0.0.0` bind + no firewall).
**Why it's unsafe:** Default Dubbo has no auth and permissive deserialization. Internet-facing Dubbo ports have led to RCE vulnerabilities *(documented: CVE history)*.
**What to do instead:** Deploy behind an API gateway; restrict to internal networks.

### M2: Using Java native serialization

**What it looks like:** Configuring `prefer-serialization=java` or including Java native serialization in the Provider's declared format set.
**Why it's unsafe:** Java native deserialization is the most common RCE vector in the JVM ecosystem. Dubbo's blocklist cannot cover all gadget chains. Java native serialization remains a supported format *(maintainer)* but is dangerous.
**What to do instead:** Use Hessian2 with `STRICT` mode, or Protobuf with Triple protocol.

### M3: Declaring all serialization formats on the Provider

**What it looks like:** A Provider declares every installed serialization format as supported, giving Consumers maximum flexibility.
**Why it's unsafe:** Even though serialization is Provider-priority *(maintainer)*, declaring all formats (including Java native) means a malicious Consumer can request the most permissive serializer. The negotiation protects only as well as the Provider's declaration is restrictive.
**What to do instead:** Declare only the serialization formats actually needed (e.g., Hessian2 for Dubbo TCP, Protobuf for Triple).

### M4: Using Basic Auth without TLS

**What it looks like:** `auth=true` + `authenticator=basic` without `ssl-enabled=true`.
**Why it's unsafe:** Basic Auth sends credentials as Base64-encoded strings (not encrypted). Network observers can capture credentials.
**What to do instead:** Always pair Basic Auth with TLS, or use `accesskey` authenticator with HMAC.

### M5: Using the HMAC authenticator without TLS

**What it looks like:** `auth=true` + `authenticator=accesskey` without TLS.
**Why it's unsafe:** While the HMAC secret is not transmitted, the request data is still in cleartext. An observer can capture valid signatures for replay (timestamps are not validated for recency, and replay protection is not planned *(maintainer)*).

### M6: Connecting `DubboCertManager` without CA certificate

**What it looks like:** Using the built-in certificate manager without setting `caCertPath`.
**Why it's unsafe:** The code falls back to `InsecureTrustManagerFactory.INSTANCE`, accepting any certificate *(documented: code warning — "will use insecure connection")*.
**What to do instead:** Always provide `caCertPath` and `oidcTokenPath` when using `DubboCertManager`.

### M7: Leaving QoS with permissive defaults

**What it looks like:** Running production Dubbo with `qos.enable=true` (default) and default `anonymousAccessPermissionLevel=PUBLIC`.
**Why it's unsafe:** The `invoke` command is an officially supported feature *(maintainer)* that can invoke any service method via reflection. While `invoke` requires PRIVATE permission, other PUBLIC-level commands may expose configuration or operational data.
**What to do instead:** Set `qos.anonymousAccessPermissionLevel=NONE` in production; restrict QoS to localhost only.

---

## §11a Known non-findings (recurring false positives)

### NF1: Telnet handler RCE on external port

**What scanners report:** Telnet handlers (`LogTelnetHandler`, `ChangeTelnetHandler`, etc.) in `dubbo-remoting-api` allow remote command execution.
**Why it's a non-finding:** These legacy telnet handlers are deprecated in 3.x and replaced by the QoS module. The telnet codec is retained for backward compatibility but is not exposed on external ports by default. The QoS module has its own permission system.
**Suppression:** Verify the finding is against `dubbo-remoting-api` telnet handlers, not the QoS module; verify the port is not externally accessible.

### NF2: Hessian2 class loading without blocklist

**What scanners report:** Hessian2 deserialization allows arbitrary class instantiation.
**Why it's a non-finding (partial):** Dubbo integrates `DefaultSerializeClassChecker` into Hessian2's serializer factory. Classes are checked against the blocklist (WARN mode) or allowlist (STRICT mode). This is a defense-in-depth measure, not a vulnerability in itself. However, if STRICT mode is not enabled, the protection is limited.
**Suppression:** Cite `Hessian2SerializerFactory.java` integration and §8 P6.

### NF3: `InsecureTrustManagerFactory` usage in tests

**What scanners report:** `InsecureTrustManagerFactory.INSTANCE` is used, accepting all certificates.
**Why it's a non-finding:** In test code and in `DubboCertManager` only as a fallback when `caCertPath` is not configured. The code explicitly warns about this case. `DubboCertManager` is production-ready *(maintainer)*; the fallback is an operator configuration issue.
**Suppression:** Verify the finding is in test code or the documented fallback path; cite §10 responsibility to provide `caCertPath`.

### NF4: Client-controlled serialization type

**What scanners report:** The Dubbo TCP protocol header contains a serialization type field that the client can set to any value.
**Why it's a non-finding:** Serialization type is determined by Provider-priority negotiation *(maintainer)*. The client cannot force a serialization format that the Provider has not declared. The protocol header field reflects the negotiated result, not an unconstrained client choice.
**Suppression:** Cite §8 P7 (Provider-priority serialization negotiation).

---

## §12 Conditions that would change this model

This threat model should be revised when:

1. **Default serialization mode changes** from `WARN` to `STRICT` (intended eventually *(maintainer)*).
2. **Authentication becomes default** (`auth=true`).
3. **New serialization formats** are added to the SPI.
4. **New protocol implementations** are added (e.g., a new transport).
5. **QoS default changes** (e.g., `anonymousAccessPermissionLevel` defaults to `NONE`).
6. **New trust boundaries** are introduced (e.g., mesh-sidecar communication).
7. **Registry integrity verification** is added.
8. **`dubbo-compatible` module is removed** *(maintainer)*.
9. **Replay protection is added** to `AccessKeyAuthenticator`.
10. **Framework-level rate limiting** is introduced.

---

## §13 Triage dispositions

| Disposition | Meaning | Licensed by |
|-------------|---------|-------------|
| `VALID` | Violates a claimed property in §8, via in-scope adversary and input | §8, §6, §7 |
| `VALID-HARDENING` | No §8 property violated, but API makes §11 misuse easy | §11 |
| `OUT-OF-MODEL: trusted-input` | Requires attacker control of a trusted parameter (registry, config center, URL config) | §6 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires excluded attacker capability (compromised registry, insider) | §7 |
| `OUT-OF-MODEL: unsupported-component` | Lands in `dubbo-demo/`, `dubbo-test/`, `dubbo-compatible/`, or deprecated code | §3 |
| `OUT-OF-MODEL: non-default-build` | Only under discouraged §5a configuration (e.g., `caCertPath` empty) | §5a |
| `OUT-OF-MODEL: separate-project` | Lands in Dubbo Admin or other separate project | §3 |
| `BY-DESIGN: property-disclaimed` | Concerns a property explicitly disclaimed in §9 (e.g., no auth by default, no replay protection) | §9 |
| `KNOWN-NON-FINDING` | Matches documented false positive in §11a | §11a |
| `MODEL-GAP` | Cannot be routed to any above | Triggers §12 revision |

---

## §14 Open questions for the maintainers

All questions from Wave 1 and Wave 2 have been resolved. Answers:

### Wave 1 — Scope and trust model

| # | Question | Answer | Provenance update |
|---|----------|--------|-------------------|
| 1 | Registry trust | Confirmed: fully trusted, compromised registry = cluster compromise | *(documented + maintainer)* |
| 2 | Consumer-Provider trust | Confirmed: consumer implicitly trusts provider return values | *(maintainer)* |
| 3 | QoS default `anonymousAccessPermissionLevel` | `PUBLIC` — historical default | *(maintainer)* |
| 4 | STRICT mode timeline | Eventually, no target version | *(maintainer)* |
| 5 | Timestamp validation in AccessKeyAuthenticator | Not planned | *(maintainer)* → §9 NP6 confirmed by-design |
| 6 | Java native serialization support | Still supported | *(maintainer)* |
| 7 | Dubbo Admin scope | Separate project | *(maintainer)* → §3 updated |

### Wave 2 — Configuration and components

| # | Question | Answer | Provenance update |
|---|----------|--------|-------------------|
| 8 | Serialization type control | Provider-priority negotiation; Provider declares, then negotiation | *(maintainer)* → §6, §7, §8 P7 rewritten |
| 9 | `dubbo-compatible` security | Will be removed at some point | *(maintainer)* → §3 updated |
| 10 | Spring Security context propagation | Dubbo assumes internal network throughout; same security domain | *(maintainer)* |
| 11 | `DubboCertManager` readiness | Production-ready | *(maintainer)* → §8 P4 updated |
| 12 | Rate limiting plans | No; delegated to Sentinel etc. | *(maintainer)* → §9 NP8 confirmed |
| 13 | QoS `invoke` command | Official feature, not just debugging | *(maintainer)* → §8 P8 added, §11 M4 rewritten |

### Remaining *(inferred)* claims

The following *(inferred)* claims remain and could benefit from further maintainer input:

1. `ServiceConfig`/`ReferenceConfig` not thread-safe for concurrent reconfiguration — §5.
2. DNS trusted for hostname resolution — §5.
3. No recursion depth limit in Hessian2 deserialization — §6.
4. No request body size limit enforced by Dubbo (Netty defaults apply) — §6.
5. No constant-time guarantees (timing observable) — §7.

These are low-impact inferences that do not materially affect triage decisions. They can be confirmed or corrected in a future revision.

---

## §15 Optional: machine-readable companion

A machine-readable companion (`threat-model.yaml`) can be generated for automated triage. Structure:

```yaml
project: apache-dubbo
version: "3.3"
date: 2026-06-05
status: reviewed

trust_boundaries:
  - name: rpc-data-plane
    description: "Consumer-Provider RPC traffic"
    default_authenticated: false
    default_encrypted: false
    serialization: "provider-priority negotiation"
  - name: registry-control-plane
    description: "Registry connections"
    default_authenticated: false
    default_encrypted: false
  - name: qos-management
    description: "QoS port 22222"
    default_authenticated: false
    default_anonymous_permission: PUBLIC
    localhost_only: true

properties_provided:
  - id: P1
    name: serialization-class-checking
    conditional: true
    requires: ["serialize.check.status=STRICT"]
  - id: P2
    name: hmac-authentication
    conditional: true
    requires: ["auth=true", "authenticator=accesskey"]
    note: "No replay protection"
  - id: P3
    name: basic-authentication
    conditional: true
    requires: ["auth=true", "authenticator=basic"]
  - id: P4
    name: tls-encryption
    conditional: true
    requires: ["ssl-enabled=true"]
  - id: P5
    name: qos-localhost-restriction
    conditional: false
    default: true
  - id: P6
    name: deserialization-blocklist
    conditional: false
    default: true
  - id: P7
    name: provider-priority-serialization-negotiation
    conditional: false
    default: true
  - id: P8
    name: qos-invoke-permission-controlled
    conditional: true
    requires: ["qos.enable=true"]

properties_not_provided:
  - id: NP1
    name: default-authentication
  - id: NP2
    name: default-encryption
  - id: NP3
    name: strict-deserialization-default
  - id: NP4
    name: result-integrity-verification
  - id: NP5
    name: built-in-authorization
  - id: NP6
    name: replay-protection
  - id: NP7
    name: consumer-protection-from-malicious-providers
  - id: NP8
    name: rate-limiting

triage_dispositions:
  - VALID
  - VALID-HARDENING
  - OUT-OF-MODEL:trusted-input
  - OUT-OF-MODEL:adversary-not-in-scope
  - OUT-OF-MODEL:unsupported-component
  - OUT-OF-MODEL:non-default-build
  - OUT-OF-MODEL:separate-project
  - BY-DESIGN:property-disclaimed
  - KNOWN-NON-FINDING
  - MODEL-GAP
```

---

*End of threat model. Maintainer interview completed 2026-06-05. Remaining 5 low-impact *(inferred)* claims listed in §14 may be confirmed in a future revision.*
