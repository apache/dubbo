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

"""Light native policy update checks; restores the retained environment in finally."""

import argparse
import copy
import importlib.util
import json
import pathlib
import shutil
import tempfile
import time
import uuid

loader = importlib.util.spec_from_file_location("native_smoke", pathlib.Path(__file__).with_name("native-smoke.py"))
smoke = importlib.util.module_from_spec(loader)
loader.loader.exec_module(smoke)

NAMESPACE = smoke.NAMESPACE
HOST = "echo." + NAMESPACE + ".svc.cluster.local"
CLUSTER = "outbound|7070||" + HOST
CDS = "type.googleapis.com/envoy.config.cluster.v3.Cluster:" + CLUSTER


def get_object(kube, kind, name):
    return json.loads(smoke.run(kube, "-n", NAMESPACE, "get", kind, name, "-o", "json"))


def configuration(resource):
    return {"apiVersion": resource["apiVersion"], "kind": resource["kind"],
            "metadata": {"name": resource["metadata"]["name"], "namespace": NAMESPACE},
            "spec": copy.deepcopy(resource["spec"])}


def apply(kube, resource):
    with tempfile.TemporaryDirectory(prefix="native-xds-policy-") as temporary:
        path = pathlib.Path(temporary) / "policy.json"
        path.write_text(json.dumps(resource), encoding="utf-8")
        smoke.run(kube, "apply", "-f", str(path))


def wait_for(label, probe, predicate, timeout):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        value = probe()
        if predicate(value):
            return value
        time.sleep(1)
    raise TimeoutError("Policy did not converge: " + label)


def successful(response, count):
    return response.get("status") == "OK" and len(response.get("values", [])) == count


