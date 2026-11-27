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

"""Light native security negatives using temporary grpc-agent Pods and real xDS credentials."""

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
KINDS = ("peerauthentication", "authorizationpolicy", "virtualservice", "destinationrule")
OWNER = "dubbo-xds-test-run"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--kubectl", default=shutil.which("kubectl") or "kubectl")
    parser.add_argument("--bundle", type=pathlib.Path, required=True)
    parser.add_argument("--identity", choices=("both", "allowed", "denied"), default="both",
                        help="Select a targeted identity; reports retain the selected scope")
    parser.add_argument("--timeout", type=int, default=120)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    options = parser.parse_args()
    if options.timeout < 1:
        parser.error("--timeout must be positive")
    kube = [options.kubectl, "--kubeconfig", options.kubeconfig, "--request-timeout=30s"]
    jar = options.bundle / "dubbo-xds-native-test-app-3.3.7-SNAPSHOT.jar"
    if not jar.is_file() or not (options.bundle / "lib").is_dir():
        raise ValueError("Build the current native application bundle first")
    original = {kind: policy.configuration(policy.get_object(kube, kind, "echo")) for kind in KINDS}
    client = policy.get_object(kube, "pod", "echo-client")
    app = next(c for c in client["spec"]["containers"] if c["name"] == "app")
    run = uuid.uuid4().hex
    denied = "native-security-denied-" + run[:12]
    pods = ["native-security-allow-" + run[:12], "native-security-deny-" + run[:12]]
    selected = [(index, name) for index, name in enumerate(pods)
                if options.identity == "both" or (index == 0) == (options.identity == "allowed")]
    report = {"namespace": policy.NAMESPACE, "status": "running", "cleaned_up": False,
              "owner": run, "pods": [name for _, name in selected], "identity": options.identity,
              "service_account": denied if any(index == 1 for index, _ in selected) else None, "cases": []}
    options.output.parent.mkdir(parents=True, exist_ok=True)
    options.output.with_suffix(".baseline.json").write_text(
        json.dumps({"apiVersion": "v1", "kind": "List", "items": list(original.values())}, indent=2) + "\n")

    def save():
        options.output.write_text(json.dumps(report, indent=2) + "\n")

    def record(name, result):
        report["cases"].append({"name": name, "result": result})
        save()

    def metadata(name):
        return {"name": name, "namespace": policy.NAMESPACE, "labels": {OWNER: run}}

    resources = []
    failure = None
    save()
    try:
        print("Checking baseline ADS and business calls", flush=True)
        record("baseline", policy.wait_for("baseline ADS", lambda: smoke.rpc(kube, method="inspect"),
                                           policy.ready, options.timeout))
        record("baseline-business", policy.wait_for("baseline business", lambda: smoke.rpc(kube),
                                                     lambda r: policy.successful(r, 1), options.timeout))
        with tempfile.TemporaryDirectory(prefix="native-security-") as temporary:
            archive = pathlib.Path(temporary) / "bundle.tgz"
            with tarfile.open(archive, "w:gz") as output:
                output.add(jar, arcname=jar.name)
                output.add(options.bundle / "lib", arcname="lib")
            if report["service_account"] is not None:
                resources.append(("serviceaccount", denied))
                policy.apply(kube, {"apiVersion": "v1", "kind": "ServiceAccount", "metadata": metadata(denied)})
            for index, name in selected:
                meta = metadata(name)
                meta["annotations"] = {"inject.istio.io/templates": "grpc-agent",
                                       "proxy.istio.io/config": '{"holdApplicationUntilProxyStarts": true}'}
                container = {"name": "app", "image": app["image"], "resources": copy.deepcopy(app["resources"]),
                             "env": [{"name": "JAVA_TOOL_OPTIONS", "value": "-XX:ActiveProcessorCount=2"}],
                             "command": ["sh", "-c", "while [ ! -f /runtime/bundle-ready ]; do sleep 1; done; exec java -jar /runtime/"
                                         + jar.name + " security " + policy.HOST + " /runtime"],
                             "volumeMounts": [{"name": "runtime", "mountPath": "/runtime"}]}
                pod = {"apiVersion": "v1", "kind": "Pod", "metadata": meta,
                       "spec": {"serviceAccountName": "native-xds" if index == 0 else denied, "restartPolicy": "Never",
                                "containers": [container], "volumes": [{"name": "runtime", "emptyDir": {}}]}}
                resources.append(("pod", name))
                print("Starting " + ("allowed" if index == 0 else "wrong-ServiceAccount") + " probe", flush=True)
                policy.apply(kube, pod)
                smoke.run(kube, "-n", policy.NAMESPACE, "wait", "--for=jsonpath={.status.phase}=Running",
                          "pod/" + name, "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
                smoke.run(kube, "-n", policy.NAMESPACE, "cp", str(archive), name + ":/runtime/bundle.tgz", "-c", "app", timeout=180)
                for args in (("tar", "xzf", "/runtime/bundle.tgz", "-C", "/runtime"),
                             ("rm", "/runtime/bundle.tgz"), ("touch", "/runtime/bundle-ready")):
                    smoke.run(kube, "-n", policy.NAMESPACE, "exec", name, "-c", "app", "--", *args)
                deadline = time.monotonic() + options.timeout
                while time.monotonic() < deadline:
                    response = subprocess.run(kube + ["-n", policy.NAMESPACE, "exec", name, "-c", "app", "--",
                                                     "cat", "/runtime/security-result.json"], text=True, capture_output=True, timeout=30)
                    if response.returncode == 0:
                        observed = {"probe": json.loads(response.stdout), "verified": False}
                        break
                    if policy.get_object(kube, "pod", name)["status"].get("phase") in ("Failed", "Succeeded"):
                        raise RuntimeError("Security probe terminated without a report")
                    time.sleep(1)
                else:
                    raise TimeoutError("Security probe did not finish")
                record("allowed" if index == 0 else "wrong-service-account", observed)
                result = observed["probe"]
                if result.get("status") != "OBSERVED" or result.get("tls_required") is not True:
                    raise AssertionError("Probe did not complete with an actual xDS TLS endpoint")
                for method in ("business", "metadata"):
                    value = result.get(method, {})
                    if value.get("connection_ready") is not True or value.get("status") != ("OK" if index == 0 else "PERMISSION_DENIED"):
                        raise AssertionError("Direct " + method + " did not enforce the expected principal")
                if index == 0 and (result["business"]["value"] != "v1:native-security" or result["metadata"]["value"] < 1):
                    raise AssertionError("Allowed business/metadata control returned no valid response")
                if result["tls_control"].get("status") != "OK" or result["tls_control"].get("protocol") != "TLSv1.2" or result["tls_control"].get("alpn") != "h2":
                    raise AssertionError("Same-endpoint TLS control did not complete")
                if result["no_client_certificate"].get("status") != "TLS_REJECTED":
                    raise AssertionError("STRICT accepted a TLS client without a certificate")
                if result["wrong_san"].get("status") != "TLS_REJECTED" or result["wrong_san"].get("san_mismatch") is not True:
                    raise AssertionError("Native trust manager did not reject the deliberately wrong SAN")
                if result.get("binary_heartbeat_control", {}).get("status") != "MAGIC_RESPONSE":
                    raise AssertionError("The binary port did not respond to the same valid heartbeat")
                for form in ("plaintext_http2", "plaintext_http1", "binary_on_triple_port"):
                    if result[form].get("connected") is not True or result[form].get("status") not in ("CLOSED", "RESET"):
                        raise AssertionError("Protected Triple port did not close " + form)
                observed["verified"] = True
                save()
    except Exception as caught:
        failure = caught
        report["status"] = "failed"
        report["error_type"] = type(caught).__name__
        if isinstance(caught, AssertionError):
            report["assertion"] = str(caught)
        save()
    finally:
        print("Removing owned probe Pods and ServiceAccount", flush=True)
        cleanup_errors = []
        for kind, name in reversed(resources):
            try:
                current = json.loads(smoke.run(kube, "-n", policy.NAMESPACE, "get", kind, name, "--ignore-not-found=true", "-o", "json") or "null")
                if current is None:
                    continue
                if current["metadata"].get("labels", {}).get(OWNER) != run:
                    raise AssertionError("Refusing cleanup of an unowned resource")
                smoke.run(kube, "-n", policy.NAMESPACE, "delete", kind, name, "--ignore-not-found=true", "--wait=false")
            except Exception as caught:
                cleanup_errors.append(type(caught).__name__)
        try:
            for kind, name in resources:
                policy.wait_for("probe cleanup", lambda k=kind, n=name: smoke.run(kube, "-n", policy.NAMESPACE,
                                "get", k, n, "--ignore-not-found=true", "-o", "name"), lambda v: not v.strip(), options.timeout)
            for kind, baseline in original.items():
                if policy.get_object(kube, kind, "echo")["spec"] != baseline["spec"]:
                    raise AssertionError("Existing echo policy changed during the probe")
            record("baseline-after", policy.wait_for("ADS after probes", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
            record("business-after", policy.wait_for("business after probes", lambda: smoke.rpc(kube), lambda r: policy.successful(r, 1), options.timeout))
        except Exception as caught:
            cleanup_errors.append(type(caught).__name__)
        if cleanup_errors:
            report["status"] = "cleanup_failed"
            report["cleanup_errors"] = cleanup_errors
            if failure is None:
                failure = RuntimeError("Probe cleanup failed")
        else:
            report["cleaned_up"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Native security checks passed; probes removed and policies unchanged", flush=True)


if __name__ == "__main__":
    main()
