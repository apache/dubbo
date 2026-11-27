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

"""Owned Istiod plugged-CA hot reload through the real pilot-agent credential path.

Supply --bundle to also verify normal Dubbo SDK TLS, business and metadata calls.
Add --inflight to hold all four RPC forms admitted under A through every CA phase.
"""

import argparse
import copy
import hashlib
import importlib.util
import json
import pathlib
import shutil
import subprocess
import tempfile
import time
import uuid

import yaml


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, pathlib.Path(__file__).with_name(filename))
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


policy = load("native_policy", "native-policy.py")
material = load("istiod_material", "native-istiod-ca-material.py")
sdk_probe = load("istiod_sdk", "native-istiod-ca-sdk.py")
smoke = policy.smoke
OWNER = "dubbo.apache.org/native-istiod-ca-owner"
CLUSTER_KINDS = {"Namespace", "ClusterRole", "ClusterRoleBinding", "MutatingWebhookConfiguration",
                 "ValidatingWebhookConfiguration"}


def read(kube, *arguments, timeout=60):
    """Retry transient observations only; never redispatch a mutation."""
    for attempt in range(4):
        try:
            value = subprocess.run(kube + list(arguments), text=True, capture_output=True, timeout=timeout)
        except subprocess.TimeoutExpired:
            value = None
        if value is not None and value.returncode == 0:
            return value.stdout
        if value is not None and not any(x in value.stderr.lower() for x in (
                "network is unreachable", "connection reset by peer", "unexpected eof",
                "i/o timeout", "tls handshake timeout", "timeout awaiting response headers")):
            raise RuntimeError("Could not observe real-CA fixture state")
        if attempt < 3:
            time.sleep(2)
    raise RuntimeError("Real-CA observation did not recover")


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True).encode()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--kubectl", default=shutil.which("kubectl") or "kubectl")
    parser.add_argument("--helm", default=shutil.which("helm") or "helm")
    parser.add_argument("--istio-dir", type=pathlib.Path, required=True)
    parser.add_argument("--bundle", type=pathlib.Path,
                        help="Enable normal Dubbo provider/consumer calls using this native application bundle")
    parser.add_argument("--inflight", action="store_true",
                        help="Hold four A-authorized RPC forms through all real CA phases; requires --bundle")
    parser.add_argument("--timeout", type=int, default=240)
    parser.add_argument("--rotation-timeout", type=int, default=420)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    options = parser.parse_args()
    if min(options.timeout, options.rotation_timeout) < 1:
        parser.error("Timeouts must be positive")
    if options.inflight and options.bundle is None:
        parser.error("--inflight requires --bundle")
    chart = options.istio_dir / "manifests/charts/istio-control/istio-discovery"
    if not chart.is_dir():
        parser.error("Provide the official Istio release directory")
    if options.bundle and (not (options.bundle / "dubbo-xds-native-test-app-3.3.7-SNAPSHOT.jar").is_file()
                           or not (options.bundle / "lib").is_dir()):
        parser.error("Build the current native application bundle first")
    kube = [options.kubectl, "--kubeconfig", options.kubeconfig, "--request-timeout=30s"]
    run = uuid.uuid4().hex
    suffix = run[:10]
    control, workload, revision = "xds-ca-control-" + suffix, "xds-ca-work-" + suffix, "xca-" + suffix
    trust = "xds-ca-trust-" + suffix
    principal = "spiffe://cluster.local/ns/" + workload + "/sa/ca-probe"
    report = {"owner": run, "control_namespace": control, "workload_namespace": workload,
              "revision": revision, "trust_configmap": trust, "cases": [], "resources": [],
              "status": "running", "cleaned_up": False,
              "sdk_enabled": options.bundle is not None,
              "inflight_enabled": options.inflight,
              "scope": ("Real isolated Istiod signing-CA consumed by normal Dubbo SDK TLS/business/metadata calls; "
                        + ("four A-admitted RPC forms held through all CA phases" if options.inflight else "in-flight CA continuity is separate")
                        if options.bundle else "Real isolated Istiod signing-CA, root distribution and pilot-agent credentials; Dubbo RPC is a subsequent gate")}
    options.output.parent.mkdir(parents=True, exist_ok=True)
    resources = []
    retained_istiod = json.loads(read(kube, "-n", "istio-system", "get", "deployment", "istiod", "-o", "json"))
    baseline = {kind: json.loads(read(kube, "-n", smoke.NAMESPACE, "get", kind, "echo", "-o", "json")) for kind in (
        "virtualservice", "destinationrule", "peerauthentication", "authorizationpolicy")}
    retained_pods = {name: policy.get_object(kube, "pod", name)["metadata"]["uid"]
                     for name in ("echo-client", "echo-v1", "echo-v2")}
    original_trust = {ns: read(kube, "-n", ns, "get", "configmap", "istio-ca-root-cert", "-o", "json")
                      for ns in ("istio-system", smoke.NAMESPACE)}
    template = policy.get_object(kube, "pod", "echo-client")
    app = next(c for c in template["spec"]["containers"] if c["name"] == "app")
    baseline_file = {"deployment/istio-system/istiod": {
        "uid": retained_istiod["metadata"]["uid"], "content_sha256": digest(retained_istiod["spec"])}}
    for ns, value in original_trust.items():
        value = json.loads(value)
        baseline_file["configmap/" + ns + "/istio-ca-root-cert"] = {
            "uid": value["metadata"]["uid"], "content_sha256": digest(value["data"])}
    for kind, value in baseline.items():
        baseline_file[kind + "/echo"] = {"uid": value["metadata"]["uid"], "spec_sha256": digest(value["spec"])}
    for name in retained_pods:
        value = policy.get_object(kube, "pod", name)
        baseline_file["pod/" + name] = {"uid": value["metadata"]["uid"], "spec_sha256": digest(value["spec"])}
    options.output.with_suffix(".baseline.json").write_text(json.dumps(baseline_file, indent=2) + "\n")

    def save():
        report["resources"] = [{"kind": k, "namespace": ns, "name": n, "uid": uid} for k, ns, n, uid in resources]
        options.output.write_text(json.dumps(report, indent=2) + "\n")

    def record(name, value):
        report["cases"].append({"name": name, "observed_at_ms": int(time.time() * 1000), "result": value})
        save()

    def arguments(kind, namespace, name):
        return ([] if kind in CLUSTER_KINDS else ["-n", namespace]) + ["get", kind, name]

    def get(kind, namespace, name):
        return json.loads(read(kube, *arguments(kind, namespace, name), "-o", "json"))

    def metadata(name, namespace=None, labels=None):
        return {"name": name, **({"namespace": namespace} if namespace else {}),
                "labels": {OWNER: run, **(labels or {})}}

    def create(value):
        kind, meta = value["kind"], value["metadata"]
        namespace, name = meta.get("namespace"), meta["name"]
        if read(kube, *arguments(kind, namespace, name), "--ignore-not-found=true", "-o", "name").strip():
            raise AssertionError("Refusing to replace an existing CA fixture resource")
        resources.append((kind, namespace, name, None))
        save()
        # Never write private Secret inputs to a log or a public manifest.
        result = subprocess.run(kube + ["create", "-f", "-"], input=json.dumps(value),
                                text=True, capture_output=True, timeout=60)
        if result.returncode:
            # A lost create response can still mean success. Recover the exact owner; do not create twice.
            observed = get(kind, namespace, name)
            if observed["metadata"].get("labels", {}).get(OWNER) != run:
                raise RuntimeError("Could not create owned CA fixture resource: " + kind)
        resources[-1] = (kind, namespace, name, get(kind, namespace, name)["metadata"]["uid"])
        save()

    def owned(kind, namespace, name):
        value = get(kind, namespace, name)
        expected = next(uid for k, ns, n, uid in resources if (k, ns, n) == (kind, namespace, name))
        if value["metadata"]["uid"] != expected or value["metadata"].get("labels", {}).get(OWNER) != run:
            raise AssertionError("CA fixture resource ownership changed")
        return value

    def public_agent():
        pod = owned("Pod", workload, "ca-probe")
        if any(c.get("restartCount", 0) for c in pod.get("status", {}).get("containerStatuses", [])):
            raise AssertionError("CA probe restarted")
        cert = subprocess.run(kube + ["-n", workload, "exec", "ca-probe", "-c", "app", "--", "cat",
                                     "/var/lib/istio/data/cert-chain.pem"], capture_output=True, timeout=45)
        root = subprocess.run(kube + ["-n", workload, "exec", "ca-probe", "-c", "app", "--", "cat",
                                     "/var/lib/istio/data/root-cert.pem"], capture_output=True, timeout=45)
        if cert.returncode or root.returncode:
            return None
        return cert.stdout, root.stdout

    ca = material.SigningMaterial(run)
    failure = None
    try:
        record("echo-baseline", policy.wait_for("retained ADS", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
        for name, labels in ((control, {}), (workload, {"istio.io/rev": revision})):
            create({"apiVersion": "v1", "kind": "Namespace", "metadata": metadata(name, labels=labels)})
        create({"apiVersion": "v1", "kind": "Secret", "metadata": metadata("cacerts", control),
                "type": "Opaque", "data": ca.data("initial")})
        values = {"revision": revision, "autoscaleEnabled": False, "replicaCount": 1,
                  "resources": {"requests": {"cpu": "200m", "memory": "256Mi"},
                                "limits": {"cpu": "2", "memory": "1Gi"}},
                  "env": {"ISTIO_MULTIROOT_MESH": "true"},
                  "global": {"istioNamespace": control, "trustBundleName": trust,
                             "proxy": {"resources": {"requests": {"cpu": "100m", "memory": "128Mi"},
                                                    "limits": {"cpu": "1", "memory": "256Mi"}}}},
                  "meshConfig": {"rootNamespace": control,
                                 "discoverySelectors": [{"matchLabels": {OWNER: run}}]}}
        with tempfile.TemporaryDirectory(prefix="dubbo-istiod-chart-") as temporary:
            path = pathlib.Path(temporary) / "values.json"
            path.write_text(json.dumps(values))
            result = subprocess.run([options.helm, "template", "istiod-" + revision, str(chart),
                                     "--namespace", control, "--values", str(path)],
                                    text=True, capture_output=True, timeout=60)
            if result.returncode:
                raise RuntimeError("Could not render the owned Istiod revision")
            objects = [x for x in yaml.safe_load_all(result.stdout) if x]
            if not objects or any(x["kind"] == "CustomResourceDefinition" for x in objects):
                raise AssertionError("Owned CA revision must reuse the existing Istio CRDs")
            record("isolated-chart", {"objects": [{"kind": x["kind"], "name": x["metadata"]["name"]} for x in objects],
                                      "values": values})
            for value in objects:
                value["metadata"].setdefault("labels", {})[OWNER] = run
                if value["kind"] not in CLUSTER_KINDS:
                    value["metadata"]["namespace"] = control
                create(value)
        print("Waiting for owned Istiod revision " + revision, flush=True)
        smoke.run(kube, "-n", control, "rollout", "status", "deployment/istiod-" + revision,
                  "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
        istiod = owned("Deployment", control, "istiod-" + revision)
        report["istiod_images"] = {c["name"]: c["image"] for c in istiod["spec"]["template"]["spec"]["containers"]}
        save()
        istiod_pods = json.loads(read(kube, "-n", control, "get", "pods", "-l", "app=istiod", "-o", "json"))["items"]
        if len(istiod_pods) != 1:
            raise AssertionError("Expected one owned Istiod instance")
        istiod_uid = istiod_pods[0]["metadata"]["uid"]
        injector_name = next(x["metadata"]["name"] for x in objects if x["kind"] == "MutatingWebhookConfiguration")
        policy.wait_for("owned injector CA publication", lambda: owned("MutatingWebhookConfiguration", None, injector_name),
                        lambda v: bool(v.get("webhooks")) and all(w.get("clientConfig", {}).get("caBundle")
                                                                 for w in v["webhooks"]), options.timeout)
        create({"apiVersion": "v1", "kind": "ServiceAccount", "metadata": metadata("ca-probe", workload)})
        pod_meta = metadata("ca-probe", workload)
        pod_meta["annotations"] = {"inject.istio.io/templates": "grpc-agent", "proxy.istio.io/config": json.dumps({
            "holdApplicationUntilProxyStarts": True, "proxyMetadata": {"SECRET_TTL": "6m",
                "SECRET_GRACE_PERIOD_RATIO": "0.5", "SECRET_GRACE_PERIOD_RATIO_JITTER": "0"}})}
        create({"apiVersion": "v1", "kind": "Pod", "metadata": pod_meta, "spec": {
            "serviceAccountName": "ca-probe", "restartPolicy": "Never", "containers": [{
                "name": "app", "image": app["image"], "resources": copy.deepcopy(app["resources"]),
                "command": ["sh", "-c", "while :; do sleep 60; done"],
                "readinessProbe": {"exec": {"command": ["test", "-f", "/var/lib/istio/data/cert-chain.pem"]},
                                   "periodSeconds": 2}}]}})
        smoke.run(kube, "-n", workload, "wait", "--for=condition=Ready", "pod/ca-probe",
                  "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
        probe = owned("Pod", workload, "ca-probe")
        probe_containers = {c["name"]: c["containerID"] for c in probe["status"]["containerStatuses"]}
        trust_mount = next(v for v in probe["spec"]["volumes"] if v["name"] == "istiod-ca-cert")
        if trust_mount.get("configMap", {}).get("name") != trust:
            raise AssertionError("Probe did not use the isolated root ConfigMap")
        sdk = None
        if options.bundle:
            sdk = sdk_probe.SdkProbe(kube, workload, trust, options.bundle, app, metadata, create,
                                     owned, read, record, policy, options.timeout, inflight=options.inflight)
            report["sdk_test_configuration"] = {"service": sdk.host, "principal": sdk.principal,
                "agent_secret_ttl": "6m", "grace_ratio": "0.5", "grace_jitter": "0", "sdk_refresh_interval": "1s",
                "metadata_ttl_ms": 10000, "metadata_refresh_ms": 1000,
                "hold_budget_ms": 1200000 if options.inflight else None,
                "bootstrap": "Pod-local copy of injected file; original Node/ADS/certificate paths retained"}
            save()
            sdk.start()

        def observe_phase(phase):
            expected = ca.public(phase)
            def observe():
                value = public_agent()
                if value is None:
                    return None
                chain, roots = value
                published = json.loads(read(kube, "-n", workload, "get", "configmap", trust, "-o", "json"))
                if (set(ca.fingerprints(roots)) != set(expected["root_authorities_sha256"])
                        or set(ca.fingerprints(published["data"]["root-cert.pem"].encode())) != set(expected["root_authorities_sha256"])):
                    return None
                try:
                    leaf = ca.verify_workload(chain, phase, principal)
                except (RuntimeError, AssertionError):
                    return None
                return {**expected, **leaf, "probe_uid": probe["metadata"]["uid"], "istiod_pod_uid": istiod_uid}
            checked = policy.wait_for("real CA phase " + phase, observe, lambda v: v is not None, options.rotation_timeout)
            record("real-ca-" + phase, checked)
            print("Real Istiod/agent phase accepted: " + phase, flush=True)
            if sdk:
                sdk.phase(ca, phase, options.rotation_timeout)

        observe_phase("initial")
        if sdk and options.inflight:
            sdk.begin_held_calls()
        for phase in ("overlap", "migrated", "final"):
            secret = owned("Secret", control, "cacerts")
            patch = [{"op": "test", "path": "/metadata/uid", "value": secret["metadata"]["uid"]},
                     {"op": "test", "path": "/metadata/resourceVersion", "value": secret["metadata"]["resourceVersion"]},
                     {"op": "replace", "path": "/data", "value": ca.data(phase)}]
            result = subprocess.run(kube + ["-n", control, "patch", "secret", "cacerts", "--type=json", "--patch-file", "/dev/stdin"],
                                    input=json.dumps(patch), text=True, capture_output=True, timeout=60)
            if result.returncode:
                # Observe the guarded patch's outcome instead of reapplying a stale resourceVersion.
                observed = owned("Secret", control, "cacerts")
                if observed.get("data") != ca.data(phase):
                    raise RuntimeError("Could not update owned signing-CA Secret")
            observe_phase(phase)
        if sdk:
            sdk.finish()
        final_probe = owned("Pod", workload, "ca-probe")
        if {c["name"]: c["containerID"] for c in final_probe["status"]["containerStatuses"]} != probe_containers:
            raise AssertionError("Probe containers changed during CA hot reload")
        final_istiod = owned("Deployment", control, "istiod-" + revision)
        if final_istiod["spec"] != istiod["spec"]:
            raise AssertionError("CA phases changed the Istiod deployment")
        final_pods = json.loads(read(kube, "-n", control, "get", "pods", "-l", "app=istiod", "-o", "json"))["items"]
        if len(final_pods) != 1 or final_pods[0]["metadata"]["uid"] != istiod_uid or any(
                c.get("restartCount", 0) for c in final_pods[0]["status"]["containerStatuses"]):
            raise AssertionError("Istiod restarted during CA hot reload")
        record("same-process-containers-and-control-plane", {"probe": probe_containers, "istiod_uid": istiod_uid})
        trust_maps = json.loads(read(kube, "get", "configmaps", "-A", "--field-selector",
                                        "metadata.name=" + trust, "-o", "json"))["items"]
        if any(x["metadata"]["namespace"] not in (control, workload) for x in trust_maps):
            raise AssertionError("Test CA root distribution escaped the discovery selector")
        record("root-distribution-scoped", {"namespaces": [x["metadata"]["namespace"] for x in trust_maps]})
    except BaseException as caught:
        failure = caught
        report["status"] = "failed"
        report["error_type"] = type(caught).__name__
        if isinstance(caught, AssertionError):
            report["assertion"] = str(caught)
        save()
    finally:
        print("Cleaning only the owned CA revision and namespaces", flush=True)
        errors = []
        for kind, namespace, name, uid in reversed(resources):
            try:
                text = read(kube, *arguments(kind, namespace, name), "--ignore-not-found=true", "-o", "json")
                value = json.loads(text) if text.strip() else None
                if value is None:
                    continue
                if value["metadata"].get("labels", {}).get(OWNER) != run or (uid and value["metadata"]["uid"] != uid):
                    raise AssertionError("Refusing to delete an unowned CA fixture resource")
                delete = ([] if kind in CLUSTER_KINDS else ["-n", namespace]) + ["delete", kind, name,
                           "--ignore-not-found=true", "--wait=false"]
                smoke.run(kube, *delete)
            except Exception as caught:
                errors.append(type(caught).__name__)
        try:
            for namespace in (workload, control):
                policy.wait_for("CA namespace cleanup", lambda ns=namespace: read(kube, "get", "namespace", ns,
                                "--ignore-not-found=true", "-o", "name"), lambda v: not v.strip(), options.timeout)
            current = json.loads(read(kube, "-n", "istio-system", "get", "deployment", "istiod", "-o", "json"))
            if current["metadata"]["uid"] != retained_istiod["metadata"]["uid"] or current["spec"] != retained_istiod["spec"]:
                raise AssertionError("Original Istiod changed")
            for ns, before in original_trust.items():
                current = json.loads(read(kube, "-n", ns, "get", "configmap", "istio-ca-root-cert", "-o", "json"))
                before = json.loads(before)
                if current["metadata"]["uid"] != before["metadata"]["uid"] or current["data"] != before["data"]:
                    raise AssertionError("Original mesh roots changed")
            for kind, before in baseline.items():
                current = policy.get_object(kube, kind, "echo")
                if current["metadata"]["uid"] != before["metadata"]["uid"] or current["spec"] != before["spec"]:
                    raise AssertionError("Retained echo policy changed")
            for name, uid in retained_pods.items():
                if policy.get_object(kube, "pod", name)["metadata"]["uid"] != uid:
                    raise AssertionError("Retained echo Pod changed")
            record("echo-baseline-after", policy.wait_for("echo ADS", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
            response = smoke.rpc(kube)
            record("echo-business-after", response)
            if not policy.successful(response, 1):
                raise AssertionError("Retained echo business failed")
        except Exception as caught:
            errors.append(type(caught).__name__)
        ca.close()
        if errors:
            report["status"] = "cleanup_failed"
            report["cleanup_errors"] = errors
            if failure is None:
                failure = RuntimeError("CA revision cleanup failed")
        else:
            report["cleaned_up"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Real Istiod CA phases passed" + (" with normal Dubbo TLS/business/metadata" if options.bundle
          else "; Dubbo RPC rollover remains a separate gate"), flush=True)


if __name__ == "__main__":
    main()
