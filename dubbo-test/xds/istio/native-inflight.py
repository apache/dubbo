#!/usr/bin/env python3
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.

"""Real in-flight unary/server/client/bidirectional-stream completion during EDS withdrawal, RBAC update, provider-agent restart or test-root rollover in owned native Dubbo Pods."""

import argparse
import copy
import importlib.util
import json
import pathlib
import shutil
import subprocess
import tarfile
import tempfile
import time
import uuid

loader = importlib.util.spec_from_file_location("native_policy", pathlib.Path(__file__).with_name("native-policy.py"))
policy = importlib.util.module_from_spec(loader)
loader.loader.exec_module(policy)
smoke = policy.smoke
OWNER = "dubbo.apache.org/native-inflight-owner"
KINDS = ("virtualservice", "destinationrule", "peerauthentication", "authorizationpolicy")
PRINCIPAL = "spiffe://cluster.local/ns/dubbo-xds-native/sa/native-xds"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--kubectl", default=shutil.which("kubectl") or "kubectl")
    parser.add_argument("--bundle", type=pathlib.Path, required=True)
    parser.add_argument("--case", choices=("withdrawal", "rbac", "provider-agent", "root"), default="withdrawal")
    parser.add_argument("--all-streams", action="store_true", help="Also hold actual IDL client-streaming and bidi calls")
    parser.add_argument("--retired-peer", action="store_true", help="Keep an A-credential peer and verify rejection after root retirement (root case only)")
    parser.add_argument("--timeout", type=int, default=180, help="Startup/cleanup timeout")
    parser.add_argument("--output", type=pathlib.Path, required=True)
    options = parser.parse_args()
    if options.timeout < 1:
        parser.error("Timeouts must be positive")
    if options.retired_peer and options.case != "root":
        parser.error("--retired-peer requires --case root")
    kube = [options.kubectl, "--kubeconfig", options.kubeconfig, "--request-timeout=30s"]
    jar = options.bundle / "dubbo-xds-native-test-app-3.3.7-SNAPSHOT.jar"
    if not jar.is_file() or not (options.bundle / "lib").is_dir():
        raise ValueError("Build the current native application bundle first")
    baseline = {kind: policy.get_object(kube, kind, "echo") for kind in KINDS}
    retained_pods = {name: policy.get_object(kube, "pod", name)["metadata"]["uid"]
                     for name in ("echo-client", "echo-v1", "echo-v2")}
    template = policy.get_object(kube, "pod", "echo-client")
    app = next(c for c in template["spec"]["containers"] if c["name"] == "app")
    run = uuid.uuid4().hex
    service = "native-inflight-" + run[:12]
    provider, consumer = service + "-server", service + "-client"
    host = service + "." + policy.NAMESPACE + ".svc.cluster.local"
    cluster = "outbound|7070||" + host
    report = {"owner": run, "namespace": policy.NAMESPACE, "service": host, "case": options.case, "retired_peer": options.retired_peer, "all_streams": options.all_streams, "status": "running",
              "cleaned_up": False, "cases": [], "observations": [], "pod_uids": {},
              "scope": {"withdrawal": "EDS withdrawal with provider-started async unary/server stream",
                        "rbac": "Business-only RBAC DENY/update/recovery with provider-started async unary/server stream",
                        "provider-agent": "Actual provider pilot-agent container restart with dispatched unary/server stream and subsequent fresh inbound RBAC",
                        "root": "SDK A/A+B/B root rollover using isolated test CA inputs and real Istio xDS; no cluster signing-CA replacement"}[options.case]
                        + ("; actual IDL CLIENT_STREAM and BI_STREAM included" if options.all_streams else "")
                        + ("; A-authorized calls span overlap, B identity migration and retirement of A"
                           if options.case == "root" and options.all_streams else "")
                        + "; no application restart or forced business reconnect"}
    options.output.parent.mkdir(parents=True, exist_ok=True)
    options.output.with_suffix(".baseline.json").write_text(json.dumps(
        {"apiVersion": "v1", "kind": "List", "items": [policy.configuration(x) for x in baseline.values()]}, indent=2) + "\n")
    resources = []
    root_material = None

    def save():
        options.output.write_text(json.dumps(report, indent=2) + "\n")

    def record(name, value):
        report["cases"].append({"name": name, "observed_at_ms": int(time.time() * 1000), "result": value})
        save()

    def metadata(name, labels=None):
        return {"name": name, "namespace": policy.NAMESPACE, "labels": {OWNER: run, **(labels or {})}}

    def create(kind, value):
        name = value["metadata"]["name"]
        existing = smoke.run(kube, "-n", policy.NAMESPACE, "get", kind, name, "--ignore-not-found=true", "-o", "name")
        if existing.strip():
            raise AssertionError("Refusing to replace an existing in-flight resource")
        resources.append((kind, name, None))
        policy.apply(kube, value)
        resources[-1] = (kind, name, policy.get_object(kube, kind, name)["metadata"]["uid"])
        report["owned_resources"] = [{"kind": k, "name": n, "uid": u} for k, n, u in resources]
        save()

    def owned_pod(name):
        uid = next(u for k, n, u in resources if (k, n) == ("pod", name))
        current = policy.get_object(kube, "pod", name)
        if current["metadata"].get("labels", {}).get(OWNER) != run or current["metadata"]["uid"] != uid:
            raise AssertionError("Fixture Pod ownership changed")
        return current

    def containers(pod):
        return {value["name"]: {"id": value.get("containerID", ""), "restart_count": value.get("restartCount", 0),
                "ready": value.get("ready", False), "state": next(iter(value.get("state", {})), "unknown")}
                for value in pod.get("status", {}).get("containerStatuses", [])}

    def delete_owned(kind, name):
        uid = next(u for k, n, u in resources if (k, n) == (kind, name))
        current = policy.get_object(kube, kind, name)
        if current["metadata"].get("labels", {}).get(OWNER) != run or current["metadata"]["uid"] != uid:
            raise AssertionError("Refusing to delete an unowned in-flight resource")
        smoke.run(kube, "-n", policy.NAMESPACE, "delete", kind, name, "--wait=false")
        policy.wait_for("owned resource deletion", lambda: smoke.run(kube, "-n", policy.NAMESPACE,
                        "get", kind, name, "--ignore-not-found=true", "-o", "name"), lambda v: not v.strip(), options.timeout)

    def transport_failure(stderr):
        return any(text in stderr.lower() for text in ("connection reset by peer", "unexpected eof"))

    def read_json(pod, filename):
        return json.loads(smoke.run(kube, "-n", policy.NAMESPACE, "exec", pod, "-c", "app", "--", "cat", filename))

    def rpc(method="echo", protocol="tri", count=1, message="native-inflight", key="native-test"):
        name = uuid.uuid4().hex
        path = "/runtime/commands/" + name
        properties = "method=" + method + "\nprotocol=" + protocol + "\ncount=" + str(count) + "\nmessage=" + message + "\nkey=" + key + "\nservice=" + host + "\ntimeout=3000\n"
        command = kube + ["-n", policy.NAMESPACE, "exec", "-i", consumer, "-c", "app", "--", "sh", "-c",
                          'cat > "$1.tmp" && mv "$1.tmp" "$1.request"', "sh", path]
        result = subprocess.run(command, input=properties, text=True, capture_output=True, timeout=60)
        if result.returncode:
            if not transport_failure(result.stderr):
                raise RuntimeError(result.stderr.strip() or "Could not publish in-flight command")
            # Publication may have succeeded. Observe this UUID rather than dispatching the call again.
            report.setdefault("transport_observations", []).append({"phase": "publish", "method": method, "command": name})
            save()
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            result = subprocess.run(kube + ["-n", policy.NAMESPACE, "exec", consumer, "-c", "app", "--",
                                            "sh", "-c", 'cat "$1"', "sh", path + ".json"],
                                    text=True, capture_output=True, timeout=30)
            if result.returncode == 0:
                return json.loads(result.stdout)
            if "No such file" not in result.stderr:
                if not transport_failure(result.stderr):
                    raise RuntimeError(result.stderr.strip() or "Could not read in-flight command")
                report.setdefault("transport_observations", []).append({"phase": "read", "method": method, "command": name})
                save()
            # Retain results until owned Pod cleanup, including when an exec stream loses its stdout.
            time.sleep(1)
        raise TimeoutError("Native in-flight command did not finish")

    def business(method="echo", protocol="tri", count=1):
        value = rpc(method, protocol, count)
        record("business-" + method + "-" + protocol, value)
        if not policy.successful(value, count * (3 if method == "stream" else 1)):
            raise AssertionError("In-flight business call failed: " + method + "/" + protocol)
        if any(not v.startswith("inflight:native-inflight") for v in value["values"]):
            raise AssertionError("In-flight call reached an unexpected provider")
        return value

    def apply_business_deny():
        print("Applying business-only DENY through the current provider agent", flush=True)
        deny_name = service + "-deny"
        create("authorizationpolicy", {"apiVersion": "security.istio.io/v1", "kind": "AuthorizationPolicy",
            "metadata": metadata(deny_name), "spec": {"selector": {"matchLabels": {"app": service}},
            "action": "DENY", "rules": [{"to": [{"operation": {
                "ports": ["17070"], "paths": ["/org.apache.dubbo.xds.test.NativeEcho/*"] + (["/xds.native.testing.NativeStreams/*"] if options.all_streams else [])}}]}]}})

        def denied(value):
            return value.get("status") == "RPC_ERROR" and value.get("errors") == {"PERMISSION_DENIED": 1} and not value.get("values")

        # Actual denial is the update barrier, rather than the Kubernetes apply response.
        record("new-call-after-deny-echo", policy.wait_for("business DENY", lambda: rpc(), denied, options.timeout))
        for method in ("async", "stream"):
            value = rpc(method=method)
            record("new-call-after-deny-" + method, value)
            if not denied(value):
                raise AssertionError("Updated RBAC did not deny a new " + method + " call")
        denied_token = "denied-" + run
        record("new-held-unary-start-after-deny", rpc("hold-start", message=denied_token, key="unary"))
        denied_call = policy.wait_for("new held unary denied", lambda: rpc("hold-status", message=denied_token),
                                     lambda v: v.get("status") != "ACTIVE", 30)
        record("new-held-unary-denied", denied_call)
        if denied_call.get("status") != "PERMISSION_DENIED" or denied_call.get("terminals") != 1 or denied_call.get("values"):
            raise AssertionError("New held unary did not terminate with one permission denial")
        smoke.run(kube, "-n", policy.NAMESPACE, "exec", provider, "-c", "app", "--", "test", "!", "-e",
                  "/runtime/holds/" + denied_token + ".json")
        record("denied-unary-not-dispatched", {"token": denied_token, "provider_hold_exists": False})
        rpc("hold-forget", message=denied_token)
        if options.all_streams:
            rejected_streams("PERMISSION_DENIED", "deny")
        business(protocol="dubbo")
        record("discovery-retained-during-deny", policy.wait_for("metadata discovery remains available",
               lambda: rpc("inspect"), lambda v: shape(v, 1), 30))
        return deny_name

    def rejected_streams(status, change):
        for kind in ("client-stream", "bidi"):
            token = "new-" + kind + "-" + run
            record("new-" + kind + "-start-after-" + change, rpc("hold-start", message=token, key=kind))
            value = policy.wait_for("new " + kind + " rejected", lambda: rpc("hold-status", message=token),
                                    lambda v: v.get("status") != "ACTIVE", 30)
            record("new-" + kind + "-rejected-after-" + change, value)
            if value.get("status") != status or value.get("terminals") != 1 or value.get("values"):
                raise AssertionError("New " + kind + " did not receive exactly one " + status)
            smoke.run(kube, "-n", policy.NAMESPACE, "exec", provider, "-c", "app", "--", "sh", "-c",
                      'test ! -e "$1.claimed" && test ! -e "$1.json"', "sh", "/runtime/holds/" + token)
            record("new-" + kind + "-not-dispatched-after-" + change, {"token": token, "provider_hold_exists": False})
            rpc("hold-forget", message=token)

    def root_snapshot(phase, previous=None):
        def observe():
            value = {"provider": read_json(provider, "/runtime/rotation-observation.json"), "consumer": rpc("certificates")}
            report["observations"].append(value)
            save()
            return value

        def matches(value):
            for role in ("provider", "consumer"):
                observed = value[role]
                if observed.get("status") != "OBSERVED" or observed.get("connection") != "READY":
                    return False
                if previous is not None and observed.get("security_epoch", -1) <= previous[role]["security_epoch"]:
                    return False
                certificates = list(observed.get("certificates", {}).values())
                if len(certificates) != 1 or certificates[0].get("ready") is not True:
                    return False
                expected = root_material.expected(phase, role)
                if (certificates[0].get("leaf_sha256") != expected["leaf_sha256"]
                        or set(certificates[0].get("root_authorities_sha256", [])) != set(expected["root_authorities_sha256"])
                        or PRINCIPAL not in certificates[0].get("uri_sans", [])):
                    return False
            peer = root_material.expected(phase, "provider")["leaf_sha256"]
            local = root_material.expected(phase, "consumer")["leaf_sha256"]
            connections = [c for c in value["consumer"].get("connections", []) if c.get("cluster") == cluster
                           and c.get("connectivity") == "READY" and c.get("tls", {}).get("ready") is True
                           and c["tls"].get("alpn") == "h2" and c["tls"].get("local_leaf_sha256") == local
                           and c["tls"].get("peer_leaf_sha256") == peer]
            if previous is not None:
                previous_ids = {c.get("tls", {}).get("channel_id") for c in previous["consumer"].get("connections", [])}
                connections = [c for c in connections if c["tls"].get("channel_id") not in previous_ids]
            return bool(connections)

        value = policy.wait_for("SDK root phase " + phase, observe, matches, options.timeout)
        record("root-phase-" + phase, value)
        return value

    def legacy_peer(method, retired=False):
        token = "retired-" + run if retired else "native-root-peer"
        value = rpc(method, message=token, key="retired" if retired else "native-test")
        record("legacy-peer-" + ("retired" if retired else method), value)
        expected_status = "PERMISSION_DENIED" if retired else "OK"
        if value.get("status") != "OBSERVED" or any(value.get(kind, {}).get("status") != expected_status for kind in ("business", "metadata")):
            raise AssertionError("Pinned peer calls did not match the expected root authorization")
        if retired and any("value" in value[kind] for kind in ("business", "metadata")):
            raise AssertionError("Retired peer received a business/metadata value")
        if not retired and (value["business"].get("value") != "inflight:native-root-peer" or value["metadata"].get("value", 0) < 1):
            raise AssertionError("Pinned peer did not execute real business and metadata calls")
        for phase in ("connections_before", "connections_after"):
            connections = value.get(phase, [])
            if not connections or any(c.get("ready") is not True or c.get("alpn") != "h2"
                    or c.get("local_leaf_sha256") != root_material.expected("initial", "consumer")["leaf_sha256"]
                    or c.get("peer_leaf_sha256") != root_material.expected("initial", "provider")["leaf_sha256"] for c in connections):
                raise AssertionError("Pinned peer did not retain the original A TLS identities")
            ids = {c["channel_id"] for c in connections}
            if method == "root-peer-check" and ids != legacy_ids:
                raise AssertionError("Pinned peer replaced its original TLS channel")
        if retired:
            for suffix in (".claimed", ".json"):
                smoke.run(kube, "-n", policy.NAMESPACE, "exec", provider, "-c", "app", "--", "test", "!", "-e",
                          "/runtime/holds/" + token + suffix)
            record("retired-peer-business-not-dispatched", {"token": token, "provider_claim_exists": False, "provider_hold_exists": False})
        return value

    def publish_root(phase):
        for name, role in ((provider, "provider"), (consumer, "consumer")):
            owned_pod(name)
            root_material.publish(kube, policy.NAMESPACE, name, role, phase)
        record("root-files-published-" + phase, {"provider": root_material.expected(phase, "provider"),
                                               "consumer": root_material.expected(phase, "consumer")})

    def migrate_and_retire_roots(overlap):
        print("Migrating both SDK identities to B under overlapping roots", flush=True)
        publish_root("migrated")
        migrated = root_snapshot("migrated", overlap)
        if options.retired_peer:
            legacy_peer("root-peer-check")
        for method in ("echo", "async", "stream"):
            business(method)
        print("Retiring A from both SDK trust bundles", flush=True)
        publish_root("final")
        final = root_snapshot("final", migrated)
        if options.retired_peer:
            legacy_peer("root-peer-check", retired=True)
            closed = rpc("root-peer-close")
            record("legacy-peer-closed", closed)
            if closed.get("status") != "CLOSED" or closed.get("retired_channel_count", 0) < 1 or closed.get("remaining_open_channels") != 0:
                raise AssertionError("Pinned peer did not release its old TLS connection")
        for role in ("provider", "consumer"):
            if final[role]["process_start_ms"] != initial_root[role]["process_start_ms"]:
                raise AssertionError("Root rollover restarted an SDK process")
        return final

    def check_terminal_timing(value):
        # Snapshot elapsed_ms includes kubectl observation overhead. Require the actual
        # first-terminal timestamp from the current fixture without extending its budget.
        elapsed = value.get("completion_elapsed_ms")
        completed = value.get("completed_at_ms")
        if (not isinstance(elapsed, int) or not 0 <= elapsed <= 180000
                or not isinstance(completed, int) or completed < value.get("started_at_ms", completed + 1)):
            raise AssertionError("Held call has no bounded actual terminal observation")

    save()
    failure = None
    try:
        print("Checking the retained echo baseline", flush=True)
        record("echo-baseline", policy.wait_for("baseline ADS", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
        if options.case == "root":
            root_loader = importlib.util.spec_from_file_location("native_root_material", pathlib.Path(__file__).with_name("native-root-material.py"))
            root_module = importlib.util.module_from_spec(root_loader)
            root_loader.loader.exec_module(root_module)
            root_material = root_module.RootMaterial(PRINCIPAL)
            report["root_scope"] = "Pod-local test identities/roots; original injected Node/ADS preserved; no cluster CA/agent credential files changed"
            save()
        create("service", {"apiVersion": "v1", "kind": "Service", "metadata": metadata(service),
                           "spec": {"selector": {"app": service}, "ports": [
                               {"name": "grpc-triple", "port": 7070, "targetPort": 17070},
                               {"name": "tcp-dubbo", "port": 7071, "targetPort": 20880}]}})
        create("peerauthentication", {"apiVersion": "security.istio.io/v1", "kind": "PeerAuthentication",
                                     "metadata": metadata(service), "spec": {"selector": {"matchLabels": {"app": service}},
                                     "mtls": {"mode": "STRICT"}, "portLevelMtls": {"20880": {"mode": "DISABLE"}}}})
        create("authorizationpolicy", {"apiVersion": "security.istio.io/v1", "kind": "AuthorizationPolicy",
                                      "metadata": metadata(service), "spec": {"selector": {"matchLabels": {"app": service}},
                                      "action": "ALLOW", "rules": [{"from": [{"source": {"principals": [PRINCIPAL.removeprefix("spiffe://")]}}],
                                                                    "to": [{"operation": {"ports": ["17070"]}}]}]}})
        create("destinationrule", {"apiVersion": "networking.istio.io/v1", "kind": "DestinationRule",
                                   "metadata": metadata(service), "spec": {"host": host, "trafficPolicy": {"portLevelSettings": [
                                       {"port": {"number": 7070}, "tls": {"mode": "ISTIO_MUTUAL"}},
                                       {"port": {"number": 7071}, "tls": {"mode": "DISABLE"}}]}}})
        create("virtualservice", {"apiVersion": "networking.istio.io/v1", "kind": "VirtualService",
                                  "metadata": metadata(service), "spec": {"hosts": [host], "http": [
                                      {"route": [{"destination": {"host": host, "port": {"number": 7070}}}]}]}})
        with tempfile.TemporaryDirectory(prefix="native-inflight-") as temporary:
            archive = pathlib.Path(temporary) / "bundle.tgz"
            with tarfile.open(archive, "w:gz") as output:
                output.add(jar, arcname=jar.name)
                output.add(options.bundle / "lib", arcname="lib")
            for name, role, argument in ((provider, "rotation-provider" if options.case == "root" else "provider", "inflight"), (consumer, "consumer", host)):
                meta = metadata(name, {"app": service if name == provider else service + "-client"})
                meta["annotations"] = {"inject.istio.io/templates": "grpc-agent", "proxy.istio.io/config": json.dumps({
                    "holdApplicationUntilProxyStarts": True})}
                container = {"name": "app", "image": app["image"], "resources": copy.deepcopy(app["resources"]),
                             "env": [{"name": "JAVA_TOOL_OPTIONS", "value": "-XX:ActiveProcessorCount=2"}]
                                    + ([{"name": "NATIVE_XDS_STREAMS", "value": "true"}] if options.all_streams else []),
                             "command": ["sh", "-c", "while [ ! -f /runtime/bundle-ready ]; do sleep 1; done; "
                                         + (("java -jar /runtime/" + jar.name + " prepare-root-rotation unused /runtime && "
                                             + "export GRPC_XDS_BOOTSTRAP=/runtime/rotation-bootstrap.json && ") if options.case == "root" else "")
                                         + "exec java -jar /runtime/" + jar.name + " " + role + " " + argument + " /runtime"],
                             "readinessProbe": {"exec": {"command": ["test", "-f", "/runtime/" +
                                                 ("provider-ready" if name == provider else "consumer-ready")]},
                                                 "periodSeconds": 2, "failureThreshold": 90},
                             "volumeMounts": [{"name": "runtime", "mountPath": "/runtime"}]}
                create("pod", {"apiVersion": "v1", "kind": "Pod", "metadata": meta, "spec": {
                    "serviceAccountName": "native-xds", "restartPolicy": ("Always" if name == provider and options.case == "provider-agent" else "Never"), "containers": [container],
                    "volumes": [{"name": "runtime", "emptyDir": {}}]}})
            for name in (provider, consumer):
                print("Uploading the native in-flight bundle to " + name, flush=True)
                smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=jsonpath={.status.phase}=Running", "pod/" + name,
                          "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
                pod = policy.get_object(kube, "pod", name)
                report["pod_uids"][name] = pod["metadata"]["uid"]
                smoke.run(kube, "-n", policy.NAMESPACE, "cp", str(archive), name + ":/runtime/bundle.tgz", "-c", "app", timeout=180)
                smoke.run(kube, "-n", policy.NAMESPACE, "exec", name, "-c", "app", "--", "sh", "-c",
                          "tar xzf /runtime/bundle.tgz -C /runtime && rm /runtime/bundle.tgz")
                if options.case == "root":
                    root_material.publish(kube, policy.NAMESPACE, name, "provider" if name == provider else "consumer", "initial")
                smoke.run(kube, "-n", policy.NAMESPACE, "exec", name, "-c", "app", "--", "touch", "/runtime/bundle-ready")
                smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=condition=Ready", "pod/" + name,
                          "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
        def shape(value, count):
            return (policy.ready(value) and value.get("runtime_count") == 1 and all(
                value.get("targets", {}).get(protocol + "://" + host + ":" + port, {}).get("available_instances") == count
                for protocol, port in (("tri", "7070"), ("dubbo", "7071"))))

        first = policy.wait_for("isolated discovery", lambda: rpc("inspect"), lambda v: shape(v, 1), options.timeout)
        record("isolated-baseline", first)
        business(count=2)
        change = {"withdrawal": "withdrawal", "rbac": "deny", "provider-agent": "provider-agent-restart", "root": "root-overlap"}[options.case]
        if options.case == "root" and options.all_streams:
            change = "root-rollover"
        if options.case == "root":
            initial_root = root_snapshot("initial")
            if options.retired_peer:
                legacy_pin = legacy_peer("root-peer-pin")
                legacy_ids = {c["channel_id"] for c in legacy_pin["connections_before"]}
                if legacy_ids != {c["channel_id"] for c in legacy_pin["connections_after"]}:
                    raise AssertionError("Pinned peer changed channels during its first calls")
        kinds = ("unary", "stream", "client-stream", "bidi") if options.all_streams else ("unary", "stream")
        tokens = {kind: kind + "-" + run for kind in kinds}
        for kind, token in tokens.items():
            started = rpc("hold-start", message=token, key=kind)
            record("started-" + kind, started)
            if started.get("status") != "ACTIVE" or started.get("terminals") != 0:
                raise AssertionError("Held request completed before policy change: " + kind)
        for kind, token in tokens.items():
            observation = read_json(provider, "/runtime/holds/" + token + ".json")
            record("provider-dispatched-" + kind, observation)
            if (observation.get("status") != "ACTIVE" or observation.get("kind") != kind
                    or observation.get("attempts") != 1 or observation.get("cancelled") is not False):
                raise AssertionError("No active provider dispatch before policy change")
            if kind in ("client-stream", "bidi") and (observation.get("request_messages") != ["hold:" + token]
                                                       or observation.get("half_closed") is not False):
                raise AssertionError("Input stream must have exactly one message before the change")
        for kind in ("stream", "bidi") if options.all_streams else ("stream",):
            stream = policy.wait_for("first " + kind + " frame", lambda k=kind: rpc("hold-status", message=tokens[k]),
                                    lambda v: v.get("status") == "ACTIVE" and len(v.get("values", [])) == 1, 30)
            record(kind + "-open-before-" + change, stream)
        active = rpc("inspect")
        record("active-admissions-before-" + change, active)
        if active.get("accepted_clusters", {}).get(cluster, {}).get("active_requests") != len(tokens):
            raise AssertionError("Each provider-started request must hold one admission")
        if options.case == "withdrawal":
            print("Withdrawing the sole endpoint with " + str(len(tokens)) + " dispatched calls active", flush=True)
            owned_service = policy.get_object(kube, "service", service)
            before_selector = copy.deepcopy(owned_service["spec"]["selector"])
            withdrawn_selector = {"app": service + "-unselected"}
            smoke.run(kube, "-n", policy.NAMESPACE, "patch", "service", service, "--type=json", "-p", json.dumps([
                {"op": "test", "path": "/metadata/uid", "value": owned_service["metadata"]["uid"]},
                {"op": "test", "path": "/spec/selector", "value": before_selector},
                {"op": "replace", "path": "/spec/selector", "value": withdrawn_selector}]))

            def withdrawn(value):
                return (value.get("connection") == "READY" and value.get("runtime_count") == 1
                        and all(value.get("targets", {}).get(protocol + "://" + host + ":" + port, {})
                                .get("available_instances") == 0 for protocol, port in (("tri", "7070"), ("dubbo", "7071")))
                        and value.get("accepted_endpoints", {}).get(cluster, {}).get("endpoint_count") == 0)

            record("accepted-eds-withdrawal", policy.wait_for("empty EDS", lambda: rpc("inspect"), withdrawn, options.timeout))
            for protocol in ("tri", "dubbo"):
                value = rpc(protocol=protocol)
                record("new-call-after-withdrawal-" + protocol, value)
                if value.get("status") != "RPC_ERROR" or value.get("errors") != {"UNAVAILABLE": 1} or value.get("values"):
                    raise AssertionError("A new call selected the withdrawn endpoint: " + protocol)
            if options.all_streams:
                rejected_streams("UNAVAILABLE", "withdrawal")
        elif options.case == "rbac":
            deny_name = apply_business_deny()
        elif options.case == "root":
            print("Publishing overlapping A+B roots while A's dispatched calls are active", flush=True)
            publish_root("overlap")
            overlap_root = root_snapshot("overlap", initial_root)
            if options.all_streams:
                # Keep the A-authorized requests active across B identity migration and
                # retirement of A, including new-call rejection on an old TLS channel.
                final_root = migrate_and_retire_roots(overlap_root)
        else:
            print("Terminating pilot-agent PID 1 only in the owned provider with active calls", flush=True)
            initial_provider = owned_pod(provider)
            initial_containers = containers(initial_provider)
            if (initial_provider["spec"].get("restartPolicy") != "Always"
                    or set(initial_containers) != {"app", "istio-proxy"}
                    or any(not c["id"] or c["restart_count"] or not c["ready"] for c in initial_containers.values())):
                raise AssertionError("Expected owned provider with two ready, never-restarted containers")
            record("provider-container-baseline", initial_containers)
            result = subprocess.run(kube + ["-n", policy.NAMESPACE, "exec", provider, "-c", "istio-proxy", "--", "sh", "-c",
                    'test "$(cat /proc/1/comm)" = pilot-agent && kill -TERM 1'], text=True, capture_output=True, timeout=35)
            record("provider-agent-sigterm", {"return_code": result.returncode, "target": "owned provider istio-proxy/pilot-agent PID 1"})
            if result.returncode and not transport_failure(result.stderr):
                raise RuntimeError("Could not send guarded provider-agent termination")

            def restarting_containers():
                value = containers(owned_pod(provider))
                report.setdefault("provider_container_observations", []).append(value)
                save()
                return value

            restarted = policy.wait_for("provider pilot-agent container restart", restarting_containers,
                lambda v: v.get("istio-proxy", {}).get("restart_count") == 1
                          and v["istio-proxy"]["ready"] and v["istio-proxy"]["state"] == "running"
                          and v["istio-proxy"]["id"] != initial_containers["istio-proxy"]["id"], options.timeout)
            record("provider-agent-container-restarted", restarted)
            if restarted.get("app", {}).get("id") != initial_containers["app"]["id"] or restarted["app"]["restart_count"]:
                raise AssertionError("Provider application restarted with its agent")
        active = rpc("inspect")
        record("active-admissions-after-" + change, active)
        for kind, token in tokens.items():
            client = rpc("hold-status", message=token)
            server = read_json(provider, "/runtime/holds/" + token + ".json")
            record("still-active-after-" + change + "-" + kind, {"client": client, "provider": server})
            expected_count = 1 if kind in ("stream", "bidi") else 0
            if (client.get("status") != "ACTIVE" or client.get("terminals") != 0
                    or len(client.get("values", [])) != expected_count or server.get("status") != "ACTIVE"
                    or server.get("cancelled") is not False):
                raise AssertionError("Policy change interrupted an already authorized request: " + kind)
        if active.get("accepted_clusters", {}).get(cluster, {}).get("active_requests") != len(tokens):
            raise AssertionError("Policy change released active request admissions early")
        if options.all_streams:
            for kind in ("client-stream", "bidi"):
                token = tokens[kind]
                sent = rpc("hold-send", message=token)
                if sent.get("half_closed") is not True or sent.get("status") != "ACTIVE" or sent.get("terminals"):
                    raise AssertionError("Existing input stream could not send/half-close after " + change)
                def input_observation(t=token):
                    return {"client": rpc("hold-status", message=t),
                            "provider": read_json(provider, "/runtime/holds/" + t + ".json")}
                value = policy.wait_for("messages after " + change, input_observation,
                    lambda v: v["provider"].get("request_messages") == ["hold:" + token, token + ":1"]
                              and v["provider"].get("half_closed") is True
                              and len(v["client"].get("values", [])) == (2 if kind == "bidi" else 0), 30)
                record("input-after-" + change + "-" + kind, value)
                if (value["client"].get("status") != "ACTIVE" or value["client"].get("terminals")
                        or value["provider"].get("status") != "ACTIVE" or value["provider"].get("cancelled")):
                    raise AssertionError("Input stream completed/cancelled before its provider release")
            active = rpc("inspect")
            record("admissions-after-input-half-close", active)
            if active.get("accepted_clusters", {}).get(cluster, {}).get("active_requests") != len(tokens):
                raise AssertionError("Half-close released an active response admission")
        if options.all_streams:
            # Observing four completed calls sequentially must not delay the later provider
            # releases; retain the same 180-second call budget and all terminal assertions.
            smoke.run(kube, "-n", policy.NAMESPACE, "exec", provider, "-c", "app", "--", "touch",
                      *("/runtime/holds/" + token + ".release" for token in tokens.values()))
            record("all-provider-releases-published", {"tokens": tokens, "count": len(tokens)})
        for kind, token in tokens.items():
            if not options.all_streams:
                smoke.run(kube, "-n", policy.NAMESPACE, "exec", provider, "-c", "app", "--", "touch",
                          "/runtime/holds/" + token + ".release")
            value = policy.wait_for("held completion " + kind, lambda t=token: rpc("hold-status", message=t),
                                   lambda v: v.get("status") != "ACTIVE", 30)
            server = read_json(provider, "/runtime/holds/" + token + ".json")
            record("completed-after-" + change + "-" + kind, {"client": value, "provider": server})
            if kind == "unary":
                expected = ["inflight:hold:" + token]
            elif kind == "client-stream":
                expected = ["inflight:hold:" + token + ":collected=2"]
            else:
                expected = ["inflight:hold:" + token + ":" + str(i) for i in range(2 if kind == "bidi" else 3)]
            if (value.get("status") != "OK" or value.get("terminals") != 1 or value.get("values") != expected
                    or server.get("status") != "RELEASED" or server.get("attempts") != 1
                    or server.get("cancelled") is not False):
                raise AssertionError("Held call did not finish exactly once on its original provider")
            check_terminal_timing(value)
            rpc("hold-forget", message=token)
        released = rpc("inspect")
        record("admissions-released-after-completion", released)
        if released.get("accepted_clusters", {}).get(cluster, {}).get("active_requests") != 0:
            raise AssertionError("Held admissions leaked after completion")
        if options.case == "withdrawal":
            smoke.run(kube, "-n", policy.NAMESPACE, "patch", "service", service, "--type=json", "-p", json.dumps([
                {"op": "test", "path": "/metadata/uid", "value": owned_service["metadata"]["uid"]},
                {"op": "test", "path": "/spec/selector", "value": withdrawn_selector},
                {"op": "replace", "path": "/spec/selector", "value": before_selector}]))
        elif options.case == "root":
            if not options.all_streams:
                final_root = migrate_and_retire_roots(overlap_root)
        else:
            if options.case == "provider-agent":
                # Finish held calls before the fresh-policy checks so the two lifecycle checks
                # each retain their existing bounded RPC budget, even on slow agent restarts.
                deny_name = apply_business_deny()
            delete_owned("authorizationpolicy", deny_name)
            record("business-allow-restored", policy.wait_for("business ALLOW restored", lambda: rpc(),
                   lambda v: policy.successful(v, 1) and v["values"] == ["inflight:native-inflight"], options.timeout))
        final = policy.wait_for("endpoint restored", lambda: rpc("inspect"), lambda v: shape(v, 1), options.timeout)
        record("restored-discovery", final)
        if final["process_start_ms"] != first["process_start_ms"]:
            raise AssertionError("Consumer JVM restarted during policy change")
        for name, uid in report["pod_uids"].items():
            pod = owned_pod(name)
            if pod["metadata"]["uid"] != uid:
                raise AssertionError("A fixture Pod was replaced")
            observed = containers(pod)
            for container_name, value in observed.items():
                expected = 1 if options.case == "provider-agent" and name == provider and container_name == "istio-proxy" else 0
                if value["restart_count"] != expected or not value["ready"]:
                    raise AssertionError("Unexpected fixture container restart count or readiness")
            if options.case == "provider-agent" and name == provider:
                if observed["app"]["id"] != initial_containers["app"]["id"] or observed["istio-proxy"]["id"] == initial_containers["istio-proxy"]["id"]:
                    raise AssertionError("Provider app/agent container identity mismatch")
                record("provider-final-container-identities", observed)
        for method in ("echo", "async", "stream"):
            business(method)
        business(protocol="dubbo", count=2)
        if options.all_streams:
            for kind in ("client-stream", "bidi"):
                token = "recover-" + kind + "-" + run
                rpc("hold-start", message=token, key=kind)
                rpc("hold-send", message=token)
                smoke.run(kube, "-n", policy.NAMESPACE, "exec", provider, "-c", "app", "--", "touch",
                          "/runtime/holds/" + token + ".release")
                value = policy.wait_for("recovered " + kind, lambda: rpc("hold-status", message=token),
                                        lambda v: v.get("status") != "ACTIVE", 30)
                server = read_json(provider, "/runtime/holds/" + token + ".json")
                record("recovered-" + kind, {"client": value, "provider": server})
                expected = (["inflight:hold:" + token + ":collected=2"] if kind == "client-stream"
                            else ["inflight:hold:" + token + ":" + str(i) for i in range(2)])
                if (value.get("status") != "OK" or value.get("terminals") != 1 or value.get("values") != expected
                        or server.get("status") != "RELEASED" or server.get("attempts") != 1 or server.get("cancelled")
                        or server.get("request_messages") != ["hold:" + token, token + ":1"] or server.get("half_closed") is not True):
                    raise AssertionError("Input stream did not recover exactly once")
                check_terminal_timing(value)
                rpc("hold-forget", message=token)
            value = rpc("inspect")
            record("admissions-after-input-recovery", value)
            if value.get("accepted_clusters", {}).get(cluster, {}).get("active_requests") != 0:
                raise AssertionError("Recovered input stream leaked admissions")
        record("same-process-and-pods", {"process_start_ms": final["process_start_ms"], "pod_uids": report["pod_uids"]})
    except Exception as caught:
        failure = caught
        if "tokens" in locals():
            for kind, token in tokens.items():
                try:
                    record("failure-observation-" + kind, {"client": rpc("hold-status", message=token),
                           "provider": read_json(provider, "/runtime/holds/" + token + ".json")})
                except Exception as observation_error:
                    record("failure-observation-" + kind, {"error_type": type(observation_error).__name__})
        report["status"] = "failed"
        report["error_type"] = type(caught).__name__
        if isinstance(caught, AssertionError):
            report["assertion"] = str(caught)
        save()
    finally:
        print("Removing only owned in-flight resources and checking the retained echo policies", flush=True)
        errors = []
        for kind, name, uid in reversed(resources):
            try:
                value = smoke.run(kube, "-n", policy.NAMESPACE, "get", kind, name, "--ignore-not-found=true", "-o", "json")
                current = json.loads(value) if value.strip() else None
                if current is None:
                    continue
                if current["metadata"].get("labels", {}).get(OWNER) != run or (uid is not None and current["metadata"]["uid"] != uid):
                    raise AssertionError("Refusing to delete an unowned in-flight resource")
                smoke.run(kube, "-n", policy.NAMESPACE, "delete", kind, name, "--ignore-not-found=true", "--wait=false")
            except Exception as caught:
                errors.append(type(caught).__name__)
        try:
            for kind, name, uid in resources:
                policy.wait_for("in-flight cleanup", lambda k=kind, n=name: smoke.run(kube, "-n", policy.NAMESPACE,
                                "get", k, n, "--ignore-not-found=true", "-o", "name"), lambda v: not v.strip(), options.timeout)
            for kind, before in baseline.items():
                current = policy.get_object(kube, kind, "echo")
                if current["metadata"]["uid"] != before["metadata"]["uid"] or current["spec"] != before["spec"]:
                    raise AssertionError("Retained echo policy changed: " + kind)
            for name, uid in retained_pods.items():
                if policy.get_object(kube, "pod", name)["metadata"]["uid"] != uid:
                    raise AssertionError("Retained echo Pod was replaced")
            record("echo-baseline-after", policy.wait_for("final echo ADS", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
            response = smoke.rpc(kube)
            record("echo-business-after", response)
            if not policy.successful(response, 1):
                raise AssertionError("Retained echo business did not recover")
        except Exception as caught:
            errors.append(type(caught).__name__)
        if root_material is not None:
            root_material.close()
        if errors:
            report["status"] = "cleanup_failed"
            report["cleanup_errors"] = errors
            if failure is None:
                failure = RuntimeError("In-flight resource cleanup failed")
        else:
            report["cleaned_up"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Native in-flight " + options.case + " passed; owned resources removed and echo baseline retained", flush=True)


if __name__ == "__main__":
    main()
