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

"""Native cross-Service routing with per-target metadata, separate ports and SPIFFE identities."""

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
OWNER = "dubbo.apache.org/native-cross-service-owner"
KINDS = ("virtualservice", "destinationrule", "peerauthentication", "authorizationpolicy")
PRINCIPAL = "spiffe://cluster.local/ns/dubbo-xds-native/sa/native-xds"


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
    source, target = ("native-cross-" + run[:10] + suffix for suffix in ("-source", "-target"))
    consumer = source + "-client"
    target_sa = target + "-sa"
    hosts = {name: name + "." + policy.NAMESPACE + ".svc.cluster.local" for name in (source, target)}
    source_app, target_app = "native-cross-source-app", "native-cross-target-app"
    target_principal = "spiffe://cluster.local/ns/" + policy.NAMESPACE + "/sa/" + target_sa
    source_key = "tri://" + hosts[source] + ":7070"
    binary_key = "dubbo://" + hosts[source] + ":7071"
    source_cluster = "outbound|7070||" + hosts[source]
    target_cluster = "outbound|8080||" + hosts[target]
    target_metadata = "outbound|8081||" + hosts[target]
    report = {"owner": run, "namespace": policy.NAMESPACE, "status": "running", "cleaned_up": False,
              "services": hosts, "cases": [], "pod_uids": {},
              "scope": "Normal Dubbo SDK references with Istio cross-Service routes, per-target metadata and separate workload identities"}
    options.output.parent.mkdir(parents=True, exist_ok=True)
    options.output.with_suffix(".baseline.json").write_text(json.dumps(
        {"apiVersion": "v1", "kind": "List", "items": [policy.configuration(x) for x in baseline.values()]}, indent=2) + "\n")
    resources = []

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
            raise AssertionError("Refusing to replace an existing cross-Service resource")
        resources.append((kind, name, None))
        policy.apply(kube, value)
        resources[-1] = (kind, name, policy.get_object(kube, kind, name)["metadata"]["uid"])
        report["owned_resources"] = [{"kind": k, "name": n, "uid": uid} for k, n, uid in resources]
        save()

    def patch(kind, name, path, before, after):
        current = policy.get_object(kube, kind, name)
        expected_uid = next(uid for k, n, uid in resources if k == kind and n == name)
        if (current["metadata"].get("labels", {}).get(OWNER) != run
                or current["metadata"]["uid"] != expected_uid):
            raise AssertionError("Refusing to patch an unowned or replaced resource")
        smoke.run(kube, "-n", policy.NAMESPACE, "patch", kind, name, "--type=json", "-p", json.dumps([
            {"op": "test", "path": "/metadata/uid", "value": current["metadata"]["uid"]},
            {"op": "test", "path": path, "value": before}, {"op": "replace", "path": path, "value": after}]))

    def transport_failure(stderr):
        return any(text in stderr.lower() for text in ("connection reset by peer", "unexpected eof"))

    def rpc(method="echo", protocol="tri", count=1):
        name = uuid.uuid4().hex
        path = "/runtime/commands/" + name
        properties = "method=" + method + "\nprotocol=" + protocol + "\ncount=" + str(count) + "\nmessage=native-cross\ntimeout=3000\n"
        command = kube + ["-n", policy.NAMESPACE, "exec", "-i", consumer, "-c", "app", "--", "sh", "-c",
                          'cat > "$1.tmp" && mv "$1.tmp" "$1.request"', "sh", path]
        value = subprocess.run(command, input=properties, text=True, capture_output=True, timeout=60)
        if value.returncode:
            if not transport_failure(value.stderr):
                raise RuntimeError(value.stderr.strip() or "Could not publish cross-Service command")
            # The atomic request may already have reached the consumer. Observe the same UUID; never re-dispatch it.
            report.setdefault("transport_observations", []).append({"phase": "publish", "method": method, "command": name})
            save()
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            value = subprocess.run(kube + ["-n", policy.NAMESPACE, "exec", consumer, "-c", "app", "--", "sh", "-c",
                                          'cat "$1"', "sh", path + ".json"],
                                   text=True, capture_output=True, timeout=30)
            if value.returncode == 0:
                return json.loads(value.stdout)
            if "No such file" not in value.stderr:
                if not transport_failure(value.stderr):
                    raise RuntimeError(value.stderr.strip() or "Could not read cross-Service command")
                report.setdefault("transport_observations", []).append({"phase": "read", "method": method, "command": name})
                save()
            # Keep result files until owned Pod cleanup so a lost stdout stream cannot destroy evidence.
            time.sleep(1)
        raise TimeoutError("Native cross-Service command did not finish")

    def business(identity, method="echo", protocol="tri", count=1):
        value = rpc(method, protocol, count)
        record("business-" + identity + "-" + method + "-" + protocol, value)
        expected = [identity + ":native-cross"] * count if method != "stream" else [
            identity + ":native-cross:" + str(i) for _ in range(count) for i in range(3)]
        if not policy.successful(value, len(expected)) or value["values"] != expected:
            raise AssertionError("Cross-Service business reached an unexpected provider: " + identity + "/" + method)
        return value

    def route(name, port):
        return [{"route": [{"destination": {"host": hosts[name], "port": {"number": port}}}]}]

    def shape(value, cluster, application, port):
        state = value.get("targets", {}).get(source_key, {})
        instances = state.get("instances", [])
        return (value.get("connection") == "READY" and value.get("runtime_count") == 1
                and state.get("available_instances") == 1 and len(instances) == 1
                and instances[0].get("cluster") == cluster and instances[0].get("application") == application
                and instances[0].get("workload_port") == port
                and value.get("targets", {}).get(binary_key, {}).get("available_instances") == 1)

    save()
    failure = None
    try:
        print("Checking retained echo and creating isolated cross-Service resources", flush=True)
        record("echo-baseline", policy.wait_for("baseline ADS", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
        create("serviceaccount", {"apiVersion": "v1", "kind": "ServiceAccount", "metadata": metadata(target_sa)})
        for name, service_ports, workload_ports in ((source, (7070, 7072, 7071), (17070, 17071, 20880)),
                                                   (target, (8080, 8081, 8082), (18070, 18071, 28080))):
            create("service", {"apiVersion": "v1", "kind": "Service", "metadata": metadata(name),
                               "spec": {"selector": {"app": name}, "ports": [
                                   {"name": "grpc-triple", "port": service_ports[0], "targetPort": workload_ports[0]},
                                   {"name": "grpc-metadata", "port": service_ports[1], "targetPort": workload_ports[1]},
                                   {"name": "tcp-dubbo", "port": service_ports[2], "targetPort": workload_ports[2]}]}})
            create("peerauthentication", {"apiVersion": "security.istio.io/v1", "kind": "PeerAuthentication",
                                         "metadata": metadata(name), "spec": {"selector": {"matchLabels": {"app": name}},
                                         "mtls": {"mode": "STRICT"}, "portLevelMtls": {str(workload_ports[2]): {"mode": "DISABLE"}}}})
            create("authorizationpolicy", {"apiVersion": "security.istio.io/v1", "kind": "AuthorizationPolicy",
                                          "metadata": metadata(name), "spec": {"selector": {"matchLabels": {"app": name}},
                                          "action": "ALLOW", "rules": [
                                              {"from": [{"source": {"principals": [PRINCIPAL.removeprefix("spiffe://")]}}],
                                               "to": [{"operation": {"ports": [str(workload_ports[0])]}}]},
                                              {"from": [{"source": {"principals": [PRINCIPAL.removeprefix("spiffe://")]}}],
                                               "to": [{"operation": {"ports": [str(workload_ports[1])]}}]}]}})
            create("destinationrule", {"apiVersion": "networking.istio.io/v1", "kind": "DestinationRule",
                                       "metadata": metadata(name), "spec": {"host": hosts[name], "trafficPolicy": {"portLevelSettings": [
                                           {"port": {"number": service_ports[0]}, "tls": {"mode": "ISTIO_MUTUAL"}},
                                           {"port": {"number": service_ports[1]}, "tls": {"mode": "ISTIO_MUTUAL"}},
                                           {"port": {"number": service_ports[2]}, "tls": {"mode": "DISABLE"}}]}}})
        create("virtualservice", {"apiVersion": "networking.istio.io/v1", "kind": "VirtualService",
                                  "metadata": metadata(source), "spec": {"hosts": [hosts[source]], "http": route(source, 7070)}})
        targets = json.dumps({"tri://" + hosts[target] + ":8080": {
            "application": target_app, "group": target_app, "service-port": "8081", "protocol": "tri"}})
        with tempfile.TemporaryDirectory(prefix="native-cross-service-") as temporary:
            archive = pathlib.Path(temporary) / "bundle.tgz"
            with tarfile.open(archive, "w:gz") as output:
                output.add(jar, arcname=jar.name)
                output.add(options.bundle / "lib", arcname="lib")
            for name, role, argument, account, overrides in (
                    (source + "-server", "provider", "source", "native-xds", {
                        "NATIVE_XDS_PROVIDER_APP": source_app, "NATIVE_XDS_METADATA_PORT": "17071"}),
                    (target + "-server", "provider", "target", target_sa, {
                        "NATIVE_XDS_PROVIDER_APP": target_app, "NATIVE_XDS_TRIPLE_PORT": "18070",
                        "NATIVE_XDS_METADATA_PORT": "18071", "NATIVE_XDS_BINARY_PORT": "28080"}),
                    (consumer, "consumer", hosts[source], "native-xds", {
                        "NATIVE_XDS_PROVIDER_APP": source_app, "NATIVE_XDS_METADATA_SERVICE_PORT": "7072",
                        "NATIVE_XDS_METADATA_TARGETS": targets})):
                labels = {"app": target if name == target + "-server" else source if role == "provider" else source + "-client"}
                meta = metadata(name, labels)
                meta["annotations"] = {"inject.istio.io/templates": "grpc-agent", "proxy.istio.io/config": json.dumps({
                    "holdApplicationUntilProxyStarts": True})}
                container = {"name": "app", "image": app["image"], "resources": copy.deepcopy(app["resources"]),
                             "env": [{"name": "JAVA_TOOL_OPTIONS", "value": "-XX:ActiveProcessorCount=2"}]
                                    + [{"name": key, "value": value} for key, value in overrides.items()],
                             "command": ["sh", "-c", "while [ ! -f /runtime/bundle-ready ]; do sleep 1; done; "
                                         + "exec java -jar /runtime/" + jar.name + " " + role + " " + argument + " /runtime"],
                             "readinessProbe": {"exec": {"command": ["test", "-f", "/runtime/" +
                                                 ("provider-ready" if role == "provider" else "consumer-ready")]},
                                                 "periodSeconds": 2, "failureThreshold": 90},
                             "volumeMounts": [{"name": "runtime", "mountPath": "/runtime"}]}
                create("pod", {"apiVersion": "v1", "kind": "Pod", "metadata": meta, "spec": {
                    "serviceAccountName": account, "restartPolicy": "Never", "containers": [container],
                    "volumes": [{"name": "runtime", "emptyDir": {}}]}})
            for name in (source + "-server", target + "-server", consumer):
                print("Uploading native cross-Service bundle to " + name, flush=True)
                smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=jsonpath={.status.phase}=Running", "pod/" + name,
                          "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
                report["pod_uids"][name] = policy.get_object(kube, "pod", name)["metadata"]["uid"]
                save()
                smoke.run(kube, "-n", policy.NAMESPACE, "cp", str(archive), name + ":/runtime/bundle.tgz", "-c", "app", timeout=180)
                smoke.run(kube, "-n", policy.NAMESPACE, "exec", name, "-c", "app", "--", "sh", "-c",
                          "tar xzf /runtime/bundle.tgz -C /runtime && rm /runtime/bundle.tgz && touch /runtime/bundle-ready")
                smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=condition=Ready", "pod/" + name,
                          "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
        first = policy.wait_for("source metadata", lambda: rpc("inspect"),
                                lambda v: shape(v, source_cluster, source_app, 17070), options.timeout)
        record("source-discovery", first)
        business("source", count=2)
        business("source", protocol="dubbo", count=2)
        print("Switching the entry Service route to the other Service", flush=True)
        patch("virtualservice", source, "/spec/http", route(source, 7070), route(target, 8080))
        changed = policy.wait_for("target metadata", lambda: rpc("inspect"),
                                  lambda v: shape(v, target_cluster, target_app, 18070), options.timeout)
        record("target-discovery", changed)
        if changed.get("accepted_endpoints", {}).get(target_metadata, {}).get("endpoint_count") != 1:
            raise AssertionError("Target metadata did not use its own CDS/EDS")
        for method in ("echo", "async", "stream"):
            business("target", method)
        business("source", protocol="dubbo")
        print("Requiring a different caller identity on the target metadata port only", flush=True)
        allowed = [PRINCIPAL.removeprefix("spiffe://")]
        other_identity = [target_principal.removeprefix("spiffe://")]
        patch("authorizationpolicy", target, "/spec/rules/1/from/0/source/principals", allowed, other_identity)
        failed = policy.wait_for("metadata caller denied and cache expired", lambda: rpc("inspect"), lambda v:
            v.get("targets", {}).get(source_key, {}).get("available_instances") == 0
            and (v.get("targets", {}).get(source_key, {}).get("last_error") or "").endswith("Exception"), options.timeout)
        record("metadata-caller-identity-denied", failed)
        value = rpc()
        record("metadata-caller-denied-call", value)
        if value.get("status") != "RPC_ERROR" or value.get("errors") != {"UNAVAILABLE": 1} or value.get("values"):
            raise AssertionError("Denied metadata caller did not isolate the destination after cache expiry")
        business("source", protocol="dubbo")
        patch("virtualservice", source, "/spec/http", route(target, 8080), route(source, 7070))
        record("source-identity-isolated", policy.wait_for("source recovery", lambda: rpc("inspect"),
               lambda v: shape(v, source_cluster, source_app, 17070), options.timeout))
        business("source")
        patch("authorizationpolicy", target, "/spec/rules/1/from/0/source/principals", other_identity, allowed)
        patch("virtualservice", source, "/spec/http", route(source, 7070), route(target, 8080))
        record("target-metadata-caller-restored", policy.wait_for("target recovery", lambda: rpc("inspect"),
               lambda v: shape(v, target_cluster, target_app, 18070), options.timeout))
        business("target", "async")
        print("Withdrawing only the target metadata Service port", flush=True)
        target_service = policy.get_object(kube, "service", target)
        ports = target_service["spec"]["ports"]
        without_metadata = [p for p in ports if p["port"] != 8081]
        patch("service", target, "/spec/ports", ports, without_metadata)
        empty = policy.wait_for("missing target metadata", lambda: rpc("inspect"), lambda v:
            v.get("targets", {}).get(source_key, {}).get("available_instances") == 0
            and v.get("accepted_endpoints", {}).get(target_cluster, {}).get("endpoint_count") == 1, options.timeout)
        record("target-business-present-metadata-missing", empty)
        value = rpc()
        record("missing-metadata-call", value)
        if value.get("status") != "RPC_ERROR" or value.get("errors") != {"UNAVAILABLE": 1} or value.get("values"):
            raise AssertionError("Missing metadata selected an incorrect destination")
        business("source", protocol="dubbo")
        patch("service", target, "/spec/ports", without_metadata, ports)
        final = policy.wait_for("metadata port restored", lambda: rpc("inspect"),
                                lambda v: shape(v, target_cluster, target_app, 18070), options.timeout)
        record("target-metadata-restored", final)
        business("target", "stream")
        if final["process_start_ms"] != first["process_start_ms"]:
            raise AssertionError("Consumer JVM restarted during cross-Service updates")
        for name, uid in report["pod_uids"].items():
            pod = policy.get_object(kube, "pod", name)
            if pod["metadata"]["uid"] != uid or any(c.get("restartCount", 0) for c in pod["status"]["containerStatuses"]):
                raise AssertionError("A cross-Service Pod or container restarted")
        record("same-process-and-pods", {"process_start_ms": final["process_start_ms"], "pod_uids": report["pod_uids"]})
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
        print("Removing only owned cross-Service resources and checking retained echo", flush=True)
        errors = []
        for kind, name, uid in reversed(resources):
            try:
                text = smoke.run(kube, "-n", policy.NAMESPACE, "get", kind, name, "--ignore-not-found=true", "-o", "json")
                current = json.loads(text) if text.strip() else None
                if current is None:
                    continue
                if current["metadata"].get("labels", {}).get(OWNER) != run or (uid is not None and current["metadata"]["uid"] != uid):
                    raise AssertionError("Refusing to delete an unowned cross-Service resource")
                smoke.run(kube, "-n", policy.NAMESPACE, "delete", kind, name, "--ignore-not-found=true", "--wait=false")
            except Exception as caught:
                errors.append(type(caught).__name__)
        try:
            for kind, name, uid in resources:
                policy.wait_for("cross-Service cleanup", lambda k=kind, n=name: smoke.run(kube, "-n", policy.NAMESPACE,
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
        if errors:
            report["status"] = "cleanup_failed"
            report["cleanup_errors"] = errors
            if failure is None:
                failure = RuntimeError("Cross-Service resource cleanup failed")
        else:
            report["cleaned_up"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Native cross-Service routing passed; owned resources removed and echo baseline retained", flush=True)


if __name__ == "__main__":
    main()
