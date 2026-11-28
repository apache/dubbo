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

"""Native three-priority transport failure, recovery and Service membership checks."""

import argparse
import copy
import hashlib
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
OWNER = "dubbo.apache.org/native-priority-owner"
ZONE = "dubbo.apache.org/priority-zone"
RACK = "dubbo.apache.org/priority-rack"
KINDS = ("virtualservice", "destinationrule", "peerauthentication", "authorizationpolicy")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--kubectl", default=shutil.which("kubectl") or "kubectl")
    parser.add_argument("--bundle", type=pathlib.Path, required=True)
    parser.add_argument("--timeout", type=int, default=180)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    options = parser.parse_args()
    if options.timeout < 1:
        parser.error("Timeout must be positive")
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
    service = "native-priority-" + run[:10]
    consumer = service + "-client"
    providers = [service + "-p" + str(i) for i in range(3)]
    host = service + "." + policy.NAMESPACE + ".svc.cluster.local"
    keys = ["tri://" + host + ":7070", "dubbo://" + host + ":7071"]
    cluster = "outbound|7070||" + host
    metadata_cluster = "outbound|7072||" + host
    report = {"owner": run, "namespace": policy.NAMESPACE, "status": "running", "cleaned_up": False,
              "service": host, "cases": [], "pod_uids": {}, "bundle_sha256": {
                  path.name: hashlib.sha256(path.read_bytes()).hexdigest()
                  for path in [jar] + sorted((options.bundle / "lib").glob("dubbo-xds-*.jar"))
                  + sorted((options.bundle / "lib").glob("dubbo-rpc-triple-*.jar"))},
              "scope": "Native Istio EDS priorities 0/1/2, business listener failure while metadata/EDS remain, recovery and membership"}
    resources = []
    initial_pods = {}
    options.output.parent.mkdir(parents=True, exist_ok=True)

    def save():
        options.output.write_text(json.dumps(report, indent=2) + "\n")

    def record(name, value):
        report["cases"].append({"name": name, "observed_at_ms": int(time.time() * 1000), "result": value})
        save()

    def metadata(name, labels=None):
        return {"name": name, "namespace": policy.NAMESPACE, "labels": {OWNER: run, **(labels or {})}}

    def create(kind, value):
        name = value["metadata"]["name"]
        if smoke.run(kube, "-n", policy.NAMESPACE, "get", kind, name, "--ignore-not-found=true", "-o", "name").strip():
            raise AssertionError("Refusing to replace an existing priority resource")
        resources.append((kind, name, None))
        policy.apply(kube, value)
        resources[-1] = (kind, name, policy.get_object(kube, kind, name)["metadata"]["uid"])
        report["owned_resources"] = [{"kind": k, "name": n, "uid": uid} for k, n, uid in resources]
        save()

    def owned_pod(name):
        value = policy.get_object(kube, "pod", name)
        uid = next(uid for k, n, uid in resources if k == "pod" and n == name)
        if value["metadata"]["uid"] != uid or value["metadata"].get("labels", {}).get(OWNER) != run:
            raise AssertionError("Refusing to operate an unowned or replaced priority Pod")
        return value

    def containers(pod):
        return {c["name"]: {"id": c["containerID"], "restarts": c.get("restartCount", 0), "ready": c.get("ready", False)}
                for c in pod["status"]["containerStatuses"]}

    def command(pod, directory, properties):
        owned_pod(pod)
        token = uuid.uuid4().hex
        path = "/runtime/" + directory + "/" + token
        value = subprocess.run(kube + ["-n", policy.NAMESPACE, "exec", "-i", pod, "-c", "app", "--", "sh", "-c",
                                      'cat > "$1.tmp" && mv "$1.tmp" "$1.request"', "sh", path],
                               input=properties, text=True, capture_output=True, timeout=60)
        transport = lambda text: any(s in text.lower() for s in ("unexpected eof", "connection reset by peer"))
        if value.returncode:
            if not transport(value.stderr):
                raise RuntimeError(value.stderr.strip() or "Could not publish native priority command")
            report.setdefault("transport_observations", []).append({"phase": "publish", "pod": pod, "command": token})
            save()
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            value = subprocess.run(kube + ["-n", policy.NAMESPACE, "exec", pod, "-c", "app", "--", "cat", path + ".json"],
                                   text=True, capture_output=True, timeout=30)
            if value.returncode == 0:
                return json.loads(value.stdout)
            if "No such file" not in value.stderr and not transport(value.stderr):
                raise RuntimeError(value.stderr.strip() or "Could not observe native priority command")
            # Observe the original UUID after a transport error; never dispatch it twice.
            time.sleep(1)
        raise TimeoutError("Native priority command did not finish")

    def rpc(method="echo", protocol="tri", count=1, timeout=30000):
        return command(consumer, "commands", "method=" + method + "\nprotocol=" + protocol + "\ncount=" + str(count)
                       + "\nmessage=native-priority\ntimeout=" + str(timeout) + "\n")

    def shape(value, size, priorities=None):
        if value.get("connection") != "READY" or value.get("runtime_count") != 1:
            return False
        for key in keys:
            state = value.get("targets", {}).get(key, {})
            if state.get("available_instances") != size or state.get("discovered_endpoints") != size or not state.get("graph_ready"):
                return False
        for name in (cluster, metadata_cluster):
            eds = value.get("accepted_endpoints", {}).get(name, {})
            if eds.get("endpoint_count") != size:
                return False
            localities = eds.get("localities", [])
            if (not localities or sum(l.get("endpoint_count", 0) for l in localities) != size
                    or any(l.get("health_counts") != {"HEALTHY": l.get("endpoint_count")}
                           or l.get("weight", 0) <= 0 for l in localities)):
                return False
            if priorities is not None and sorted(l["priority"] for l in localities) != priorities:
                return False
        return True

    def discovery(label, size=3, priorities=None):
        value = policy.wait_for(label, lambda: rpc("inspect"), lambda v: shape(v, size, priorities), options.timeout)
        record(label, value)
        return value

    def business(identity, label, method="echo", count=1):
        value = rpc(method, count=count)
        record(label + "-" + method, value)
        expected = [identity + ":native-priority"] * count if method != "stream" else [
            identity + ":native-priority:" + str(i) for _ in range(count) for i in range(3)]
        if not policy.successful(value, len(expected)) or value.get("values") != expected:
            raise AssertionError("Priority RPC selected an unexpected provider: " + label + "/" + method)

    def converged_business(identity, label):
        attempts = []
        def probe():
            value = rpc()
            attempts.append(value)
            return value
        expected = [identity + ":native-priority"]
        value = policy.wait_for(label, probe, lambda v: policy.successful(v, 1) and v.get("values") == expected, options.timeout)
        record(label + "-transition-observations", attempts)
        for method, count in (("echo", 6), ("async", 2), ("stream", 1)):
            business(identity, label, method, count)
        return value

    def close_listener(index):
        pod = providers[index]
        before = rpc("inspect")
        value = command(pod, "provider-commands", "method=close-business-listener\n")
        record("close-priority-" + str(index), value)
        if (value.get("status") != "CLOSED" or value.get("closed_port") != 17070
                or value.get("business_listener_open") is not False or not value.get("metadata_listener_open")):
            raise AssertionError("Listener fault did not close business while preserving metadata")
        close_time = value["completed_at_ms"]
        before_success = {key: before["targets"][key]["metadata_successes"] for key in keys}
        before_failures = {key: before["targets"][key]["metadata_failures"] for key in keys}
        def fresh(v):
            return (shape(v, 3, [0, 1, 2]) and int(time.time() * 1000) >= close_time + 10000
                    and all(v["targets"][key].get("metadata_successes", 0) >= before_success[key] + 3
                            and v["targets"][key].get("metadata_failures") == before_failures[key]
                            and not v["targets"][key].get("metadata_failure_statuses") for key in keys))
        checked = policy.wait_for("fresh metadata after business close", lambda: rpc("inspect"), fresh, options.timeout)
        record("fresh-metadata-after-priority-" + str(index) + "-close", checked)
        if containers(owned_pod(pod)) != initial_pods[pod]:
            raise AssertionError("Provider container changed during listener-only fault")
        converged_business("priority-" + str(index + 1), "failover-from-" + str(index))
        discovery("still-three-healthy-EDS-after-failover-" + str(index), priorities=[0, 1, 2])
        binary = rpc(protocol="dubbo", count=3)
        record("binary-discovery-after-close-" + str(index), binary)
        if not policy.successful(binary, 3) or any(v not in [
                "priority-" + str(i) + ":native-priority" for i in range(3)] for v in binary.get("values", [])):
            raise AssertionError("Binary discovery stopped with a Triple-only listener fault")

    save()
    failure = None
    try:
        record("echo-baseline", policy.wait_for("baseline ADS", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
        create("service", {"apiVersion": "v1", "kind": "Service", "metadata": metadata(service), "spec": {
            "selector": {"app": service}, "ports": [
                {"name": "grpc-triple", "port": 7070, "targetPort": 17070},
                {"name": "tcp-dubbo", "port": 7071, "targetPort": 20880},
                {"name": "grpc-metadata", "port": 7072, "targetPort": 17072}]}})
        create("peerauthentication", {"apiVersion": "security.istio.io/v1", "kind": "PeerAuthentication",
            "metadata": metadata(service), "spec": {"selector": {"matchLabels": {OWNER: run}},
            "mtls": {"mode": "STRICT"}, "portLevelMtls": {"20880": {"mode": "DISABLE"}}}})
        create("authorizationpolicy", {"apiVersion": "security.istio.io/v1", "kind": "AuthorizationPolicy",
            "metadata": metadata(service), "spec": {"selector": {"matchLabels": {OWNER: run}}, "action": "ALLOW",
            "rules": [{"from": [{"source": {"principals": ["cluster.local/ns/" + policy.NAMESPACE + "/sa/native-xds"]}}],
                       "to": [{"operation": {"ports": ["17070", "17072"]}}]}]}})
        priority_policy = {
            "loadBalancer": {"simple": "ROUND_ROBIN", "localityLbSetting": {
                "enabled": True, "failoverPriority": [ZONE, RACK]}},
            "outlierDetection": {"consecutive5xxErrors": 5, "interval": "5s", "baseEjectionTime": "30s",
                                 "maxEjectionPercent": 100, "minHealthPercent": 0}}
        create("destinationrule", {"apiVersion": "networking.istio.io/v1", "kind": "DestinationRule",
            "metadata": metadata(service), "spec": {"host": host, "trafficPolicy": {
                # Port policies replace destination-level fields instead of inheriting them.
                "portLevelSettings": [{"port": {"number": port}, "tls": {"mode": mode},
                                      **(copy.deepcopy(priority_policy) if port != 7071 else {})}
                                      for port, mode in ((7070, "ISTIO_MUTUAL"), (7071, "DISABLE"), (7072, "ISTIO_MUTUAL"))]}}})
        with tempfile.TemporaryDirectory(prefix="native-priority-") as temporary:
            archive = pathlib.Path(temporary) / "bundle.tgz"
            with tarfile.open(archive, "w:gz") as output:
                output.add(jar, arcname=jar.name)
                output.add(options.bundle / "lib", arcname="lib")
            for i, name in enumerate(providers + [consumer]):
                provider = i < 3
                role = "provider" if provider else "consumer"
                labels = {"app": service if provider else consumer,
                          ZONE: "b" if i == 2 else "a", RACK: "b" if i in (1, 2) else "a"}
                meta = metadata(name, labels)
                meta["annotations"] = {"inject.istio.io/templates": "grpc-agent", "proxy.istio.io/config": json.dumps({
                    "holdApplicationUntilProxyStarts": True})}
                overrides = {"NATIVE_XDS_METADATA_PORT": "17072", "NATIVE_XDS_LISTENER_CONTROL": "true"} if provider else {
                    "NATIVE_XDS_METADATA_SERVICE_PORT": "7072", "NATIVE_XDS_METADATA_TTL": "10000",
                    "NATIVE_XDS_METADATA_REFRESH": "1000"}
                container = {"name": "app", "image": app["image"], "resources": copy.deepcopy(app["resources"]),
                    "env": [{"name": "JAVA_TOOL_OPTIONS", "value": "-XX:ActiveProcessorCount=2"}]
                           + [{"name": key, "value": value} for key, value in overrides.items()],
                    "command": ["sh", "-c", "while [ ! -f /runtime/bundle-ready ]; do sleep 1; done; "
                                + "rm -f /runtime/provider-ready /runtime/consumer-ready; exec java -jar /runtime/" + jar.name
                                + " " + role + " " + ("priority-" + str(i) if provider else host) + " /runtime"],
                    "readinessProbe": {"exec": {"command": ["test", "-f", "/runtime/" + role + "-ready"]},
                                       "periodSeconds": 2, "failureThreshold": 90},
                    "volumeMounts": [{"name": "runtime", "mountPath": "/runtime"}]}
                create("pod", {"apiVersion": "v1", "kind": "Pod", "metadata": meta, "spec": {
                    "serviceAccountName": "native-xds", "restartPolicy": "Always", "containers": [container],
                    "volumes": [{"name": "runtime", "emptyDir": {}}]}})
            for name in providers + [consumer]:
                print("Uploading native priority bundle to " + name, flush=True)
                smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=jsonpath={.status.phase}=Running",
                          "pod/" + name, "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
                report["pod_uids"][name] = owned_pod(name)["metadata"]["uid"]
                smoke.run(kube, "-n", policy.NAMESPACE, "cp", str(archive), name + ":/runtime/bundle.tgz", "-c", "app", timeout=180)
                smoke.run(kube, "-n", policy.NAMESPACE, "exec", name, "-c", "app", "--", "sh", "-c",
                          "tar xzf /runtime/bundle.tgz -C /runtime && rm /runtime/bundle.tgz && touch /runtime/bundle-ready")
                smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=condition=Ready", "pod/" + name,
                          "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
                initial_pods[name] = containers(owned_pod(name))
        first = discovery("initial-three-priorities", priorities=[0, 1, 2])
        report["initial_containers"] = initial_pods
        converged_business("priority-0", "initial-priority-zero")
        print("Closing priority zero business listener with metadata and EDS retained", flush=True)
        close_listener(0)
        print("Closing priority one business listener with metadata and EDS retained", flush=True)
        close_listener(1)
        for name in providers[:2]:
            owned_pod(name)
            smoke.run(kube, "-n", policy.NAMESPACE, "exec", name, "-c", "app", "--", "sh", "-c",
                      'test "$(cat /proc/1/comm)" = java && kill -TERM 1')
            expected = initial_pods[name]
            observed = policy.wait_for("provider application restart", lambda: containers(owned_pod(name)), lambda v:
                v["app"]["id"] != expected["app"]["id"] and v["app"]["restarts"] == 1 and v["app"]["ready"]
                and v["istio-proxy"] == expected["istio-proxy"], options.timeout)
            record("provider-app-restored-" + name, observed)
        discovery("all-business-listeners-restored", priorities=[0, 1, 2])
        converged_business("priority-0", "recovered-priority-zero")
        name = providers[0]
        current = owned_pod(name)
        smoke.run(kube, "-n", policy.NAMESPACE, "patch", "pod", name, "--type=json", "-p", json.dumps([
            {"op": "test", "path": "/metadata/uid", "value": current["metadata"]["uid"]},
            {"op": "test", "path": "/metadata/labels/app", "value": service},
            {"op": "replace", "path": "/metadata/labels/app", "value": service + "-withdrawn"}]))
        discovery("membership-priority-zero-withdrawn", size=2)
        converged_business("priority-1", "membership-selects-priority-one")
        current = owned_pod(name)
        smoke.run(kube, "-n", policy.NAMESPACE, "patch", "pod", name, "--type=json", "-p", json.dumps([
            {"op": "test", "path": "/metadata/uid", "value": current["metadata"]["uid"]},
            {"op": "test", "path": "/metadata/labels/app", "value": service + "-withdrawn"},
            {"op": "replace", "path": "/metadata/labels/app", "value": service}]))
        final = discovery("membership-priority-zero-returned", priorities=[0, 1, 2])
        converged_business("priority-0", "membership-restores-priority-zero")
        if final["process_start_ms"] != first["process_start_ms"]:
            raise AssertionError("Consumer JVM restarted during priority updates")
        for name in providers + [consumer]:
            state = containers(owned_pod(name))
            expected = initial_pods[name]
            if state["istio-proxy"] != expected["istio-proxy"]:
                raise AssertionError("A fixture pilot-agent changed")
            if name in providers[:2]:
                if state["app"]["restarts"] != 1 or state["app"]["id"] == expected["app"]["id"] or not state["app"]["ready"]:
                    raise AssertionError("Restored provider did not have exactly one intended application restart")
            elif state != expected:
                raise AssertionError("Consumer or priority two restarted")
        record("same-consumer-runtime-and-pods", {"process_start_ms": final["process_start_ms"], "runtime_count": final["runtime_count"],
                                                "pod_uids": report["pod_uids"]})
    except Exception as caught:
        failure = caught
        try:
            record("failure-discovery", rpc("inspect"))
        except Exception as observation:
            record("failure-discovery", {"error_type": type(observation).__name__})
        report["status"] = "failed"
        report["error_type"] = type(caught).__name__
        if isinstance(caught, AssertionError):
            report["assertion"] = str(caught)
        save()
    finally:
        print("Removing owned priority resources and checking retained echo", flush=True)
        errors = []
        for kind, name, uid in reversed(resources):
            try:
                text = smoke.run(kube, "-n", policy.NAMESPACE, "get", kind, name, "--ignore-not-found=true", "-o", "json")
                current = json.loads(text) if text.strip() else None
                if current is None:
                    continue
                if current["metadata"].get("labels", {}).get(OWNER) != run or (uid is not None and current["metadata"]["uid"] != uid):
                    raise AssertionError("Refusing to delete an unowned priority resource")
                smoke.run(kube, "-n", policy.NAMESPACE, "delete", kind, name, "--ignore-not-found=true", "--wait=false")
            except Exception as caught:
                errors.append(type(caught).__name__)
        try:
            for kind, name, uid in resources:
                policy.wait_for("priority cleanup", lambda k=kind, n=name: smoke.run(kube, "-n", policy.NAMESPACE,
                                "get", k, n, "--ignore-not-found=true", "-o", "name"), lambda v: not v.strip(), options.timeout)
            for kind, before in baseline.items():
                current = policy.get_object(kube, kind, "echo")
                if current["metadata"]["uid"] != before["metadata"]["uid"] or current["spec"] != before["spec"]:
                    raise AssertionError("Retained echo policy changed")
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
        if errors:
            report["status"] = "cleanup_failed"
            report["cleanup_errors"] = errors
            if failure is None:
                failure = RuntimeError("Priority cleanup failed")
        else:
            report["cleaned_up"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Native priority transport and membership checks passed", flush=True)


if __name__ == "__main__":
    main()
