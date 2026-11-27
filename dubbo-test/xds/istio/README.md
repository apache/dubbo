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

# Istio proxyless xDS test environment

This environment validates Istio's control plane using its gRPC-Go echo application
and Dubbo's native ADS probes. Separate native Dubbo applications have passed
light checks for discovery, traffic policies, security, recovery and four in-flight
RPC forms across actual Istiod CA transitions; concentrated native acceptance remains pending.
Work is in the main Dubbo checkout, on branch `3.3`.

See the [implementation plan](../IMPLEMENTATION-PLAN.md) for the current native
Dubbo milestones and acceptance criteria.

## Installation

Validated on Kubernetes `v1.36.2-aliyun.1`, with virtual-kubelet workloads, using
Istio `1.31.1`. Install the official release's local Helm charts; its public chart
repository did not contain this version during setup. Prerequisites: kubectl,
Helm, Python 3, grpcurl, and the official Istio release directory with its
`manifests/charts` directory. The application image is pinned by digest.

```bash
bash install.sh /path/to/kubeconfig /path/to/istio-1.31.1
python3 verify-rpc.py --kubeconfig /path/to/kubeconfig --grpcurl /path/to/grpcurl --count 30
kubectl --kubeconfig /path/to/kubeconfig apply -f traffic-policy.yaml
kubectl --kubeconfig /path/to/kubeconfig apply -f strict-mtls.yaml
kubectl --kubeconfig /path/to/kubeconfig apply -f authorization-policy.yaml
```

There is one agent-only client and two agent-only providers, in `dubbo-xds-test`.
No Envoy traffic interception, gateway, or public application endpoint is used.
Port forwarding binds only to loopback and the verification script terminates it.
The providers' control port 17171 is an intentionally unsecured test utility;
security tests target business port 17070 through Service port 7070.

`traffic-policy.yaml` configures v1/v2 weights 20/80. `strict-mtls.yaml` replaces
its DestinationRule with `ISTIO_MUTUAL`, while retaining both subsets.
Applying `traffic-policy.yaml` afterwards removes that TLS policy, so always
reapply `strict-mtls.yaml` after it when maintaining the secured environment.
`authorization-policy.yaml` allows the test service account on workload port 17070.

```bash
python3 verify-rpc.py --kubeconfig /path/to/kubeconfig --count 200
python3 verify-rpc.py --kubeconfig /path/to/kubeconfig --count 1 \
  --target grpc://echo.dubbo-xds-test.svc.cluster.local:7070 \
  --expect-error 'error reading server preface'
```

For the authorization negative test, apply `deny-test-policy.yaml`, call
`verify-rpc.py --expect-error PermissionDenied`, and **delete that policy even
if verification fails**. The normal environment must not retain this DENY rule.

## Deployment contract for Dubbo

Enable injection on the namespace and use these Pod-template annotations:

```yaml
inject.istio.io/templates: grpc-agent
proxy.istio.io/config: '{"holdApplicationUntilProxyStarts": true}'
```

Istio injects an agent with `DISABLE_ENVOY=true`. It also injects
`GRPC_XDS_BOOTSTRAP=/etc/istio/proxy/grpc-bootstrap.json` into the application and
shares `/etc/istio/proxy` and `/var/lib/istio/data`. The agent owns Kubernetes
service-account authentication, its upstream connection to Istiod on port 15012,
and workload certificate issuance/rotation. Dubbo reads the bootstrap and consumes
the local ADS socket and certificates. It does not need the user's kubeconfig or
a Kubernetes API client inside the application.

The observed bootstrap contains:

| Field | Observed value / meaning |
| --- | --- |
| `xds_servers[0].server_uri` | `unix:///etc/istio/proxy/XDS` |
| `channel_creds` | `insecure` for the local Unix socket |
| `server_features` | `xds_v3` |
| `node.id`, `node.metadata` | Agent-generated workload identity; preserve them, including `GENERATOR=grpc` |
| `certificate_providers.default.plugin_name` | `file_watcher` |
| `certificate_file` | `/var/lib/istio/data/cert-chain.pem` |
| `private_key_file` | `/var/lib/istio/data/key.pem` |
| `ca_certificate_file` | `/var/lib/istio/data/root-cert.pem` |
| `refresh_interval` | `900s` |
| `server_listener_resource_name_template` | `xds.istio.io/grpc/lds/inbound/%s` |

Local `insecure` channel credentials do not select business-connection security.
That is determined separately by the xDS cluster/listener TLS contexts. The supplied
certificate provider backs those contexts. Verification checks key readability
without reading or retaining key contents. `bootstrap-observed.json` contains
only bootstrap configuration and public workload identifiers, not credentials.

## Dubbo implementation decisions

1. Implement a complete bootstrap parser. Standard discovery order is
   `GRPC_XDS_BOOTSTRAP` (file), then `GRPC_XDS_BOOTSTRAP_CONFIG` (inline JSON), as
   in gRPC-Go. An explicit Dubbo override may take precedence. Do not prioritize
   a hardcoded default path over explicitly supplied configuration. Treat invalid
   explicit configuration as an error. Allow a bounded startup wait for an
   agent-generated file/socket, followed by reconnect with backoff.
2. Support a Linux Unix Domain Socket transport for ADS, in addition to TCP,
   using a compatible Netty native transport. Ordinary TCP-only Netty connections
   cannot consume this injected bootstrap. Select the first supported configured
   channel credential; reject a server with no supported credential rather than
   silently choosing plaintext.
3. Share one ADS session per local agent/workload. Reuse it across consumers,
   providers, references, registries, and ApplicationModels in the same process.
   Put the runtime at the framework/workload scope, with subscriptions and
   reference-counted lifecycle; reject incompatible identities for the same local
   agent endpoint. Istio 1.31.1 closes an existing stream when another registers.
   The combined echo client/server reproduced overlapping-stream EOF failures;
   separate client/provider pods resolved them. Multiple SDK processes sharing
   the same agent would also need coordination; one SDK workload per Pod is the
   initial deployment contract.
4. Preserve generated Node identity/metadata. Parse the inbound listener template
   and substitute the actual server bind address/port. Use the Service FQDN and
   **Service port** for outbound discovery; use EDS' workload port for connecting.
   This test uses Service port 7070 and workload port 17070, which differ.
5. Supply shared certificate providers to Triple's client/server TLS contexts,
   refresh certificate snapshots consistently, validate peer identities/SANs,
   and preserve required TLS/authentication on missing or invalid updates.
   Implement actual listener RBAC enforcement; receiving configuration alone is
   insufficient. Unsupported mandatory security policies must not be ignored.
6. Bootstrap only discovers the control plane. References still need the explicit
   Kubernetes Service/application target, Service port, and invocation protocol.
   Discovery caches retain these dimensions. A MetadataService call can recover
   interface and protocol metadata after an instance is found; it cannot invent
   a reachable metadata port/protocol/group or bypass the service's authorization.
   The revision-free metadata entry must therefore be supplied or agreed as a
   deployment convention. Neither revision lookup nor interface-application mapping
   is required for this phase.

The `3.3-dev-xds` Bootstrapper is a useful starting point but currently returns
null after JSON parsing and has incomplete source-selection behavior. It is not
a functioning replacement for the above runtime contract.

## Observed verification, 2026-10-11

- Istiod and all three echo deployments ready; agent-only injection and app-side
  bootstrap/certificate-path access verified.
- Initial xDS discovery: 30/30 successful calls.
- 20/80 routing: 200 calls, v1=48 and v2=152 (sampling, not an exact ratio assertion).
- Reconfigured to v2 only: 30/30 v2 calls, then restored 20/80.
- STRICT mTLS plus principal ALLOW policy: 50/50 calls successful (v1=12, v2=38).
- Plaintext to business Service port: rejected with server-preface EOF.
- Authorization DENY test: 5/5 rejected with `PermissionDenied`. The temporary
  policy was removed; a further 30/30 calls succeeded (v1=7, v2=23).
- Istio analysis and all server-side dry-run admission requests passed. Local
  OpenAPI download timed out; the successful dry-run used `--validate=false`.

These checks establish a working reference environment.

The native Java `XdsProbe` has also been compiled and run in a Linux agent-only
Pod using `native-probe.yaml`: the injected bootstrap connected over the local
UDS and resolved two outbound clusters/EDS resources and an inbound listener,
with one shared runtime. The process exited with code 0; the temporary Pod and
Service were removed. This is a control-plane probe, without business RPCs.
Triple TLS/RBAC execution, certificate rotation and complete traffic-feature
acceptance remain subsequent implementation/test work.

## Sources

