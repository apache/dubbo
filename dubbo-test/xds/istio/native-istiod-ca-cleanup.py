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

"""Resume owner/UID guarded cleanup from a terminal real-CA run report."""

import argparse
import hashlib
import importlib.util
import json
import pathlib
import re
import shutil
import subprocess
import time

spec = importlib.util.spec_from_file_location("real_ca", pathlib.Path(__file__).with_name("native-istiod-ca.py"))
real_ca = importlib.util.module_from_spec(spec)
spec.loader.exec_module(real_ca)
policy, smoke = real_ca.policy, real_ca.smoke


def read(kube, *arguments):
    for attempt in range(4):
        value = subprocess.run(kube + list(arguments), text=True, capture_output=True, timeout=45)
        if value.returncode == 0:
            return value.stdout
        if not any(x in value.stderr.lower() for x in (
                "network is unreachable", "connection reset by peer", "unexpected eof",
                "i/o timeout", "tls handshake timeout", "timeout awaiting response headers")):
            raise RuntimeError("Could not observe CA fixture resource")
        if attempt < 3:
            time.sleep(2)
    raise RuntimeError("CA fixture observation did not recover")


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True).encode()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--kubectl", default=shutil.which("kubectl") or "kubectl")
    parser.add_argument("--report", type=pathlib.Path, required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    parser.add_argument("--timeout", type=int, default=240)
    options = parser.parse_args()
    run = json.loads(options.report.read_text())
    if run.get("status") not in ("failed", "cleanup_failed") or not re.fullmatch("[a-f0-9]{32}", run.get("owner", "")):
        raise ValueError("Cleanup requires a terminal failed CA report")
    suffix = run["owner"][:10]
    namespaces = {run["control_namespace"], run["workload_namespace"]}
    if namespaces != {"xds-ca-control-" + suffix, "xds-ca-work-" + suffix}:
        raise ValueError("Report namespaces do not match the CA fixture owner")
    kube = [options.kubectl, "--kubeconfig", options.kubeconfig, "--request-timeout=20s"]
    result = {"owner": run["owner"], "status": "running", "deleted": [], "cleaned_up": False}
    options.output.parent.mkdir(parents=True, exist_ok=True)

    def save():
        options.output.write_text(json.dumps(result, indent=2) + "\n")

    def snapshot():
        values = {}
        for kind, namespace, name in (
                ("deployment", "istio-system", "istiod"),
                ("configmap", "istio-system", "istio-ca-root-cert"),
                ("configmap", smoke.NAMESPACE, "istio-ca-root-cert")):
            item = json.loads(read(kube, "-n", namespace, "get", kind, name, "-o", "json"))
            values[kind + "/" + namespace + "/" + name] = {
                "uid": item["metadata"]["uid"], "content_sha256": digest(item.get("spec", item.get("data")))}
        for kind, name in [(k, "echo") for k in (
                "virtualservice", "destinationrule", "peerauthentication", "authorizationpolicy")] + [
                ("pod", n) for n in ("echo-client", "echo-v1", "echo-v2")]:
            item = json.loads(read(kube, "-n", smoke.NAMESPACE, "get", kind, name, "-o", "json"))
            values[kind + "/" + name] = {"uid": item["metadata"]["uid"], "spec_sha256": digest(item["spec"])}
        return values

    before = snapshot()
    result["recovery_baseline"] = before
    # This snapshot is at recovery, not an invented reconstruction of the failed run's start.
    original_path = options.report.with_suffix(".baseline.json")
    result["original_baseline_available"] = original_path.is_file()
    if original_path.is_file() and json.loads(original_path.read_text()) != before:
        raise AssertionError("Failed run's retained baseline changed before cleanup")
    save()
    try:
        for item in reversed(run["resources"]):
            kind, namespace, name, uid = (item[k] for k in ("kind", "namespace", "name", "uid"))
            if kind not in real_ca.CLUSTER_KINDS and namespace not in namespaces:
                raise AssertionError("Report resource escapes owned namespaces")
            prefix = [] if kind in real_ca.CLUSTER_KINDS else ["-n", namespace]
            text = read(kube, *prefix, "get", kind, name, "--ignore-not-found=true", "-o", "json")
            current = json.loads(text) if text.strip() else None
            if current is None:
                continue
            if current["metadata"].get("labels", {}).get(real_ca.OWNER) != run["owner"] or (
                    uid is not None and current["metadata"]["uid"] != uid):
                raise AssertionError("Refusing to delete an unowned or replaced CA resource")
            # A delete transport failure has an unknown outcome. Observe removal, never retry blind.
            value = subprocess.run(kube + prefix + ["delete", kind, name, "--ignore-not-found=true", "--wait=false"],
                                   text=True, capture_output=True, timeout=45)
            if value.returncode:
                policy.wait_for("delete outcome", lambda: read(kube, *prefix, "get", kind, name,
                                "--ignore-not-found=true", "-o", "name"), lambda v: not v.strip(), options.timeout)
            result["deleted"].append({"kind": kind, "namespace": namespace, "name": name, "uid": current["metadata"]["uid"]})
            save()
        for item in run["resources"]:
            prefix = [] if item["kind"] in real_ca.CLUSTER_KINDS else ["-n", item["namespace"]]
            policy.wait_for("owned CA resource removal", lambda p=prefix, x=item: read(kube, *p, "get", x["kind"],
                            x["name"], "--ignore-not-found=true", "-o", "name"), lambda v: not v.strip(), options.timeout)
        if snapshot() != before:
            raise AssertionError("Retained recovery baseline changed during cleanup")
        if not policy.ready(smoke.rpc(kube, method="inspect")) or not policy.successful(smoke.rpc(kube), 1):
            raise AssertionError("Retained echo business/ADS did not recover")
        trust_maps = json.loads(read(kube, "get", "configmaps", "-A", "--field-selector",
                                   "metadata.name=" + run["trust_configmap"], "-o", "json"))
        if trust_maps["items"]:
            raise AssertionError("Test trust ConfigMap remains after namespace cleanup")
        result["cleaned_up"] = True
        result["status"] = "cleaned"
    except BaseException as caught:
        result["status"] = "failed"
        result["error_type"] = type(caught).__name__
        save()
        raise
    finally:
        save()
    print("Owned CA resources removed; recovery baseline and echo verified", flush=True)


if __name__ == "__main__":
    main()
