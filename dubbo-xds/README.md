<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements. See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the License for the
specific language governing permissions and limitations
under the License.
-->

# Native Istio xDS integration (in development)

`dubbo-xds` is an optional integration for the `3.3` branch. Add it explicitly
alongside Dubbo. The implementation and evidence are tracked in
[the implementation plan](../dubbo-test/xds/IMPLEMENTATION-PLAN.md).
The current implementation checkpoint and subsequent acceptance scope are
summarized in [the delivery record](DELIVERY.md).

The module currently provides bootstrap loading, a process-wide shared ADS
runtime, LDS/RDS/CDS/EDS subscriptions and resource graphs, and application-level
ServiceDiscovery with per-instance MetadataService V1 lookup. The client TLS
adapter reads bootstrap file providers, validates PKIX chains and xDS SANs,
and isolates connections by target, policy and certificate generation.
Provider LDS activation and transport-entry RBAC protect MetadataService
and business RPCs. Native routing applies virtual hosts,
wire paths/headers, weighted subsets and separate locality/endpoint selection.
Route/HCM deadlines, cancellation propagation, unary retry and shared
`max_requests` admission are implemented, along with fault,
RR/LEAST_REQUEST/RING_HASH and priority waiting. Local checks and native Kubernetes
light batches cover the main discovery, traffic, security and recovery paths,
including four admitted RPC forms across real Istiod CA transitions and actual
destination SAN rejection with cross-Service isolation. These batches do not
replace concentrated load/recovery acceptance. The current security scope is
mTLS/SAN/ALLOW-DENY RBAC, with JWT/CUSTOM deferred to the next implementation
round. Absent Istio LRS/outlier output is not an
end-to-end execution claim. See the current support matrix below and
[the application assembly](../dubbo-test/xds/istio/README.md#native-dubbo-application-assembly).

## Deployment contract

Use Istio's `grpc-agent` injection template. The application reads
`GRPC_XDS_BOOTSTRAP`; `pilot-agent` owns authentication to Istiod and supplies the
local ADS Unix socket and certificate files. No kubeconfig or Kubernetes API
credentials belong in the application. A Linux epoll transport is required for
ADS over UDS. Applications and registries in the same JVM share one ADS client
per agent endpoint; conflicting bootstrap identities are rejected.

The native client uses v3 state-of-the-world ADS for LDS/RDS/CDS/EDS; it does
not open Delta ADS or a v2 stream. Nonempty federation `authorities` are rejected
at bootstrap loading. A `Resource` wrapper is accepted only with a complete
embedded supported resource and a matching name. TTL, alias and heartbeat-only
wrappers are rejected rather than treated as ordinary full resources. These
boundaries belong to the current Istio grpc-agent contract; the module is not
a general implementation of every Envoy discovery transport or extension.

## Discovery configuration

Configure the registry address as `xds://localhost`, registry type `service`,
and provider register mode `instance`. Remote workload registration is owned
by Kubernetes; Dubbo registration still maintains local service metadata.
Native xDS uses the ServiceDiscovery path; do not enable the separate
`mesh-enable` direct-Service mode for these references.

Each reference supplies:

| Parameter | Meaning |
| --- | --- |
| `provided-by` | Explicit Kubernetes Service or comma-separated Services |
| `provider-port` | Required Kubernetes Service port, independent of workload port |
| `protocol` | `tri` (default) or `dubbo` discovery target |
| `provider-namespace` | Namespace for a short Service name; defaults to bootstrap Node `NAMESPACE` |
| `xds.cluster-domain` | Defaults to `cluster.local` |
| `xds.metadata.application` | Provider application; defaults to the destination Service's first label |
| `xds.metadata.group` | MetadataService group; defaults to the metadata application |
| `xds.metadata.targets` | JSON object keyed by destination protocol, Service FQDN and Service port; fields override metadata defaults |
| `xds.metadata.service` | Metadata Kubernetes Service/FQDN; defaults to the business Service |
| `xds.metadata.service-port` | Metadata Service port; defaults to the business Service port |
| `xds.metadata.protocol` | Metadata `tri` or `dubbo`; defaults to the business protocol; changing it requires an explicit metadata Service port |
| `xds.metadata.timeout` | Per metadata request timeout in milliseconds, default 3000 |
| `xds.ring-size-cap` | Per Reference RING_HASH cap, default 4096, valid 1..8388608 |

The `provided-by` Reference option uses Dubbo's existing explicit application
binding. It does not fetch interface-application mapping. FQDN, Service port,
and protocol form the target identity; EDS supplies the actual workload port.
A short `service.namespace` name expands to `service.namespace.svc.cluster.local`.
Different cluster contexts at the same IP:port remain separate Directory
entries.

By default MetadataService V1 shares the business target's workload port and
protocol. A separate metadata Service/port/protocol subscribes its own resource
graph through the shared ADS runtime. It must expose the same workloads: each
business endpoint is matched to a healthy metadata endpoint at the same host.
Missing or ambiguous matches are isolated; the SDK never substitutes a different
workload's metadata. The metadata connection uses that metadata cluster's TLS,
SANs and authority, independently of the business connection. Inbound RBAC
protects the metadata port even with `filter=-default`. Istio 1.31.1 grpcgen
constructs ISTIO_MUTUAL SAN matchers from the destination ServiceAccounts;
its [CDS generator](https://github.com/istio/istio/blob/1.31.1/pilot/pkg/networking/grpcgen/cds.go#L241)
does not consume DestinationRule's custom `subjectAltNames`. The SDK enforces
the SAN matchers actually delivered by xDS. A native observation of an incorrect
custom metadata SAN still allowing destination calls records this control-plane
limit; it is not counted as a SAN rejection acceptance pass. Enable V1 on the provider
(`only-use-metadata-v2=false`); for Triple, set
`metadata-service-protocol=tri` and `metadata-service-port` to the exposed Triple
workload port. Set `xds.metadata.application` when the Dubbo application differs
from the Kubernetes Service name. `xds.metadata.service-port` names the Kubernetes
Service port, and EDS resolves the actual metadata workload port; setting a
raw workload port without its corresponding xDS resource is not supported.
For example, business Service port 7070 and metadata Service port 7071 may
resolve to workload ports 17070 and 18081. Metadata V2's empty-revision request is not
used as an equivalent operation.

When metadata uses a separate target, its addresses come from that Service's
default CDS/EDS cluster. Business RDS subset selection does not narrow these
fixed per-instance metadata addresses. Outbound LDS/RDS remains available for
metadata fault and deadline handling, while TLS/SAN and admission use the
metadata target's own cluster.

Cross-Service routes resolve metadata options from the actual Istio destination
cluster (`outbound|Service-port|subset|Service-FQDN`), while business endpoints
keep the entry Reference's routing context. Use `xds.metadata.targets` in the
Reference or Registry parameters when destinations have different metadata
applications, groups, Service ports or protocols:

```json
{
  "tri://orders.shop.svc.cluster.local:7070": {
    "application": "orders-app",
    "group": "orders-metadata",
    "service-port": "7071",
    "protocol": "tri"
  },
  "tri://payments.shop.svc.cluster.local:8080": {
    "application": "payments-app",
    "service-port": "8081"
  }
}
```

Keys contain the **business** Service port and call protocol. Values describe
the metadata entry. Supported fields are `application`, `group`, `service`,
`service-port`, `protocol` and `timeout`; all values are strings. Registry and
Reference target maps merge by target and field, with Reference fields taking
precedence. Target fields override the existing `xds.metadata.*` defaults;
unspecified fields inherit those defaults, then the destination-derived values.
A short metadata `service` uses the destination's Kubernetes namespace. Unknown
fields, invalid target/port/protocol/timeout values and malformed JSON fail
Reference binding; maps are limited to 128 targets and 64 KiB per parameter.
References sharing an entry discovery target must share the complete metadata
configuration because future route updates may select any destination.

Cross-Service metadata uses the destination metadata target's fixed CDS/EDS
and its TLS/SAN/authority context. Its endpoint must uniquely match the business
workload host. The SDK isolates missing metadata and incorrect identity rather
than querying the entry Service's metadata. Local real RPC checks cover source
and destination groups on the same metadata socket, different business ports,
route changes, wrong destination SAN, recovery and metadata endpoint removal.
Native normal-SDK light checks passed source/destination application, separate
metadata/business ports and ServiceAccounts, sync/async/server streaming route
changes, metadata caller RBAC/cache-expiry isolation, metadata port withdrawal
with business EDS retained, and recovery without JVM/Pod restart. Native wrong-destination SAN rejection and recovery have also passed in the
isolated single-destination and cross-Service fixtures. Concentrated acceptance
remains a subsequent phase. The per-target configuration is the
current additive implementation assumption for D5 and may be adjusted after
user feedback.

Each endpoint is queried directly, outside the Registry/Directory chain.
Metadata is shared across references to the same target. The listener exposes
only interfaces actually published on the discovered protocol and workload
port. Failed initial queries isolate the endpoint and retry; refresh failure
may retain that same endpoint's valid cached metadata until expiry. Removal
immediately evicts addresses, and late responses cannot resurrect them. Metadata
endpoint/security-context changes cancel its tasks and invalidate its previous
cache. Published interface filtering always uses the business protocol/workload
port, even when metadata is fetched over another protocol or port.

Registry parameters `xds.metadata.concurrency` (default 4),
`xds.metadata.max-endpoints` (2048), `xds.metadata.ttl` (60000 milliseconds) and
`xds.metadata.refresh` (30000 milliseconds) bound metadata work and freshness.
Explicit bootstrap overrides are `xds.bootstrap.file` or
`xds.bootstrap.config`; otherwise the standard bootstrap environment variables
are used. Invalid explicit configuration does not fall through to another
source.

Without an explicit override, Registry initialization uses
`Bootstrapper.loadDefault(waitMillis)`. It caches the first successfully parsed
process default from `GRPC_XDS_BOOTSTRAP` or `GRPC_XDS_BOOTSTRAP_CONFIG`, including
the original Node. Later ApplicationModels/FrameworkModels reuse that snapshot,
even if pilot-agent rewrites the file with newly populated Pod annotations or
labels. Failed reads/parses are not cached and initialization can retry. The
snapshot remains after the last runtime lease closes, until process exit; bootstrap
configuration changes require an SDK process restart. Certificate **contents**
continue to refresh through their file provider without restarting the process.

Explicit `xds.bootstrap.file`/`xds.bootstrap.config` are loaded and validated on
each new Registry. The pool still rejects a different bootstrap fingerprint at
the same ADS endpoint; it does not ignore Node metadata or security fields to
force compatibility. `Bootstrapper.load(...)` is the fresh-source API, distinct
from the process-default accessor. This matches grpc-java 1.78's
[default bootstrap caching](https://github.com/grpc/grpc-java/blob/v1.78.0/xds/src/main/java/io/grpc/xds/GrpcBootstrapperImpl.java#L104).

## Normal SDK startup

Use normal `DubboBootstrap`, `ServiceConfig` and `ReferenceConfig` APIs with the
registry and reference parameters above. The provider's `ApplicationConfig`
must set `onlyUseMetadataV2=false`, `metadataServiceProtocol=tri` and
`metadataServicePort` to its Triple workload port. The Reference's `providedBy`
is the target Kubernetes Service, and its `provider-port` is the Service port.
When the provider application name differs, set `xds.metadata.application`.

The executable example in `dubbo-test/xds` exports the same business interface
on Triple and Dubbo, and configures the binary Reference to fetch metadata via
the Triple Service port. No custom Directory or MetadataService implementation
is needed. Normal ServiceConfig wildcard binding now selects the matching
inbound LDS resource even when its published `bind.ip` is a concrete address.
Explicit non-wildcard binding continues to use that address.

Two local bootstrap checks cover real V1 metadata, protocol/port filtering,
synchronous/asynchronous calls, mTLS and online DENY/restore. A separate native
cluster smoke covers Triple server streaming as well. These checks do not
replace the concentrated acceptance matrix.

## Traffic integration

Triple references from a native xDS registry install `XdsRouter` and
`XdsCluster` in the existing Directory/Cluster pipeline. Routing shares Triple's
request metadata builder, including the actual method descriptor and Service
FQDN:Service port authority. Socket connections still use EDS workload ports.
Multiple explicit Services are selected uniformly before each Service's own
route policy is evaluated; they do not share subset identities.

When ServiceDiscovery removes every provider, native xDS reports `UNAVAILABLE`.
This also applies to a call whose fault delay ends after withdrawal. The normal
Dubbo Directory's empty-provider forbidden signal is translated only during
native xDS address listing; upstream RBAC `PERMISSION_DENIED` remains unchanged.

Virtual-host precedence and ordered path/header/fraction matching choose the
route, then its configured weights choose the cluster independently of endpoint
count or connectivity. An unavailable selected subset fails; its weight is not
redirected to another subset. Locality selection is separate from endpoint policy
within the selected cluster. Legacy `ROUND_ROBIN` uses endpoint weights;
`LEAST_REQUEST` uniformly samples unique logical endpoints and prefers the one
with fewer active attempts. Its choice_count defaults to 2, rejects values below
2 and caps larger values at 10, following
[gRFC A48](https://github.com/grpc/proposal/blob/master/A48-xds-least-request-lb-policy.md).
Locality weights remain independent. Weighted least-request, active_request_bias
and slow-start are not executed. Zero locality weights and unhealthy endpoints
are excluded before priority selection. RR excludes zero endpoint weights;
least-request ignores endpoint weights, including zero, in discovery and selection.
EDS rejects sparse priorities and duplicate localities within one priority.
Each Reference tracks priority children and transport state independently.
Native Triple connections publish nonblocking snapshots and repeated,
independently cancellable state notifications. READY requires the HTTP/2 preface.
An initially CONNECTING priority has a 10-second grace period; explicit failure
allows immediate fallback. READY/IDLE recovery restores promotion and the next
CONNECTING grace; reconnecting after an explicit failure does not restart it.
If every remaining priority is connecting, calls keep waiting within their
original deadline. Health and zero-weight eligibility are evaluated first.

A bounded executor buffers unary and streaming calls before dispatch. Waiting
consumes the existing call budget without acquiring endpoint-active or
`max_requests` permits. Future/stream cancellation and Reference destruction
release pending work; picks consult current endpoints while retaining the chosen
route cluster. Timer threads enqueue work and never run application callbacks.
Native HTTP/2 initial connections start asynchronously; closing an unfinished
attempt releases TCP and HTTP/2 negotiation waits promptly. Point-to-point
metadata reads use the same readiness gate before admission. Native reconnect
uses bounded exponential backoff with jitter; other transports retain their
existing constructor behavior. HTTP/3 readiness is outside this integration's
HTTP/2 acceptance scope.

Legacy `RING_HASH` uses one weighted XXH64 ring across all localities in the
selected priority. Each address weight is locality weight times endpoint weight;
zero weights and unhealthy endpoints are excluded. Ring sizes must satisfy
`1 <= minimum <= maximum <= 8388608`. The reference option `xds.ring-size-cap`
defaults to 4096 and caps both sizes, following the gRPC Java baseline. Duplicate
sockets within a priority and combined weights or sums exceeding uint32 are
rejected. A certificate or Invoker replacement preserves the ring for unchanged
addresses and weights.

Route hash policies support text headers with optional RE2 rewriting and
`filter_state` key `io.grpc.channel_id`, represented by a stable identifier per
native Reference. Multiple header values use their comma-joined wire value;
`content-type` is `application/grpc`, and binary headers contribute no hash.
Supported policies combine in order, honoring terminal. Cookie, source-IP and
query-parameter hash policies follow the gRPC baseline and contribute no hash.
If none produces a hash, one random hash is chosen for the call and retained
through retries, fault/readiness waits and resource refresh. See
[gRFC A42](https://github.com/grpc/proposal/blob/master/A42-xds-ring-hash-lb-policy.md).

Connecting or idle addresses stay on the ring and buffer their selected calls;
transient failures advance to the next usable ring address. This preserves
mapping when a connection recovers. The priority aggregate follows the ring-hash
baseline, including immediate fallback after two children fail when none is
ready. `MAGLEV` is deliberately unsupported: a native Triple subscription NACKs
it and retains its last valid configuration. It never substitutes round-robin.

Each native Reference keeps its own endpoint counters, separate from shared
cluster admission. All policies record active attempts, so switching RR and
least-request or rebuilding Invokers for security/traffic updates does not erase
ongoing load on the same logical endpoint. Idle counters are removed; endpoint
removal prevents new selection while old permits release on their call's end.
Unary completion, errors and cancellation release each attempt before retry
backoff; streaming keeps its count until transport completion/cancellation,
including Reference destruction. Fault waiting and local abort perform no pick.
Point-to-point metadata calls retain their fixed instance and do not participate
in the Reference's endpoint selection counter.

Native Triple graph subscriptions validate their LB policy before accepting CDS
updates. An invalid choice_count or unsupported policy NACKs the response and
preserves the last good graph. Validators are owned by individual subscriptions;
diagnostic and binary-discovery watches can still read resources without executing
a Triple LB policy. Cached resources are validated before delivery to a new strict
subscriber.

New calls read the latest graph even when addresses and Invokers are unchanged.
Each call retains its selected route/cluster context. Native Cluster bypasses
the default Failover retry loop and applies the selected route's unary retry policy. Route
`grpc_timeout_header_max`, otherwise route `max_stream_duration`, otherwise HCM
`common_http_protocol_options.max_stream_duration` can shorten the Dubbo call
budget. An explicit zero adds no xDS limit; it does not remove Dubbo's timeout.
`RouteAction.timeout` and header offset are validated but do not implement Envoy
response timers. Remaining budget is carried in the real `grpc-timeout` header.

The local deadline covers unary and streaming calls, including a stream that has
not yet sent its first message. Expiry cancels transport immediately; terminal
observer delivery is serialized with messages and may wait for a blocked user
callback. Explicit Future cancellation propagates through deadline calls' filter
wrappers to the Triple stream. An expired call is rejected before dispatch.
Unary retry follows gRFC A44: route policy overrides virtual-host policy;
`cancelled`, `deadline-exceeded`, `internal`, `resource-exhausted` and `unavailable`
are supported, with at most five total attempts, exponential jittered backoff
and server `grpc-retry-pushback-ms`. Negative or invalid pushback stops retries.
Attempts and backoff consume the same total deadline. Cancellation and Cluster
destruction stop both pending retries and the active Triple stream. Each retry
uses current endpoints within the original Service/cluster; it does not select
another weighted subset. Requests retain their serialized bytes for replay,
bounded to 1 MiB per call. Larger requests make one attempt. Response headers
commit unary calls, and streaming calls are never replayed.

Explicit `StatusRpcException` failures use gRPC trailers for POJO methods as
well as IDL. Ordinary business exceptions retain Dubbo's response behavior and
are not automatically retried. Both immediate and asynchronous failures carry
response attachments, including retry pushback, to transport. The native Cluster
also owns unary transport Futures when no retry policy is present. Synchronous
dispatch obtains and registers its asynchronous transport Future before waiting.
Destroying one Reference cancels its own calls while preserving another
Reference's calls on the shared connection.

Per-try timeout, hedging, HTTP retry conditions and proxy host-selection
predicates are ignored according to the gRPC baseline. Deprecated
`max_grpc_timeout` and route `Decorator` are accepted without adding proxy
execution semantics. Compatibility with complete, actual Istio RDS output still
requires the later cluster acceptance suite. Fault, supported LB and priority waiting are integrated as described below. The plan
records remaining capabilities and semantic differences.

## Protocol scope and current support

A discovery identity contains Service FQDN, Service port and call protocol.
Explicit target URIs must match the Reference's protocol. Triple subscribes
LDS/RDS/CDS/EDS; binary Dubbo subscribes its default CDS/EDS directly for TCP
Service discovery. Binary References use Dubbo's normal Directory/Cluster/LB
behavior. They do not execute xDS HTTP routing, retry, fault, ring hash or RBAC.
A binary cluster requiring xDS TLS is rejected locally; its addresses are
removed while other targets remain usable. There is no plaintext fallback.

For a Service exposing Triple at 7070 and Dubbo at 7071, configure separate
References with `protocol=tri, provider-port=7070` and
`protocol=dubbo, provider-port=7071`. These are Service ports; EDS supplies each
workload port. Metadata defaults to each protocol's own socket. To read Triple
MetadataService for binary business discovery, explicitly set
`xds.metadata.protocol=tri` and `xds.metadata.service-port=7070`. The metadata
response is still filtered by the binary business protocol and workload port.
Applications using binary business or metadata calls must include
`dubbo-rpc-dubbo` through their normal Dubbo distribution or explicit dependency;
it is a test-only dependency of this optional module.

| Capability | Istio capture | Native behavior / recorded evidence | Remaining scope |
| --- | --- | --- | --- |
| Bootstrap, UDS, shared ADS | grpc-agent bootstrap and native probes | Native SDK-to-agent ADS and HTTP/2 reconnect passed; native consumer agent container restart retains the JVM/References and accepts a fresh route, with one agent restart and zero app restarts; a second FrameworkModel/Registry created after agent bootstrap rewrite shares one runtime, executes Triple/Binary calls and releases without disrupting original References; native agent-to-Istiod connection loss retains accepted routes during BACKOFF and accepts a fresh route after upstream recovery, with one runtime and unchanged processes; native provider-agent restart preserves dispatched async unary/server streams and actual IDL client-stream/bidi calls; existing input writers continue messages and half-close, four admissions stay held until successful completion and return to zero, the application container/JVM stays unchanged, and the restarted agent accepts fresh inbound DENY/recovery for ordinary and input-stream RPCs | Other in-flight disruptions and concentrated recovery/load acceptance |
| App discovery, MetadataService V1 | CDS/EDS and Service/workload ports | Fixed endpoint reads; interface/protocol/port filtering, TTL and isolation; native all-EDS withdrawal keeps dispatched async unary/server streams and actual IDL client-stream/bidi calls alive; existing input writers send a second message and half-close after withdrawal with four admissions retained, then each call finishes once and releases admission; new input streams fail UNAVAILABLE without dispatch, and both input forms plus ordinary business recover without restart | Per-target metadata config, local cross-Service SAN isolation and native cross-Service route/metadata-caller authorization/port-withdrawal recovery light checks passed; actual same-CA wrong-ServiceAccount destination SAN rejection and correct-identity recovery on separate business/metadata ports pass with retained EDS and metadata-driven isolation; native Entry A→B routing with different SA/metadata application/group also passes actual B SAN rejection, selective Triple isolation while binary A continues, and correct-identity recovery; other resource/in-flight and concentrated cross-Service boundaries remain |
| Binary discovery | Dual Service TCP CDS/EDS captured separately | Dual-protocol real RPC and TLS refusal checks; native Triple-subset metadata isolation, endpoint withdrawal and Service deletion/recreation passed; native empty binary discovery reports UNAVAILABLE through ordinary Cluster policy | Native in-flight and mixed-load acceptance |
| mTLS, SAN, certificate files | ISTIO_MUTUAL/SPIFFE fields | TLS/rotation local checks; native valid/wrong-SA, no-client-cert, test-only SAN mismatch and plaintext negatives passed; isolated native client/server leaf renewal advances SDK epochs and new TLSv1.3/h2 sessions consume both renewed certificates without process/Pod restart; native SDK A/A+B/B test-root rollover with actual Istio xDS advances both epochs 0→1→2→3 and uses new h2 sessions for every phase; four actual async unary/server/client/bidi RPCs admitted under A remain active through overlap, B identity migration and retirement of A, accept continued input/half-close under B-only trust, and complete once without cancellation within the unchanged budget, releasing four admissions to zero; real local mTLS and established Triple connections reject retired-root peers; native pinned A-credential business/MetadataService calls succeed under A+B, then receive PERMISSION_DENIED on the same ready TLSv1.3/h2 channel under B-only trust, with no business dispatch and normal B Reference recovery; actual isolated Istiod signing-CA A/A+B/B-identity/B-only phases also pass with both SDK epochs 0→1→2→3, new TLSv1.3/h2 business sessions, authenticated fresh metadata reads and no process/Pod restart, 38 native records and cleanup of all 28 owned resources; actual Istio destination SAN rules reject a different SA server leaf under the same mesh CA at business/metadata ports while PKIX-only test controls succeed, metadata isolates the still-healthy endpoint, and correct-identity restoration recovers normal References without restart (33 native records, 10 owned resources cleaned); Entry A→B with distinct SAs/metadata applications also rejects A credentials at B while preserving A binary metadata/calls, and restores the original Triple Reference (33 cross-Service records, 15 resources cleaned); four actual async unary/server/client/bidi calls admitted under A also span the real Istiod CA overlap, B identity migration and B-only trust, continue input/half-close, complete once inside explicitly declared 20-minute test budgets and release four admissions to zero (77 native records, 28 resources cleaned) | Subsequent concentrated CA/cross-Service/lifecycle acceptance |
| ALLOW/DENY RBAC | Inbound grpcgen LDS | Native HTTP/2 entry protects business and MetadataService; native DENY/recovery and wrong-SA direct business/metadata checks passed; native business-only DENY rejects new sync/async/server-stream and actual IDL client-stream/bidi RPCs before dispatch; previously authorized async unary/server/client/bidi streams continue, input streams accept a second message and half-close, each call finishes once and releases admission, then all forms recover without restart; request cancellation is isolated across mixed POJO/IDL streams | Other in-flight lifecycle combinations and concentrated security acceptance |
| JWT/CUSTOM | grpcgen gap documented | No complete JWT/CUSTOM integration | Next implementation round (D4) |
| Routes, subsets, wire headers | Real RDS samples | Ordered routing, weighted clusters, POJO/IDL wire checks | Cross-Service acceptance |
| Timeout, cancel, unary retry | RDS baseline fields | Original budget, transport cleanup, replay and ownership checks; native budget/retry/cancel light checks passed | Mixed load and cluster updates |
| Fault delay/abort | LDS/RDS extension capture | Unary and three streaming forms locally; native sync/async/server-stream abort and delay/budget checks passed | Mixed load and remaining stream forms in cluster |
| max_requests | CDS threshold=2 | Shared admission/release; native saturation, one local drop without retry, completion and recovery passed | Remaining lifecycle/load acceptance |
| Diagnostics | Runtime-local snapshots and counts | Shared cluster/EDS attempt and drop totals, per-instance metadata status/cause types, lazy certificate validity/refresh state; local regression and public API/artifact checks passed | Subsequent native-load acceptance; latest diagnostic bundle not deployed to retained echo Pods |
| EDS drop | Absent from current native captures | Ordered per-attempt drop gate, cumulative totals and LRS categories implemented; synthetic EDS real-RPC/NACK checks recorded in the plan | Native control-plane capability evidence and empty Directory boundaries |
| LRS | No lrs_server in current native CDS captures | SELF negotiation, per-origin locality intervals, filters, streaming gauges, uncategorized circuit drops and EDS categories; real ADS/LRS and Triple fixtures | Native control-plane enablement and concentrated load acceptance; no endpoint/ORCA metric support |
| ROUND_ROBIN, LEAST_REQUEST | Actual CDS policies | Weighted RR / unweighted A48 least-request; local real RPC/lifecycle checks, native RR route/priority batches | Native LEAST_REQUEST execution and concentrated policy/load combinations |
| RING_HASH | CDS minimum=1024 and `x-user` RDS | A42 hash/ring and stable attempts; native header-key stability and MAGLEV last-good retention passed | Membership changes and concentrated load |
| MAGLEV | CDS tableSize=65537 | Explicit NACK, last-good retained (D6) | Deliberately unsupported this round |
| Priority/locality | Native EDS 0/1/2 from explicit Triple/metadata port failoverPriority; ordinary binary EDS remains priority=0 | Synthetic EDS plus real transport/RPC failover and waiting; deterministic delayed-notification/selection races fixed with failing-before regressions and a passing 56-check mixed local batch; native three-provider listener faults select P1 then P2 while all three EDS members and fresh per-instance metadata remain, restored listeners preempt to P0, and owned Service member withdrawal/return selects P1/P0 with the same consumer JVM/References/runtime | Concentrated failover timing/load and additional policy/lifecycle combinations |
| Outlier detection | Configured but absent from captured grpcgen CDS | No outlier execution claim | Control-plane/product decision |
| Per-try timeout, hedging, proxy-only retry | Baseline fields accepted where applicable | Ignored according to the selected gRPC semantics | Separate scope decision if required |
| Revision / interface-application mapping | Not used | Explicit Services and direct MetadataService | Deliberately excluded this round |

These entries distinguish control-plane generation, SDK execution and local
verification. Capturing a resource does not establish native Kubernetes business
acceptance. Sources, commands and failures are recorded in the implementation plan.

## Light verification

The current-source reactor regression on 2026-10-12 passed 277 checks in one
command: all 37 xDS test classes (202 checks) and 18 selected core test classes
(75 checks), with no failures, errors or skips. XML reports and the actual test
classpath were checked against the source inventory; the relevant core modules
were loaded from the reactor's `target/classes`. A separate ordinary registry
factory/cache cleanup follow-up passed four checks. These results cover the
local xDS suite and selected changed-core behavior, not the whole project's
test suite, release process or concentrated Kubernetes acceptance. Commands and
the verification manifest are recorded in the implementation plan below.

The module's public API Javadoc jar also builds with errors treated as failures
(offline, without external API links). Its generated pages include the current
certificate status and metadata-cause inspection APIs. Source-header checks using
the repository's `.licenserc.yaml` passed for the xDS/native sources and the
whole checkout. Maven's `licenseCheck` profile resolved 107 dependency metadata
entries, or 82 with compile/runtime scope only, without an unknown license name;
the runtime report excludes test-only crypto/JUnit/Mockito/Hamcrest dependencies.
These are build/header/metadata results, not completion of the full release workflow.

From the main checkout:

```sh
mvn -pl dubbo-xds -am \
  -Dtest=BootstrapperTest,XdsClientTest,XdsServiceDiscoveryTest,XdsMetadataPortTest,XdsWireContextTest,XdsResourceLeaseTest,XdsSecurityTest,InboundSecurityTest,RbacPolicyTest,XdsRoutingTest,XdsRouteTableTest,XdsLoadBalanceTest,XdsDeadlineTest,XdsTimeoutsTest,XdsRetryTest,XdsRetryPolicyTest,XdsRetryExecutorTest,XdsCircuitBreakerTest,XdsCircuitBreakingTest,IstioResourceCaptureTest,XdsFaultPolicyTest,XdsFaultResourceValidationTest,XdsFaultExecutionTest,XdsLeastRequestTest,XdsHashPolicyTest,XdsRingHashTest,XdsRingHashRoutingTest,XdsPriorityStateTest,XdsPriorityTest,XdsDualProtocolTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

The tests include a fake ADS server plus real local Triple metadata and
business RPCs through the native Registry/Directory, address removal and
same-socket cluster isolation. Separate metadata tests cover per-workload
matching, missing/ambiguous addresses, port changes and late results, actual
metadata/business port separation, and independent metadata mTLS/RBAC/SAN
policies with a plaintext business port. Security tests exercise real local mutual TLS
handshakes, wrong SAN/root rejection, missing client identity, coherent
certificate rotation and native CertProvider loading. Real Triple RPC tests
cover MetadataService exported with `filter=-default`, business RPCs, wrong
ServiceAccount denial, plaintext rejection, online DENY/removal/restore,
last-good policy after NACK and no export with invalid initial LDS. An explicit
plaintext listener still enforces RBAC and rejects the HTTP/1 path. Routing tests
use two real providers and verify weighted/header routing, live subset changes
without Invoker recreation, an unavailable selected subset, and route/address
removal. Deadline tests use real provider calls and verify wire timeout, shorter
caller budgets, explicit-zero/HCM fallback, streaming expiry, NACK retention and
provider-visible cancellation. Retry tests cover real synchronous/asynchronous
failures, retry limits, business/permission errors, pushback, a deadline spanning
attempts, cancellation/destruction, frozen requests, live endpoint changes and
NACK retention. A transport-listener check distinguishes unary response-header
commitment from trailers-only errors. HTTP/2 regression tests cover event-loop stream lookup during deferred
flow-control callbacks and stream retirement.

Route selection acquires a cluster reference atomically with the current RDS
snapshot, following [gRFC A31](https://github.com/grpc/proposal/blob/master/A31-xds-timeout-support-and-config-selector.md).
Removing a cluster from RDS stops new calls selecting it; active calls retain its
CDS/EDS subscriptions until completion or cancellation. Unary ownership spans
all attempts and backoff; retries retain the selected cluster and consult current
CDS/EDS. CDS deletion removes its usable endpoints even while a call owns the
subscription. An old call cannot revive removed endpoints or switch to another
cluster. Streaming ownership ends on transport completion or cancellation rather
than completion of its initial invocation result. Reference destruction cancels
active streams and retry calls, and releases their ownership.

Wire-context tests use checked-in Dubbo/protobuf generated sources from
`src/test/proto/wire_echo.proto`. The fixture deliberately uses different proto
and Java packages. Metadata is produced from exported URLs using the Java
interface name and the exported wire path, as in normal configuration.
Native discovery and real synchronous/asynchronous IDL calls verify
`/xds.testing.WireEcho/SayHello`; POJO calls verify a custom exported path.
A test-only transport decorator records the provider's received HTTP/2 headers
to check authority, path, group and version. Missing group/version references
stay isolated in their own Directories. Triple now registers exact exported
wire-path mappings with group/version and removes mappings only for their owner;
the existing configured legacy fallback behavior remains available.

The fixture was generated with dubbo-maven-plugin/dubbo-compiler
3.3.7-SNAPSHOT and protoc 4.33.4, then its imports and formatting were normalized.
It is test source only and adds no generated API to the production artifact.
To regenerate from the repository root:

```sh
mvn -pl dubbo-xds org.apache.dubbo:dubbo-maven-plugin:3.3.7-SNAPSHOT:compile \
  -DdubboVersion=3.3.7-SNAPSHOT \
  -DprotoSourceDir="$PWD/dubbo-xds/src/test/proto" \
  -DoutputDir="$PWD/dubbo-xds/target/wire-fixture-generated"
```

Copy the five generated Java files under `org/apache/dubbo/xds/fixture` into
`dubbo-xds/src/test/java`, preserve the ASF license headers, and normalize imports/formatting.

The previous native-module verification covered 117 unique cases in 24 classes
across two runs (2026-10-11 16:43/16:45). The main run passed 116 cases and had one
mock still targeting the old watch signature; after updating it, all 10 related
watch/client/shared-transport checks passed. It is not a single green 117-case
command. This includes six real least-request/lifecycle scenarios, 17 fault
execution checks, IDL client streaming and LDS/RDS NACK checks. The related core
regressions retain their earlier 62-case evidence; this batch changed no core
module code. The implementation plan records commands and
the failure/fix history. These checks are not a substitute for the later
concentrated Istio acceptance suite. The Linux agent-only control-plane probe
and reference environment are documented in
[the cluster test assets](../dubbo-test/xds/istio/README.md).

The priority/EDS batch passed 26 checks in five classes (2026-10-11
17:01), including four real Triple scenarios for health/weight changes, listener
loss and recovery, preserving an active fallback call, and EDS NACK retaining
the last usable snapshot. These overlap the previous suite. That batch used short
heartbeat/reconnect intervals. The transport follow-up passed 43 native checks
and seven related core checks in separate runs (17:09–17:13), including default
heartbeat recovery and a real TCP listener that never sends HTTP/2 settings.
Priority state selection, cancellable call waiting and asynchronous initial
connections have passed related local checks. The follow-up covers 53 native
checks and 38 core checks across separate runs, reconstructed after fixing the
direct-transport security fixture and cancellation propagation. The 52-check
native main run had one failure; the final 10-check targeted run passed, including
security, waiting, shared-connection destruction and cancellation. Overlapping
checks are counted once; this is not one all-green 91-check invocation or a full
module regression. Evidence is recorded in the plan. Concentrated native
Kubernetes acceptance remains pending.

The hash follow-up captured real RING_HASH and MAGLEV resources. Local hash,
wire routing, NACK and priority checks cover 51 unique cases across separate
runs. The 51-case main rerun passed 50 and had one intermittent priority timing
failure; its standalone rerun and the final 10-case hash/priority combination
passed. This is not one green 51-case command. An exact test-only POJO allowlist
removed a serialization fixture order dependency while preserving production
STRICT checks. The priority timing issue was retained for follow-up; the later deterministic fixes and mixed rerun are recorded below.
The related reactor build passed. Native Kubernetes business acceptance remains
pending.

The dependency-alignment follow-up covers 60 related checks across separate
runs, including bootstrap/client, mTLS/RBAC, actual Istio samples, hash RPCs,
dual-protocol metadata and LB diagnostics. The main 51-case command had one
refresh-fixture failure; final targeted runs passed after preserving the
original fixture port and publishing subscription diagnostics at creation.
The related reactor install passed (2026-10-11 18:26). Full native-cluster
acceptance remained pending at that batch; later native evidence and priority follow-up are recorded in the current matrix and plan.

`XdsServiceDiscovery.inspectTargets()` reports discovered/available counts,
graph readiness and the latest error category without returning credentials or
metadata payloads. ADS connection state and resource metadata are available
from `XdsClient` for further diagnostics.

`XdsClient.ResourceMetadata.getStatus()` distinguishes `REQUESTED`, `ACKED`,
`ABSENT`, `NACKED_WITH_LAST_GOOD` and `NACKED_WITHOUT_RESOURCE`. Version and
acceptedAt identify the last accepted resource; nonce/rejection describe the
latest response. Unsupported LB diagnostics report the policy enum, including
MAGLEV, without dumping resource payloads. An ACK describes ADS acceptance;
a Reference can still be locally unavailable because its metadata, TLS or
Directory is not ready. Consult `inspectTargets()` alongside ADS metadata.
The current rejection is response-wide for its resource type. It is not a
historical rejection counter or a per-RPC diagnostic.

`grpc-xds` follows the dependency BOM's `grpc.version`, matching the core,
API, transport and other gRPC artifacts at 1.81.0. The chosen gRFC behavior and
MAGLEV support boundary are implemented by Dubbo's native adapters. The module
does not invoke grpc-java's private NameResolver/LB implementation; the private
XXH64 class is used only as an independent test oracle.

## Circuit breaking and captured Istio resources

Native Triple dispatch, including per-instance MetadataService lookup, applies
the first DEFAULT `circuit_breakers.thresholds.max_requests` (default 1024).
Counters are shared across references and applications for the same workload,
cluster and EDS service name. Lowering a limit retains existing active calls;
excess calls return a local UNAVAILABLE drop without unary retry. Unary completion
releases the permit; streaming holds it until transport completion or cancellation,
including Reference destruction. Traffic-only CDS updates preserve valid metadata
cache entries while dispatch observes the current limit.

`XdsCircuitBreaker.active()` and `dropped()` describe the current active counter
lifetime. Cumulative diagnostics are separate: `XdsServiceDiscovery.inspectLoadStats()`
returns immutable shared-workload counters keyed by CDS cluster and EDS service,
including issued transport attempts, in-progress attempts, successes, errors,
circuit drops and EDS drops. Each retry is a separate attempt; metadata RPCs use
the same accounting. Streaming initial results do not finish an attempt. Actual
stream completion, non-OK status, cancellation and destruction do. Admission and
EDS drops are excluded from issued/finished counts and cannot trigger an upstream retry.
Snapshots do not reset totals. Subscribed clusters and outstanding attempts are
retained; idle retired history is bounded to 128 cluster/EDS identities and all
state is released with the shared runtime. LRS uses separate interval counters;
collecting a load report does not reset these diagnostic totals.

Native Triple evaluates EDS `policy.drop_overloads` once per attempt, in order,
before connection waiting. It supports HUNDRED, TEN_THOUSAND and MILLION denominators,
clamps unsigned numerators to 100%, and selects the first matching category.
Malformed categories/denominators NACK the EDS response and preserve last-good
resources. Point-to-point Triple metadata calls also obey their own EDS drop policy.
Binary Dubbo remains discovery-only. The sampling follows
[gRPC A27](https://github.com/grpc/proposal/blob/master/A27-xds-global-load-balancing.md);
no native Istio-generated EDS drop or LRS execution is claimed from the current captures.

## Load reporting

Accepted CDS `lrs_server: { self: {} }` enables native LRS automatically. Other LRS
server sources NACK the CDS update and preserve the last accepted resource. An
absent `lrs_server` opens no LRS stream. Each needed bootstrap server has its own
LRS control channel and shares the ADS protocol executor; it adds no ADS stream.
The initial request preserves the bootstrap Node and advertises
`envoy.lrs.supports_send_all_clusters`. The server controls cluster filters,
`send_all_clusters` and report intervals. A zero interval pauses reporting; invalid
intervals end the LRS stream and trigger an independent reconnect with backoff.
LRS failure does not restart ADS or stop native business calls.

Reports distinguish the CDS origin, cluster name and EDS service name. Resource
snapshots retain their own origin even while a fallback CDS callback is pending;
an identical CDS from a different bootstrap server still updates graph watchers.
Weak resource identity references avoid retaining obsolete protobuf snapshots.
Each locality reports interval issued/success/error counts and the current
in-progress gauge. Every actual retry is an attempt; stream completion ends its
gauge. Metadata calls follow the same accounting. Local circuit drops contribute
to total drops without an EDS category, while EDS drops retain their actual category.
Endpoint-level statistics, ORCA metrics and backend utilization are not implemented.

Preparing a report does not clear its window. Backpressure postpones collection;
synchronous send failure leaves counts pending. Committing the local gRPC handoff
advances only captured checkpoints, preserving events that arrive during the send.
LRS has no report ACK, so this is best-effort delivery, not remote exactly-once
delivery. Filtered-out clusters retain their pending windows. Retired clusters and
control-plane origins remain until pending counts and in-flight attempts drain;
unlike cumulative diagnostic history, pending reports have no 128-entry eviction.
Disabling LRS stops accounting for new snapshots but retains old unsent results,
including retries to an unavailable LRS service, until reporting or runtime close.
The shared runtime closes streams/channels and releases all reporting state.

`XdsServiceDiscovery.inspectLoadReporting()` exposes immutable status entries keyed
by bootstrap index, such as `server-0: READY`, `PAUSED` or
`BACKOFF:UNIMPLEMENTED`. It exposes no server URI, channel credential, bootstrap
contents or exception message. The optional native application's `inspect`
includes the same map as `load_reporting`.

The protocol was checked against grpc-java 1.78's
[LoadReportClient](https://github.com/grpc/grpc-java/blob/v1.78.0/xds/src/main/java/io/grpc/xds/client/LoadReportClient.java)
and [LoadStatsManager2](https://github.com/grpc/grpc-java/blob/v1.78.0/xds/src/main/java/io/grpc/xds/client/LoadStatsManager2.java).
Dubbo owns the counters and reporting lifecycle; it does not call those private
implementations. The current Istio 1.31.1
[grpcgen CDS builder](https://github.com/istio/istio/blob/1.31.1/pilot/pkg/networking/grpcgen/cds.go)
does not set `lrs_server`, consistent with the saved captures. Local protocol and
real Triple tests do not establish Istio-generated LRS enablement.

`inspectTargets()` also reports cumulative metadata-fetch successes/failures and
an immutable per-instance `metadataFailureStatuses` map. A successful read on one
instance does not erase another instance's last failure. A successful reread clears
that instance's status; removal drops its entry. The map contains status-code names
(or EMPTY_METADATA), with no exception message, RPC payload or credential content.
`metadataFailureCauses` adds an immutable per-instance list of exception class
names, outermost first, bounded to eight causes with identity-based cycle detection.
It exposes wrapped TLS/KeyStore/connection failure types without exception messages,
stack traces or suppressed exceptions. A successful reread or endpoint removal
clears that instance's cause list; previously returned snapshots remain immutable.
An empty metadata response has a status but no exception cause list.
Fetch counts and transport-attempt counts differ when a fetch invokes multiple
MetadataService methods. Existing `lastError` remains available for compatibility.

See the [operations guide](OPERATIONS.md) for deployment checks and diagnosis of
ADS, discovery, metadata, TLS, authorization and invocation failures.

The checked-in `src/test/resources/istio-1.31.1/extension-policy/*.textproto`
fixtures preserve real grpc-agent LDS/RDS/CDS/EDS. They contain public routing,
identity and endpoint configuration, without certificate/private-key contents.
The sample contains fault delay/abort, LEAST_REQUEST and max_requests=2; the
configured outlierDetection was absent from its CDS. Baseline RDS accepts Istio
provenance metadata and the Envoy attempt-count option (ignored by gRPC xDS).
The parser accepts fault overrides and retains them for the invocation gate;
the fixture test retains the captured fault configuration. Parser/fixture checks
and local real-RPC fault execution have passed. The captured LEAST_REQUEST policy
is now accepted and the SDK executes it with local real-RPC evidence. This resource
capture is not a native cluster business-RPC
acceptance.

## Client certificate lifecycle

The bootstrap `file_watcher` configuration supplies identity certificate,
private key and trust root paths. Files are read lazily when TLS is needed;
the configured `refresh_interval` controls subsequent refreshes. A mismatched
or incomplete update retains only the previously valid snapshot. Expired or
unavailable credentials fail TLS setup; they never select plaintext.
Missing first files or an interrupted update retry within one second, rather
than waiting for the normal refresh interval before recovery.

`XdsServiceDiscovery.inspectCertificateProviders()` and
`XdsSecurityRuntime.inspectCertificateProviders()` expose immutable snapshots by
configured provider name without initializing providers or reading files.
`NOT_LOADED` means the provider has not yet been needed; inspection does not force
TLS initialization. `READY` means accepted material is still locally usable,
including a retained valid snapshot during a failed refresh. `EXPIRED` and
`NOT_YET_VALID` describe the accepted leaf at observation time; `UNAVAILABLE`
means no currently usable material is retained. This is file-material state,
not proof of a successful handshake, peer trust or RBAC authorization.

Each snapshot contains its observation time, last accepted identity validity
timestamps, the time when changed coherent material was accepted, and only the
last refresh error's class name. Validity dates are null before an identity was
accepted or for a roots-only provider. After an accepted identity becomes
unavailable, its last accepted dates remain available for diagnosis. Unchanged
successful polling does not change the accepted-refresh timestamp. Restoring
the same accepted bytes clears a prior refresh error without a new certificate
generation or security epoch. These fields exclude paths, bootstrap settings,
certificates, SANs, private keys and key-derived fingerprints. Closed runtimes
and destroyed discoveries return an empty map.

Istiod's CA response can append its complete root bundle after an intermediate
chain, including a repeated root or an independent root during overlap. The
SDK separates that bundle from the identity chain before constructing the
KeyManager: complete DER duplicates are removed, adjacent issuer signatures are
checked, and appended configured roots or self-signed CA roots are excluded from
extra identity issuers. Unrelated workload certificates are rejected. The
TrustManager still uses only the configured root file and xDS SAN constraints;
a retired root left in an identity file never restores trust. This also avoids
the JDK PKCS12 `Certificate chain is not valid` failure. Intermediate/duplicate/
dual-root and retired-root-tail real mTLS regressions pass, including wrong SAN,
wrong root and unrelated workload negatives. The corrected native bundle also
passed all four actual Istiod signing-CA phases with normal Dubbo TLS, business
and authenticated metadata calls: both SDK epochs advanced 0→1→2→3 with new
TLSv1.3/h2 channels, unchanged JVMs/agents/Pod UIDs, 38 recorded checks and all
28 owned resources removed. A separate native batch also proves four admitted RPC forms across all actual
Istiod CA transitions, with original input writers continuing under B-only trust,
one terminal per call and four admissions released to zero. That test explicitly
uses fixed 20-minute fixture budgets before call creation; ordinary fixtures keep
180 seconds and production timeouts remain unchanged. Concentrated load
acceptance remains separate.

The shared runtime notifies discovery owners after a valid rotation. Endpoint
snapshots carry an immutable certificate generation; discovery refreshes
metadata and replaces the associated Invokers. Connection keys include the
bootstrap identity, target/cluster policy and certificate version. Credentials
remain frozen for an existing connection; rotation changes new connections.
The same TLS adapter protects point-to-point metadata lookup. Binary Dubbo
rejects a cluster requiring xDS TLS because its scope is discovery only.

The parser supports the new and deprecated provider fields generated together
by Istio and rejects conflicting values. Inline secrets, SDS, custom validators
and unsupported certificate restrictions are rejected before ACK. This is a
deliberate supported-field contract, not an implementation of every Envoy TLS
option.

## Inbound security lifecycle

The provider derives its inbound listener from the bootstrap template and the
Triple bind address/port. An `xds://` application registry activates security
even when internal MetadataService is exported before RegistryProtocol.
Standalone exports may explicitly use `xds.enabled=true` with the same bootstrap
parameters. `xds.bootstrap.wait` bounds both initial LDS and certificate-file
readiness (default 30000 ms, 1..60000). Export fails before opening the socket
when either is unavailable. The inbound lease shares the consumer ADS runtime.
The Triple workload port must be represented by a Kubernetes Service target;
grpcgen does not generate inbound LDS for an unexposed metadata-only port.

The supported grpcgen layout has one filter chain, one inline HCM default
non-forwarding route, RBAC ALLOW/DENY filters and a terminal router. Unsupported
mandatory filters, TLS restrictions, listener/address mismatches and unknown
security fields are rejected before ACK. DENY wins; each ALLOW filter must
permit the request. Authenticated principals come from the verified certificate
(URI SAN, then DNS SAN, then subject), never caller-supplied identity headers.
CIDRs use actual socket addresses; there is no implicit forwarded-IP trust.

Authorization runs at the HTTP/2 transport entry before method dispatch and
message deserialization, including internal services without default filters.
Managed Triple ports admit HTTP/2 only. ALPN and the actual request `:scheme`
reflect xDS transport security. Strict TLS rejects plaintext; an explicitly
plaintext policy prevents fallback to unrelated global SSL configuration.

Each new RPC reads the current inbound policy and revalidates its TLS peer
against the current roots/SANs, including on an existing connection. Accepted
streams continue under the policy that admitted them; messages are not replayed
or individually reauthorized. LDS absence or the last exporter leaving closes
admission to new RPCs. A retired binding remains on a still-listening port until
protocol destruction closes the server, preventing a plaintext fallback window.
A malformed update retains the last accepted policy; a valid deletion does not.

## Fault execution

`XdsFaultPolicy` and `XdsFaultFilters` implement configuration validation,
independent delay/abort decisions, header percentage caps, gRPC status mapping,
ordered filter instances, most-specific overrides and a process-wide active-fault
permit. Nine configuration/decision checks passed in the last verified baseline,
including the real captured Istio RDS. Outbound LDS validates filter instance names,
mandatory types and terminal router order.

`XdsFaultExecutor` runs after route selection and before endpoint selection,
admission and upstream retry. A local abort sends no upstream RPC and is never
retried. Delay consumes the original call budget and is applied once per logical
call; subsequent dispatch and retry keep the selected cluster while reading its
current CDS/EDS. Ordinary Dubbo business exceptions retain their response behavior.
Per-instance MetadataService applies its own wire-context policy without selecting
a different instance. Its route/HCM deadline applies even when no fault action is
active.

`XdsDeferredStream` exposes cancellation and flow control during delay, calls
`beforeStart` once, and buffers at most 128 operations per pending stream. This
is an operation limit, not a byte limit. Overflow returns RESOURCE_EXHAUSTED;
cancelled or expired waiting calls send no messages. Unary, server/client and
bidirectional streaming retain the existing transport lifecycle after dispatch.
An OK abort completes server/bidirectional streams without messages; unary and
client-streaming calls fail INTERNAL because their required response is missing.

Each fault executor permits at most 1024 registered calls and uses bounded workers.
Deadline timers release waiting resources immediately and dispatch terminal
notifications to workers, so user callbacks do not run on the deadline timer.
A terminal observer notification waits for `beforeStart` to finish; destruction
still cancels resources while that callback is blocked. The process-wide active
fault permit remains held through terminal delivery and is released exactly once.
These bounds do not establish a process-wide pending-byte limit; queue saturation,
mixed concurrency and full resource recovery remain part of M6 acceptance.


The 2026-10-11 priority follow-up fixes deterministic state-publication races:
priority aggregation reads live connectivity while notifications catch up, and
an empty READY candidate group is reconciled before failing or selecting a lower
priority. Two controlled-clock regressions failed before this change. Real socket
fixtures now keep negotiation separate from short RPC deadlines and control the
exact physical business channel; transport failure before deadline remains
UNAVAILABLE with no dispatch/admission. Seven state-machine and eight real
transport checks passed together. A historical mixed batch exposed a fixture
peer-accept timeout; the fixed physical-channel diagnostic combination passed
11 checks. The final mixed rerun passed all 56 checks in ten classes (23:29:15), and the optional native app package passed (23:29:46). The preceding 15/11-check runs overlap with this batch and are not additional unique checks. Production grace and RPC
budgets retain their existing values. Multi-priority native cluster and membership
changes were pending at that batch; the later 2026-10-12 native 42-record listener
fault/recovery and membership batch passed as recorded in the current matrix.
Concentrated timing/load and additional combinations remain pending.