def ready(diagnostics):
    resources = diagnostics.get("resources", {})
    return (diagnostics.get("connection") == "READY" and bool(resources)
            and all(resource.get("status") == "ACKED" for resource in resources.values()))


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
    original_route = configuration(get_object(kube, "virtualservice", "echo"))
    original_cluster = configuration(get_object(kube, "destinationrule", "echo"))
    if original_route["spec"].get("hosts") != [HOST] or original_cluster["spec"].get("host") != HOST:
        raise ValueError("Expected the owned native echo configuration")
    # Preserve an exact public configuration backup before the first mutation.
    options.output.parent.mkdir(parents=True, exist_ok=True)
    backup = options.output.with_suffix(".baseline.json")
    backup.write_text(json.dumps({"apiVersion": "v1", "kind": "List", "items": [original_route, original_cluster]},
                                 indent=2) + "\n", encoding="utf-8")
    report = {"namespace": NAMESPACE, "cases": [], "status": "running", "restored": False}

    def save():
        options.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    def record(name, result):
        report["cases"].append({"name": name, "result": result})
        save()

    def inspect():
        return smoke.rpc(kube, "inspect")

    def call(**kwargs):
        return smoke.rpc(kube, **kwargs)

    deny_name = "native-probe-deny-" + uuid.uuid4().hex[:12]
    report["temporary_deny"] = deny_name
    changed = False
    failure = None
    save()
    try:
        print("Checking baseline diagnostics", flush=True)
        baseline = wait_for("baseline", inspect, ready, options.timeout)
        if "accepted_clusters" not in baseline:
            raise ValueError("Consumer must use the current application bundle with accepted cluster diagnostics")
        record("baseline", baseline)

        for identity in ("v1", "v2"):
            print("Switching route to " + identity, flush=True)
            route = copy.deepcopy(original_route)
            route["spec"]["http"] = [{"route": [{"destination": {"host": HOST,
                                            "port": {"number": 7070}, "subset": identity}, "weight": 100}]}]
            changed = True
            apply(kube, route)
            result = wait_for("route " + identity, lambda: call(count=4),
                              lambda r: successful(r, 4) and all(v == identity + ":native-smoke" for v in r["values"]),
                              options.timeout)
            record("route-" + identity, result)

        print("Applying temporary DENY", flush=True)
        deny = {"apiVersion": "security.istio.io/v1", "kind": "AuthorizationPolicy",
                "metadata": {"name": deny_name, "namespace": NAMESPACE},
                "spec": {"selector": {"matchLabels": {"app": "native-xds-echo"}}, "action": "DENY",
                         "rules": [{"to": [{"operation": {"ports": ["17070"]}}]}]}}
        apply(kube, deny)
        for method in ("echo", "async", "stream"):
            result = wait_for("DENY " + method, lambda: call(method=method),
                              lambda r: r.get("status") == "RPC_ERROR" and r.get("errors") == {"PERMISSION_DENIED": 1},
                              options.timeout)
            record("deny-" + method, result)
        smoke.run(kube, "-n", NAMESPACE, "delete", "authorizationpolicy", deny_name, "--ignore-not-found=true")
        restored = wait_for("ALLOW restore", call, lambda r: successful(r, 1), options.timeout)
        record("allow-restored", restored)

        print("Applying RING_HASH", flush=True)
        route = copy.deepcopy(original_route)
        route["spec"]["http"] = [{"route": [{"destination": {"host": HOST, "port": {"number": 7070}}}]}]
        cluster = copy.deepcopy(original_cluster)
        triple = next(setting for setting in cluster["spec"]["trafficPolicy"]["portLevelSettings"]
                      if setting["port"]["number"] == 7070)
        triple["loadBalancer"] = {"consistentHash": {"httpHeaderName": "x-user",
                                  "ringHash": {"minimumRingSize": 1024}}}
        apply(kube, cluster)
        apply(kube, route)
        ring = wait_for("RING_HASH", inspect,
                        lambda r: ready(r) and r.get("accepted_clusters", {}).get(CLUSTER, {}).get("lb_policy") == "RING_HASH",
                        options.timeout)
        record("ring-accepted", ring)
        mapping = {}
        for key in ("native-key-0", "native-key-1", "native-key-2", "native-key-3"):
            result = call(count=8, key=key)
            if not successful(result, 8) or len(set(result["values"])) != 1:
                raise AssertionError("RING_HASH did not retain the endpoint for a fixed header")
            if not result["values"][0].startswith(("v1:", "v2:")):
                raise AssertionError("Unexpected hash provider identity")
            mapping[key] = result["values"][0]
            record("ring-stable-" + key, result)

        print("Applying unsupported MAGLEV", flush=True)
        triple["loadBalancer"]["consistentHash"] = {"httpHeaderName": "x-user", "maglev": {"tableSize": 65537}}
        apply(kube, cluster)
        rejected = wait_for("MAGLEV NACK", inspect,
                            lambda r: r.get("resources", {}).get(CDS, {}).get("status") == "NACKED_WITH_LAST_GOOD"
                            and "MAGLEV" in r["resources"][CDS].get("rejection", ""), options.timeout)
        if rejected["accepted_clusters"][CLUSTER]["lb_policy"] != "RING_HASH":
            raise AssertionError("Unsupported policy replaced the accepted ring")
        if rejected["resources"][CDS]["accepted_at"] != ring["resources"][CDS]["accepted_at"]:
            raise AssertionError("Unsupported CDS changed the accepted resource timestamp")
        record("maglev-nacked-with-ring", rejected)
        for key in ("native-key-0", "native-key-1"):
            result = call(count=4, key=key)
            if not successful(result, 4) or set(result["values"]) != {mapping[key]}:
                raise AssertionError("MAGLEV NACK did not preserve the previous key mapping")
            record("last-good-" + key, result)
    except Exception as error:
        failure = error
        report["status"] = "failed"
        report["error_type"] = type(error).__name__
        save()
    finally:
        print("Restoring baseline policies", flush=True)
        cleanup_errors = []
        try:
            smoke.run(kube, "-n", NAMESPACE, "delete", "authorizationpolicy", deny_name, "--ignore-not-found=true")
        except Exception as error:
            cleanup_errors.append(type(error).__name__)
        if changed:
            for resource in (original_cluster, original_route):
                try:
                    apply(kube, resource)
                except Exception as error:
                    cleanup_errors.append(type(error).__name__)
        try:
            for kind, original in (("destinationrule", original_cluster), ("virtualservice", original_route)):
                if get_object(kube, kind, "echo")["spec"] != original["spec"]:
                    raise AssertionError("Baseline configuration was not restored")
            if changed:
                diagnostics = wait_for("restored ADS", inspect, ready, options.timeout)
                result = wait_for("restored business RPC", call, lambda r: successful(r, 1), options.timeout)
                record("baseline-restored", {"diagnostics": diagnostics, "rpc": result})
        except Exception as error:
            cleanup_errors.append(type(error).__name__)
        if cleanup_errors:
            report["status"] = "cleanup_failed"
            report["cleanup_errors"] = cleanup_errors
            if failure is None:
                failure = RuntimeError("Native policy cleanup failed; restore the saved baseline")
        else:
            report["restored"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Native policy checks passed; baseline restored", flush=True)


if __name__ == "__main__":
    main()
