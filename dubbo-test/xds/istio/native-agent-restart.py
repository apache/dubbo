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

"""Restart an owned pilot-agent container while preserving the native Dubbo JVM and references."""

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
OWNER = "dubbo.apache.org/native-agent-owner"
KINDS = ("virtualservice", "destinationrule", "peerauthentication", "authorizationpolicy")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--kubectl", default=shutil.which("kubectl") or "kubectl")
    parser.add_argument("--bundle", type=pathlib.Path, required=True)
    parser.add_argument("--new-registry", action="store_true", help="Create and destroy a second FrameworkModel/Registry after bootstrap rewrite")
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
    run = uuid.uuid4().hex
    service = "native-agent-" + run[:12]
    pod = service + "-client"
    host = service + "." + policy.NAMESPACE + ".svc.cluster.local"
    report = {"namespace": policy.NAMESPACE, "owner": run, "service": host, "pod": pod,
              "status": "running", "cleaned_up": False, "cases": [], "observations": [],
              "scope": "Actual pilot-agent container restart; unchanged Dubbo JVM, references and Pod",
              "new_registry": options.new_registry,
              "injection": "SIGTERM pilot-agent PID 1 only in an owned Always-restart consumer Pod"}
    options.output.parent.mkdir(parents=True, exist_ok=True)
    options.output.with_suffix(".baseline.json").write_text(json.dumps({"apiVersion": "v1", "kind": "List",
        "items": [policy.configuration(x) for x in original.values()]}, indent=2) + "\n")
    resources = []
    uids = {}

    def save():
        options.output.write_text(json.dumps(report, indent=2) + "\n")

    def record(name, value):
        report["cases"].append({"name": name, "observed_at_ms": int(time.time() * 1000), "result": value})
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
        report["owned_resources"] = [{"kind": k, "name": n, "uid": uids.get((k, n))} for k, n in resources]
        save()

    def owned(kind, name):
        value = policy.get_object(kube, kind, name)
        if value["metadata"].get("labels", {}).get(OWNER) != run or value["metadata"]["uid"] != uids[(kind, name)]:
            raise AssertionError("Resource ownership changed: " + kind + "/" + name)
        return value

    def transport_failure(stderr):
        return any(text in stderr.lower() for text in ("connection reset by peer", "unexpected eof"))

    def rpc(method="echo", protocol="tri", count=1):
        name = uuid.uuid4().hex
        path = "/runtime/commands/" + name
        properties = "method=" + method + "\nprotocol=" + protocol + "\ncount=" + str(count) + "\nmessage=native-agent\nservice=" + host + "\ntimeout=3000\n"
        command = kube + ["-n", policy.NAMESPACE, "exec", "-i", pod, "-c", "app", "--", "sh", "-c",
                          'cat > "$1.tmp" && mv "$1.tmp" "$1.request"', "sh", path]
        value = subprocess.run(command, input=properties, text=True, capture_output=True, timeout=60)
        if value.returncode:
            if not transport_failure(value.stderr):
                raise RuntimeError(value.stderr.strip() or "Could not publish agent-restart command")
            # The atomic request may already have reached the pod. Observe the same UUID; never re-dispatch it.
            report.setdefault("transport_observations", []).append({"phase": "publish", "method": method, "command": name})
            save()
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            value = subprocess.run(kube + ["-n", policy.NAMESPACE, "exec", pod, "-c", "app", "--", "sh", "-c",
                                          'cat "$1"', "sh", path + ".json"],
                                   text=True, capture_output=True, timeout=30)
            if value.returncode == 0:
                return json.loads(value.stdout)
            if "No such file" not in value.stderr:
                if not transport_failure(value.stderr):
                    raise RuntimeError(value.stderr.strip() or "Could not read agent-restart command")
                report.setdefault("transport_observations", []).append({"phase": "read", "method": method, "command": name})
                save()
            # Keep result files until owned Pod cleanup so a lost stdout stream cannot destroy evidence.
            time.sleep(1)
        raise TimeoutError("Native agent-restart command did not finish")

    def inspect():
        value = rpc("inspect")
        report["observations"].append(value)
        save()
        return value

    def changed_paths(before, after, prefix=""):
        if isinstance(before, dict) and isinstance(after, dict):
            result = []
            for key in sorted(before.keys() | after.keys()):
                path = prefix + "." + key if prefix else key
                if key not in before or key not in after:
                    result.append(path)
                else:
                    result.extend(changed_paths(before[key], after[key], path))
            return result
        return [] if before == after else [prefix]

    def bootstrap_document(filename):
        if not filename.startswith("/etc/istio/proxy/"):
            raise AssertionError("Unexpected injected bootstrap file path")
        # Keep values only in local memory for comparison; report changed field paths, never bootstrap contents.
        return json.loads(smoke.run(kube, "-n", policy.NAMESPACE, "exec", pod, "-c", "app", "--", "cat", filename))

    def containers(value):
        return {c["name"]: {"id": c.get("containerID"), "restart_count": c.get("restartCount", 0),
                             "ready": c.get("ready", False), "state": next(iter(c.get("state", {})), "unknown")}
                for c in value["status"].get("containerStatuses", [])}

    def route_name(diagnostics, name):
        return any(r.get("name") == name for routes in diagnostics.get("accepted_routes", {}).values() for r in routes)

    def business(label, expected=None, protocol="tri", method="echo", count=4):
        response = rpc(method, protocol, count)
        record(label, response)
        if not policy.successful(response, count * (3 if method == "stream" else 1)):
            raise AssertionError("Agent-restart business call failed: " + label)
        if expected is not None and any(not v.startswith(expected + ":native-agent") for v in response["values"]):
            raise AssertionError("Agent-restart call did not use the expected route: " + label)
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
    initial_route, fresh_route = "native-agent-initial-" + run[:12], "native-agent-fresh-" + run[:12]
    virtualservice = {"apiVersion": "networking.istio.io/v1", "kind": "VirtualService", "metadata": metadata(service),
                      "spec": {"hosts": [host], "http": [{"name": initial_route, "route": [{"destination": {
                          "host": host, "port": {"number": 7070}, "subset": "v1"}, "weight": 100}]}]}}
    save()
    failure = None
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
        meta = metadata(pod)
        meta["annotations"] = {"inject.istio.io/templates": "grpc-agent"}
        container = {"name": "app", "image": app["image"], "resources": copy.deepcopy(app["resources"]),
                     "env": [{"name": "JAVA_TOOL_OPTIONS", "value": "-XX:ActiveProcessorCount=2"}],
                     "command": ["sh", "-c", "while [ ! -f /runtime/bundle-ready ]; do sleep 1; done; exec java -jar /runtime/"
                                 + jar.name + " consumer " + host + " /runtime"],
                     "readinessProbe": {"exec": {"command": ["test", "-f", "/runtime/consumer-ready"]}, "periodSeconds": 2},
                     "volumeMounts": [{"name": "runtime", "mountPath": "/runtime"}]}
        create("pod", {"apiVersion": "v1", "kind": "Pod", "metadata": meta, "spec": {
            "serviceAccountName": "native-xds", "restartPolicy": "Always", "containers": [container],
            "volumes": [{"name": "runtime", "emptyDir": {}}]}})
        with tempfile.TemporaryDirectory(prefix="native-agent-") as temporary:
            archive = pathlib.Path(temporary) / "bundle.tgz"
            with tarfile.open(archive, "w:gz") as output:
                output.add(jar, arcname=jar.name)
                output.add(options.bundle / "lib", arcname="lib")
            print("Starting the owned native SDK consumer", flush=True)
            smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=jsonpath={.status.phase}=Running", "pod/" + pod,
                      "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
            actual = owned("pod", pod)
            if actual["spec"].get("restartPolicy") != "Always":
                raise AssertionError("The owned Pod must restart its terminated agent container")
            smoke.run(kube, "-n", policy.NAMESPACE, "cp", str(archive), pod + ":/runtime/bundle.tgz", "-c", "app", timeout=180)
            smoke.run(kube, "-n", policy.NAMESPACE, "exec", pod, "-c", "app", "--", "sh", "-c",
                      "tar xzf /runtime/bundle.tgz -C /runtime && rm /runtime/bundle.tgz && touch /runtime/bundle-ready")
            smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=condition=Ready", "pod/" + pod,
                      "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
        before = policy.wait_for("initial v1 route", inspect, lambda d: policy.ready(d) and route_name(d, initial_route), options.timeout)
        if before.get("runtime_count") != 1:
            raise AssertionError("Update the native bundle with shared runtime diagnostics")
        record("sdk-baseline", before)
        initial_containers = containers(owned("pod", pod))
        if (set(initial_containers) != {"app", "istio-proxy"}
                or any(not c["id"] or c["restart_count"] or not c["ready"] for c in initial_containers.values())):
            raise AssertionError("Expected two ready, never-restarted test containers")
        record("container-baseline", initial_containers)
        injected_app = next(c for c in owned("pod", pod)["spec"]["containers"] if c["name"] == "app")
        bootstrap_path = next(e["value"] for e in injected_app["env"] if e["name"] == "GRPC_XDS_BOOTSTRAP")
        initial_bootstrap = bootstrap_document(bootstrap_path)
        business("initial-v1-business", "v1")
        business("initial-binary-business", protocol="dubbo")
        owned("pod", pod)
        print("Terminating only pilot-agent PID 1 in the owned consumer", flush=True)
        result = subprocess.run(kube + ["-n", policy.NAMESPACE, "exec", pod, "-c", "istio-proxy", "--", "sh", "-c",
                                       'test "$(cat /proc/1/comm)" = pilot-agent && kill -TERM 1'],
                                text=True, capture_output=True, timeout=35)
        record("agent-sigterm", {"return_code": result.returncode, "target": "owned istio-proxy/pilot-agent PID 1"})
        if result.returncode and not transport_failure(result.stderr):
            raise RuntimeError("Could not send the guarded agent termination signal")

        def restarting_containers():
            value = containers(owned("pod", pod))
            report.setdefault("container_observations", []).append(value)
            save()
            return value

        restarted = policy.wait_for("pilot-agent container restart", restarting_containers,
            lambda value: value.get("istio-proxy", {}).get("restart_count") == 1
                          and value["istio-proxy"]["ready"] and value["istio-proxy"]["state"] == "running"
                          and value["istio-proxy"]["id"] != initial_containers["istio-proxy"]["id"], options.timeout)
        record("agent-container-restarted", restarted)
        rewritten_bootstrap = bootstrap_document(bootstrap_path)
        record("agent-bootstrap-rewrite", {"changed_field_paths": changed_paths(initial_bootstrap, rewritten_bootstrap)})

        if restarted.get("app", {}).get("id") != initial_containers["app"]["id"] or restarted["app"]["restart_count"]:
            raise AssertionError("Business container was restarted with the agent")
        recovered = policy.wait_for("SDK ADS after agent restart", inspect,
            lambda d: policy.ready(d) and route_name(d, initial_route), options.timeout)
        record("sdk-recovered", recovered)
        if (recovered.get("process_start_ms") != before.get("process_start_ms") or recovered.get("runtime_count") != 1
                or not set(recovered.get("resources", {})).issuperset(before.get("resources", {}))):
            raise AssertionError("The SDK did not preserve its process, runtime and resource subscriptions")
        business("restarted-agent-cached-v1-business", "v1")
        print("Applying a fresh v2 route through the restarted agent", flush=True)
        apply_route(fresh_route, "v2")
        updated = policy.wait_for("new RDS after agent restart", inspect,
            lambda d: policy.ready(d) and route_name(d, fresh_route) and not route_name(d, initial_route), options.timeout)
        record("post-restart-new-route", updated)
        for method in ("echo", "async", "stream"):
            business("post-restart-v2-" + method, "v2", method=method, count=1)
        business("post-restart-binary", protocol="dubbo")
        if options.new_registry:
            print("Creating a second normal SDK FrameworkModel and Registry after bootstrap rewrite", flush=True)
            value = rpc("new-registry")
            record("new-framework-registry", value)
            if (value.get("status") != "OBSERVED" or value.get("application_model_distinct") is not True
                    or value.get("framework_model_distinct") is not True
                    or value.get("runtime_count_during") != 1 or value.get("runtime_count_after") != 1
                    or value.get("triple_echo") != "v2:native-new-registry"
                    or value.get("triple_async") != "v2:native-new-registry"
                    or value.get("triple_stream") != ["v2:native-new-registry:" + str(i) for i in range(3)]
                    or value.get("binary_echo") not in ("v1:native-new-registry", "v2:native-new-registry")):
                raise AssertionError("New Registry did not share the active runtime and execute real business calls")
            for method in ("echo", "async", "stream"):
                business("original-reference-after-new-registry-" + method, "v2", method=method, count=1)

        final = inspect()
        actual = owned("pod", pod)
        final_containers = containers(actual)
        if (final.get("process_start_ms") != before.get("process_start_ms") or final.get("runtime_count") != 1
                or actual["metadata"]["uid"] != uids[("pod", pod)]
                or final_containers["app"]["id"] != initial_containers["app"]["id"]
                or final_containers["app"]["restart_count"] != 0
                or final_containers["istio-proxy"]["restart_count"] != 1):
            raise AssertionError("Unexpected SDK/Pod/agent identity or restart count")
        record("same-sdk-process-pod-and-runtime", final)
        record("final-container-identities", final_containers)
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
                failure = RuntimeError("Agent-restart cleanup failed")
        else:
            report["cleaned_up"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Native agent-restart checks passed; owned resources removed and original echo baseline retained", flush=True)


if __name__ == "__main__":
    main()
