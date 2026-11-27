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

"""Isolated agent-to-Istiod disconnect and real Service/xDS resource deletion checks."""

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
OWNER = "dubbo.apache.org/native-control-owner"
KINDS = ("virtualservice", "destinationrule", "peerauthentication", "authorizationpolicy")
CDS_TYPE = "type.googleapis.com/envoy.config.cluster.v3.Cluster:"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--kubectl", default=shutil.which("kubectl") or "kubectl")
    parser.add_argument("--bundle", type=pathlib.Path, required=True)
    parser.add_argument("--case", choices=("both", "reconnect", "deletion"), default="both")
    parser.add_argument("--timeout", type=int, default=180)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    options = parser.parse_args()
    if options.timeout < 1:
        parser.error("--timeout must be positive")
    kube = [options.kubectl, "--kubeconfig", options.kubeconfig, "--request-timeout=30s"]
    jar = options.bundle / "dubbo-xds-native-test-app-3.3.7-SNAPSHOT.jar"
    if not jar.is_file() or not (options.bundle / "lib").is_dir():
        raise ValueError("Build the current native application bundle first")
    original = {kind: policy.get_object(kube, kind, "echo") for kind in KINDS}
    echo_pods = {name: policy.get_object(kube, "pod", name)["metadata"]["uid"] for name in smoke.PODS}
    template = policy.get_object(kube, "pod", "echo-client")
    app = next(c for c in template["spec"]["containers"] if c["name"] == "app")
    agent = next(c for c in template["spec"]["containers"] if c["name"] == "istio-proxy")
    environment = {e["name"]: e.get("value") for e in agent["env"]}
    upstream = json.loads(environment["PROXY_CONFIG"]).get("discoveryAddress") or environment["CA_ADDR"]
    upstream_host, upstream_port = upstream.rsplit(":", 1)
    if not upstream_host or int(upstream_port) != 15012:
        raise ValueError("Expected a TLS Istiod endpoint on port 15012")
    run = uuid.uuid4().hex
    service = "native-control-" + run[:12]
    pod, configmap = service + "-client", service + "-relay"
    host = service + "." + policy.NAMESPACE + ".svc.cluster.local"
    clusters = {"default": "outbound|7070||" + host, "v1": "outbound|7070|v1|" + host,
                "v2": "outbound|7070|v2|" + host, "binary": "outbound|7071||" + host}
    report = {"namespace": policy.NAMESPACE, "owner": run, "service": host, "pod": pod, "case": options.case,
              "status": "running", "cleaned_up": False, "cases": [], "observations": [],
              "injection": {"relay_listener": "127.0.0.1:15013", "upstream": upstream, "istiod_san": upstream_host,
                            "tls": "Opaque forwarding; agent verifies original Istiod certificate; CA stays direct"}}
    options.output.parent.mkdir(parents=True, exist_ok=True)
    options.output.with_suffix(".baseline.json").write_text(json.dumps({"apiVersion": "v1", "kind": "List",
        "items": [policy.configuration(x) for x in original.values()]}, indent=2) + "\n")
    resources = []
    uids = {}

    def save():
        options.output.write_text(json.dumps(report, indent=2) + "\n")

    def record(name, value):
        report["cases"].append({"name": name, "result": value})
        save()

    def metadata(name):
        return {"name": name, "namespace": policy.NAMESPACE, "labels": {OWNER: run}}

    def create(kind, value):
        name = value["metadata"]["name"]
        if smoke.run(kube, "-n", policy.NAMESPACE, "get", kind, name, "--ignore-not-found=true", "-o", "name").strip():
            raise AssertionError("Refusing to replace an existing control-plane test resource")
        resources.append((kind, name))
        policy.apply(kube, value)
        uids[(kind, name)] = policy.get_object(kube, kind, name)["metadata"]["uid"]

    def owned(kind, name):
        value = policy.get_object(kube, kind, name)
        if value["metadata"].get("labels", {}).get(OWNER) != run or value["metadata"]["uid"] != uids[(kind, name)]:
            raise AssertionError("Resource ownership changed: " + kind + "/" + name)
        return value

    def rpc(method="echo", protocol="tri", count=1):
        name = uuid.uuid4().hex
        path = "/runtime/commands/" + name
        properties = "method=" + method + "\nprotocol=" + protocol + "\ncount=" + str(count) + "\nmessage=native-control\ntimeout=3000\n"
        result = subprocess.run(kube + ["-n", policy.NAMESPACE, "exec", "-i", pod, "-c", "app", "--", "sh", "-c",
                                       'cat > "$1.tmp" && mv "$1.tmp" "$1.request"', "sh", path],
                                input=properties, text=True, capture_output=True, timeout=60)
        if result.returncode:
            raise RuntimeError(result.stderr.strip() or "Could not publish control-plane command")
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            response = subprocess.run(kube + ["-n", policy.NAMESPACE, "exec", pod, "-c", "app", "--", "sh", "-c",
                                               'cat "$1" && rm "$1"', "sh", path + ".json"],
                                      text=True, capture_output=True, timeout=30)
            if response.returncode == 0:
                return json.loads(response.stdout)
            if "No such file" not in response.stderr:
                raise RuntimeError(response.stderr.strip() or "Could not read control-plane command")
            time.sleep(1)
        raise TimeoutError("Native control-plane command did not finish")

    def inspect():
        value = rpc("inspect")
        report["observations"].append(value)
        save()
        return value

    def relay():
        return json.loads(smoke.run(kube, "-n", policy.NAMESPACE, "exec", pod, "-c", "app", "--", "cat", "/relay/status.json"))

    def route_name(diagnostics, name):
        return any(r.get("name") == name for routes in diagnostics.get("accepted_routes", {}).values() for r in routes)

    def business(label, expected=None, protocol="tri", method="echo", count=4):
        response = rpc(method, protocol, count)
        record(label, response)
        if not policy.successful(response, count * (3 if method == "stream" else 1)):
            raise AssertionError("Control-plane business call failed: " + label)
        if expected is not None and any(not v.startswith(expected + ":native-control") for v in response["values"]):
            raise AssertionError("Control-plane call did not use the expected route: " + label)
        return response

    def apply_route(name, subset):
        owned("virtualservice", service)
        value = copy.deepcopy(virtualservice)
        value["spec"]["http"] = [{"name": name, "route": [{"destination": {
            "host": host, "port": {"number": 7070}, "subset": subset}, "weight": 100}]}]
        policy.apply(kube, value)

    service_resource = {"apiVersion": "v1", "kind": "Service", "metadata": metadata(service), "spec": {
        "selector": {"app": "native-xds-echo"}, "ports": [
            {"name": "grpc-triple", "port": 7070, "targetPort": 17070},
            {"name": "tcp-dubbo", "port": 7071, "targetPort": 20880}]}}
    initial_route, fresh_route = "native-control-initial-" + run[:12], "native-control-fresh-" + run[:12]
    virtualservice = {"apiVersion": "networking.istio.io/v1", "kind": "VirtualService", "metadata": metadata(service),
                      "spec": {"hosts": [host], "http": [{"name": initial_route, "route": [{"destination": {
                          "host": host, "port": {"number": 7070}, "subset": "v1"}, "weight": 100}]}]}}
    save()
    failure = None
    pod_created = False
    try:
        print("Checking the retained echo baseline", flush=True)
        record("echo-baseline", policy.wait_for("baseline ADS", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
        create("service", service_resource)
        create("destinationrule", {"apiVersion": "networking.istio.io/v1", "kind": "DestinationRule", "metadata": metadata(service),
            "spec": {"host": host, "trafficPolicy": {"portLevelSettings": [
                {"port": {"number": 7070}, "tls": {"mode": "ISTIO_MUTUAL"}},
                {"port": {"number": 7071}, "tls": {"mode": "DISABLE"}}]},
                "subsets": [{"name": "v1", "labels": {"version": "v1"}}, {"name": "v2", "labels": {"version": "v2"}}]}})
        create("virtualservice", virtualservice)
        create("configmap", {"apiVersion": "v1", "kind": "ConfigMap", "metadata": metadata(configmap),
                             "data": {"IstiodRelay.java": pathlib.Path(__file__).with_name("IstiodRelay.java").read_text()}})
        meta = metadata(pod)
        meta["annotations"] = {"inject.istio.io/templates": "grpc-agent", "proxy.istio.io/config": json.dumps({
            "discoveryAddress": "127.0.0.1:15013", "proxyMetadata": {"ISTIOD_SAN": upstream_host}})}
        relay_container = {"name": "relay", "image": app["image"],
                           "command": ["java", "-XX:ActiveProcessorCount=1", "/relay-config/IstiodRelay.java", upstream_host, upstream_port],
                           "resources": {"requests": {"cpu": "100m", "memory": "128Mi"}, "limits": {"cpu": "1", "memory": "256Mi"}},
                           "readinessProbe": {"exec": {"command": ["test", "-f", "/relay/ready"]}, "periodSeconds": 2},
                           "volumeMounts": [{"name": "relay", "mountPath": "/relay"},
                                            {"name": "relay-config", "mountPath": "/relay-config", "readOnly": True}]}
        container = {"name": "app", "image": app["image"], "resources": copy.deepcopy(app["resources"]),
                     "env": [{"name": "JAVA_TOOL_OPTIONS", "value": "-XX:ActiveProcessorCount=2"}],
                     "command": ["sh", "-c", "while [ ! -f /runtime/bundle-ready ]; do sleep 1; done; exec java -jar /runtime/"
                                 + jar.name + " consumer " + host + " /runtime"],
                     "readinessProbe": {"exec": {"command": ["test", "-f", "/runtime/consumer-ready"]}, "periodSeconds": 2},
                     "volumeMounts": [{"name": "runtime", "mountPath": "/runtime"}, {"name": "relay", "mountPath": "/relay"}]}
        create("pod", {"apiVersion": "v1", "kind": "Pod", "metadata": meta, "spec": {
            "serviceAccountName": "native-xds", "restartPolicy": "Never", "containers": [relay_container, container],
            "volumes": [{"name": "runtime", "emptyDir": {}}, {"name": "relay", "emptyDir": {}},
                        {"name": "relay-config", "configMap": {"name": configmap}}]}})
        pod_created = True
        with tempfile.TemporaryDirectory(prefix="native-control-") as temporary:
            archive = pathlib.Path(temporary) / "bundle.tgz"
            with tarfile.open(archive, "w:gz") as output:
                output.add(jar, arcname=jar.name)
                output.add(options.bundle / "lib", arcname="lib")
            print("Starting the isolated consumer and transparent TLS relay", flush=True)
            smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=jsonpath={.status.phase}=Running", "pod/" + pod,
                      "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
            actual = owned("pod", pod)
            injected = next(c for c in actual["spec"]["containers"] if c["name"] == "istio-proxy")
            env = {e["name"]: e.get("value") for e in injected["env"]}
            if (json.loads(env["PROXY_CONFIG"]).get("discoveryAddress") != "127.0.0.1:15013"
                    or env.get("ISTIOD_SAN") != upstream_host or env.get("CA_ADDR") != environment["CA_ADDR"]):
                raise AssertionError("Agent did not retain TLS/CA identity with the relay discovery address")
            smoke.run(kube, "-n", policy.NAMESPACE, "cp", str(archive), pod + ":/runtime/bundle.tgz", "-c", "app", timeout=180)
            smoke.run(kube, "-n", policy.NAMESPACE, "exec", pod, "-c", "app", "--", "sh", "-c",
                      "tar xzf /runtime/bundle.tgz -C /runtime && rm /runtime/bundle.tgz && touch /runtime/bundle-ready")
            smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=condition=Ready", "pod/" + pod,
                      "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
        before = policy.wait_for("initial v1 route", inspect, lambda d: policy.ready(d) and route_name(d, initial_route), options.timeout)
        if before.get("runtime_count") != 1:
            raise AssertionError("Update the native bundle with shared runtime diagnostics")
        record("sdk-baseline", before)
        before_relay = policy.wait_for("active TLS relay", relay, lambda r: r.get("active", 0) >= 1 and r.get("forwarded", 0) >= 1
                                       and r.get("server_to_client_bytes", 0) > 0 and not r.get("blocked"), options.timeout)
        record("relay-baseline", before_relay)
        business("initial-v1-business", "v1")
        if options.case in ("both", "reconnect"):
            print("Closing only this agent's real upstream TCP connection", flush=True)
            smoke.run(kube, "-n", policy.NAMESPACE, "exec", pod, "-c", "app", "--", "touch", "/relay/block")
            blocked = policy.wait_for("closed upstream", relay, lambda r: r.get("blocked") is True and r.get("active") == 0
                                      and r.get("dropped_by_block", 0) > before_relay.get("dropped_by_block", 0), options.timeout)
            record("relay-blocked", blocked)
            apply_route(fresh_route, "v2")
            stale = inspect()
            record("blocked-cached-rds", stale)
            if route_name(stale, fresh_route) or not route_name(stale, initial_route):
                raise AssertionError("Blocked consumer did not retain its previously accepted RDS")
            business("blocked-cached-v1-business", "v1")
            checked = relay()
            record("blocked-upstream-control", checked)
            if checked.get("active") != 0 or checked.get("forwarded") != blocked.get("forwarded"):
                raise AssertionError("An upstream connection bypassed the blocked relay")
            print("Restoring upstream connectivity and awaiting the fresh v2 policy", flush=True)
            smoke.run(kube, "-n", policy.NAMESPACE, "exec", pod, "-c", "app", "--", "rm", "/relay/block")
            recovered = policy.wait_for("fresh recovered RDS", inspect, lambda d: policy.ready(d) and route_name(d, fresh_route), options.timeout)
            record("sdk-recovered", recovered)
            after_relay = relay()
            record("relay-recovered", after_relay)
            if (after_relay.get("active", 0) < 1 or after_relay.get("forwarded", 0) <= before_relay["forwarded"]
                    or after_relay.get("blocked") or after_relay["process_start_ms"] != before_relay["process_start_ms"]):
                raise AssertionError("Relay did not establish a new upstream TCP connection in the same process")
            for method in ("echo", "async", "stream"):
                business("recovered-v2-" + method, "v2", method=method, count=1)
        if options.case in ("both", "deletion"):
            print("Deleting the owned Service and checking actual CDS absence", flush=True)
            owned("service", service)
            smoke.run(kube, "-n", policy.NAMESPACE, "delete", "service", service, "--wait=true")
            absent = policy.wait_for("CDS resource deletion", inspect, lambda d: any(
                d.get("resources", {}).get(CDS_TYPE + name, {}).get("status") == "ABSENT" for name in clusters.values())
                and d.get("targets") and all(t.get("available_instances") == 0 for t in d["targets"].values()), options.timeout)
            record("accepted-resource-deletion", absent)
            for protocol in ("tri", "dubbo"):
                response = rpc(protocol=protocol)
                record("deleted-service-new-call-" + protocol, response)
                if (response.get("status") != "RPC_ERROR" or response.get("calls") != 1
                        or response.get("errors") != {"UNAVAILABLE": 1} or response.get("values")):
                    raise AssertionError("Deleted Service kept a callable stale provider or wrong status: " + protocol)
            print("Recreating the owned Service with the same live references", flush=True)
            if smoke.run(kube, "-n", policy.NAMESPACE, "get", "service", service, "--ignore-not-found=true", "-o", "name").strip():
                raise AssertionError("Deleted Service was recreated by another writer")
            old_uid = uids[("service", service)]
            policy.apply(kube, service_resource)
            recreated = policy.get_object(kube, "service", service)
            if recreated["metadata"].get("labels", {}).get(OWNER) != run or recreated["metadata"]["uid"] == old_uid:
                raise AssertionError("Expected a new owned Service UID")
            uids[("service", service)] = recreated["metadata"]["uid"]
            restored = policy.wait_for("recreated Service", inspect, lambda d: policy.ready(d) and d.get("targets")
                                       and all(t.get("available_instances", 0) > 0 for t in d["targets"].values()), options.timeout)
            record("recreated-service-sdk", restored)
            business("recreated-service-triple", "v2" if options.case == "both" else "v1")
            business("recreated-service-binary", protocol="dubbo")
        final = inspect()
        actual = owned("pod", pod)
        if (final.get("process_start_ms") != before.get("process_start_ms") or final.get("runtime_count") != 1
                or any(c.get("restartCount", 0) for c in actual["status"]["containerStatuses"])):
            raise AssertionError("SDK runtime, process or test containers restarted")
        record("same-process-pod-and-runtime", final)
    except Exception as caught:
        failure = caught
        report["status"] = "failed"
        report["error_type"] = type(caught).__name__
        if isinstance(caught, AssertionError):
            report["assertion"] = str(caught)
        save()
    finally:
        print("Removing owned resources and checking original echo policies/Pods", flush=True)
        errors = []
        if pod_created:
            try:
                owned("pod", pod)
                smoke.run(kube, "-n", policy.NAMESPACE, "exec", pod, "-c", "app", "--", "rm", "-f", "/relay/block")
            except Exception as caught:
                errors.append(type(caught).__name__)
        for kind, name in reversed(resources):
            try:
                value = smoke.run(kube, "-n", policy.NAMESPACE, "get", kind, name, "--ignore-not-found=true", "-o", "json")
                current = json.loads(value) if value.strip() else None
                if current is None:
                    continue
                if (current["metadata"].get("labels", {}).get(OWNER) != run
                        or ((kind, name) in uids and current["metadata"]["uid"] != uids[(kind, name)])):
                    raise AssertionError("Refusing to delete an unowned resource")
                smoke.run(kube, "-n", policy.NAMESPACE, "delete", kind, name, "--ignore-not-found=true", "--wait=false")
            except Exception as caught:
                errors.append(type(caught).__name__)
        try:
            for kind, name in resources:
                policy.wait_for("owned resource cleanup", lambda k=kind, n=name: smoke.run(kube, "-n", policy.NAMESPACE,
                                "get", k, n, "--ignore-not-found=true", "-o", "name"), lambda value: not value.strip(), options.timeout)
            for kind, baseline in original.items():
                current = policy.get_object(kube, kind, "echo")
                if current["metadata"]["uid"] != baseline["metadata"]["uid"] or current["spec"] != baseline["spec"]:
                    raise AssertionError("Original echo policy changed")
            for name, uid in echo_pods.items():
                if policy.get_object(kube, "pod", name)["metadata"]["uid"] != uid:
                    raise AssertionError("Original echo Pod changed")
            record("echo-baseline-after", policy.wait_for("restored echo ADS", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
            response = smoke.rpc(kube)
            record("echo-business-after", response)
            if not policy.successful(response, 1):
                raise AssertionError("Original echo business unavailable")
        except Exception as caught:
            errors.append(type(caught).__name__)
        if errors:
            report["status"] = "cleanup_failed"
            report["cleanup_errors"] = errors
            if failure is None:
                failure = RuntimeError("Control-plane test cleanup failed")
        else:
            report["cleaned_up"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Native control-plane checks passed; owned resources removed and original echo baseline retained", flush=True)


if __name__ == "__main__":
    main()
