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

"""Call the proxyless echo client through a loopback-only Kubernetes port forward."""

import argparse
from collections import Counter
import json
import re
import selectors
import subprocess
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--grpcurl", default="grpcurl")
    parser.add_argument("--namespace", default="dubbo-xds-test")
    parser.add_argument("--count", type=int, default=100)
    parser.add_argument("--expected-version", choices=("v1", "v2"))
    parser.add_argument("--target", help="Override the xds:/// service target")
    parser.add_argument("--expect-error", help="Require a failed RPC containing this text")
    args = parser.parse_args()
    if args.count < 1:
        parser.error("--count must be positive")
    prefix = ["kubectl", "--kubeconfig", args.kubeconfig, "--request-timeout=20s"]
    pods = json.loads(subprocess.check_output(
        prefix + ["get", "pods", "-n", args.namespace, "-l", "app=xds-echo-client",
                  "-o", "json"], text=True, timeout=30))["items"]
    pod = next((p for p in pods if any(c["type"] == "Ready" and c["status"] == "True"
                                     for c in p["status"].get("conditions", []))), None)
    if pod is None:
        raise RuntimeError("No ready proxyless client pod")
    process = subprocess.Popen(
        prefix + ["port-forward", "-n", args.namespace, "pod/" + pod["metadata"]["name"],
                  "--address=127.0.0.1", "0:17171"],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    selector = selectors.DefaultSelector()
    try:
        selector.register(process.stdout, selectors.EVENT_READ)
        deadline = time.monotonic() + 30
        port = None
        while time.monotonic() < deadline:
            if process.poll() is not None:
                raise RuntimeError("Port forward exited before becoming ready")
            for key, _ in selector.select(timeout=1):
                line = key.fileobj.readline()
                match = re.search(r"Forwarding from 127\.0\.0\.1:(\d+)", line)
                if match:
                    port = match.group(1)
                    break
            if port:
                break
        if port is None:
            raise RuntimeError("Port forward did not become ready")
        target = args.target or "xds:///echo." + args.namespace + ".svc.cluster.local:7070"
        request = json.dumps({"url": target, "count": args.count})
        result = subprocess.run(
            [args.grpcurl, "-plaintext", "-max-time", "45", "-d", request,
             "127.0.0.1:" + port, "proto.EchoTestService/ForwardEcho"],
            text=True, capture_output=True, timeout=50)
        if args.expect_error:
            if result.returncode == 0 or args.expect_error not in result.stderr:
                raise RuntimeError("Expected RPC rejection containing " + args.expect_error +
                                   ": " + result.stdout + result.stderr)
            print(json.dumps({"target": target, "calls": args.count,
                              "expected_rejection": args.expect_error,
                              "error": result.stderr.strip()}, indent=2))
            return
        if result.returncode != 0:
            raise RuntimeError("RPC failed: " + result.stderr.strip())
        response = json.loads(result.stdout)
        output = "\n".join(response.get("output", []))
        versions = Counter(re.findall(r"ServiceVersion=(v\d+)", output))
        if sum(versions.values()) != args.count:
            raise RuntimeError("Unexpected number of successful echo responses: " + output)
        if args.expected_version and versions[args.expected_version] != args.count:
            raise RuntimeError("Route has not converged to " + args.expected_version + ": " + str(versions))
        print(json.dumps({"target": target, "calls": args.count,
                          "service_versions": dict(versions)}, indent=2))
    finally:
        selector.close()
        process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)


if __name__ == "__main__":
    main()
