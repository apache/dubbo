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

# Native Istio xDS operations

This guide applies to the optional `dubbo-xds` module on Dubbo 3.3 with the
validated Istio 1.31.1 `grpc-agent` contract. The [configuration guide](README.md)
contains the supported fields and capability matrix. Comprehensive load and
recovery acceptance remains separate from the recorded light checks.

## Deployment checks

Include `dubbo-xds` in the application's dependencies using the same Dubbo version
as its other modules. Binary discovery also needs the normal `dubbo-rpc-dubbo`
dependency. Keep gRPC dependencies aligned with Dubbo's dependency BOM.

Use the `grpc-agent` injection template and
`proxy.istio.io/config: '{"holdApplicationUntilProxyStarts": true}'` on the Pod
template. Verify that injection added the agent, application bootstrap environment
variable and shared volumes. The application needs Linux epoll support for the
local ADS Unix socket. The agent owns Istiod authentication and certificates;
the application reads the agent's files and needs no Kubernetes API credentials.
Do not replace the bootstrap Node identity or `GENERATOR=grpc` metadata.

Use registry `xds://localhost`, service registry type and instance registration.
Provide each Reference's Service, Service port and protocol. Service ports select
the xDS target; EDS supplies workload ports. A binary Reference and a Triple
Reference to the same Service must use their own Service ports.

Export MetadataService V1 with `onlyUseMetadataV2=false`. Match the metadata
application/group to the provider, and configure separate metadata Service ports
explicitly. Every business address must match exactly one healthy metadata address
on the same host. Cross-Service routing needs metadata overrides for the actual
destination. Apply TLS and authorization policies to both business and metadata
workload ports. The [native deployment example](../dubbo-test/xds/istio/native-application.yaml)
demonstrates port-specific Triple mTLS and discovery-only binary traffic.

## Read diagnostics in order

First inspect ADS connectivity and resource acceptance, then the target's graph,
metadata availability and finally invocation outcomes. An ACK means the resource
passed validation; it does not prove that a Reference has a usable endpoint.

| Observation | Meaning and next check |
| --- | --- |
| ADS BACKOFF with previously accepted resources | Last-good resources remain available. Check agent health, local UDS access and the agent's upstream connection to Istiod. Record whether a fresh version is accepted after recovery. |
| REQUESTED or ABSENT resource | The resource is pending or missing. Check the exact FQDN, Service port, protocol and dependency resource names; inspect the Service and its endpoint membership. |
| NACKED_WITH_LAST_GOOD | The update was rejected and the accepted version remains. Check the reported rejection category and accepted version; MAGLEV deliberately NACKs in this release. |
| NACKED_WITHOUT_RESOURCE | No usable accepted version exists. Correct the unsupported or malformed policy; retrying business calls cannot fix resource validation. |
| graph_ready=false | At least one business or metadata resource graph is incomplete. Check each graph before treating the problem as a transport failure. |
| discovered_endpoints greater than available_instances | Some discovered workloads have no unexpired usable metadata. Inspect their metadata statuses and cause types; check interface export, protocol/port filtering, metadata application/group and TTL. |
| MetadataEndpointUnavailable or MetadataEndpointAmbiguous | A business host has zero or multiple matching metadata addresses. Correct metadata Service selection and endpoint ports rather than borrowing another workload's metadata. |
| PERMISSION_DENIED during metadata reads | Check the metadata port's ALLOW/DENY policy, authenticated caller identity and destination identity. Do not assume business-port authorization also permits metadata. |
| SSLHandshakeException, CertificateException or KeyStoreException in metadata causes | Check the public certificate validity/issuer/SPIFFE identity, agent rotation, root distribution and the destination SAN constraints. Cause types identify a layer, not a unique root cause. |
| Certificate provider READY with a refresh error | A previous coherent, still-valid snapshot is retained. Check agent file-update completion and the accepted leaf expiry; READY does not mean the latest file update succeeded. |
| Certificate provider NOT_LOADED | No TLS use has initialized this provider. Inspection intentionally does not load files or prove the configured paths work. |
| Certificate provider EXPIRED / NOT_YET_VALID / UNAVAILABLE | Check the accepted validity timestamps, sanitized refresh error and agent renewal. Unavailable providers may retain only the last accepted identity dates for diagnosis. |
| Empty binary discovery for a TLS-required cluster | Binary support is discovery-only and deliberately rejects xDS TLS. Use Triple for the protected path or an explicitly configured binary discovery-only port. |
| Local UNAVAILABLE with a circuit or EDS drop increment | The invocation was locally rejected. Compare active attempts and configured limits/drop policy; drops are not upstream retry attempts. |
| Calls waiting while endpoints are present | EDS membership does not establish a ready transport. Inspect priority/locality and connections; waiting and unary retries consume the original deadline. |

`XdsServiceDiscovery.inspectTargets()` returns serialized immutable target
snapshots. Fetch success/failure counters are cumulative; per-instance status and
cause maps describe the last failure on each current entry. A successful reread
clears that instance's maps, while another instance's successful read does not.
TTL controls how long previous same-instance metadata remains usable after a
failed refresh. Endpoint removal removes its current diagnostic entry.

Cause lists contain at most eight exception class names, outermost first, with
cycle detection. They exclude messages, stack traces and suppressed exceptions.
Use public certificate summaries and these fields when collecting incident
evidence; bootstrap contents, tokens and private-key files are unnecessary.
The optional native application's `inspect` command exposes these fields as
`metadata_failure_statuses` and `metadata_failure_causes`. That command is a test
utility, not a production management endpoint.

`XdsServiceDiscovery.inspectCertificateProviders()` returns immutable snapshots
by configured provider name, also exposed by `XdsSecurityRuntime`. These include
state, observation time, last accepted identity validity, changed-material
acceptance time and the last refresh error class name. Configure renewal alerts
using the accepted expiry and the observation time with your renewal margin.
Roots-only providers have no identity dates. An unchanged successful poll does
not move the accepted-material timestamp, so that timestamp alone is not a
heartbeat. Inspection neither loads files nor proves peer trust or authorization.
The native test application exposes the same fields as `certificate_providers`;
the map excludes paths, certificates, identities, private keys and fingerprints.

## Updates and recovery

Traffic updates retain accepted policies when an unsupported update NACKs.
Compare both the latest rejection and the last accepted version. A control-plane
disconnect does not itself remove accepted endpoints. An accepted withdrawal does;
new calls then fail or select another usable endpoint according to policy.

Certificate files are loaded lazily. Valid refreshes advance the shared security
epoch and affect new connections. Partial or mismatched updates retain the last
valid material until it expires; unavailable or expired credentials fail TLS
setup. Binary TLS rejection never falls back to plaintext. Monitor agent renewal
and verify a new TLS session uses renewed public identity after rotation.

Previously admitted streams retain their connection and are not replayed.
Updated RBAC governs newly admitted calls; do not interpret an old authorized
stream's completion as a policy-update failure. Unary retry keeps its original
cluster choice and deadline. Reference destruction cancels its owned calls while
other References sharing the runtime remain usable.

For test rollout and rollback, use an isolated namespace with owner labels and
record resource UIDs before mutations. Cleanup must match both owner and UID and
verify the retained environment afterwards. The [native verifier guide](../dubbo-test/xds/istio/README.md)
records commands and evidence; production configuration rollback should restore
the previously reviewed policy and confirm acceptance and real calls.