- [Istio proxyless gRPC architecture](https://istio.io/latest/blog/2021/proxyless-grpc/)
- [Istio 1.31.1 grpc-agent injection template](https://github.com/istio/istio/blob/1.31.1/manifests/charts/istio-control/istio-discovery/files/grpc-agent.yaml)
- [Istio agent bootstrap generation](https://github.com/istio/istio/blob/1.31.1/pkg/istio-agent/grpcxds/grpc_bootstrap.go)
- [Istio agent stream registration](https://github.com/istio/istio/blob/1.31.1/pkg/istio-agent/xds_proxy.go)
- [gRPC-Go bootstrap parser](https://github.com/grpc/grpc-go/blob/master/internal/xds/bootstrap/bootstrap.go)
- [gRFC A27: xDS and bootstrap](https://github.com/grpc/proposal/blob/master/A27-xds-global-load-balancing.md)
- [gRFC A29: xDS TLS security](https://github.com/grpc/proposal/blob/master/A29-xds-tls-security.md)


## M5 extension resource capture

`extension-policy.yaml` creates a separate `echo-policy` Service and policies,
using the existing two Go providers without replacing the baseline `echo`
policies. The fixture asks for fault, max_requests=2, LEAST_REQUEST and outlier
settings. `extension-probe.yaml` runs only `XdsResourceCapture` with its own
agent; it does not implement or prove the configured traffic behavior.

Build the module and assemble its runtime dependency jars in a local bundle,
as for the existing native probe. Do not put kubeconfig or certificate/private
key contents in the bundle. Then run:

```bash
kubectl --kubeconfig /path/to/kubeconfig apply -f extension-policy.yaml
kubectl --kubeconfig /path/to/kubeconfig apply -f extension-probe.yaml
kubectl --kubeconfig /path/to/kubeconfig -n dubbo-xds-test wait \
  --for=condition=Ready pod/native-xds-policy-probe --timeout=180s
kubectl --kubeconfig /path/to/kubeconfig -n dubbo-xds-test cp \
  /path/to/runtime-bundle/. native-xds-policy-probe:/probe/ -c app
kubectl --kubeconfig /path/to/kubeconfig -n dubbo-xds-test exec \
  native-xds-policy-probe -c app -- touch /probe/ready
kubectl --kubeconfig /path/to/kubeconfig -n dubbo-xds-test logs \
  native-xds-policy-probe -c app
```

After the capture finishes, its log must contain `XDS_CAPTURE_READY ... resources=4`
and `/probe/exit-code` must contain 0. Copy `/probe/capture` out of the application
container. It contains protobuf binaries and JSON views for LDS/RDS/CDS/EDS.
When a transitive Any descriptor is unavailable, the corresponding JSON uses
`encoding=protobuf-base64` for the entire protobuf; the `.pb` remains authoritative.
Do not mistake the diagnostic capture for resource validation or business readiness.

Always delete the temporary resources when finished, including after a failure:

```bash
kubectl --kubeconfig /path/to/kubeconfig delete \
  -f extension-probe.yaml -f extension-policy.yaml
```

The 2026-10-11 native capture succeeded through UDS with all four resources.
It observed fault in LDS/RDS, max_requests=2 and LEAST_REQUEST in CDS, two healthy
17070 endpoints in EDS, and no outlier_detection in CDS. Public textproto fixtures
and baseline parser checks live in `dubbo-xds/src/test/resources/istio-1.31.1`.
Temporary resources were removed after copying; the original Go baseline remains.


## M5 hash resource capture

`hash-policy.yaml` creates independent `echo-ring` and `echo-maglev` Services,
DestinationRules and VirtualServices using the existing two providers. Both
request header `x-user` hashing and ISTIO_MUTUAL. `hash-probe.yaml` runs the
native diagnostic collector against both Services. It only verifies control-plane
output; native Dubbo execution is covered separately by local RPC tests and
later Kubernetes acceptance.

Build the module and assemble its runtime jars. Replace the bundle's
`dubbo-xds-3.3.7-SNAPSHOT.jar` with the latest built jar before copying it;
remove stale duplicate module jars. The jar must contain `XdsResourceCapture`.
No kubeconfig or certificate/private-key contents belong in this bundle.

```bash
kubectl --kubeconfig /path/to/kubeconfig apply -f hash-policy.yaml
kubectl --kubeconfig /path/to/kubeconfig apply -f hash-probe.yaml
kubectl --kubeconfig /path/to/kubeconfig -n dubbo-xds-test wait \
  --for=condition=Ready pod/native-xds-hash-probe --timeout=180s
kubectl --kubeconfig /path/to/kubeconfig -n dubbo-xds-test cp \
  /path/to/runtime-bundle/. native-xds-hash-probe:/probe/ -c app
kubectl --kubeconfig /path/to/kubeconfig -n dubbo-xds-test exec \
  native-xds-hash-probe -c app -- touch /probe/ready
kubectl --kubeconfig /path/to/kubeconfig -n dubbo-xds-test logs \
  native-xds-hash-probe -c app
kubectl --kubeconfig /path/to/kubeconfig -n dubbo-xds-test exec \
  native-xds-hash-probe -c app -- cat /probe/exit-code
kubectl --kubeconfig /path/to/kubeconfig -n dubbo-xds-test cp \
  native-xds-hash-probe:/probe/ring /path/to/hash-capture/ring -c app
kubectl --kubeconfig /path/to/kubeconfig -n dubbo-xds-test cp \
  native-xds-hash-probe:/probe/maglev /path/to/hash-capture/maglev -c app
```

Require both `XDS_CAPTURE_READY ... resources=4` messages and exit-code 0.
Always clean up even after a failure:

```bash
kubectl --kubeconfig /path/to/kubeconfig delete \
  -f hash-probe.yaml -f hash-policy.yaml
```

The 2026-10-11 capture observed legacy RING_HASH with minimum size 1024,
MAGLEV with table size 65537, `x-user` route hash policy in both, two workload
port 17070 endpoints and file-provider mTLS/SPIFFE identities. Eight public
textproto resources are checked in under `istio-1.31.1/hash-ring` and
`istio-1.31.1/hash-maglev`. Native Triple accepts RING_HASH and explicitly NACKs
MAGLEV, preserving the prior valid graph.

The first Pod command failed because its copied bundle was stale and lacked the
collector class. After replacing the module jar, both collectors were rerun
manually in the same Pod and each exited 0 with four resources. The original
Pod exit-code file retained the earlier failure and is not success evidence for
these reruns. All seven temporary objects were deleted; an explicit Pod/Service
absence check passed. The original Go baseline remained in place.


## M5 dual-protocol resource capture

`dual-protocol-policy.yaml` creates an independent `echo-dual` Service with
Triple/grpc Service port 7070 targeting workload port 17070, and TCP/Dubbo
Service port 7071 targeting 20880. DestinationRule port settings use
ISTIO_MUTUAL for Triple and DISABLE for binary discovery. The existing Go
providers do not listen for Dubbo at 20880: this is a control-plane port-mapping
fixture, not a binary business deployment.

Build a current runtime bundle with consistent dependency versions, including
the current module jar. Apply `dual-protocol-policy.yaml` and
`dual-protocol-probe.yaml`, wait for Pod `native-xds-dual-probe`, copy the bundle
into `/probe`, then touch `/probe/ready`, as in the hash-capture steps above.
The collector's optional fourth argument selects `tri` or `dubbo`. It requires
four resources for Triple and only CDS/EDS for binary discovery.

Require both READY messages and `/probe/exit-code` 0. Copy `/probe/tri` and
`/probe/dubbo` out separately; their authoritative protobufs and JSON views
contain no certificate or private-key contents. Always clean up:

```bash
kubectl --kubeconfig /path/to/kubeconfig delete \
  -f dual-protocol-probe.yaml -f dual-protocol-policy.yaml
```

The 2026-10-11 capture completed with resources=4 and resources=2, exit-code 0.
The Triple CDS contains TLS and its two EDS endpoints use 17070; the TCP CDS
has no transport_socket and its two endpoints use 20880. Public fixtures are
checked in under `istio-1.31.1/dual-triple` and `dual-dubbo`. All three temporary
objects were removed and a final absence check passed. Actual Triple/Dubbo RPC,
metadata filtering and TLS refusal have local test evidence; native Kubernetes
business acceptance remains pending.

## Native Dubbo application assembly

The optional `xds-native-test` reactor profile builds a normal `DubboBootstrap`,
`ServiceConfig` and `ReferenceConfig` application. It does not manually construct
Directories or supply fake metadata. Run from the main repository root:

```bash
mvn -P xds-native-test -pl dubbo-test/xds -am -Dmaven.test.skip=true package
python3 dubbo-test/xds/istio/native-smoke.py \
  --kubeconfig /path/to/kubeconfig --deploy \
  --bundle "$PWD/dubbo-test/xds/target" \
  --output /path/to/native-smoke-report.json
```

The root reactor is required; a standalone `-f dubbo-test/xds/pom.xml` build
requires the parent/import BOMs and dependencies to have been installed first.
The bundle contains the executable application jar and its `lib` dependencies.
Rebuild a fresh bundle after dependency-version changes to avoid stale jars.

`native-application.yaml` owns the isolated namespace `dubbo-xds-native` and
three Pods: `echo-v1`, `echo-v2` and `echo-client`. Every Pod uses `grpc-agent`
injection, a pinned JRE image and an emptyDir runtime volume. The script waits
for Running, uploads the bundle and starts each provider before the consumer.
Readiness intentionally stays false until the application starts. This upload
assembly is a test fixture; it is not an immutable production application image.
Running `--deploy` again does not restart an already running JVM. Add
`--recreate` to replace only the three owned test Pods before uploading a
changed application bundle. The namespace and policies are retained.

The Service exposes Triple 7070 → workload 17070 and Dubbo 7071 → workload 20880.
Triple uses STRICT/ISTIO_MUTUAL and an ALLOW rule for service account `native-xds`.
The Dubbo port explicitly disables mTLS and provides discovery only. Both
references share the same interface/group/version but use different target
ports/protocols. MetadataService V1 is exported on Triple workload port 17070;
the binary reference explicitly reads metadata through Triple Service port 7070.
Both set the metadata application to `native-xds-provider`.

On 2026-10-11, five cases passed: eight Triple synchronous calls, four Triple
asynchronous calls, two Triple server-streaming calls returning six messages,
four binary synchronous calls and two binary asynchronous calls. Both v1/v2
providers returned values. The ADS diagnostic state was READY and all eight
LDS/RDS/CDS/EDS resources were ACKED. This verifies assembly and light business
execution; it does not establish a weighted distribution, negative security,
rotation, reconnect or mixed-load acceptance result.

The script retains this environment for subsequent acceptance. Run without
`--deploy` to repeat calls against the retained Pods. The original Go baseline
in `dubbo-xds-test` is unaffected. To remove the owned native test environment,
including its namespace:

```bash
kubectl --kubeconfig /path/to/kubeconfig delete \
  -f dubbo-test/xds/istio/native-application.yaml
```

## Native online policy checks

Build and deploy the current application bundle before running:

```bash
python3 dubbo-test/xds/istio/native-policy.py \
  --kubeconfig /path/to/kubeconfig \
  --output /path/to/native-policy-report.json
```

This light verifier targets only `dubbo-xds-native`. It snapshots the current
echo VirtualService and DestinationRule into the report's `.baseline.json`
companion before making changes. It switches to 100% v1 and then 100% v2,
temporarily applies a uniquely named DENY policy and checks synchronous,
asynchronous and streaming rejection, then removes the DENY and checks recovery.
It next configures RING_HASH, confirms the actual accepted CDS policy through
the consumer's shared runtime, and checks repeated calls with four header keys.
An update to MAGLEV must report NACKED_WITH_LAST_GOOD with a controlled MAGLEV
reason, keep the accepted RING_HASH resource timestamp and retain the tested
key mappings. The application diagnostics expose only the accepted cluster
policy and ring sizes, not raw TLS/certificate resources.
The size fields are raw CDS wrapper values; omitted wrappers are omitted from
the current diagnostics. Effective bounds also depend on the Reference's cap.
The initial policy-run bundle reported zero for an omitted wrapper; that value
was not an effective ring bound.

The verifier restores both baseline configurations and removes its temporary
DENY in `finally`, on success or failure. A passed report requires exact spec
restoration, ACKED/READY diagnostics and a successful business call afterwards.
If the process is terminated before cleanup, remove only the uniquely named
DENY recorded by the test and restore the saved public baseline with
`kubectl apply -f /path/to/native-policy-report.baseline.json`. Do not use this
script concurrently with other edits to the owned echo policies.

These checks cover a small set of online changes, rather than the concentrated
mixed-load, certificate rotation and reconnection acceptance matrix.
The 2026-10-11 native run passed and restored the baseline: both 100% subset
routes, all three DENY call forms, RING_HASH acceptance/stability, MAGLEV NACK
with the previous ring and tested mappings, and final ACKED/READY business
recovery. Its 16 report records include diagnostics and cleanup checks;
they are not 16 independent business test cases.

## Native deadline, retry and fault checks

Rebuild the current bundle, upgrade the three test Pods with
`native-smoke.py --deploy --recreate --bundle /path/to/target ...`, then run:

```bash
python3 dubbo-test/xds/istio/native-traffic.py \
  --kubeconfig /path/to/kubeconfig \
  --output /path/to/native-traffic-report.json
```

The verifier installs six header-selected routes and a default v1 route in the
owned echo VirtualService. The consumer reports accepted RDS deadline/retry
knobs and fault-configuration presence, rather than treating the submitted
VirtualService as execution evidence. The business fixture recognizes
`probe:<token>:<delay-ms>:<failures>`, `stats:<token>` and `reset:<token>` messages.
All verification/statistics calls use the secured Triple business path. These
are test-app behaviors; the binary port retains its existing plain discovery
test scope. Provider counters are bounded to 256 tokens and 32 recorded attempts
per token, delays to five seconds and injected failures to ten.

The checks cover a 180ms route deadline for synchronous/asynchronous/server
streaming calls, a shorter 120ms caller budget, two unary retries with attempt
headers and a decreasing wire budget, streaming without retries, a 350ms budget
spanning attempts, 100% fault abort without provider dispatch, a 250ms fault delay
consuming the caller's budget, and deadline expiry during a 500ms fault delay
without dispatch. Provider statistics record attempts, active work, cancellation
signals, cancellation while work is still running, received wire timeout and
previous-attempt headers. A normal asynchronous completion is the control for
the timing counter, bringing the default batch to 14 business scenarios.

The script backs up the original VirtualService in a public `.baseline.json`
List, resets its recorded counters, and restores/checks the original spec, ADS
and business calls in `finally`. If interrupted, restore that saved List with
`kubectl apply -f /path/to/native-traffic-report.baseline.json`; the report lists
its test tokens for any subsequent counter cleanup. Run it without concurrent edits to echo policies.
This is a light execution batch; it does not run the mixed-load or full lifecycle
acceptance suite.
Use repeated `--case` options for a targeted rerun, such as
`--case deadline-async`. The report records the selected scope. Each business
observation is saved with `verified=false` before assertions and changes to true
only after all client/provider checks pass; failed observations are retained.
The consumer unwraps standard Future exception wrappers before reporting the
underlying RPC status, while recording exception class names without messages.

On 2026-10-11, the 13-scenario main batch passed after correcting the test
consumer's Future exception reporting. Its two initial failures and restored
baselines remain recorded. A subsequent five-scenario targeted batch passed
with stronger cancellation timing observations: normal completion counted zero
notifications while working, and each of the four deadline variants counted one.
The batches cover 14 distinct business scenarios, with four overlapping cases.
Both restored the exact original VirtualService, ACKED/READY diagnostics and
business calls. This does not establish the full concentrated acceptance result.

## Native concurrent request admission

Use the current application bundle, then run this verifier without concurrent
edits to the owned echo policies:

```bash
python3 dubbo-test/xds/istio/native-admission.py \
  --kubeconfig /path/to/kubeconfig \
  --output /path/to/native-admission-report.json
```

It routes calls to v1 with three retries configured and sets the Triple port's
`http2MaxRequests` to two. It waits for the actual accepted RDS/CDS values, starts
two delayed asynchronous business calls, and observes the SDK's shared active
counter. A third call must return the typed local `DropException` immediately,
increment the drop counter once and dispatch no provider request. Both admitted
calls must complete, release their permits and allow a recovery call.

The application reads the real SDK counters; it does not acquire synthetic
permits. Drop observations are sampled while the two admitted calls are active,
because the current counter is removed when active requests reach zero. They
are not cumulative workload statistics or an LRS report.

The report saves observations before assertions. Cleanup resets the recorded
provider tokens and restores both original DestinationRule and VirtualService
specs, then checks ADS and a business call. If interrupted, restore its saved
public `.baseline.json` List with `kubectl apply -f /path/to/baseline.json`.
This checks one small saturation/completion scenario; cancellation, streaming,
shared-reference load and the concentrated lifecycle suite have separate scope.

The 2026-10-11 native run initially failed because the readiness/fault gate
converted a typed local drop into an ordinary status exception, causing three
retries. After preserving the unary dispatch exception, 32 related local checks
passed and the native rerun passed: one local drop in about 1ms, no overflow
dispatch, two admitted completions and a successful recovery call, with zero
active requests afterwards. Both original policy specs, ADS and business calls
were restored. The initial failed/restored report remains separate from the
passed rerun; the five report records include setup and cleanup observations.

## Native security negative checks

Build the current application bundle, retain the normal native test environment,
and run:

```bash
python3 dubbo-test/xds/istio/native-security.py \
  --kubeconfig /path/to/kubeconfig \
  --bundle /path/to/dubbo-test/xds/target \
  --output /path/to/native-security-report.json
```

The verifier creates two temporary grpc-agent Pods: one uses the allowed
`native-xds` ServiceAccount, the other uses a new ServiceAccount outside the
provider's ALLOW rule. The Java probe reads actual LDS/RDS/CDS/EDS and credentials
from its injected agent. It selects the v1 workload endpoint and directly calls
the normal Triple business interface and MetadataService V1 with consumer
filters disabled. Direct calls require a ready TLS connection first, so a
missing Directory address cannot be mistaken for an RBAC denial.

The allowed control must return a business response and nonempty metadata; the
wrong ServiceAccount must receive PERMISSION_DENIED on both interfaces. Each
probe also checks a successful TLS 1.2/h2 handshake, rejection without a client
certificate, and rejection by the native trust manager with a deliberately
incorrect expected SPIFFE SAN. This last check uses a test-only SAN predicate
against the actual server certificate; it does not change Istio's CDS or the
server certificate. The same protected workload port must close HTTP/2 and
HTTP/1 plaintext connections and a binary Dubbo heartbeat without a response.
HTTP/2 includes its initial SETTINGS frame. The binary heartbeat is encoded
with Dubbo's Hessian2 serialization and must first receive a Dubbo response
magic on the same provider's open binary fixture port 20880. This positive
control distinguishes a usable binary request from a malformed header.

Credentials stay in the injected Pods. Reports include statuses, exception class
names and a controlled SAN-mismatch flag, without key/certificate contents.
The test does not modify echo policies or restart the three business Pods. Its
`finally` cleanup deletes only resources with the matching run label, waits for
their deletion, checks all four original echo policy specs, and verifies normal
ADS/business calls. If interrupted, the report lists its temporary Pod and
ServiceAccount names and `dubbo-xds-test-run` label value for cleanup.
Do not run alongside edits to the echo policies. These are small transport and
principal negatives; JWT/CUSTOM and certificate rotation have separate scope.
Use `--identity allowed` or `--identity denied` for a targeted run. The report
records the selected scope; a single-identity pass does not replace both
identity checks. The application publishes its completed observation by renaming
a temporary report file before the verifier reads it.

On 2026-10-11, both identity probes passed, their temporary resources were
removed, and all four echo policy specs plus ADS/business calls were checked.
The initial binary check used only a header. A subsequent allowed-identity run
passed with the complete HTTP/2 SETTINGS frame and the valid binary heartbeat
control: the open binary port responded, while the protected Triple port closed
the same request. This targeted run also repeated the allowed business/metadata,
no-certificate, SAN mismatch and HTTP/1 checks. Both reports retain their scopes;
their six and five records include setup/cleanup, not independent business cases.

## Service endpoint withdrawal and discovery isolation

Run the light verifier against the current native SDK bundle:

```sh
python3 dubbo-test/xds/istio/native-withdrawal.py \
  --kubectl /opt/homebrew/bin/kubectl \
  --kubeconfig /Users/ken/.kube/dubbo-xds-test.yaml \
  --output /Users/ken/.local/share/dubbo-xds-tools/native-withdrawal-report.json
```

It first routes Triple exclusively to v1 and requires binary discovery to retain
both providers through their independent metadata addresses. It then adds an
owned temporary Service selector label, removes v1 from that selector, and
checks accepted empty/absent EDS, Triple unavailability, binary calls to v2,
and v1 recovery. The security selector labels remain unchanged. Provider
counters verify that the new call after withdrawal never reached v1.

The verifier saves the original route/Service selector, checks resource UIDs and
label ownership before mutation, and restores the selector, provider labels,
route, discovery and business calls in `finally`. A failed run retains its
failure observation even when cleanup restores healthy diagnostics. Check both
`status` and `restored`; a restored baseline does not make a failed test pass.
This cluster test covers discovery and new calls. Separate local real-RPC tests
cover completion of an already dispatched unary call and stream after withdrawal;
native in-flight and mixed-load acceptance remain pending.

On 2026-10-11, the final light run passed and restored the baseline, with twelve
records including setup and cleanup. Triple-only v1 routing retained both
binary providers; accepted empty v1 EDS removed Triple addresses while binary
calls went to v2. A new Triple call returned local `UNAVAILABLE`, its provider
token had zero attempts, and v1 calls recovered after restoration. Earlier
reports retain the two discovered SDK issues and a kubectl upload connection
reset; their restored baselines do not count as successful test runs.

## ADS and native HTTP/2 reconnect

After updating the consumer with the current optional test-application bundle:

```sh
python3 dubbo-test/xds/istio/native-reconnect.py \
  --kubectl /opt/homebrew/bin/kubectl \
  --kubeconfig /Users/ken/.kube/dubbo-xds-test.yaml \
  --output /Users/ken/.local/share/dubbo-xds-tools/native-reconnect-report.json
```

The test application closes its actual SDK ADS transport, waits for a replacement
and checks that subscriptions stay on one shared runtime. Only this optional
test application uses reflection to reach the private ADS transport; the SDK
has no public reset hook. A fresh named v1 RDS route then proves that subsequent
control-plane updates reach the recovered client.

The next step closes actual v1 native HTTP/2 channels and observes the same
connection clients recover through native backoff. It records state transitions,
new channel identity, and TLS/h2 negotiation. The SHA-256 values come only from
public leaf certificates in the TLS session, never private-key material.
Sync, async and server-streaming calls must recover; one stream returns the
fixture's three messages. Binary calls also remain available.

The verifier compares the SDK process start time and Pod UID, and retains the
same references during both injections. It snapshots four echo policies and
checks their UIDs/specs on restoration, with ownership checks before changing
or restoring the temporary route. Failed observations are preserved separately.
This is a light SDK-to-agent ADS/native channel check. Agent-to-Istiod failure,
certificate rotation, in-flight disruption and mixed load remain separate
acceptance cases.

The 2026-10-11 fixed run (`m6-native-reconnect-fixed-report.json`) passed all
13 baseline/action/restoration records with `restored=true`. ADS recovered in
231ms and the native business connection in 1028ms; the same SDK process and
consumer Pod were retained. The first run failed because the verifier expected
one response from a three-message stream fixture; its failed/restored report is
retained, and the corrected run used the same deployed bundle.

## Native workload certificate rotation

Build the current optional native test bundle, then run:

```sh
python3 dubbo-test/xds/istio/native-rotation.py \
  --kubectl /opt/homebrew/bin/kubectl \
  --kubeconfig /Users/ken/.kube/dubbo-xds-test.yaml \
  --bundle dubbo-test/xds/target \
  --output /Users/ken/.local/share/dubbo-xds-tools/native-rotation-report.json
```

The verifier creates a unique Service, provider/consumer Pods and narrowly
selected STRICT/ALLOW/ISTIO_MUTUAL policies in the existing native namespace.
The existing `native-xds` ServiceAccount is reused. The original echo policies
are retained and checked by UID/spec; cleanup removes only resources carrying
the run's owner label. Wait for both Running and successful bundle upload before
expecting the application readiness files.

Only these new Pods request `SECRET_TTL=6m`, `SECRET_GRACE_PERIOD_RATIO=0.5`
and zero ratio jitter from pilot-agent. Istio 1.31.1 backdates CSR certificate
NotBefore by two minutes, so the public validity span is eight minutes even
though the requested remaining lifetime at issuance is six minutes; see the
[Istio signing implementation](https://github.com/istio/istio/blob/1.31.1/security/pkg/pki/util/generate_cert.go).
See the official
[pilot-agent environment reference](https://istio.io/latest/docs/reference/commands/pilot-agent/#envvars).
A test launcher copies the actual injected bootstrap inside the Pod, changes only
file_watcher `refresh_interval` to `1s`, and sets `GRPC_XDS_BOOTSTRAP` for the
subsequent application JVM. Node, ADS endpoint and certificate paths are retained;
the original agent bootstrap is never overwritten. The tuning report records
the actual original and test intervals. Production defaults are unchanged.

The baseline and renewal observations contain only public certificate DER
SHA-256 values, validity, URI SANs, SDK security epochs and native TLS session
information. No private-key contents or digests are recorded. Passing requires
both agent leaf certificates to change with the same workload identity, both
SDK epochs to advance, and a ready native h2/mTLS session using the renewed
client and server certificates. New native channel identities, unchanged JVM
start times/Pod UIDs and zero restarts are checked. Sync, async, server-streaming
and independent binary calls are checked after renewal.

This case observes workload leaf renewal and native SDK consumption. CA/root
rotation, uninterrupted in-flight streams and mixed load have separate acceptance
requirements. A timeout or a cleaned baseline alone is not a passing result.

The 2026-10-11 corrected run (`m6-native-rotation-fixed-report.json`) passed
all 14 baseline/action/cleanup records with `cleaned_up=true`. Both leaf
fingerprints changed, both SDK security epochs advanced from 0 to 1, and a new
native TLSv1.3/h2 session used both renewed certificates. Neither JVM/Pod
restarted; subsequent sync, async, server-streaming and binary calls succeeded.
The two Pods, Service and four policies were removed, with original echo policy
UID/spec and ADS/business checks passing afterward. Five polling observations
were retained. The first run failed because its verifier treated the requested
TTL as the full validity span without Istio's two-minute backdating; its failed
report and successful cleanup are retained, and the fixed run used the same
application bundle. This result does not cover CA/root rotation or in-flight
continuity.

## Agent upstream loss and Service resource deletion

Build the current optional native bundle and run:

```sh
python3 dubbo-test/xds/istio/native-control-plane.py \
  --kubectl /opt/homebrew/bin/kubectl \
  --kubeconfig /Users/ken/.kube/dubbo-xds-test.yaml \
  --bundle dubbo-test/xds/target \
  --output /Users/ken/.local/share/dubbo-xds-tools/native-control-plane-report.json
```

`--case reconnect` or `--case deletion` selects a targeted follow-up; the default
is both and the report records its selected scope. The verifier creates a unique
Service/DR/VS selecting the existing echo providers, a consumer Pod and a public
source ConfigMap. Original provider labels, Pods and echo policies are retained.
The existing STRICT/ALLOW provider policy covers the new Service's workload
ports; the new consumer uses the allowed native-xds ServiceAccount.

The consumer Pod's extra Java relay listens only on loopback. Only its agent's
discovery address is redirected to this relay; `ISTIOD_SAN` preserves the original
Istiod server name and the CA endpoint stays direct. See the Istio 1.31.1
[agent SAN option](https://github.com/istio/istio/blob/1.31.1/pilot/cmd/pilot-agent/options/options.go)
and [upstream TLS configuration](https://github.com/istio/istio/blob/1.31.1/pkg/istio-agent/xds_proxy.go).
The relay forwards opaque TLS bytes, with no termination, credential inspection,
protocol rewriting or TLS verification bypass. The SDK reads the unchanged
agent-generated bootstrap and continues using its normal ADS UDS endpoint.

The reconnect case requires an actual active upstream connection before fault
injection. A shared flag closes it and rejects subsequent connections; public
relay counters prove closure and absence of a bypass. A new owned v2 route is
applied while disconnected: the consumer must retain its earlier v1 RDS and
continue calling v1. Removing the flag must establish a new upstream connection,
accept the fresh RDS and route sync/async/server-streaming calls to v2.

The deletion case deletes only the owned Service, waits for real CDS `ABSENT`
and zero discovered callable instances, and checks new calls stop using cached
providers. Recreating the Service with a new UID must restore discovery on the
same live references. The final checks retain one SDK runtime, JVM/Pod identities
and zero restarts. Cleanup validates run ownership/resource UIDs, removes the
new resources and rechecks original echo policy/Pod identities and business.
Failed observations and cleanup failures remain distinct from passing runs.
This is a light upstream-loss/CDS-deletion check; mixed load and all resource-type
deletion/in-flight boundaries require separate acceptance evidence.

Binary discovery retains the ordinary Dubbo Cluster and retry policy. A native
xDS binary Directory with no addresses reports a network/unavailable failure
from the listing stage. It does not classify that absence as provider
authorization denial, and provider RPC failures are outside this translation.
Ordinary service discovery and Triple's existing Cluster-level translation keep
their prior behavior. The local scope and real dual-protocol RPC regressions
cover this boundary.

The final 2026-10-11 run (`m6-native-control-plane-binary-fixed-report.json`)
passed all 22 baseline/action/cleanup records with `cleaned_up=true`. The relay
closed one actual upstream connection, rejected seven attempts, then established
a second connection after unblocking. Cached v1 calls continued during BACKOFF;
fresh v2 sync/async/server-streaming calls succeeded after recovery. Service
deletion produced LDS/CDS ABSENT and zero instances; new Triple/binary calls both
reported UNAVAILABLE without values. Service recreation restored the same live
references to one Triple/two binary instances, with successful calls and unchanged
JVM/Pod/relay identities, zero restarts and one shared runtime. Owned resources
were removed; original echo policies/Pods and ADS/business checks passed.

The first run failed because the verifier checked the outer status instead of
the fixture's RPC_ERROR/errors envelope. The corrected second run exposed a
real empty-binary Directory misclassification as PERMISSION_DENIED. Both failed
reports and successful cleanups are retained. The final run used the repaired
core jar, whose SHA-256 matched the reactor artifact; three local scope checks
and four dual-protocol real RPC checks passed before repackaging. These failures
and follow-ups are recorded separately from the final passing run.


### Pilot-agent container restart

Use the current native bundle with the isolated restart verifier:

```sh
python3 dubbo-test/xds/istio/native-agent-restart.py \
  --kubectl /opt/homebrew/bin/kubectl \
  --kubeconfig /Users/ken/.kube/dubbo-xds-test.yaml \
  --bundle dubbo-test/xds/target \
  --output /Users/ken/.local/share/dubbo-xds-tools/native-agent-restart-report.json
```

It creates one owned Service/DR/VS and consumer Pod selecting the retained echo
providers. The new Pod uses `restartPolicy: Always`; the original echo Pods remain
unchanged. Before signalling, the verifier checks owner label/Pod UID and confirms
that PID 1 in `istio-proxy` is `pilot-agent`. It sends SIGTERM only to that process
and requires exactly one agent restart, a new agent container ID, and unchanged
app container ID with zero app restarts. It does not close the SDK ADS channel
through a test hook or delete/recreate the consumer Pod.

Test diagnostics retain the first bootstrap used to inspect the running SDK.
The agent can rewrite its bootstrap on restart; reading that new file on every
inspect and acquiring the existing endpoint can trigger the pool's intentional
configuration-conflict guard. The verifier compares the rewritten documents only
in memory and records changed field paths, without saving their contents.
Production runtime ownership and conflict checks retain their behavior.

After Kubernetes restarts the agent, the same consumer must retain its JVM start
time, one shared runtime and resource subscription keys, resume its original v1
route, and accept a uniquely named new v2 route. Sync/async/server-stream calls
then verify v2 and binary discovery remains usable. The consumer initializes its
References once before its command loop. Result files remain until Pod cleanup;
transient exec reset/EOF is observed using the same command UUID without RPC
redispatch. Ownership/UID checks protect cleanup, followed by original echo
policy/Pod and business verification. This is a light consumer-agent restart
check; provider-agent restart, in-flight security changes, root rotation and
mixed-load acceptance retain separate scope.

The 2026-10-12 run `m6-native-agent-restart-fixed-report.json` passed all 19
baseline/action/cleanup records (`cleaned_up=true`). Only the agent restarted once;
the app restarted zero times, kept its container ID and JVM start time, and used
one runtime. Original v1 calls resumed, the new v2 RDS was accepted and actual
sync/async/server-streaming calls used v2, with binary calls also successful. Four
owned resources were removed; original echo policies/Pods and ADS/business passed.

The first run failed at diagnostic convergence and completed cleanup. Comparison
in the passing run found 25 changed paths only under Node metadata annotations
and labels added by the cloud platform; Node ID, server configuration and
certificate-provider configuration were unchanged. The old diagnostic re-read
the new file and hit the pool's bootstrap conflict guard. The passing run retains
the first diagnostic configuration; runtime conflict checks remain enforced.
This original run verifies existing References. A subsequent implementation pins
the process's first successfully loaded default bootstrap in production, matching
grpc-java's default accessor. Normal Registry initialization and test diagnostics
now use that same accessor; explicit bootstrap overrides still read/validate their
own source and conflicting endpoint configurations still fail.

Add `--new-registry` to exercise creation of a second FrameworkModel, ApplicationModel,
normal Registry and Triple/Binary References **after** the actual agent restart.
The new References wait for their own metadata discovery, execute real calls,
and then their model is destroyed. The verifier requires one shared runtime both
during and after that model and checks original sync/async/server-stream References
still work. The process default remains fixed until JVM exit, even after all leases
close; certificate-file contents continue to refresh independently. Bootstrap
configuration changes require an SDK process restart. Final run results are recorded
in the implementation plan.

### In-flight requests during endpoint withdrawal

Build the optional application bundle, then run:

```sh
python3 dubbo-test/xds/istio/native-inflight.py \
  --kubeconfig /Users/ken/.kube/dubbo-xds-test.yaml \
  --bundle dubbo-test/xds/target \
  --output /Users/ken/.local/share/dubbo-xds-tools/m6-native-inflight-report.json
```

The verifier creates an isolated Service, provider/consumer Pods and four
STRICT/ALLOW/routing policies using the injected grpc-agent bootstrap and normal
Dubbo APIs. Provider token files prove both requests were dispatched; one server
stream frame must reach the consumer before removing the Service selector.
After accepted empty EDS, new Triple and binary calls must fail UNAVAILABLE while
the original async unary and server stream remain active. Releasing each request
must produce the expected ordered responses and exactly one successful terminal
event. Admissions must stay at two during withdrawal and return to zero after
completion. Discovery and business recover after restoring the selector without
restarting the consumer or its Pods. Run labels and UIDs guard cleanup; retained
echo policy/Pod identities and business are checked afterward. This does not cover
client streams, bidi, CA rotation, agent restarts or mixed load.

The first two native runs failed and removed all owned resources. The detailed
second observation showed unary UNAVAILABLE after about 77 seconds and stream
CANCELLED after about 87 seconds while their providers were still ACTIVE; the
180-second fixture bound had not elapsed. The connection hold fix adds one
reference per native Triple call until completion/cancellation/allocation failure.
Ordinary Triple connection ownership keeps its existing behavior. Seven core
lifecycle checks and two all-address-withdrawal real-RPC checks passed together;
the rebuilt native run (`m6-native-inflight-lease-report.json`) passed all 26
baseline/action/cleanup records with `cleaned_up=true`. Both new protocols failed
UNAVAILABLE while the original async unary and server stream remained ACTIVE.
Release then yielded one unary response and three ordered stream responses,
exactly one successful terminal per call and zero remaining admissions. Selector
restoration recovered both protocols on the same JVM/Pods with zero restarts.

### Actual client-streaming and bidirectional in-flight calls

Add `--all-streams` to the owned in-flight fixture:

```sh
python3 dubbo-test/xds/istio/native-inflight.py \
  --kubeconfig /Users/ken/.kube/dubbo-xds-test.yaml \
  --bundle dubbo-test/xds/target --case withdrawal --all-streams \
  --output /Users/ken/.local/share/dubbo-xds-tools/m6-native-input-stream-withdrawal-report.json
```

The flag enables the separate `native_streams.proto` service in both owned Pods.
`Collect` is an IDL CLIENT_STREAM RPC; `Chat` is an IDL BI_STREAM RPC, with normal
Triple stubs, discovery and per-instance MetadataService V1. Sending one response
from a bidi method is not used as a substitute for client-streaming behavior.
Before the policy change the provider records one input message, and bidi also
returns its first response. After the change the same request writer sends a
second message and half-closes; actual provider observations must show both
messages and half-close while responses remain active. Four request admissions
must remain held until response completion. New input streams must fail exactly
once and cannot create provider dispatch files. Completion, ordered responses,
zero admissions, post-restoration calls, unchanged JVM/Pod identities and owned
cleanup are checked. `--case rbac`, `provider-agent` and `root` accept the same
flag; only runs with terminal evidence are marked verified in the plan.

The first `--case withdrawal --all-streams` native run
(`m6-native-input-stream-withdrawal-report.json`) passed 47
baseline/action/recovery/cleanup records with `cleaned_up=true`. The consumer
accepted empty EDS for Triple and empty discovery for both protocols; new input
streams failed UNAVAILABLE once without provider dispatch. Original client and
bidi writers sent their second message and half-close after withdrawal; their
providers remained active. Client-streaming returned one collected response,
bidi returned two ordered responses, and all four calls ended successfully once
with one provider dispatch each and no cancellation. Admissions remained four
through half-close, then returned to zero. Both input types recovered alongside
ordinary Triple/binary calls on the same JVM/Pods, with one shared runtime and
zero container restarts. Seven owned resources were removed and the retained
echo Pod/policy/ADS/business baseline passed. The 47 records include the previous
withdrawal baseline and are not added to it as distinct checks.

For subsequent `--all-streams` runs the verifier publishes all four release files
in one exec before observing completions sequentially. This avoids delaying the
later calls' release with observation overhead; the 180-second test RPC/provider
budget and terminal/admission conditions are unchanged. The default two-call
fixture retains its release order. RBAC/provider-agent/root combinations require
their own terminal evidence.

The `--case rbac --all-streams` run
(`m6-native-input-stream-rbac-context-report.json`) passed 54 records with
`cleaned_up=true`. New business calls and both input stream types were denied
before dispatch; all four existing calls remained active. The same input writers
sent a second message and half-closed after DENY, admissions stayed at four,
then each call completed successfully once with one provider dispatch and no
cancellation. Admissions returned to zero; removing DENY restored ordinary and
input-stream calls. The same JVM/Pods and one runtime were retained, with zero
container restarts. Eight owned resources were removed and the retained echo
baseline passed. These records include the earlier two-call RBAC baseline;
they are not added to it as distinct checks.

The first four-call RBAC run failed: denial of a new input stream cancelled the
previous POJO async unary. A local real-RPC regression reproduced the shared
cancellation context. HTTP/2 server listeners now own a context per request,
shared by that request's unary/streaming response observers. Both POJO and IDL
isolation regressions, existing wire/security/deadline checks (14 total), and the
rebuilt native run passed. The failed report remains separate. Client snapshot
elapsed time reflects observation time; provider release records establish that
all four calls completed within the unchanged 180-second fixture budget.
The four-call full root-rollover result is recorded below; Istiod signing-CA rotation remains separate.

The `--case provider-agent --all-streams` run
(`m6-native-input-stream-provider-agent-report.json`) passed 58 records with
`cleaned_up=true`. Guarded SIGTERM restarted only the owned provider's
pilot-agent container once. Its application container identity, JVM and Pod
remained unchanged. All four dispatched calls stayed active; both input writers
continued a second message and half-close, four admissions remained held, then
all calls completed once on the original provider without cancellation and
released admissions. The restarted agent accepted a fresh business DENY: new
ordinary and actual IDL input-stream calls failed PERMISSION_DENIED without input
provider dispatch. Removing DENY restored all forms, with one shared runtime
and zero final admissions. Eight owned resources were removed and the retained
echo baseline passed. The 58 records include the earlier two-call restart
baseline; they are not added to it as 94 distinct checks. This run used app hash 746239c1...;
the subsequent diagnostic-field build has its own hash and awaits native use.

For SDK root rollover, run `--case root --all-streams --retired-peer` with the
current app bundle. Four A-authorized requests remain held through overlapping
A+B roots, migration of both SDK identities to B, and B-only trust. After final
trust is accepted, the same input writers send their second message and
half-close; only then are all four providers released. The default two-call
root fixture keeps its earlier overlap-then-release sequence.

`m6-native-all-streams-complete-root-report.json` passed 54 records with
`cleaned_up=true`. Both epochs advanced 0→1→2→3, roots were 1→2→2→1, and each
phase had a new matching READY TLSv1.3/h2 session. The four original calls stayed
active with four admissions under B-only trust, continued their input and
half-close, and completed successfully once with one dispatch and no cancellation.
Actual first-terminal durations were approximately 173/168/164/159 seconds,
within the unchanged 180-second bound; admissions returned to zero. A pinned
old-A TLS connection executed business and metadata under A+B, then both new
calls were PERMISSION_DENIED on the same channel under B-only trust, without
business dispatch. Its own transport closed to zero open channels, while the
main B SDK and every ordinary/input-stream form recovered. JVM/Pods and all
container restart counts stayed unchanged. Seven owned resources were removed
and the retained echo baseline passed. These 54 records include the earlier
38-record root baseline; they are not added as 92 distinct checks.

This run used app hash d7a018b7... and verified the new accepted EDS locality
summaries and actual terminal timing fields. Its case names retain the old
`root-overlap` label, but the final-root/retired-peer records precede all
still-active/input/completion records. Subsequent four-call runs use
`root-rollover` labels; execution and assertions are unchanged. Root inputs
remain isolated SDK test CAs with real Istio ADS; cluster signing-CA rotation
has its own remaining acceptance scope.

Generated Java sources are checked in beside the optional app, matching the
existing xDS IDL fixture convention. Regenerate `native_streams.proto` with the
Dubbo Triple compiler and the BOM protobuf version, then format/import the stub
sources before building. The production xDS module does not depend on this service.

### In-flight requests during RBAC updates

Use the same optional bundle and isolated fixture with `--case rbac`:

```sh
python3 dubbo-test/xds/istio/native-inflight.py \
  --kubeconfig /Users/ken/.kube/dubbo-xds-test.yaml \
  --bundle dubbo-test/xds/target --case rbac \
  --output /Users/ken/.local/share/dubbo-xds-tools/m6-native-inflight-rbac-report.json
```

Both held calls must be active in the provider and the first server-stream frame
must have reached the consumer before applying DENY. The temporary policy matches
only `/org.apache.dubbo.xds.test.NativeEcho/*` on workload port 17070; MetadataService
and binary discovery remain available. An actual new RPC returning PERMISSION_DENIED
is the policy-update barrier. Sync, async and server-stream calls must be denied;
a new held unary must terminate once without creating its provider dispatch file.
The previously authorized unary/server stream must remain active with two admissions,
then finish successfully when explicitly released while DENY still applies. Removing
only the owned DENY must restore new calls without restarting either Pod or the JVM.
Command results survive transient kubectl exec reset/EOF and are observed by the
same UUID until fixture cleanup. The withdrawal case remains the default. This
fixture covers initial-header authorization; it does not establish CA rotation,
JWT/CUSTOM, client-stream/bidi or mixed-load behavior. Results are recorded in the
implementation plan after the native process reaches its terminal state.

The first RBAC run (`m6-native-inflight-rbac-report.json`) exited successfully:
32 baseline/action/cleanup records, `status=passed`, `cleaned_up=true`. Both original
calls remained ACTIVE after the real denial barrier and completed once with one
unary response and three ordered stream frames. The newly denied held call had one
PERMISSION_DENIED terminal and no provider hold file; both discovery targets stayed
available and binary calls continued. Admissions went from two to zero, removal of
DENY restored all three Triple forms, and eight owned resources were removed. Both
fixture Pods had zero container restarts and the consumer JVM/runtime stayed the
same. Retained echo policy specs/UIDs, Pod UIDs, ADS and business were verified.

### Provider agent restart with in-flight requests

Run the isolated in-flight fixture with `--case provider-agent`:

```sh
python3 dubbo-test/xds/istio/native-inflight.py \
  --kubeconfig /Users/ken/.kube/dubbo-xds-test.yaml \
  --bundle dubbo-test/xds/target --case provider-agent \
  --output /Users/ken/.local/share/dubbo-xds-tools/m6-native-provider-agent-report.json
```

Only the owned provider Pod uses `restartPolicy=Always`; the consumer and retained
echo Pods are unchanged. After provider dispatch and the first server-stream frame,
the verifier checks ownership/UID and pilot-agent PID 1 before sending SIGTERM to
the provider's istio-proxy container. The agent must have a new container ID, one
restart and READY status while the application container retains its ID with zero
restarts. Both dispatched calls must remain active, then complete once when released,
and admissions must return to zero. Business HTTP/2 connections are kept open.
After completion, a fresh business-only DENY must reject new RPCs before dispatch,
proving that the provider's inbound LDS subscription recovers through the restarted
agent. Removing this policy must restore Triple sync/async/server-stream and binary
calls with the same consumer JVM/runtime and both Pod UIDs. These are sequential
restart/in-flight and post-restart-policy checks, each with a bounded RPC budget;
they do not establish provider JVM restart, CA rotation or mixed-load behavior.
The first provider run (`m6-native-provider-agent-report.json`) passed with 36
baseline/action/cleanup records and `cleaned_up=true`. The provider agent changed
container ID and restarted exactly once; its app container kept its ID with zero
restarts. Held unary/server-stream calls stayed ACTIVE after restart, then each
completed successfully once on its original provider (one response/three ordered
frames) and admissions returned to zero. A new DENY rejected all three Triple
forms and a newly held unary before dispatch; removal restored calls. Consumer
JVM/runtime and both Pod UIDs stayed unchanged, eight owned resources were removed,
and retained echo policies/Pods, ADS and business were verified. Detailed terminal
results are recorded in the implementation plan.

### SDK root rollover with real Istio xDS

Build the current optional bundle and run the isolated fixture with `--case root`:

```sh
python3 dubbo-test/xds/istio/native-inflight.py \
  --kubeconfig /Users/ken/.kube/dubbo-xds-test.yaml \
  --bundle dubbo-test/xds/target --case root \
  --output /Users/ken/.local/share/dubbo-xds-tools/m6-native-root-rollover-report.json
```

The test generates two independent CAs and provider/consumer identities locally.
CA signing keys stay in a private temporary directory; only workload identities
and public trust bundles are uploaded to owned Pods. The test bootstrap copy
preserves the injected Node/ADS configuration, points `file_watcher` at Pod-local
test files and uses one-second polling. Agent credentials and the cluster's
signing CA are unchanged. This exercises SDK input rotation with real Istio
LDS/RDS/CDS/EDS and STRICT/ALLOW business/metadata enforcement.

The stages are A identity/A trust, A identity/A+B trust, B identity/A+B trust,
and B identity/B trust. Held async unary/server-stream calls cross the overlap
update and must complete once with admissions released. Each phase requires
matching public leaf/root fingerprints, growing SDK security epochs for both
roles and a new ready mTLS/h2 channel using the expected local/peer identities;
the verifier does not force business reconnects. Triple calls continue after
identity migration and after A is removed, with unchanged SDK processes and Pods.
Root fingerprints are public certificate hashes; private contents and key hashes
are never reported. Local real-mTLS/Triple checks cover retired-root rejection,
including new business/metadata RPCs on an established A connection. This native
fixture does not replace that negative proof or establish rotation of Istiod's
own signing CA. The first native run (`m6-native-root-rollover-report.json`) passed
with 33 baseline/action/cleanup records and `cleaned_up=true`. Both SDK epochs
progressed 0→1→2→3, trust-bundle sizes were 1→2→2→1 and every phase used a newly
observed ready h2 channel with the expected local/peer certificates. Held calls
crossed the overlap update and completed once with zero remaining admissions.
Triple forms and binary calls passed after migration and retirement; both JVMs
and Pod UIDs stayed unchanged with zero container restarts. Seven owned resources
and all locally generated signing keys were removed, and retained echo policies,
Pods, ADS and business were verified. Detailed evidence is in the implementation plan.

Add `--retired-peer` to the root command to exercise native rejection of a peer
that retains the original A endpoint/credentials in a separate direct-call
FrameworkModel. The peer executes business and MetadataService RPCs before
rotation and again after the normal SDK References migrate to B with A+B trust.
After both trust bundles become B-only, its new async business and metadata calls
must receive PERMISSION_DENIED with no values on the same ready TLS/h2 Channel IDs
and original A local/peer certificates. A provider claim/hold file must not exist
for the denied business token. Closing the peer must close its own transports,
while normal B References and the one shared xDS runtime continue to work.
The pin barrier explicitly awaits TLS handshake/ALPN; Invoker availability alone
does not establish an initialized TLS session. This uses test CAs with actual
Istio xDS, while the cluster's signing CA stays unchanged.

The first native attempt (`m6-native-retired-root-report.json`) failed its pre-call
TLS baseline: calls succeeded, but the initial snapshot raced with handshake
completion. It removed all seven owned resources. After adding an explicit TLS/h2
pin barrier and rebuilding, `m6-native-retired-root-ready-report.json` passed 38
baseline/action/cleanup records with `cleaned_up=true`. The pinned peer's Channel ID
and original A certificate hashes remained unchanged and READY through both checks.
Business and metadata succeeded with A+B, then both returned PERMISSION_DENIED with
no values under B-only roots; the business token had neither claim nor hold files.
Peer close reported one retired channel and zero remaining open channels. Normal
B References still worked with one xDS runtime, unchanged JVMs/Pods and zero
container restarts. Seven owned resources and generated signing keys were removed;
retained echo policies, Pods, ADS and business were verified. This 38-record run
repeats the base rollover scenario and must not be added to its previous 33 records
as 71 distinct checks. Detailed evidence is in the implementation plan.
Independent ownership inventory found no remaining temporary resources. The
retained echo policies, Pod identities and business remained intact. Deliberate
holds and kubectl observation time are not SDK latency measurements. Failed
reports are retained.


### Cross-Service metadata and workload identity

Build the optional application bundle, then run:

```sh
python3 dubbo-test/xds/istio/native-cross-service.py \
  --kubeconfig /Users/ken/.kube/dubbo-xds-test.yaml \
  --bundle dubbo-test/xds/target \
  --output /Users/ken/.local/share/dubbo-xds-tools/m6-native-cross-service-report.json
```

The verifier creates 13 isolated resources: two Services with independent
business/metadata ports, two STRICT/ALLOW/DestinationRule sets, one entry
VirtualService, three normal SDK Pods and an independent destination ServiceAccount.
The source uses Service ports 7070/7072 and workload ports 17070/17071; the
destination uses 8080/8081 and 18070/18071. Their Dubbo application and metadata
groups differ, and `xds.metadata.targets` configures the destination entry.
Binary discovery remains bound to the source's 7071/20880 port and its own
metadata entry.

The scenario switches the existing Triple Reference to the destination through
Istio, checks sync/async/server streaming calls, allows only a different caller SPIFFE identity on the destination metadata
port, and verifies metadata authorization rejection, cache expiry and source/binary
isolation. The provider rejects metadata reads immediately; previously accepted
same-instance metadata may remain until its configured TTL, while business
permission is controlled independently. The verifier waits for cache expiry
before asserting address isolation. It then restores the authorized caller,
removes only the destination metadata Service port while business EDS remains,
checks that business cannot proceed with missing metadata, restores the port,
and verifies recovery on the same JVM/Pods with zero restarts and one runtime.
Run labels and recorded UIDs guard mutation/cleanup; retained echo policy specs,
UIDs, Pods, ADS and business are checked afterward. The report records final
success/failure and cleanup separately; the current run's result is recorded in
the implementation plan after completion. This light scenario does not replace
multi-target load, priority/member changes, CA/root rotation or the remaining
stream/agent disruption acceptance.

The optional fixture accepts `NATIVE_XDS_PROVIDER_APP`, `NATIVE_XDS_TRIPLE_PORT`,
`NATIVE_XDS_BINARY_PORT`, `NATIVE_XDS_METADATA_PORT`,
`NATIVE_XDS_METADATA_SERVICE_PORT` and `NATIVE_XDS_METADATA_TARGETS`.
Defaults preserve the original echo deployment. These are acceptance-application
parameters; production applications configure the documented Registry/Reference
parameters directly. Public diagnostics include instance application, workload
port and actual cluster name, excluding bootstrap, credentials and metadata payloads.


The final 2026-10-12 run (`m6-native-cross-service-caller-policy-report.json`)
passed all 24 records with `cleaned_up=true`. The same Triple Reference switched
from source application/workload port 17070 to destination application/18070,
while binary stayed on source/20880. Destination sync/async/server streaming,
metadata caller RBAC with cache expiry, source isolation, authorization recovery,
metadata-only Service-port withdrawal with business EDS present, and restoration
passed on the same JVM/three Pods, zero restarts and one runtime. All 13 owned
resources were removed; independent owner inventory found none. Retained echo
policies, Pods, ADS and business remained intact. No transport retries occurred
in this final run.

The first run was interrupted by a kubectl stdout connection reset; result
files now remain readable until Pod cleanup, and only known transport errors
retry observation of the same UUID without re-dispatch. The second run's custom
DestinationRule metadata SAN never caused rejection: Istio 1.31.1 grpcgen uses
ServiceAccounts rather than this field. A separate real observation recorded an
incorrect custom SAN with an OK destination response, consistent with the
[CDS generator](https://github.com/istio/istio/blob/1.31.1/pilot/pkg/networking/grpcgen/cds.go#L241).
Both failed reports and successful cleanup are retained. The final caller-RBAC
negative is distinct from wrong server SAN; cross-target SAN isolation retains
local real-RPC evidence and its native negative remains pending.

### Shared workload and metadata diagnostics

The optional native application's `inspect` result includes `load_stats`, shared
across references and registries in the workload: cluster/EDS identity,
subscription state, issued/in-progress/success/error attempts, circuit drops and
EDS drops. Metadata RPCs count as transport attempts; a metadata fetch may call
more than one method. Binary business calls use ordinary Dubbo discovery and are
not accounted as native Triple dispatches. Totals survive an idle admission counter,
while retired idle history is bounded to 128 identities. This diagnostic field does
not prove that Istio/agent has enabled LRS.

`certificate_providers` observes the SDK's accepted material without reading
agent files or loading an unused provider. Each configured provider reports
`state`, `observed_at_ms`, `identity_not_before_ms`, `identity_not_after_ms`,
`last_successful_refresh_ms` and a sanitized `last_refresh_error`. READY can
describe a retained valid snapshot while refresh fails; NOT_LOADED means TLS
has not initialized the provider. Expired/not-yet-valid or unavailable material
cannot establish a new protected connection. Unavailable state retains last
accepted identity dates; roots-only and never-loaded providers have null identity
dates. Same-byte recovery clears the error without a new generation; unchanged
successful polls do not advance the accepted-material timestamp. This snapshot
is not a peer-handshake result and excludes paths, material, SANs and fingerprints.
It requires a rebuilt app/xDS bundle; retained original Pods and old reports
remain tied to their original bundle's diagnostic scope.

`load_reporting` separately exposes native LRS status by bootstrap server index,
without bootstrap contents or credentials. Accepted CDS `lrs_server: SELF` starts
reporting; an absent field opens no stream, so an empty map is expected for the
current Istio captures. `PAUSED` means the server requested a zero interval;
`BACKOFF:UNIMPLEMENTED` means that origin has no implemented LRS service. ADS and
business RPCs continue independently. Removing the configuration stops counting
new calls, while unsent retired windows can keep their reporter alive until they
drain or the runtime closes. Cumulative `load_stats` does not reset on an LRS send.
The protocol and real Triple fixtures are local evidence; the original echo Pods
must receive a rebuilt bundle before these new inspect fields are available there.

The optional application's `accepted_endpoints` keeps `endpoint_count` and adds
`localities`: accepted region/zone/sub-zone, unsigned priority, optional locality
weight, endpoint count and health-status counts. An absent weight is reported
as null rather than invented as zero. This is a control-plane snapshot, not a
claim that a transport is ready or that failover has occurred. Use it to verify
that a priority scenario actually received multiple priority groups before
asserting provider selection. Original echo Pods need the rebuilt app for these
new fields; older run reports retain their original bundle's scope.

Held-call diagnostics retain `elapsed_ms` as time at observation and additionally
record the first terminal's `completed_at_ms` and monotonic
`completion_elapsed_ms`. Repeated terminal callbacks still increment `terminals`,
so the timestamp cannot hide duplicate completion. This separates RPC completion
from slow kubectl observation without changing the 180-second call budget.

Each target also has `metadata_successes`, `metadata_failures` and
`metadata_failure_statuses` and `metadata_failure_causes`. The status map is per instance and uses sanitized
status-code names rather than exception messages. One successful instance does not
hide another instance's metadata denial. Successful rereads clear that instance's
status; endpoint removal clears its entry. Metadata TTL still governs any retained
same-instance cache, so these diagnostics do not imply immediate business revocation.
The cause map contains at most eight exception class names per failed instance,
outermost first, and stops on cycles. It never contains exception messages, stack
traces or suppressed exceptions. This helps expose TLS/KeyStore errors hidden by
an outer RuntimeException. A successful reread or removal clears the cause entry;
an empty metadata response has no exception causes. These fields require a rebuilt
bundle; historical reports and the retained original echo Pods retain their
original diagnostic scope. See the SDK [operations guide](../../../dubbo-xds/OPERATIONS.md).

### Native priority transport and membership fixture

`native-priority.py` creates three providers and one consumer with owned Pod
zone/rack labels and a DestinationRule `failoverPriority`. It first requires
actual accepted EDS priorities 0/1/2.
The Triple and metadata port policies explicitly carry locality and outlier
settings, because a TLS-only port policy does not inherit destination-level LB
fields. The outlier setting enables Istio's EDS priority generation here; this
fixture does not assert SDK outlier ejection execution.
An explicit test-only
`NATIVE_XDS_LISTENER_CONTROL=true` file command closes a provider's business
Triple listener through public Dubbo transport APIs, while a separate metadata
listener and binary listener remain. After the 10-second metadata TTL, successful
rereads and all three available instances must still be observed before RPC
selection is asserted. This distinguishes transport failover from EDS withdrawal.
The check waits for metadata freshness before sampling business selection, so
the RPC's elapsed time is not a measurement of failover detection latency.
Concentrated timing/load acceptance remains separate.

```sh
python3 dubbo-test/xds/istio/native-priority.py \
  --kubeconfig /path/to/kubeconfig \
  --bundle dubbo-test/xds/target \
  --output /path/to/native-priority-report.json
```

The fixture closes priority zero and then priority one, restores only those
provider application containers with one guarded restart each, and withdraws/
returns priority zero through its owned Service membership label. The consumer
JVM/References/shared ADS runtime and every agent/Pod must remain. Binary checks
prove discovery and calls only, without claiming Triple's priority semantics.
All resources are guarded by owner and recorded UID; cleanup also checks the
retained echo policies, Pod UIDs and business baseline. The native batch on
2026-10-12 passed 42 baseline/action/recovery/cleanup records in
`m6-native-priority-refused-report.json`, with all eight owned resources removed.
Actual EDS and fresh metadata retained three members during P0/P1 listener
faults, and sync/async/server-stream calls selected P1/P2. Restored listeners
selected P0; P0 membership withdrawal/return selected P1/P0. The same consumer
JVM/References/runtime and all agents/Pod UIDs remained, while P0/P1 each had
exactly one intended application restart. Earlier startup/port-policy/closed-flag
failed reports remain separate and are not counted as passes. Production defaults
and real node labels are unchanged.

### Real Istiod signing-CA environment

`native-istiod-ca.py` provides an owned control-plane/agent gate before the
normal Dubbo RPC rollover gate. It renders the official Istio chart into a
unique revision, with separate control/workload namespaces, a private plugged
intermediate signing key, a unique trust ConfigMap and discovery selectors.
`ISTIO_MULTIROOT_MESH=true` permits the A → A+B → B root transitions. Workload
certificates are issued by Istiod through the injected pilot-agent; the script
checks the expected intermediate signature, exact SPIFFE URI and rejection
under the other root, plus actual root files/ConfigMap distribution.

Requires Helm, OpenSSL and Python PyYAML:

```sh
python3 dubbo-test/xds/istio/native-istiod-ca.py \
  --kubeconfig /path/to/kubeconfig \
  --istio-dir /path/to/istio-1.31.1 \
  --output /path/to/real-istiod-ca-report.json
```

Keys stay in a private temporary directory and the owned Kubernetes Secret;
reports contain public certificate fingerprints and resource identities.
The run must keep the Istiod/probe containers and Pod UIDs, clean owned
cluster/namespace resources, and preserve the original Istiod, roots and echo
baseline. The retry batch on 2026-10-12 passed all four phases and ten
baseline/phase/process/scope/recovery records in
`m6-native-real-istiod-ca-retry-report.json`, with its 21 owned resources removed.
Independent cluster-resource and unique trust ConfigMap queries were empty.
The earlier network-interrupted batch and its separate successful recovery
report are retained. Passing this environment gate alone does not prove Dubbo
RPC continuity across a real Istiod signing-CA rollover.

To include normal Dubbo SDK provider/consumer verification, add
`--bundle dubbo-test/xds/target`. `native-istiod-ca-sdk.py` creates an owned
ServiceAccount, three-port Service, STRICT/ALLOW policies, per-port TLS
DestinationRule and two SDK Pods in the isolated workload namespace. The SDKs
use Pod-local copies of the injected bootstrap with the original Node, ADS
and credential paths; only file polling is shortened to one second. Agent
credentials are still signed by the actual owned Istiod CA.

Each phase requires both public workload chains to validate under that phase's
intermediate and roots, exact workload SPIFFE identities, matching SDK public
certificate observations, advancing security epochs, and new TLSv1.2/1.3 h2
business sessions consuming both current leaf certificates. Normal application
discovery must retain one shared runtime and both protocol targets. The separate
STRICT metadata listener must receive successful rereads beyond the ten-second
test metadata TTL. The temporary point-to-point metadata Invoker is destroyed
after each read, so this verifies the fresh authenticated metadata path without
claiming a retained metadata channel snapshot. Sync/async/server-stream Triple
and binary discovery business samples run at every phase. SDK JVMs, Pods and
all container identities must remain. The corrected native batch on 2026-10-12
passed 38 records across all four signing-CA phases, baseline/process/scope
checks and cleanup, in `m6-native-real-istiod-ca-sdk-chain-report.json`.
Both SDK epochs advanced 0→1→2→3, actual roots were 1→2→2→1, and new TLSv1.3/h2
business channels consumed both verified current identities at every phase.
Normal two-protocol discovery and authenticated metadata rereads remained
available with zero recorded metadata failures. All 28 owned resources were
removed and the retained echo/roots/Istiod baseline was checked. In-flight CA
continuity and concentrated acceptance remain separate.

The first SDK batch failed before initial business/metadata discovery could
converge. Its actual Istiod chain contained a duplicated root, which the JDK
PKCS12 KeyManager rejected; the report remains `failed` with `cleaned_up=true`
and all 28 owned resources removed. `IdentityCertificateChain` now separates
the identity issuer chain from appended roots while leaving the TrustManager's
root bytes and SAN checks unchanged. Fifteen local security checks pass,
including intermediate/duplicate/dual-root mTLS, unrelated workload rejection,
and a retired root still present in an identity-file tail without restoring its
trust. The rebuilt bundle passed the native batch above. The earlier failed
SDK report and regression failures remain separate, with their cleanup evidence.

### Actual destination SAN mismatch and recovery

`native-destination-san.py` preserves the actual Istio CDS destination SAN rules
while changing an owned provider's test-app identity files to a different
ServiceAccount's certificate and key, issued by the real mesh Istiod through
that account's injected pilot-agent. The provider Pod's ServiceAccount and
Service selector remain, as do the original Node/ADS and agent credential files.
The test keeps Triple business and metadata on separate Service ports 7070/7072
and workload ports 17070/17072; binary 7071/20880 proves discovery only.

```sh
python3 dubbo-test/xds/istio/native-destination-san.py \
  --kubeconfig /path/to/kubeconfig \
  --bundle dubbo-test/xds/target \
  --output /path/to/native-destination-san-report.json
```

The test application's `inspect` reports public accepted CDS TLS/SAN matchers.
Its `tls-peer` command uses the existing shared runtime and actual default
CDS/EDS for a fresh handshake at each port. A separate PKIX-only test handshake
keeps the same roots while removing SAN constraints solely for that handshake:
the wrong server leaf must succeed there and fail with `san_mismatch=true`
under the actual xDS policy. Normal References and metadata fetches always keep
the original SAN checks. A handshake result includes public peer fingerprints,
so the control must actually see the different ServiceAccount certificate.
Actual CDS payload hashes must remain identical throughout all three phases.

The native batch on 2026-10-12 passed 33 records in
`m6-native-destination-san-report.json`. Both actual business/metadata policies
required the provider SA through exact SPIFFE matchers. Both wrong-identity
handshakes rejected the SAN while PKIX-only controls succeeded with TLSv1.2/h2.
All three EDS clusters retained one endpoint; fresh authenticated metadata
failures isolated both business targets to zero available instances. Sync/async/
server-stream Triple and binary References returned UNAVAILABLE. Restoring
the provider's current agent identity recovered metadata and all business
samples, with the same JVMs, agents/containers, Pod UIDs and one shared runtime.
All ten owned resources were removed and the original Istiod/roots/echo baseline
was checked. Private keys stay in memory, mode-600 temporary verification files
and the owned app's test identity files; they are never written to reports/logs.
This is a single-Service two-port destination check; cross-Service composition,
additional lifecycle combinations and concentrated acceptance remain separate.

Add `--cross-service` to the destination-SAN command to make Entry A's Triple
RDS select B while its binary Reference still discovers A. A/B use different
ServiceAccounts and metadata applications/groups; `xds.metadata.targets`
overrides B's metadata entry. Both use the same Service port numbers, so the
test also requires the destination FQDN to keep their contexts distinct.
The former donor becomes the ordinary A provider. B's test-app identity is
temporarily replaced with A's real agent credential, then restored from B's
current agent files; actual CDS, selectors, SAs and normal SAN rules stay.

The native cross-Service batch passed 33 records in
`m6-native-cross-destination-san-report.json` on 2026-10-12. Triple instances
used B's cluster/application/workload port, while binary instances used A's.
A metadata retained its exact A-SA SAN constraint; B business and metadata
retained B's different constraint. The wrong A identity at B rejected both
actual TLS handshakes despite successful same-root PKIX controls. Healthy EDS
remained; only Triple A→B discovery became unavailable, while A's binary
metadata had zero failures and its business call continued successfully.
Restoring B identity recovered the original Triple Reference and all samples.
The same JVMs/containers/Pod UIDs and one runtime remained; all 15 owned
resources were removed and the original mesh/echo baseline was verified.
More lifecycle/load combinations remain; actual CA in-flight evidence is recorded below.

### Four admitted RPC forms across actual Istiod CA transitions

Add `--inflight` to `native-istiod-ca.py --bundle ...` to keep one async unary,
server stream, actual IDL client stream and bidi call admitted under A through
root overlap, B identity migration and retirement of A. The owned provider and
consumer enable the stream fixture and explicitly set
`NATIVE_XDS_HOLD_BUDGET_MS=1200000` (20 minutes) before starting Java. This
bounded test budget accommodates real agent renewal and Kubernetes projection
latency. Other fixtures retain the 180-second default; production timeouts and
grace remain unchanged. Each call records its budget once at creation, and CA
phases never extend it.

The verifier requires four active admissions, one provider dispatch per token,
zero client terminals and unchanged initial input/half-close state at every
phase. New-call TLS evidence selects fresh channels rather than counting the
old A channel retained by admitted streams. Under B-only trust, the original
input writers send the second message and half-close; admissions remain held
until all provider releases are published together. Completion must contain
the exact ordered responses and one terminal, provider RELEASED without
cancellation or redispatch, and elapsed time inside the original test budget.
Admissions must return to zero. The same SDK processes/References/runtime,
agents/Pod UIDs, control plane and retained baseline are still checked.

The corrected native batch passed 77 records in
`m6-native-real-istiod-ca-inflight-upload-report.json` on 2026-10-12 and removed
all 28 owned resources. The same four A-admitted calls stayed ACTIVE without
cancellation or early admission release through overlap, migration and B-only
trust. Both original input writers sent the second message and half-closed
under B-only; each call completed once on its original provider, with ordered
responses and elapsed times 733395/727966/722461/717142 milliseconds, inside
their initial fixed 1200000-ms budgets. Four admissions returned to zero.
Normal new-identity TLS/business/metadata, unchanged SDK/agent/Pod/control-plane
processes and root-distribution scope also passed. The first run failed
during consumer bundle upload with an API-stream timeout, before any CA phase or
held RPC, and cleaned all 28 resources. The retry checks the exact archive's
remote SHA before extraction: a lost response with complete content continues;
only a missing/partial owned file is retransferred. It never redispatches a
business RPC or CA update. Reports keep the failed batch separate. Four fixed
calls provide a lifecycle check, not concentrated load acceptance.
