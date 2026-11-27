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

"""Verify injected bootstrap and shared certificate paths without reading secrets."""

import argparse
import json
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--namespace", default="dubbo-xds-test")
    args = parser.parse_args()
    prefix = ["kubectl", "--kubeconfig", args.kubeconfig, "--request-timeout=20s"]

    def kubectl(*command):
        return subprocess.check_output(prefix + list(command), text=True, timeout=45)

    pods = json.loads(kubectl("get", "pods", "-n", args.namespace,
                            "-l", "app in (xds-echo,xds-echo-client)", "-o", "json"))["items"]
    pods = [pod for pod in pods if not pod["metadata"].get("deletionTimestamp")]
    if not pods:
        raise RuntimeError("No proxyless test pods found")
    reports = []
    for pod in pods:
        name = pod["metadata"]["name"]
        spec = pod["spec"]
        ready = any(c["type"] == "Ready" and c["status"] == "True"
                    for c in pod["status"].get("conditions", []))
        if not ready:
            raise RuntimeError("Test pod is not ready: " + name)
        if any(c["name"] == "istio-init" for c in spec.get("initContainers", [])):
            raise RuntimeError("Unexpected traffic interception init container")
        containers = {c["name"]: c for c in spec["containers"]}
        if "istio-proxy" not in containers:
            raise RuntimeError("Missing pilot-agent container")
        agent_env = {e["name"]: e.get("value") for e in containers["istio-proxy"].get("env", [])}
        if agent_env.get("DISABLE_ENVOY") != "true":
            raise RuntimeError("Expected an agent-only deployment")
        app = containers["app"]
        app_env = {e["name"]: e.get("value") for e in app.get("env", [])}
        path = app_env.get("GRPC_XDS_BOOTSTRAP")
        if not path:
            raise RuntimeError("Missing injected GRPC_XDS_BOOTSTRAP")
        bootstrap = json.loads(kubectl("exec", "-n", args.namespace, name,
                                      "-c", "app", "--", "cat", path))
        servers = bootstrap.get("xds_servers", [])
        if not servers or not bootstrap.get("node", {}).get("id"):
            raise RuntimeError("Missing xDS server or node identity")
        providers = bootstrap.get("certificate_providers", {})
        if not providers or not bootstrap.get("server_listener_resource_name_template"):
            raise RuntimeError("Missing certificate providers or server listener template")
        for provider in providers.values():
            if provider.get("plugin_name") != "file_watcher":
                raise RuntimeError("Unexpected certificate provider")
            for key in ("certificate_file", "private_key_file", "ca_certificate_file"):
                certificate_path = provider["config"][key]
                kubectl("exec", "-n", args.namespace, name, "-c", "app", "--",
                        "test", "-r", certificate_path)
        reports.append({
            "pod": name,
            "ready": ready,
            "agent_only": True,
            "bootstrap_path": path,
            "xds_servers": servers,
            "node_id": bootstrap["node"]["id"],
            "generator": bootstrap["node"].get("metadata", {}).get("GENERATOR"),
            "certificate_providers": providers,
            "server_listener_resource_name_template": bootstrap["server_listener_resource_name_template"],
            "certificate_files_readable_from_app": True,
        })
    print(json.dumps(reports, indent=2))


if __name__ == "__main__":
    main()
