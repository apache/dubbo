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

"""Light actual ADS/Triple connection loss with unchanged SDK process and references."""

import argparse
import copy
import importlib.util
import json
import pathlib
import shutil
import uuid

loader = importlib.util.spec_from_file_location("native_policy", pathlib.Path(__file__).with_name("native-policy.py"))
policy = importlib.util.module_from_spec(loader)
loader.loader.exec_module(policy)
smoke = policy.smoke
KINDS = ("virtualservice", "destinationrule", "peerauthentication", "authorizationpolicy")
CLUSTER = "outbound|7070|v1|" + policy.HOST


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--kubectl", default=shutil.which("kubectl") or "kubectl")
    parser.add_argument("--timeout", type=int, default=120)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    options = parser.parse_args()
    if options.timeout < 1:
        parser.error("--timeout must be positive")
    kube = [options.kubectl, "--kubeconfig", options.kubeconfig, "--request-timeout=30s"]
    resources = {kind: policy.get_object(kube, kind, "echo") for kind in KINDS}
    original = {kind: policy.configuration(value) for kind, value in resources.items()}
    route = original["virtualservice"]
    if route["spec"].get("hosts") != [policy.HOST]:
        raise ValueError("Expected the owned native echo route")
    client = policy.get_object(kube, "pod", "echo-client")
    run = uuid.uuid4().hex
    name = "native-reconnect-" + run[:12]
    single = copy.deepcopy(route)
    single["spec"]["http"] = [{"name": name, "route": [{"destination": {
        "host": policy.HOST, "port": {"number": 7070}, "subset": "v1"}, "weight": 100}]}]
    report = {"namespace": policy.NAMESPACE, "owner": run, "status": "running", "restored": False,
              "client_uid": client["metadata"]["uid"], "cases": [],
              "policy_uids": {kind: value["metadata"]["uid"] for kind, value in resources.items()},
              "scope": "SDK-to-agent ADS and native HTTP/2 channel loss; no agent/Istiod restart or certificate rotation"}
    options.output.parent.mkdir(parents=True, exist_ok=True)
    options.output.with_suffix(".baseline.json").write_text(json.dumps({"apiVersion": "v1", "kind": "List",
                                                                       "items": list(original.values())}, indent=2) + "\n")

    def save():
        options.output.write_text(json.dumps(report, indent=2) + "\n")

    def record(label, value):
        report["cases"].append({"name": label, "result": value})
        save()

    def inspect():
        value = smoke.rpc(kube, method="inspect")
        report["last_observation"] = value
        save()
        return value

    def call(method="echo", protocol="tri", count=1):
        response = smoke.rpc(kube, method=method, protocol=protocol, count=count)
        expected = count * (3 if method == "stream" else 1)
        if not policy.successful(response, expected):
            report["failed_business_response"] = response
            save()
            raise AssertionError("Reconnect business check failed: " + method + "/" + protocol)
        return response

    changed = False
    failure = None
    save()
    try:
        print("Checking ADS and business baseline", flush=True)
        baseline = policy.wait_for("baseline ADS", inspect, policy.ready, options.timeout)
        record("baseline", baseline)
        record("baseline-business", call())
        if not isinstance(baseline.get("process_start_ms"), int):
            raise AssertionError("Update the native consumer with the lifecycle probe bundle first")
        report["process_start_ms"] = baseline["process_start_ms"]

        print("Closing the actual SDK ADS transport and awaiting a replacement", flush=True)
        ads = smoke.rpc(kube, method="reconnect", protocol="ads")
        record("ads-transport-reconnect", ads)
        if (ads.get("status") != "OBSERVED" or ads.get("previous_channel_shutdown") is not True
                or ads.get("transport_replaced") is not True or ads.get("connection") != "READY"
                or ads.get("runtime_count") != 1 or ads.get("subscription_keys_retained") is not True):
            raise AssertionError("ADS did not reconnect with its shared runtime and subscriptions")

        print("Applying a fresh v1 route to verify subscriptions after ADS reconnect", flush=True)
        current = policy.get_object(kube, "virtualservice", "echo")
        if current["metadata"]["uid"] != report["policy_uids"]["virtualservice"] or current["spec"] != route["spec"]:
            raise AssertionError("Original route changed before the test update")
        changed = True
        policy.apply(kube, single)
        diagnostics = policy.wait_for("post-reconnect RDS", inspect, lambda d: policy.ready(d) and any(
            r["name"] == name for routes in d.get("accepted_routes", {}).values() for r in routes), options.timeout)
        record("post-reconnect-route", diagnostics)
        response = call(count=4)
        record("post-reconnect-routed-business", response)
        if set(response["values"]) != {"v1:native-smoke"}:
            raise AssertionError("Fresh RDS did not route calls to v1")

        print("Closing actual v1 HTTP/2 channels and awaiting native backoff recovery", flush=True)
        business = smoke.rpc(kube, method="reconnect", protocol="tri", key=CLUSTER)
        record("business-transport-reconnect", business)
        connections = business.get("connections", [])
        if business.get("status") != "OBSERVED" or not connections:
            raise AssertionError("No native business transport observation")
        for connection in connections:
            if (connection.get("previous_channel_closed") is not True or connection.get("channel_replaced") is not True
                    or connection.get("connectivity") != "READY" or connection.get("client_closed") is not False
                    or "TRANSIENT_FAILURE" not in connection.get("states", [])):
                raise AssertionError("Native transport was not disconnected and replaced with the same client")
            for phase in ("before", "after"):
                tls = connection.get(phase, {})
                if (tls.get("ready") is not True or tls.get("alpn") != "h2"
                        or tls.get("protocol") not in ("TLSv1.2", "TLSv1.3")
                        or not tls.get("local_leaf_sha256") or not tls.get("peer_leaf_sha256")):
                    raise AssertionError("Reconnected channel did not complete mutual TLS and HTTP/2")
        for method in ("echo", "async", "stream"):
            response = call(method=method)
            record("recovered-" + method, response)
            if any(not value.startswith("v1:") for value in response["values"]):
                raise AssertionError("Recovered native call changed the selected provider")
        record("independent-binary-business", call(protocol="dubbo", count=4))
        current = inspect()
        record("same-process", current)
        if current.get("process_start_ms") != report["process_start_ms"]:
            raise AssertionError("SDK process restarted during recovery")
        if policy.get_object(kube, "pod", "echo-client")["metadata"]["uid"] != report["client_uid"]:
            raise AssertionError("Native consumer Pod was replaced")
    except Exception as caught:
        failure = caught
        report["status"] = "failed"
        report["error_type"] = type(caught).__name__
        report["failure_observation"] = copy.deepcopy(report.get("last_observation"))
        if isinstance(caught, AssertionError):
            report["assertion"] = str(caught)
        save()
    finally:
        print("Restoring the original route and checking the retained policies", flush=True)
        cleanup_errors = []
        if changed:
            try:
                current = policy.get_object(kube, "virtualservice", "echo")
                if (current["metadata"]["uid"] != report["policy_uids"]["virtualservice"]
                        or current["spec"] not in (single["spec"], route["spec"])):
                    raise AssertionError("Refusing to overwrite an unowned route change")
                policy.apply(kube, route)
            except Exception as caught:
                cleanup_errors.append(type(caught).__name__)
        try:
            for kind, before in original.items():
                current = policy.get_object(kube, kind, "echo")
                if current["metadata"]["uid"] != report["policy_uids"][kind] or current["spec"] != before["spec"]:
                    raise AssertionError("Original policy not restored: " + kind)
            record("baseline-restored", policy.wait_for("restored ADS", inspect, policy.ready, options.timeout))
            record("baseline-business-restored", call())
        except Exception as caught:
            cleanup_errors.append(type(caught).__name__)
        if cleanup_errors:
            report["status"] = "cleanup_failed"
            report["cleanup_errors"] = cleanup_errors
            if failure is None:
                failure = RuntimeError("Reconnect cleanup failed; restore the saved baseline")
        else:
            report["restored"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Native reconnect checks passed; baseline restored", flush=True)


if __name__ == "__main__":
    main()
