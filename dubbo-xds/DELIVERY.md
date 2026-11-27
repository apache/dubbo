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

# Implementation delivery, 2026-10-12

The current implementation round is complete with light verification. Full
project regression and concentrated Kubernetes acceptance remain a subsequent
phase, as requested. This is an implementation checkpoint, not a production
release or a claim that every Envoy feature is supported.

## Delivered scope

| Area | Implementation | Evidence |
| --- | --- | --- |
| Istio connection | `grpc-agent` bootstrap, pilot-agent ADS UDS and file providers; shared JVM runtime; v3 SotW LDS/RDS/CDS/EDS, ACK/NACK, dependency watches, reconnect and cancellation | Local state-machine/bootstrap checks; native normal SDK, agent restart and upstream-loss light batches |
| Discovery | Existing ServiceDiscovery/Directory/Invoker path; FQDN + Service port + protocol identity; per-endpoint MetadataService V1; interface/group/version/protocol/workload-port filtering; bounded refresh, TTL and isolation | Local real RPCs; native dual-port, withdrawal/recovery and cross-Service metadata batches |
| Security | Triple client/server mTLS, PKIX/SAN validation, file rotation and retained-connection revalidation; transport-entry ALLOW/DENY RBAC covers business and MetadataService | Local negative checks; native wrong identity/plaintext/SAN rejection, policy update, leaf renewal and real Istiod CA migration batches |
| Traffic | Triple wire path/headers, virtual hosts, ordered route matching, subsets/weighted clusters; RR, LEAST_REQUEST, RING_HASH, priority/locality selection and readiness waiting | Local real unary/streaming calls and captured Istio resources; native route/hash/priority light batches |
| Call policy | Unified deadline, cancellation, bounded unary replay/retry and pushback; pre-dispatch fault delay/abort; shared `max_requests`; per-attempt EDS drops and SELF LRS | Local real RPC/ADS/LRS fixtures; native deadline/retry/fault/admission light batches |
| Lifecycle | Call ownership, removal/last-good semantics, independent Reference cancellation, certificate/policy generations and runtime lease cleanup | Local regressions; four actual RPC forms across native EDS withdrawal, RBAC updates, provider-agent restart and real Istiod CA transitions |
| Operations/build | Resource/graph/metadata/load diagnostics; safe metadata exception types and certificate validity status; optional module/BOM/SPI/source/native assembly; operations guide and threat-model supplement | Current-source reactor regression, artifact/source checks, API Javadoc, source-header/license metadata checks and full release-profile compilation/package |

See [configuration and the detailed support matrix](README.md),
[operations](OPERATIONS.md), [native fixtures](../dubbo-test/xds/istio/README.md)
and [the implementation/evidence journal](../dubbo-test/xds/IMPLEMENTATION-PLAN.md).

## Current verification baseline

- One current-source reactor command passed **277 tests in 55 classes**, with
  zero failures, errors or skips: all 37 xDS classes (202 tests), plus 18 selected
  core classes (75 tests). A separate registry-factory follow-up passed four
  tests. These counts are separate runs; overlapping historical batches are
  not added to them.
- The **120-module** `release,checkstyle` reactor compiled and packaged
  successfully. That command skipped tests, integration tests, Javadoc and
  signing. The xDS API Javadoc was generated separately with errors fatal.
- The final source/evidence audit matched **107 xDS main/test source files** to
  the green regression snapshot, **235 changed code/build files** to the release
  source archive, and eight artifacts to their saved hashes. Final delivery
  documentation written after packaging is not part of that archive snapshot.
- Source-header checks found zero invalid headers in the configured xDS/native
  scope and the whole checkout. Dependency license metadata reports contained
  107 entries, or 82 compile/runtime entries, without an unknown license name.
  These checks are not legal approval or a signed release.
- Historical isolated native reports for actual Istiod CA in-flight calls (77
  records), cross-Service SAN (33), single-destination SAN (33) and priority
  failure/recovery (42) are terminal passes with owned resources cleaned up.
  Records include setup, transitions and cleanup; they are not independent
  test-case counts. Each report identifies its own tested bundle.
- A final retained-environment smoke passed five call batches: Triple sync,
  async and server streaming, and binary Dubbo sync and async. ADS resources
  were ACKED. It did not deploy a new bundle or replace Pods/policies.

Current binary hashes:

```text
dubbo-xds:        ade5ee8da9f60209dba406a96eda4b7a281ed426b393e87197644919ab215be5
native test app:  6873b8eae992c39adca25f76e8dcc3733ddbf7a0c09167d6317053266448439a
```

The latest bundle includes the new certificate/metadata diagnostics. It has
not replaced the retained native echo deployment. Earlier native evidence does
not prove those new diagnostic fields are running in the retained Pods.

## Scope boundaries and subsequent acceptance

Application discovery uses explicit Services and direct MetadataService V1;
revision lookup and interface-application mapping are excluded. Binary Dubbo
supports discovery only and rejects an xDS TLS-required cluster. Triple is the
full implementation path within the documented gRPC baseline.

The current security round covers mTLS/SAN/ALLOW-DENY RBAC. JWT/CUSTOM is deferred
to the next round by the user's D4 decision. MAGLEV explicitly NACKs and retains
the last valid resource under D6. The additive per-target metadata configuration
is the documented D5 implementation assumption and remains adjustable.

Current native grpcgen captures have no `lrs_server` or outlier-detection output.
SDK SELF LRS and EDS drops have local fixture evidence; native control-plane
execution is not claimed. Outlier execution, JWT/CUSTOM and proxy-specific
semantics need their own control-plane/product scope before implementation.

Subsequent concentrated acceptance should cover mixed unary/streaming/binary
load, simultaneous policy/resource/certificate changes, retry/admission/queue
saturation, cancellation and callback isolation, stable hash membership
changes, priority timing and long-run resource/thread/cache recovery. Complete
project CI and the latest-bundle deployment also belong to that phase. The
existing native scripts and local fixtures provide its starting point.

All implementation changes remain in the main `3.3` checkout. This round has
not committed, pushed, created an xDS PR, signed artifacts or published them.
