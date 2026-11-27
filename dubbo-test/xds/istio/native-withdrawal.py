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

"""Light Service endpoint withdrawal/recovery and Triple/binary discovery isolation."""

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
LABEL = "dubbo-xds-discovery-probe"
CLUSTER = "outbound|7070|v1|" + policy.HOST
EDS = "type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment:" + CLUSTER


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
    route = policy.configuration(policy.get_object(kube, "virtualservice", "echo"))
    service = policy.get_object(kube, "service", "echo")
    providers = {name: policy.get_object(kube, "pod", name) for name in ("echo-v1", "echo-v2")}
    selector = copy.deepcopy(service["spec"]["selector"])
    if route["spec"].get("hosts") != [policy.HOST] or selector.get("app") != "native-xds-echo" or LABEL in selector:
        raise ValueError("Expected the owned native echo Service and route")
    if any(pod["metadata"].get("labels", {}).get("app") != "native-xds-echo" or LABEL in pod["metadata"]["labels"]
           for pod in providers.values()):
        raise ValueError("Provider discovery labels must be unused")
    run = uuid.uuid4().hex
    token = "withdrawal-" + run
    shadow = dict(selector, **{LABEL: run})
    report = {"namespace": policy.NAMESPACE, "status": "running", "restored": False, "label": LABEL, "owner": run,
              "token": token, "service_uid": service["metadata"]["uid"], "original_selector": selector,
              "provider_uids": {name: pod["metadata"]["uid"] for name, pod in providers.items()}, "cases": [],
              "inflight_scope": "Separate local real-RPC tests; this cluster verifier checks new calls and discovery"}
    options.output.parent.mkdir(parents=True, exist_ok=True)
    options.output.with_suffix(".baseline.json").write_text(json.dumps({"apiVersion": "v1", "kind": "List",
        "items": [route, policy.configuration(service)]}, indent=2) + "\n")

    def save():
        options.output.write_text(json.dumps(report, indent=2) + "\n")

    def record(name, value):
        report["cases"].append({"name": name, "result": value})
        save()

    def inspect():
        return smoke.rpc(kube, method="inspect")

    def target(value, protocol):
        return value.get("targets", {}).get(protocol + "://" + policy.HOST + (":7070" if protocol == "tri" else ":7071"), {})

    def shape(value, triple, binary):
        return (policy.ready(value) and all(target(value, p).get("available_instances") == n and target(value, p).get("graph_ready") is True
                                           for p, n in (("tri", triple), ("dubbo", binary))))

    def wait(label, predicate, timeout=None):
        def probe():
            value = inspect()
            report["last_observation"] = {"phase": label, "diagnostics": value}
            save()
            return value
        return policy.wait_for(label, probe, predicate, options.timeout if timeout is None else timeout)

    def patch(kind, name, operations, uid):
        current = policy.get_object(kube, kind, name)
        if current["metadata"]["uid"] != uid:
            raise AssertionError("Owned resource was replaced during the test")
        smoke.run(kube, "-n", policy.NAMESPACE, "patch", kind, name, "--type=json", "-p", json.dumps(operations))

    def label(name, include):
        current = policy.get_object(kube, "pod", name)
        value = current["metadata"]["labels"].get(LABEL)
        if value not in (None, run):
            raise AssertionError("Refusing to overwrite another discovery probe label")
        if include or value is not None:
            patch("pod", name, [{"op": "add" if include else "remove", "path": "/metadata/labels/" + LABEL,
                                 **({"value": run} if include else {})}], report["provider_uids"][name])

    def selector_patch(value):
        current = policy.get_object(kube, "service", "echo")["spec"]["selector"]
        if current not in (selector, shadow):
            raise AssertionError("Refusing to overwrite an unowned Service selector change")
        patch("service", "echo", [{"op": "replace", "path": "/spec/selector", "value": value}], report["service_uid"])

    route_changed = False
    selector_changed = False
    touched = []
    failure = None
    save()
    try:
        print("Checking two-protocol discovery baseline", flush=True)
        record("baseline", wait("baseline discovery", lambda d: shape(d, 2, 2)))
        single = copy.deepcopy(route)
        single["spec"]["http"] = [{"name": "native-withdrawal-" + run[:12], "route": [{"destination": {
            "host": policy.HOST, "port": {"number": 7070}, "subset": "v1"}, "weight": 100}]}]
        route_changed = True
        print("Routing Triple only to v1; binary discovery must keep both providers", flush=True)
        policy.apply(kube, single)
        record("route-accepted", wait("single route", lambda d: policy.ready(d) and any(
            r["name"] == "native-withdrawal-" + run[:12] for routes in d.get("accepted_routes", {}).values() for r in routes)))
        record("route-discovery-isolation", wait("binary discovery after Triple route", lambda d: shape(d, 1, 2), min(options.timeout, 40)))
        warm = smoke.rpc(kube)
        if not policy.successful(warm, 1) or warm["values"] != ["v1:native-smoke"]:
            raise AssertionError("Triple route did not use v1")
        for name in providers:
            touched.append(name)
            label(name, True)
        selector_changed = True
        selector_patch(shadow)
        record("selector-ready", wait("shadow selector", lambda d: shape(d, 1, 2)))
        print("Withdrawing v1 without changing security selector labels", flush=True)
        label("echo-v1", False)

        def withdrawn(d):
            raw = d.get("accepted_endpoints", {}).get(CLUSTER, {}).get("endpoint_count")
            status = d.get("resources", {}).get(EDS, {}).get("status")
            return (d.get("connection") == "READY" and (raw == 0 or status == "ABSENT")
                    and target(d, "tri").get("available_instances") == 0
                    and target(d, "dubbo").get("available_instances") == 1
                    and target(d, "dubbo").get("graph_ready") is True)
        record("endpoint-withdrawn", wait("accepted withdrawal", withdrawn))
        response = smoke.rpc(kube, message="probe:" + token + ":0:0", timeout=500)
        observed = {"rpc": response, "verified": False}
        record("new-call-after-withdrawal", observed)
        if response.get("status") != "RPC_ERROR" or response.get("values") or not response.get("errors") or not set(response["errors"]).issubset({"UNAVAILABLE", "DEADLINE_EXCEEDED"}):
            raise AssertionError("New Triple call did not fail with the revoked address")
        observed["verified"] = True
        binary = smoke.rpc(kube, protocol="dubbo", count=4)
        record("remaining-binary-provider", binary)
        if not policy.successful(binary, 4) or any(value != "v2:native-smoke" for value in binary["values"]):
            raise AssertionError("Binary discovery did not retain only v2")
        print("Restoring v1 and checking call recovery", flush=True)
        label("echo-v1", True)
        record("endpoint-restored", wait("restored endpoint", lambda d: shape(d, 1, 2)))
        response = smoke.rpc(kube)
        record("recovered-business", response)
        if not policy.successful(response, 1) or response["values"] != ["v1:native-smoke"]:
            raise AssertionError("Restored Triple endpoint did not recover")
        stats = smoke.rpc(kube, message="stats:" + token)
        record("withdrawn-call-provider-count", stats)
        if not policy.successful(stats, 1) or not stats["values"][0].startswith("v1:{") or json.loads(stats["values"][0][3:])["attempts"] != 0:
            raise AssertionError("Revoked request reached the provider")
    except Exception as caught:
        failure = caught
        report["failure_observation"] = copy.deepcopy(report.get("last_observation"))
        report["status"] = "failed"
        report["error_type"] = type(caught).__name__
        if isinstance(caught, AssertionError):
            report["assertion"] = str(caught)
        save()
    finally:
        print("Restoring Service selector, provider labels and route", flush=True)
        cleanup_errors = []
        if selector_changed:
            try:
                selector_patch(selector)
            except Exception as caught:
                cleanup_errors.append(type(caught).__name__)
        for name in touched:
            try:
                label(name, False)
            except Exception as caught:
                cleanup_errors.append(type(caught).__name__)
        if route_changed:
            try:
                policy.wait_for("counter reset", lambda: smoke.rpc(kube, message="reset:" + token),
                                lambda r: policy.successful(r, 1), min(options.timeout, 20))
            except Exception as caught:
                cleanup_errors.append(type(caught).__name__)
            try:
                policy.apply(kube, route)
            except Exception as caught:
                cleanup_errors.append(type(caught).__name__)
        try:
            if policy.get_object(kube, "service", "echo")["spec"]["selector"] != selector:
                raise AssertionError("Original Service selector not restored")
            if policy.get_object(kube, "virtualservice", "echo")["spec"] != route["spec"]:
                raise AssertionError("Original route not restored")
            for name, original in providers.items():
                current = policy.get_object(kube, "pod", name)
                if current["metadata"]["uid"] != original["metadata"]["uid"] or current["metadata"]["labels"] != original["metadata"]["labels"]:
                    raise AssertionError("Original provider labels or identity changed")
            record("baseline-restored", wait("restored discovery", lambda d: shape(d, 2, 2)))
            response = policy.wait_for("restored business", lambda: smoke.rpc(kube), lambda r: policy.successful(r, 1), options.timeout)
            record("baseline-business", response)
        except Exception as caught:
            cleanup_errors.append(type(caught).__name__)
        if cleanup_errors:
            report["status"] = "cleanup_failed"
            report["cleanup_errors"] = cleanup_errors
            if failure is None:
                failure = RuntimeError("Withdrawal cleanup failed; restore the saved baseline")
        else:
            report["restored"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Native withdrawal checks passed; baseline restored", flush=True)


if __name__ == "__main__":
    main()
