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

"""Light deadline/retry/fault checks using secured provider-side counters."""

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
CASES = ["cancel-normal-completion", "deadline-echo", "deadline-async", "deadline-stream", "caller-shorter", "retry-echo", "retry-async",
         "stream-no-retry", "retry-total-budget", "fault-abort-echo", "fault-abort-async", "fault-abort-stream",
         "fault-delay", "fault-delay-deadline"]


def error(response, code):
    return response.get("status") == "RPC_ERROR" and response.get("errors") == {code: 1}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--kubectl", default=shutil.which("kubectl") or "kubectl")
    parser.add_argument("--timeout", type=int, default=120)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    parser.add_argument("--case", dest="selected", action="append", choices=CASES,
                        help="Run selected cases; omitted means the complete light batch")
    options = parser.parse_args()
    if options.timeout < 1:
        parser.error("--timeout must be positive")
    selected = set(options.selected or CASES)
    kube = [options.kubectl, "--kubeconfig", options.kubeconfig, "--request-timeout=30s"]
    original = policy.configuration(policy.get_object(kube, "virtualservice", "echo"))
    if original["spec"].get("hosts") != [policy.HOST]:
        raise ValueError("Expected the owned native echo VirtualService")
    options.output.parent.mkdir(parents=True, exist_ok=True)
    backup = options.output.with_suffix(".baseline.json")
    backup.write_text(json.dumps({"apiVersion": "v1", "kind": "List", "items": [original]}, indent=2) + "\n",
                      encoding="utf-8")
    report = {"namespace": policy.NAMESPACE, "status": "running", "restored": False, "cases": [], "tokens": [],
              "selected_cases": sorted(selected)}

    def save():
        options.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    def record(name, result):
        report["cases"].append({"name": name, "result": result})
        save()

    def call(**kwargs):
        return smoke.rpc(kube, **kwargs)

    def inspect():
        return call(method="inspect")

    def token():
        value = "traffic-" + uuid.uuid4().hex
        report["tokens"].append(value)
        save()
        return value

    def stats(value):
        response = call(message="stats:" + value, key="native-stats")
        if not policy.successful(response, 1) or not response["values"][0].startswith("v1:{"):
            raise AssertionError("Provider must use the current probe fixture and v1 route")
        return json.loads(response["values"][0][3:])

    def finished_stats(value):
        return policy.wait_for("provider completion", lambda: stats(value), lambda s: s["active"] == 0, options.timeout)

    def observation(name, response, value):
        result = {"rpc": response, "verified": False}
        record(name, result)
        result["provider"] = finished_stats(value)
        save()
        return result

    def verified(result):
        result["verified"] = True
        save()

    def require_deadline(name, method, phase, budget, upper, server_bound):
        print("Checking " + name, flush=True)
        value = token()
        response = call(method=method, message="probe:" + value + ":900:0", key=phase, timeout=budget)
        observed_case = observation(name, response, value)
        if not error(response, "DEADLINE_EXCEEDED") or response["elapsed_ms"] > upper:
            raise AssertionError("Deadline did not bound the " + name + " call")
        observed = observed_case["provider"]
        if observed["attempts"] != 1 or observed["cancelled"] < 1 or observed.get("cancelled_while_running", 0) < 1:
            raise AssertionError("Deadline did not notify cancellation before provider work finished")
        if len(observed["received_timeout_ms"]) != 1 or not 0 < observed["received_timeout_ms"][0] <= server_bound:
            raise AssertionError("Provider received an incorrect wire deadline")
        verified(observed_case)

    def require_retry(method):
        print("Checking " + method + " retries", flush=True)
        value = token()
        response = call(method=method, message="probe:" + value + ":30:2", key="native-retry", timeout=3000)
        observed_case = observation("retry-" + method, response, value)
        observed = observed_case["provider"]
        if not policy.successful(response, 1) or not response["values"][0].endswith(":attempt=3"):
            raise AssertionError("Two configured retries did not complete the third attempt")
        if observed["attempts"] != 3 or observed["previous_attempts"][0] not in ("", "0"):
            raise AssertionError("Unexpected retry attempt count")
        if observed["previous_attempts"][1:] != ["1", "2"]:
            raise AssertionError("Previous-attempt headers did not reach the provider")
        budgets = observed["received_timeout_ms"]
        if len(budgets) != 3 or not all(0 < budget <= 1000 for budget in budgets) or budgets[-1] >= budgets[0]:
            raise AssertionError("Retries did not share the original wire deadline")
        verified(observed_case)

    route = copy.deepcopy(original)
    destination = {"host": policy.HOST, "port": {"number": 7070}, "subset": "v1"}

    def phase(name, **settings):
        return {"name": name, "match": [{"headers": {"x-user": {"exact": name}}}],
                "route": [{"destination": destination, "weight": 100}], **settings}

    retry = {"attempts": 2, "retryOn": "unavailable"}
    route["spec"]["http"] = [
        phase("native-deadline", timeout="180ms"),
        phase("native-retry", timeout="1s", retries=retry),
        phase("native-retry-budget", timeout="350ms", retries=retry),
        phase("native-fault-abort", fault={"abort": {"grpcStatus": "UNAVAILABLE", "percentage": {"value": 100}}}),
        phase("native-fault-delay", fault={"delay": {"fixedDelay": "250ms", "percentage": {"value": 100}}}),
        phase("native-fault-deadline", timeout="120ms",
              fault={"delay": {"fixedDelay": "500ms", "percentage": {"value": 100}}}),
        {"name": "native-stats", "route": [{"destination": destination, "weight": 100}]},
    ]
    changed = False
    failure = None
    save()
    try:
        print("Checking baseline ADS", flush=True)
        baseline = policy.wait_for("baseline", inspect, policy.ready, options.timeout)
        if "accepted_routes" not in baseline:
            raise ValueError("Consumer must use the current application bundle with route diagnostics")
        record("baseline", baseline)
        print("Applying controlled traffic routes", flush=True)
        changed = True
        policy.apply(kube, route)
        expected = {r["name"] for r in route["spec"]["http"]}
        accepted = policy.wait_for("traffic routes", inspect,
                                  lambda d: policy.ready(d) and expected.issubset(
                                      {r["name"] for routes in d.get("accepted_routes", {}).values() for r in routes}),
                                  options.timeout)
        record("traffic-routes-accepted", accepted)
        warm = policy.wait_for("v1 route", call,
                               lambda r: policy.successful(r, 1) and r["values"][0] == "v1:native-smoke", options.timeout)
        record("v1-warm", warm)

        if "cancel-normal-completion" in selected:
            print("Checking normal completion cancellation control", flush=True)
            value = token()
            response = call(method="async", message="probe:" + value + ":30:0", key="native-stats")
            observed_case = observation("cancel-normal-completion", response, value)
            observed = observed_case["provider"]
            if (not policy.successful(response, 1) or observed["attempts"] != 1
                    or observed.get("cancelled_while_running") != 0):
                raise AssertionError("Normal completion was observed as cancellation during provider work")
            verified(observed_case)

        for method in ("echo", "async", "stream"):
            if "deadline-" + method in selected:
                require_deadline("deadline-" + method, method, "native-deadline", 3000, 700, 180)
        if "caller-shorter" in selected:
            require_deadline("caller-shorter", "async", "native-deadline", 120, 500, 120)
        for method in ("echo", "async"):
            if "retry-" + method in selected:
                require_retry(method)

        if "stream-no-retry" in selected:
            print("Checking stream is not retried", flush=True)
            value = token()
            response = call(method="stream", message="probe:" + value + ":0:2", key="native-retry")
            observed_case = observation("stream-no-retry", response, value)
            observed = observed_case["provider"]
            if not error(response, "UNAVAILABLE") or observed["attempts"] != 1:
                raise AssertionError("Streaming invocation was retried")
            verified(observed_case)

        if "retry-total-budget" in selected:
            print("Checking retry total budget", flush=True)
            value = token()
            response = call(method="async", message="probe:" + value + ":250:10", key="native-retry-budget")
            observed_case = observation("retry-total-budget", response, value)
            observed = observed_case["provider"]
            budgets = observed["received_timeout_ms"]
            if not error(response, "DEADLINE_EXCEEDED") or response["elapsed_ms"] > 900:
                raise AssertionError("Retry extended the original deadline")
            if not 1 <= observed["attempts"] <= 2 or not all(0 < b <= 350 for b in budgets):
                raise AssertionError("Retry count or wire budget exceeded the route limit")
            if len(budgets) > 1 and budgets[-1] >= budgets[0]:
                raise AssertionError("Retry restarted the wire budget")
            verified(observed_case)

        for method in ("echo", "async", "stream"):
            if "fault-abort-" + method not in selected:
                continue
            print("Checking fault abort " + method, flush=True)
            value = token()
            response = call(method=method, message="probe:" + value + ":0:0", key="native-fault-abort")
            observed_case = observation("fault-abort-" + method, response, value)
            observed = observed_case["provider"]
            if not error(response, "UNAVAILABLE") or observed["attempts"] != 0:
                raise AssertionError("Fault abort dispatched a provider request")
            verified(observed_case)

        if "fault-delay" in selected:
            print("Checking fault delay", flush=True)
            value = token()
            response = call(method="async", message="probe:" + value + ":0:0", key="native-fault-delay", timeout=800)
            observed_case = observation("fault-delay", response, value)
            observed = observed_case["provider"]
            if not policy.successful(response, 1) or not 200 <= response["elapsed_ms"] <= 1500:
                raise AssertionError("Fault delay was not executed")
            if observed["attempts"] != 1 or len(observed["received_timeout_ms"]) != 1 or not 0 < observed["received_timeout_ms"][0] <= 650:
                raise AssertionError("Fault delay did not consume the original call budget")
            verified(observed_case)

        if "fault-delay-deadline" in selected:
            print("Checking deadline during fault delay", flush=True)
            value = token()
            response = call(method="async", message="probe:" + value + ":0:0", key="native-fault-deadline")
            observed_case = observation("fault-delay-deadline", response, value)
            observed = observed_case["provider"]
            if not error(response, "DEADLINE_EXCEEDED") or response["elapsed_ms"] > 600 or observed["attempts"] != 0:
                raise AssertionError("Expired fault delay dispatched a provider request")
            verified(observed_case)
    except Exception as caught:
        failure = caught
        report["status"] = "failed"
        report["error_type"] = type(caught).__name__
        if isinstance(caught, AssertionError):
            report["assertion"] = str(caught)
        save()
    finally:
        print("Releasing probe counters and restoring baseline route", flush=True)
        cleanup_errors = []
        if changed:
            # Reset through the secured default v1 route while it is still installed.
            for value in report["tokens"]:
                try:
                    response = policy.wait_for("counter reset", lambda: call(message="reset:" + value, key="native-stats"),
                                               lambda r: policy.successful(r, 1) and r["values"][0] == "v1:reset", min(options.timeout, 15))
                except Exception as caught:
                    cleanup_errors.append(type(caught).__name__)
            try:
                policy.apply(kube, original)
            except Exception as caught:
                cleanup_errors.append(type(caught).__name__)
        try:
            if policy.get_object(kube, "virtualservice", "echo")["spec"] != original["spec"]:
                raise AssertionError("Original VirtualService spec was not restored")
            restored = policy.wait_for("restored ADS", inspect, policy.ready, options.timeout)
            restored_rpc = policy.wait_for("restored business call", call, lambda r: policy.successful(r, 1), options.timeout)
            record("baseline-restored", {"diagnostics": restored, "rpc": restored_rpc})
        except Exception as caught:
            cleanup_errors.append(type(caught).__name__)
        if cleanup_errors:
            report["status"] = "cleanup_failed"
            report["cleanup_errors"] = cleanup_errors
            if failure is None:
                failure = RuntimeError("Traffic cleanup failed; restore the saved baseline")
        else:
            report["restored"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Native deadline/retry/fault checks passed; baseline restored", flush=True)


if __name__ == "__main__":
    main()
