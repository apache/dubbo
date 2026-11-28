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

"""Light native max_requests saturation/drop/recovery check with exact policy restoration."""

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
    original_route = policy.configuration(policy.get_object(kube, "virtualservice", "echo"))
    original_cluster = policy.configuration(policy.get_object(kube, "destinationrule", "echo"))
    if original_route["spec"].get("hosts") != [policy.HOST] or original_cluster["spec"].get("host") != policy.HOST:
        raise ValueError("Expected the owned native echo policies")
    options.output.parent.mkdir(parents=True, exist_ok=True)
    options.output.with_suffix(".baseline.json").write_text(
        json.dumps({"apiVersion": "v1", "kind": "List", "items": [original_route, original_cluster]}, indent=2) + "\n",
        encoding="utf-8")
    token = "admission-" + uuid.uuid4().hex
    report = {"namespace": policy.NAMESPACE, "status": "running", "restored": False,
              "tokens": [token, token + "-overflow"], "cases": []}

    def save():
        options.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    def record(name, result):
        report["cases"].append({"name": name, "result": result})
        save()

    def call(**kwargs):
        return smoke.rpc(kube, **kwargs)

    def inspect():
        return call(method="inspect")

    def stats(value):
        response = call(message="stats:" + value)
        if not policy.successful(response, 1) or not response["values"][0].startswith("v1:{"):
            raise AssertionError("Provider stats did not use the secured v1 route")
        return json.loads(response["values"][0][3:])

    changed = False
    failure = None
    save()
    try:
        print("Checking baseline ADS", flush=True)
        baseline = policy.wait_for("baseline", inspect, policy.ready, options.timeout)
        if not all("max_requests" in value for value in baseline.get("accepted_clusters", {}).values()):
            raise ValueError("Consumer must use the current admission/diagnostic bundle")
        record("baseline", baseline)
        route = copy.deepcopy(original_route)
        route["spec"]["http"] = [{"name": "native-admission", "retries": {"attempts": 3, "retryOn": "unavailable"},
                                   "route": [{"destination": {"host": policy.HOST, "port": {"number": 7070},
                                                              "subset": "v1"}, "weight": 100}]}]
        changed = True
        print("Routing to v1 with retries configured", flush=True)
        policy.apply(kube, route)
        accepted = policy.wait_for("admission route", inspect,
                                  lambda d: policy.ready(d) and any(
                                      r["name"] == "native-admission" and r.get("num_retries") == 3
                                      for routes in d.get("accepted_routes", {}).values() for r in routes), options.timeout)
        record("retry-route-accepted", accepted)
        policy.wait_for("v1 warm", call, lambda r: policy.successful(r, 1) and r["values"][0] == "v1:native-smoke",
                        options.timeout)
        cluster = copy.deepcopy(original_cluster)
        triple = next(setting for setting in cluster["spec"]["trafficPolicy"]["portLevelSettings"]
                      if setting["port"]["number"] == 7070)
        triple["connectionPool"] = {"http": {"http2MaxRequests": 2}}
        print("Applying max_requests=2", flush=True)
        policy.apply(kube, cluster)
        accepted = policy.wait_for("max_requests", inspect,
                                  lambda d: policy.ready(d) and d.get("accepted_clusters", {}).get(CLUSTER, {}).get("max_requests") == 2,
                                  options.timeout)
        record("limit-accepted", accepted)

        print("Checking saturation, local drop and recovery", flush=True)
        response = call(method="admission", message=token, key=CLUSTER)
        observed = {"rpc": response, "verified": False}
        record("saturation-drop-recovery", observed)
        observed["provider"] = stats(token)
        observed["overflow_provider"] = stats(token + "-overflow")
        save()
        if response.get("status") != "OBSERVED" or response.get("active_at_saturation") != 2:
            raise AssertionError("Two actual SDK attempts did not saturate admission")
        if response.get("overflow", {}).get("status") != "UNAVAILABLE" or response["overflow"].get("error_type") != "DropException":
            raise AssertionError("Overflow did not return the typed local drop")
        if response.get("drop_delta") != 1 or response.get("overflow_elapsed_ms", 1000) > 100:
            raise AssertionError("Local drop was delayed or retried")
        if response.get("active_after_drop") != 2 or len(response.get("admitted", [])) != 2:
            raise AssertionError("Overflow modified the admitted requests")
        if any(value.get("status") != "OK" for value in response["admitted"]):
            raise AssertionError("Admitted requests did not complete")
        if response.get("active_after_completion") != 0 or response.get("recovery", {}).get("status") != "OK" or response.get("active_after_recovery") != 0:
            raise AssertionError("Completion did not release permits and restore admission")
        if observed["provider"]["attempts"] != 3 or observed["provider"]["active"] != 0:
            raise AssertionError("Provider dispatch count exceeded two admitted and one recovery call")
        if observed["overflow_provider"]["attempts"] != 0:
            raise AssertionError("Locally dropped request reached the provider")
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
        print("Resetting counters and restoring both policies", flush=True)
        cleanup_errors = []
        if changed:
            for value in report["tokens"]:
                try:
                    policy.wait_for("counter reset", lambda: call(message="reset:" + value),
                                    lambda r: policy.successful(r, 1) and r["values"][0] == "v1:reset", min(options.timeout, 15))
                except Exception as caught:
                    cleanup_errors.append(type(caught).__name__)
            for resource in (original_cluster, original_route):
                try:
                    policy.apply(kube, resource)
                except Exception as caught:
                    cleanup_errors.append(type(caught).__name__)
        try:
            for kind, original in (("destinationrule", original_cluster), ("virtualservice", original_route)):
                if policy.get_object(kube, kind, "echo")["spec"] != original["spec"]:
                    raise AssertionError("Original policy was not exactly restored")
            restored = policy.wait_for("restored ADS", inspect, policy.ready, options.timeout)
            business = policy.wait_for("restored business", call, lambda r: policy.successful(r, 1), options.timeout)
            record("baseline-restored", {"diagnostics": restored, "rpc": business})
        except Exception as caught:
            cleanup_errors.append(type(caught).__name__)
        if cleanup_errors:
            report["status"] = "cleanup_failed"
            report["cleanup_errors"] = cleanup_errors
            if failure is None:
                failure = RuntimeError("Admission cleanup failed; restore the saved baseline")
        else:
            report["restored"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Native admission checks passed; baseline restored", flush=True)


if __name__ == "__main__":
    main()
