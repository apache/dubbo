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

"""Real Istio workload certificate renewal in isolated, owned native Dubbo test Pods."""

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
OWNER = "dubbo.apache.org/native-rotation-owner"
KINDS = ("virtualservice", "destinationrule", "peerauthentication", "authorizationpolicy")
PRINCIPAL = "spiffe://cluster.local/ns/dubbo-xds-native/sa/native-xds"


def leaf(observation):
    certificates = list(observation.get("certificates", {}).values())
    if len(certificates) != 1 or certificates[0].get("ready") is not True:
        raise AssertionError("Expected one ready agent-owned public identity certificate")
    value = certificates[0]
    if PRINCIPAL not in value.get("uri_sans", []):
        raise AssertionError("Unexpected workload identity")
    lifetime = value["not_after_ms"] - value["not_before_ms"]
    # Istio 1.31.1 CSR signing backdates NotBefore by two minutes for clock skew.
    # NotAfter is signing time + requested TTL, so the full validity span is eight minutes.
    if not 470000 <= lifetime <= 490000:
        raise AssertionError("Agent did not receive the requested six-minute workload certificate")
    return value


def matches(observation, local, peer, cluster):
    return [c for c in observation.get("connections", []) if c.get("cluster") == cluster
            and c.get("connectivity") == "READY" and c.get("tls", {}).get("ready") is True
            and c["tls"].get("alpn") == "h2" and c["tls"].get("protocol") in ("TLSv1.2", "TLSv1.3")
            and c["tls"].get("local_leaf_sha256") == local["leaf_sha256"]
            and c["tls"].get("peer_leaf_sha256") == peer["leaf_sha256"]]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--kubectl", default=shutil.which("kubectl") or "kubectl")
    parser.add_argument("--bundle", type=pathlib.Path, required=True)
    parser.add_argument("--timeout", type=int, default=180, help="Startup/cleanup timeout")
    parser.add_argument("--rotation-timeout", type=int, default=420)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    options = parser.parse_args()
    if min(options.timeout, options.rotation_timeout) < 1:
        parser.error("Timeouts must be positive")
    kube = [options.kubectl, "--kubeconfig", options.kubeconfig, "--request-timeout=30s"]
    jar = options.bundle / "dubbo-xds-native-test-app-3.3.7-SNAPSHOT.jar"
    if not jar.is_file() or not (options.bundle / "lib").is_dir():
        raise ValueError("Build the current native application bundle first")
    baseline = {kind: policy.get_object(kube, kind, "echo") for kind in KINDS}
    template = policy.get_object(kube, "pod", "echo-client")
    app = next(c for c in template["spec"]["containers"] if c["name"] == "app")
    run = uuid.uuid4().hex
    service = "native-rotation-" + run[:12]
    provider, consumer = service + "-server", service + "-client"
    host = service + "." + policy.NAMESPACE + ".svc.cluster.local"
    cluster = "outbound|7070||" + host
    report = {"owner": run, "namespace": policy.NAMESPACE, "service": host, "status": "running",
              "cleaned_up": False, "cases": [], "observations": [], "pod_uids": {},
              "test_configuration": {"agent_secret_ttl": "6m", "grace_ratio": "0.5", "grace_jitter": "0",
                                     "sdk_refresh_interval": "1s", "bootstrap": "Pod-local copy of injected agent file"},
              "scope": "Workload leaf renewal, SDK reload and new mTLS sessions; no CA rotation or in-flight guarantee"}
    options.output.parent.mkdir(parents=True, exist_ok=True)
    options.output.with_suffix(".baseline.json").write_text(json.dumps(
        {"apiVersion": "v1", "kind": "List", "items": [policy.configuration(x) for x in baseline.values()]}, indent=2) + "\n")
    resources = []

    def save():
        options.output.write_text(json.dumps(report, indent=2) + "\n")

    def record(name, value):
        report["cases"].append({"name": name, "result": value})
        save()

    def metadata(name, labels=None):
        return {"name": name, "namespace": policy.NAMESPACE, "labels": {OWNER: run, **(labels or {})}}

    def create(kind, value):
        name = value["metadata"]["name"]
        existing = smoke.run(kube, "-n", policy.NAMESPACE, "get", kind, name, "--ignore-not-found=true", "-o", "name")
        if existing.strip():
            raise AssertionError("Refusing to replace an existing rotation resource")
        resources.append((kind, name))
        policy.apply(kube, value)

    def read_json(pod, filename):
        return json.loads(smoke.run(kube, "-n", policy.NAMESPACE, "exec", pod, "-c", "app", "--", "cat", filename))

    def rpc(method="echo", protocol="tri", count=1):
        name = uuid.uuid4().hex
        path = "/runtime/commands/" + name
        properties = "method=" + method + "\nprotocol=" + protocol + "\ncount=" + str(count) + "\nmessage=native-rotation\ntimeout=3000\n"
        command = kube + ["-n", policy.NAMESPACE, "exec", "-i", consumer, "-c", "app", "--", "sh", "-c",
                          'cat > "$1.tmp" && mv "$1.tmp" "$1.request"', "sh", path]
        result = subprocess.run(command, input=properties, text=True, capture_output=True, timeout=60)
        if result.returncode:
            raise RuntimeError(result.stderr.strip() or "Could not publish rotation command")
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            result = subprocess.run(kube + ["-n", policy.NAMESPACE, "exec", consumer, "-c", "app", "--",
                                            "sh", "-c", 'cat "$1" && rm "$1"', "sh", path + ".json"],
                                    text=True, capture_output=True, timeout=30)
            if result.returncode == 0:
                return json.loads(result.stdout)
            if "No such file" not in result.stderr:
                raise RuntimeError(result.stderr.strip() or "Could not read rotation command")
            time.sleep(1)
        raise TimeoutError("Native rotation command did not finish")

    def business(method="echo", protocol="tri", count=1):
        value = rpc(method, protocol, count)
        record("business-" + method + "-" + protocol, value)
        if not policy.successful(value, count * (3 if method == "stream" else 1)):
            raise AssertionError("Rotation business call failed: " + method + "/" + protocol)
        if any(not v.startswith("rotation:native-rotation") for v in value["values"]):
            raise AssertionError("Rotation call reached an unexpected provider")
        return value

    save()
    failure = None
    try:
        print("Checking the retained echo baseline", flush=True)
        record("echo-baseline", policy.wait_for("baseline ADS", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
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
        with tempfile.TemporaryDirectory(prefix="native-rotation-") as temporary:
            archive = pathlib.Path(temporary) / "bundle.tgz"
            with tarfile.open(archive, "w:gz") as output:
                output.add(jar, arcname=jar.name)
                output.add(options.bundle / "lib", arcname="lib")
            for name, role, argument in ((provider, "rotation-provider", "rotation"), (consumer, "consumer", host)):
                meta = metadata(name, {"app": service if name == provider else service + "-client"})
                meta["annotations"] = {"inject.istio.io/templates": "grpc-agent", "proxy.istio.io/config": json.dumps({
                    "holdApplicationUntilProxyStarts": True, "proxyMetadata": {"SECRET_TTL": "6m",
                    "SECRET_GRACE_PERIOD_RATIO": "0.5", "SECRET_GRACE_PERIOD_RATIO_JITTER": "0"}})}
                container = {"name": "app", "image": app["image"], "resources": copy.deepcopy(app["resources"]),
                             "env": [{"name": "JAVA_TOOL_OPTIONS", "value": "-XX:ActiveProcessorCount=2"}],
                             "command": ["sh", "-c", "while [ ! -f /runtime/bundle-ready ]; do sleep 1; done; "
                                         + "java -jar /runtime/" + jar.name + " prepare-rotation unused /runtime && "
                                         + "export GRPC_XDS_BOOTSTRAP=/runtime/rotation-bootstrap.json && exec java -jar /runtime/"
                                         + jar.name + " " + role + " " + argument + " /runtime"],
                             "readinessProbe": {"exec": {"command": ["test", "-f", "/runtime/" +
                                                 ("provider-ready" if name == provider else "consumer-ready")]},
                                                 "periodSeconds": 2, "failureThreshold": 90},
                             "volumeMounts": [{"name": "runtime", "mountPath": "/runtime"}]}
                create("pod", {"apiVersion": "v1", "kind": "Pod", "metadata": meta, "spec": {
                    "serviceAccountName": "native-xds", "restartPolicy": "Never", "containers": [container],
                    "volumes": [{"name": "runtime", "emptyDir": {}}]}})
            for name in (provider, consumer):
                print("Uploading the native rotation bundle to " + name, flush=True)
                smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=jsonpath={.status.phase}=Running", "pod/" + name,
                          "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
                pod = policy.get_object(kube, "pod", name)
                report["pod_uids"][name] = pod["metadata"]["uid"]
                agent = next(c for c in pod["spec"]["containers"] if c["name"] == "istio-proxy")
                env = {e["name"]: e.get("value") for e in agent["env"]}
                for key, expected in (("SECRET_TTL", "6m"), ("SECRET_GRACE_PERIOD_RATIO", "0.5"), ("SECRET_GRACE_PERIOD_RATIO_JITTER", "0")):
                    if env.get(key) != expected:
                        raise AssertionError("Injection did not apply the requested rotation environment: " + key)
                smoke.run(kube, "-n", policy.NAMESPACE, "cp", str(archive), name + ":/runtime/bundle.tgz", "-c", "app", timeout=180)
                smoke.run(kube, "-n", policy.NAMESPACE, "exec", name, "-c", "app", "--", "sh", "-c",
                          "tar xzf /runtime/bundle.tgz -C /runtime && rm /runtime/bundle.tgz && touch /runtime/bundle-ready")
                smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=condition=Ready", "pod/" + name,
                          "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
                tuning = read_json(name, "/runtime/rotation-tuning.json")
                record("bootstrap-tuning-" + name, tuning)
                if tuning.get("status") != "PREPARED" or not tuning.get("certificate_providers") or any(
                        v.get("test_refresh_interval") != "1s" for v in tuning["certificate_providers"].values()):
                    raise AssertionError("Test bootstrap polling interval was not prepared")
        business(count=2)
        first_provider = read_json(provider, "/runtime/rotation-observation.json")
        first_consumer = rpc("certificates")
        record("rotation-baseline-files", {"provider": first_provider, "consumer": first_consumer})
        provider_leaf, consumer_leaf = leaf(first_provider), leaf(first_consumer)
        first_matches = matches(first_consumer, consumer_leaf, provider_leaf, cluster)
        if not first_matches:
            raise AssertionError("Baseline native mTLS session did not match both agent public certificates")
        record("rotation-baseline", {"provider": first_provider, "consumer": first_consumer})
        print("Waiting for real Istio leaf renewal and native TLS sessions with both renewed certificates", flush=True)
        deadline = time.monotonic() + options.rotation_timeout
        while time.monotonic() < deadline:
            server = read_json(provider, "/runtime/rotation-observation.json")
            client = rpc("certificates")
            current_provider, current_consumer = leaf(server), leaf(client)
            report["observations"].append({"provider": server, "consumer": client})
            save()
            changed = (current_provider["leaf_sha256"] != provider_leaf["leaf_sha256"]
                       and current_consumer["leaf_sha256"] != consumer_leaf["leaf_sha256"])
            reloaded = (server.get("security_epoch", 0) > first_provider.get("security_epoch", 0)
                        and client.get("security_epoch", 0) > first_consumer.get("security_epoch", 0))
            sessions = matches(client, current_consumer, current_provider, cluster)
            if changed and reloaded and sessions:
                if any(c["tls"].get("channel_id") in {x["tls"].get("channel_id") for x in first_matches} for c in sessions):
                    raise AssertionError("TLS session identity was retained despite both changed leaf certificates")
                record("rotation-loaded-and-handshaken", {"provider": server, "consumer": client})
                break
            print("Rotation observation: both_files_changed=" + str(changed) + ", both_sdk_reloaded=" + str(reloaded)
                  + ", matching_ready_sessions=" + str(len(sessions)), flush=True)
            time.sleep(5)
        else:
            raise TimeoutError("Agent leaf renewal and native SDK TLS consumption did not complete")
        for name, before, after in ((provider, first_provider, server), (consumer, first_consumer, client)):
            pod = policy.get_object(kube, "pod", name)
            if (pod["metadata"]["uid"] != report["pod_uids"][name]
                    or any(c.get("restartCount", 0) for c in pod["status"]["containerStatuses"])
                    or before["process_start_ms"] != after["process_start_ms"]):
                raise AssertionError("Rotation restarted the SDK process or Pod")
        for method in ("echo", "async", "stream"):
            business(method)
        business(protocol="dubbo", count=4)
        record("same-processes-and-pods", {"provider_start_ms": server["process_start_ms"],
                                           "consumer_start_ms": client["process_start_ms"], "pod_uids": report["pod_uids"]})
    except Exception as caught:
        failure = caught
        report["status"] = "failed"
        report["error_type"] = type(caught).__name__
        if isinstance(caught, AssertionError):
            report["assertion"] = str(caught)
        save()
    finally:
        print("Removing only owned rotation resources and checking the retained echo policies", flush=True)
        errors = []
        for kind, name in reversed(resources):
            try:
                value = smoke.run(kube, "-n", policy.NAMESPACE, "get", kind, name, "--ignore-not-found=true", "-o", "json")
                current = json.loads(value) if value.strip() else None
                if current is None:
                    continue
                if current["metadata"].get("labels", {}).get(OWNER) != run:
                    raise AssertionError("Refusing to delete an unowned rotation resource")
                smoke.run(kube, "-n", policy.NAMESPACE, "delete", kind, name, "--ignore-not-found=true", "--wait=false")
            except Exception as caught:
                errors.append(type(caught).__name__)
        try:
            for kind, name in resources:
                policy.wait_for("rotation cleanup", lambda k=kind, n=name: smoke.run(kube, "-n", policy.NAMESPACE,
                                "get", k, n, "--ignore-not-found=true", "-o", "name"), lambda v: not v.strip(), options.timeout)
            for kind, before in baseline.items():
                current = policy.get_object(kube, kind, "echo")
                if current["metadata"]["uid"] != before["metadata"]["uid"] or current["spec"] != before["spec"]:
                    raise AssertionError("Retained echo policy changed: " + kind)
            record("echo-baseline-after", policy.wait_for("final echo ADS", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
            response = smoke.rpc(kube)
            record("echo-business-after", response)
            if not policy.successful(response, 1):
                raise AssertionError("Retained echo business did not recover")
        except Exception as caught:
            errors.append(type(caught).__name__)
        if errors:
            report["status"] = "cleanup_failed"
            report["cleanup_errors"] = errors
            if failure is None:
                failure = RuntimeError("Rotation resource cleanup failed")
        else:
            report["cleaned_up"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Native certificate rotation passed; owned resources removed and echo baseline retained", flush=True)


if __name__ == "__main__":
    main()
