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

"""Run native Dubbo business calls through the injected Istio grpc-agent."""

import argparse
import json
import pathlib
import shutil
import subprocess
import tarfile
import tempfile
import time
import uuid

NAMESPACE = "dubbo-xds-native"
PODS = ("echo-v1", "echo-v2", "echo-client")


def run(kube, *args, timeout=60):
    command = kube + list(args)
    result = subprocess.run(command, text=True, capture_output=True, timeout=timeout)
    if result.returncode:
        # Kubectl errors describe resources; no kubeconfig or bootstrap payload is printed.
        raise RuntimeError(result.stderr.strip() or result.stdout.strip())
    return result.stdout


def rpc(kube, method="echo", protocol="tri", count=1, message="native-smoke", key="native-smoke", timeout=3000):
    name = uuid.uuid4().hex
    properties = ("method=" + method + "\nprotocol=" + protocol + "\ncount=" + str(count)
                  + "\nmessage=" + message + "\nkey=" + key + "\ntimeout=" + str(timeout) + "\n")
    with tempfile.TemporaryDirectory(prefix="native-xds-command-") as temporary:
        local = pathlib.Path(temporary) / (name + ".tmp")
        local.write_text(properties, encoding="utf-8")
        run(kube, "-n", NAMESPACE, "cp", str(local), "echo-client:/runtime/commands/" + name + ".tmp", "-c", "app")
        run(kube, "-n", NAMESPACE, "exec", "echo-client", "-c", "app", "--", "mv",
            "/runtime/commands/" + name + ".tmp", "/runtime/commands/" + name + ".request")
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        result = subprocess.run(kube + ["-n", NAMESPACE, "exec", "echo-client", "-c", "app", "--", "cat",
                                       "/runtime/commands/" + name + ".json"], text=True, capture_output=True, timeout=30)
        if result.returncode == 0:
            response = json.loads(result.stdout)
            run(kube, "-n", NAMESPACE, "exec", "echo-client", "-c", "app", "--", "rm",
                "/runtime/commands/" + name + ".json")
            return response
        time.sleep(1)
    raise TimeoutError("Native command did not finish: " + method + "/" + protocol)


def deploy(kube, bundle, timeout, recreate=False):
    manifest = pathlib.Path(__file__).with_name("native-application.yaml")
    jar = bundle / "dubbo-xds-native-test-app-3.3.7-SNAPSHOT.jar"
    if not jar.is_file() or not (bundle / "lib").is_dir():
        raise ValueError("Bundle must contain the application jar and lib directory")
    with tempfile.TemporaryDirectory(prefix="native-xds-bundle-") as temporary:
        archive = pathlib.Path(temporary) / "bundle.tgz"
        with tarfile.open(archive, "w:gz") as output:
            output.add(jar, arcname=jar.name)
            output.add(bundle / "lib", arcname="lib")
        if recreate:
            print("Recreating the three owned native test Pods", flush=True)
            run(kube, "-n", NAMESPACE, "delete", "pod", *PODS, "--ignore-not-found=true")
        print("Applying native application resources", flush=True)
        run(kube, "apply", "-f", str(manifest))
        for pod in PODS:
            print("Uploading runtime bundle to " + pod, flush=True)
            run(kube, "-n", NAMESPACE, "wait", "--for=jsonpath={.status.phase}=Running", "pod/" + pod,
                "--timeout=" + str(timeout) + "s", timeout=timeout + 20)
            run(kube, "-n", NAMESPACE, "cp", str(archive), pod + ":/runtime/bundle.tgz", "-c", "app", timeout=180)
            run(kube, "-n", NAMESPACE, "exec", pod, "-c", "app", "--", "tar", "xzf", "/runtime/bundle.tgz", "-C", "/runtime")
            run(kube, "-n", NAMESPACE, "exec", pod, "-c", "app", "--", "rm", "/runtime/bundle.tgz")
            if pod != "echo-client":
                run(kube, "-n", NAMESPACE, "exec", pod, "-c", "app", "--", "touch", "/runtime/bundle-ready")
                run(kube, "-n", NAMESPACE, "wait", "--for=condition=Ready", "pod/" + pod,
                    "--timeout=" + str(timeout) + "s", timeout=timeout + 20)
    run(kube, "-n", NAMESPACE, "exec", "echo-client", "-c", "app", "--", "touch", "/runtime/bundle-ready")
    run(kube, "-n", NAMESPACE, "wait", "--for=condition=Ready", "pod/echo-client",
        "--timeout=" + str(timeout) + "s", timeout=timeout + 20)


def main():
    parser = argparse.ArgumentParser(description="Light native SDK business smoke; retains its isolated test environment")
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--kubectl", default=shutil.which("kubectl") or "kubectl")
    parser.add_argument("--deploy", action="store_true")
    parser.add_argument("--recreate", action="store_true", help="Replace the three owned test Pods before deployment")
    parser.add_argument("--bundle", type=pathlib.Path)
    parser.add_argument("--timeout", type=int, default=180)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    options = parser.parse_args()
    if options.deploy and options.bundle is None:
        parser.error("--deploy requires --bundle")
    if options.recreate and not options.deploy:
        parser.error("--recreate requires --deploy")
    kube = [options.kubectl, "--kubeconfig", options.kubeconfig, "--request-timeout=30s"]
    if options.deploy:
        deploy(kube, options.bundle, options.timeout, options.recreate)
    report = {"namespace": NAMESPACE, "native_application": True, "cases": []}
    for method, protocol, count in (("echo", "tri", 8), ("async", "tri", 4), ("stream", "tri", 2),
                                    ("echo", "dubbo", 4), ("async", "dubbo", 2)):
        print("Checking " + method + "/" + protocol, flush=True)
        response = rpc(kube, method, protocol, count)
        report["cases"].append(response)
        options.output.parent.mkdir(parents=True, exist_ok=True)
        options.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        expected = count * (3 if method == "stream" else 1)
        if response.get("status") != "OK" or len(response.get("values", [])) != expected:
            raise AssertionError("Native smoke failed: " + json.dumps(response))
        if not all(value.startswith(("v1:", "v2:")) for value in response["values"]):
            raise AssertionError("Unexpected provider identity")
    diagnostics = rpc(kube, "inspect")
    report["diagnostics"] = diagnostics
    options.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    resources = diagnostics.get("resources", {})
    if diagnostics.get("connection") != "READY" or not resources:
        raise AssertionError("Native ADS diagnostics are not ready")
    if any(resource.get("status") != "ACKED" for resource in resources.values()):
        raise AssertionError("Native ADS resources are not all ACKED")
    report["status"] = "passed"
    options.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print("Native business smoke passed; environment retained in " + NAMESPACE, flush=True)


if __name__ == "__main__":
    main()
