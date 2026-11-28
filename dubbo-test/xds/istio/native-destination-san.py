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

"""Actual Istio destination SAN rejection/recovery with a different agent-issued ServiceAccount identity."""

import argparse
import copy
import importlib.util
import json
import pathlib
import re
import shutil
import subprocess
import tarfile
import tempfile
import time
import uuid


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, pathlib.Path(__file__).with_name(filename))
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


ca_gate = load("san_ca_gate", "native-istiod-ca.py")
policy, smoke = ca_gate.policy, ca_gate.smoke
OWNER = "dubbo.apache.org/native-destination-san-owner"


class Probe(ca_gate.sdk_probe.SdkProbe):
    def rpc(self, method="echo", protocol="tri", count=1, port=7070):
        self.pod(self.consumer)
        token = uuid.uuid4().hex
        path = "/runtime/commands/" + token
        properties = "\n".join(k + "=" + str(v) for k, v in {
            "method": method, "protocol": protocol, "count": count, "port": port,
            "service": self.host, "message": "native-destination-san", "timeout": 3000}.items()) + "\n"
        try:
            result = subprocess.run(self.kube + ["-n", self.namespace, "exec", "-i", self.consumer, "-c", "app", "--",
                "sh", "-c", 'cat > "$1.tmp" && mv "$1.tmp" "$1.request"', "sh", path],
                input=properties, text=True, capture_output=True, timeout=60)
        except subprocess.TimeoutExpired:
            result = None
        if result is None or result.returncode:
            self.record("command-publish-observation", {"token": token, "method": method})
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            try:
                result = subprocess.run(self.kube + ["-n", self.namespace, "exec", self.consumer, "-c", "app", "--",
                    "cat", path + ".json"], text=True, capture_output=True, timeout=30)
            except subprocess.TimeoutExpired:
                result = None
            if result is not None and result.returncode == 0:
                return json.loads(result.stdout)
            if result is not None and not any(x in result.stderr.lower() for x in (
                    "no such file", "network is unreachable", "unexpected eof", "connection reset by peer",
                    "i/o timeout", "tls handshake timeout", "timeout awaiting response headers")):
                raise RuntimeError("Could not observe destination SAN command")
            time.sleep(1)
        raise TimeoutError("Destination SAN command did not finish")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kubeconfig", required=True)
    parser.add_argument("--kubectl", default=shutil.which("kubectl") or "kubectl")
    parser.add_argument("--bundle", type=pathlib.Path, required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    parser.add_argument("--timeout", type=int, default=240)
    parser.add_argument("--cross-service", action="store_true",
                        help="Route Entry A to B with different metadata applications and ServiceAccounts; binary stays on A")
    options = parser.parse_args()
    if options.timeout < 1:
        parser.error("Timeout must be positive")
    if not (options.bundle / "dubbo-xds-native-test-app-3.3.7-SNAPSHOT.jar").is_file():
        parser.error("Build the current native application bundle first")
    kube = [options.kubectl, "--kubeconfig", options.kubeconfig, "--request-timeout=30s"]
    run = uuid.uuid4().hex
    namespace = "xds-san-" + run[:12]
    donor = "ca-other"
    donor_principal = "spiffe://cluster.local/ns/" + namespace + "/sa/" + donor
    entry = "entry-a." + namespace + ".svc.cluster.local"
    entry_app, target_app = "san-entry-a", "san-dest-b"
    resources = []
    report = {"owner": run, "namespace": namespace, "status": "running", "cleaned_up": False,
              "cases": [], "resources": [], "scope": "Actual Istio CDS SAN matchers, real same-CA wrong-ServiceAccount server certificate; SDK discovery denial and recovery without restart"}
    report["cross_service"] = options.cross_service
    if options.cross_service:
        report["scope"] += "; Entry A routes to B with per-target metadata application/group; binary A remains isolated"
    options.output.parent.mkdir(parents=True, exist_ok=True)

    def save():
        report["resources"] = [{"kind": k, "namespace": ns, "name": n, "uid": uid} for k, ns, n, uid in resources]
        options.output.write_text(json.dumps(report, indent=2) + "\n")

    def record(name, value):
        report["cases"].append({"name": name, "observed_at_ms": int(time.time() * 1000), "result": value})
        save()

    def args(kind, ns, name):
        return ([] if kind == "Namespace" else ["-n", ns]) + ["get", kind, name]

    def get(kind, ns, name):
        return json.loads(ca_gate.read(kube, *args(kind, ns, name), "-o", "json"))

    def baseline():
        values = {}
        for kind, ns, name in [("deployment", "istio-system", "istiod"),
                ("configmap", "istio-system", "istio-ca-root-cert"),
                ("configmap", smoke.NAMESPACE, "istio-ca-root-cert")] + [
                (kind, smoke.NAMESPACE, "echo") for kind in ("virtualservice", "destinationrule", "peerauthentication", "authorizationpolicy")] + [
                ("pod", smoke.NAMESPACE, name) for name in ("echo-client", "echo-v1", "echo-v2")]:
            value = get(kind, ns, name)
            values[kind + "/" + ns + "/" + name] = {"uid": value["metadata"]["uid"],
                "content_sha256": ca_gate.digest(value["data"] if kind == "configmap" else value["spec"])}
        return values

    before = baseline()
    options.output.with_suffix(".baseline.json").write_text(json.dumps(before, indent=2) + "\n")
    template = policy.get_object(kube, "pod", "echo-client")
    app = next(c for c in template["spec"]["containers"] if c["name"] == "app")

    def metadata(name, ns=None, labels=None):
        return {"name": name, **({"namespace": ns} if ns else {}), "labels": {OWNER: run, **(labels or {})}}

    def create(value):
        kind, meta = value["kind"], value["metadata"]
        ns, name = meta.get("namespace"), meta["name"]
        if ca_gate.read(kube, *args(kind, ns, name), "--ignore-not-found=true", "-o", "name").strip():
            raise AssertionError("Refusing to replace a SAN fixture resource")
        if kind == "Pod" and name == "ca-sdk-server":
            container = value["spec"]["containers"][0]
            command = container["command"][2]
            preparation = ("umask 077; mkdir -p /runtime/root-rotation; "
                "n=0; while [ ! -s /var/lib/istio/data/cert-chain.pem ] || [ ! -s /var/lib/istio/data/key.pem ] || "
                "[ ! -s /var/lib/istio/data/root-cert.pem ]; do n=$((n+1)); [ $n -le 30 ] || exit 1; sleep 1; done; "
                "cp /var/lib/istio/data/cert-chain.pem /runtime/root-rotation/cert.pem && "
                "cp /var/lib/istio/data/key.pem /runtime/root-rotation/key.pem && "
                "cp /var/lib/istio/data/root-cert.pem /runtime/root-rotation/root.pem && ")
            container["command"][2] = command.replace("done; java -jar", "done; " + preparation + "java -jar", 1).replace(
                " prepare-rotation unused ", " prepare-root-rotation unused ", 1)
        if options.cross_service and kind == "Pod" and name in ("ca-sdk-server", "ca-sdk-client"):
            container = value["spec"]["containers"][0]
            overrides = {"NATIVE_XDS_PROVIDER_APP": target_app} if name == "ca-sdk-server" else {
                "NATIVE_XDS_PROVIDER_APP": entry_app, "NATIVE_XDS_METADATA_TARGETS": json.dumps({
                    "tri://ca-sdk." + namespace + ".svc.cluster.local:7070": {
                        "application": target_app, "group": target_app, "service-port": "7072", "protocol": "tri"}})}
            container["env"] += [{"name": k, "value": v} for k, v in overrides.items()]
            if name == "ca-sdk-client":
                container["command"][2] = container["command"][2].replace(" consumer ca-sdk." + namespace + ".svc.cluster.local ",
                                                                         " consumer " + entry + " ", 1)
        resources.append((kind, ns, name, None))
        save()
        result = subprocess.run(kube + ["create", "-f", "-"], input=json.dumps(value), text=True, capture_output=True, timeout=60)
        current = get(kind, ns, name)
        if current["metadata"].get("labels", {}).get(OWNER) != run:
            raise AssertionError("Created SAN resource owner changed")
        resources[-1] = (kind, ns, name, current["metadata"]["uid"])
        save()

    def owned(kind, ns, name):
        current = get(kind, ns, name)
        expected = next(uid for k, s, n, uid in resources if (k, s, n) == (kind, ns, name))
        if current["metadata"]["uid"] != expected or current["metadata"].get("labels", {}).get(OWNER) != run:
            raise AssertionError("SAN fixture resource ownership changed")
        return current

    probe = Probe(kube, namespace, "istio-ca-root-cert", options.bundle, app, metadata, create, owned,
                  ca_gate.read, record, policy, options.timeout)
    if options.cross_service:
        probe.targets = ["tri://" + entry + ":7070", "dubbo://" + entry + ":7071"]
        report["entry"] = entry
        report["destination"] = probe.host
    openssl = shutil.which("openssl") or "openssl"

    def capture(pod, principal, publish=False, application_copy=False):
        # Private keys exist only in memory and a mode-600 temporary verification file, never in reports.
        for attempt in range(4):
            owned("Pod", namespace, pod)
            paths = ({"cert.pem": "cert.pem", "root.pem": "root.pem"} if application_copy
                     else {"cert.pem": "cert-chain.pem", "root.pem": "root-cert.pem"})
            if publish:
                paths["key.pem"] = "key.pem"
            source = "/runtime/root-rotation/" if application_copy else "/var/lib/istio/data/"
            data = {name: ca_gate.read(kube, "-n", namespace, "exec", pod, "-c", "app", "--", "cat",
                                      source + path).encode() for name, path in paths.items()}
            with tempfile.TemporaryDirectory(prefix="dubbo-san-certificate-") as directory:
                directory = pathlib.Path(directory)
                for name, payload in data.items():
                    path = directory / name
                    path.write_bytes(payload)
                    path.chmod(0o600)
                def execute(*values):
                    return subprocess.run([openssl, *map(str, values)], capture_output=True, timeout=30)
                verify = execute("verify", "-CAfile", directory / "root.pem", "-untrusted", directory / "cert.pem", directory / "cert.pem")
                identity = execute("x509", "-in", directory / "cert.pem", "-noout", "-ext", "subjectAltName")
                assert verify.returncode == 0 and principal in re.findall(r"URI:([^,\s]+)", identity.stdout.decode()), "Unexpected real Istio public identity"
                if publish:
                    key = execute("pkey", "-in", directory / "key.pem", "-pubout")
                    certificate_key = execute("x509", "-in", directory / "cert.pem", "-pubkey", "-noout")
                    if key.returncode or key.stdout != certificate_key.stdout:
                        time.sleep(1)
                        continue
            public = {"uri_san": principal, "leaf_sha256": ca_gate.material.SigningMaterial.fingerprints(data["cert.pem"])[0],
                      "root_authorities_sha256": ca_gate.material.SigningMaterial.fingerprints(data["root.pem"]), "pkix_verified": True}
            if publish:
                owned("Pod", namespace, probe.provider)
                for name in ("root.pem", "key.pem", "cert.pem"):
                    result = subprocess.run(kube + ["-n", namespace, "exec", "-i", probe.provider, "-c", "app", "--", "sh", "-c",
                        'umask 077; cat > "$1.tmp" && mv "$1.tmp" "$1"', "sh", "/runtime/root-rotation/" + name],
                        input=data[name], capture_output=True, timeout=45)
                    if result.returncode:
                        raise RuntimeError("Could not publish owned SAN identity input")
            return public
        raise RuntimeError("Real Istio identity files did not form a coherent pair")

    def observed_identity(principal, public, previous_epoch=None):
        def accepted(value):
            certs = list(value.get("certificates", {}).values())
            return (len(certs) == 1 and certs[0].get("ready") and principal in certs[0].get("uri_sans", [])
                and certs[0].get("leaf_sha256") == public["leaf_sha256"]
                and (previous_epoch is None or value.get("security_epoch", 0) > previous_epoch))
        return policy.wait_for("server SDK identity", lambda: json.loads(probe.exec_read(probe.provider,
            "/runtime/rotation-observation.json")), accepted, options.timeout)

    def discovery(label, available):
        def shape(value):
            return (value.get("connection") == "READY" and value.get("runtime_count") == 1
                and all(value.get("targets", {}).get(t, {}).get("discovered_endpoints") == 1
                    and value["targets"][t].get("available_instances") == (1 if options.cross_service and t.startswith("dubbo://") else available)
                    and value["targets"][t].get("graph_ready") for t in probe.targets)
                and all(value.get("accepted_endpoints", {}).get(c, {}).get("endpoint_count") == 1
                        for c in probe.clusters + [probe.metadata_cluster]))
        value = policy.wait_for(label, lambda: probe.rpc("inspect"), shape, options.timeout)
        record(label, value)
        if options.cross_service:
            for target, cluster, application, port in ((probe.targets[0], probe.clusters[0], target_app, 17070),
                    (probe.targets[1], "outbound|7071||" + entry, entry_app, 20880)):
                instances = value["targets"][target].get("instances", [])
                if instances and any(x.get("cluster") != cluster or x.get("application") != application
                        or x.get("workload_port") != port for x in instances):
                    raise AssertionError("Cross-Service discovery mixed destination metadata/address context")
            source_tls = value.get("accepted_clusters", {}).get("outbound|7072||" + entry, {}).get("tls", {})
            if not source_tls.get("san_matchers") or any(m.get("pattern_type") != "EXACT"
                    or m.get("pattern") != donor_principal for m in source_tls["san_matchers"]):
                raise AssertionError("Entry A metadata did not retain its distinct actual SA constraint")
        return value

    fingerprints = {}
    def tls(label, expected, peer_sha=None):
        for port in (7070, 7072):
            value = probe.rpc("tls-peer", port=port)
            record(label + "-tls-" + str(port), value)
            matchers = value.get("actual_tls_policy", {}).get("san_matchers", [])
            if not matchers or any(m.get("pattern_type") != "EXACT" or m.get("pattern") != probe.principal
                    or m.get("ignore_case") for m in matchers):
                raise AssertionError("Actual Istio CDS did not constrain the expected destination identity")
            if port in fingerprints and value.get("cluster_sha256") != fingerprints[port]:
                raise AssertionError("SAN test changed the actual destination CDS payload")
            fingerprints.setdefault(port, value["cluster_sha256"])
            actual, control = value.get("actual_xds", {}), value.get("pkix_only_test_control", {})
            if actual.get("status") != expected or control.get("status") != "OK" or control.get("alpn") != "h2":
                raise AssertionError("Destination SAN handshake/control did not match")
            if expected == "TLS_REJECTED" and actual.get("san_mismatch") is not True:
                raise AssertionError("Actual TLS failed for a reason other than destination SAN mismatch")
            if peer_sha and control.get("tls", {}).get("peer_leaf_sha256") != peer_sha:
                raise AssertionError("PKIX control did not see the expected server certificate")

    def business(label):
        for method, protocol, count in (("echo", "tri", 3), ("async", "tri", 2), ("stream", "tri", 1), ("echo", "dubbo", 3)):
            value = probe.rpc(method, protocol, count)
            identity = "entry-a" if options.cross_service and protocol == "dubbo" else "real-ca"
            expected = ([identity + ":native-destination-san:" + str(i) for i in range(3)] if method == "stream"
                        else [identity + ":native-destination-san"] * count)
            record(label + "-" + method + "-" + protocol, value)
            if not policy.successful(value, len(expected)) or value.get("values") != expected:
                raise AssertionError("Destination SAN business sample failed")

    failure = None
    try:
        record("echo-baseline", policy.wait_for("retained echo ADS", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
        create({"apiVersion": "v1", "kind": "Namespace", "metadata": metadata(namespace, labels={"istio-injection": "enabled"})})
        create({"apiVersion": "v1", "kind": "ServiceAccount", "metadata": metadata(donor, namespace)})
        meta = metadata(donor, namespace, {"app": donor})
        meta["annotations"] = {"inject.istio.io/templates": "grpc-agent", "proxy.istio.io/config": json.dumps({
            "holdApplicationUntilProxyStarts": True, "proxyMetadata": {"SECRET_TTL": "30m"}})}
        donor_container = {"name": "app", "image": app["image"], "resources": copy.deepcopy(app["resources"]),
            "command": ["sh", "-c", "while :; do sleep 60; done"], "readinessProbe": {"exec": {"command": [
                "test", "-f", "/var/lib/istio/data/cert-chain.pem"]}, "periodSeconds": 2}}
        donor_volumes = []
        if options.cross_service:
            donor_container.update({"env": [{"name": k, "value": v} for k, v in {
                "JAVA_TOOL_OPTIONS": "-XX:ActiveProcessorCount=2", "NATIVE_XDS_PROVIDER_APP": entry_app,
                "NATIVE_XDS_METADATA_PORT": "17072"}.items()],
                "command": ["sh", "-c", "while [ ! -f /runtime/bundle-ready ]; do sleep 1; done; "
                    "java -jar /runtime/dubbo-xds-native-test-app-3.3.7-SNAPSHOT.jar prepare-rotation unused /runtime && "
                    "export GRPC_XDS_BOOTSTRAP=/runtime/rotation-bootstrap.json && exec java -jar "
                    "/runtime/dubbo-xds-native-test-app-3.3.7-SNAPSHOT.jar rotation-provider entry-a /runtime"],
                "volumeMounts": [{"name": "runtime", "mountPath": "/runtime"}],
                "readinessProbe": {"exec": {"command": ["test", "-f", "/runtime/provider-ready"]}, "periodSeconds": 2}})
            donor_volumes = [{"name": "runtime", "emptyDir": {}}]
            create({"apiVersion": "v1", "kind": "Service", "metadata": metadata("entry-a", namespace), "spec": {
                "selector": {"app": donor}, "ports": [{"name": name, "port": port, "targetPort": target}
                    for name, port, target in (("grpc-triple", 7070, 17070), ("tcp-dubbo", 7071, 20880), ("grpc-metadata", 7072, 17072))]}})
            create({"apiVersion": "security.istio.io/v1", "kind": "PeerAuthentication", "metadata": metadata("entry-a", namespace),
                "spec": {"selector": {"matchLabels": {"app": donor}}, "mtls": {"mode": "STRICT"}, "portLevelMtls": {"20880": {"mode": "DISABLE"}}}})
            create({"apiVersion": "security.istio.io/v1", "kind": "AuthorizationPolicy", "metadata": metadata("entry-a", namespace),
                "spec": {"selector": {"matchLabels": {"app": donor}}, "action": "ALLOW", "rules": [{
                    "from": [{"source": {"principals": [probe.principal.removeprefix("spiffe://")]}}],
                    "to": [{"operation": {"ports": ["17070", "17072"]}}]}]}})
            create({"apiVersion": "networking.istio.io/v1", "kind": "DestinationRule", "metadata": metadata("entry-a", namespace),
                "spec": {"host": entry, "trafficPolicy": {"portLevelSettings": [{"port": {"number": p}, "tls": {"mode": m}}
                    for p, m in ((7070, "ISTIO_MUTUAL"), (7071, "DISABLE"), (7072, "ISTIO_MUTUAL"))]}}})
            create({"apiVersion": "networking.istio.io/v1", "kind": "VirtualService", "metadata": metadata("entry-a", namespace),
                "spec": {"hosts": [entry], "http": [{"match": [{"port": p}], "route": [{"destination": {
                    "host": h, "port": {"number": p}}}]} for p, h in ((7070, probe.host), (7072, entry))]}})
        create({"apiVersion": "v1", "kind": "Pod", "metadata": meta, "spec": {"serviceAccountName": donor,
            "restartPolicy": "Never", "containers": [{"name": "app", "image": app["image"], "resources": copy.deepcopy(app["resources"]),
                **donor_container}], **({"volumes": donor_volumes} if donor_volumes else {})}})
        if options.cross_service:
            ca_gate.read(kube, "-n", namespace, "wait", "--for=jsonpath={.status.phase}=Running", "pod/" + donor,
                         "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
            with tempfile.TemporaryDirectory(prefix="dubbo-san-entry-bundle-") as temporary:
                archive = pathlib.Path(temporary) / "bundle.tgz"
                with tarfile.open(archive, "w:gz") as output:
                    output.add(options.bundle / "dubbo-xds-native-test-app-3.3.7-SNAPSHOT.jar", arcname="dubbo-xds-native-test-app-3.3.7-SNAPSHOT.jar")
                    output.add(options.bundle / "lib", arcname="lib")
                owned("Pod", namespace, donor)
                smoke.run(kube, "-n", namespace, "cp", str(archive), donor + ":/runtime/bundle.tgz", "-c", "app", timeout=180)
                smoke.run(kube, "-n", namespace, "exec", donor, "-c", "app", "--", "sh", "-c",
                          "tar xzf /runtime/bundle.tgz -C /runtime && rm /runtime/bundle.tgz && touch /runtime/bundle-ready")
        ca_gate.read(kube, "-n", namespace, "wait", "--for=condition=Ready", "pod/" + donor,
                     "--timeout=" + str(options.timeout) + "s", timeout=options.timeout + 20)
        probe.start()
        initial_pods = {name: probe.containers(owned("Pod", namespace, name)) for name in (probe.provider, probe.consumer, donor)}
        first = discovery("initial-discovery", 1)
        initial_public = capture(probe.provider, probe.principal, application_copy=True)
        server = observed_identity(probe.principal, initial_public)
        record("initial-server-identity", server)
        tls("initial", "OK", initial_public["leaf_sha256"])
        business("initial-business")
        wrong = capture(donor, donor_principal, publish=True)
        record("real-donor-identity", wrong)
        if set(wrong["root_authorities_sha256"]) != set(initial_public["root_authorities_sha256"]):
            raise AssertionError("Donor identity was not issued under the same mesh CA")
        changed = observed_identity(donor_principal, wrong, server["security_epoch"])
        record("wrong-server-identity-loaded", changed)
        tls("wrong-identity", "TLS_REJECTED", wrong["leaf_sha256"])
        denied = discovery("metadata-isolates-wrong-destination", 0)
        affected = probe.targets[:1] if options.cross_service else probe.targets
        if any(not denied["targets"][t].get("metadata_failure_statuses")
                or denied["targets"][t].get("metadata_failures", 0) <= first["targets"][t].get("metadata_failures", 0)
                for t in affected):
            raise AssertionError("Discovery did not record failing authenticated metadata reads")
        if options.cross_service:
            stable = denied["targets"][probe.targets[1]]
            if stable.get("metadata_failure_statuses") or stable["metadata_successes"] <= first["targets"][probe.targets[1]]["metadata_successes"]:
                raise AssertionError("Wrong B identity interfered with A binary metadata")
        for method, protocol in (("echo", "tri"), ("async", "tri"), ("stream", "tri"), ("echo", "dubbo")):
            value = probe.rpc(method, protocol)
            record("wrong-destination-reference-" + method + "-" + protocol, value)
            if options.cross_service and protocol == "dubbo":
                if not policy.successful(value, 1) or value.get("values") != ["entry-a:native-destination-san"]:
                    raise AssertionError("Binary A stopped with wrong B identity")
                continue
            if value.get("status") != "RPC_ERROR" or value.get("errors") != {"UNAVAILABLE": 1} or value.get("values"):
                raise AssertionError("Reference used an instance with invalid destination identity")
        restored = capture(probe.provider, probe.principal, publish=True)
        record("restored-agent-identity", restored)
        final_server = observed_identity(probe.principal, restored, changed["security_epoch"])
        record("restored-server-identity-loaded", final_server)
        final = discovery("restored-discovery", 1)
        if any(final["targets"][t].get("metadata_failure_statuses") or final["targets"][t]["metadata_successes"]
                <= denied["targets"][t]["metadata_successes"] for t in probe.targets):
            raise AssertionError("Metadata did not recover with the correct destination identity")
        tls("restored", "OK", restored["leaf_sha256"])
        business("restored-business")
        if final["process_start_ms"] != first["process_start_ms"] or final_server["process_start_ms"] != server["process_start_ms"]:
            raise AssertionError("SAN test restarted an SDK JVM")
        for name, ids in initial_pods.items():
            pod = owned("Pod", namespace, name)
            if probe.containers(pod) != ids or any(c.get("restartCount", 0) for c in pod["status"]["containerStatuses"]):
                raise AssertionError("SAN test restarted an agent/application/Pod")
        record("same-processes-containers-and-CDS", {"consumer_start_ms": final["process_start_ms"],
            "provider_start_ms": final_server["process_start_ms"], "containers": initial_pods, "cds_sha256": fingerprints})
    except BaseException as caught:
        failure = caught
        report["status"], report["error_type"] = "failed", type(caught).__name__
        if isinstance(caught, AssertionError):
            report["assertion"] = str(caught)
        save()
    finally:
        print("Cleaning only owned destination SAN resources", flush=True)
        errors = []
        for kind, ns, name, uid in reversed(resources):
            try:
                text = ca_gate.read(kube, *args(kind, ns, name), "--ignore-not-found=true", "-o", "json")
                current = json.loads(text) if text.strip() else None
                if current is None:
                    continue
                if current["metadata"].get("labels", {}).get(OWNER) != run or (uid and current["metadata"]["uid"] != uid):
                    raise AssertionError("Refusing to delete an unowned SAN resource")
                prefix = [] if kind == "Namespace" else ["-n", ns]
                result = subprocess.run(kube + prefix + ["delete", kind, name, "--ignore-not-found=true", "--wait=false"],
                    text=True, capture_output=True, timeout=45)
                if result.returncode:
                    policy.wait_for("delete outcome", lambda: ca_gate.read(kube, *args(kind, ns, name),
                        "--ignore-not-found=true", "-o", "name"), lambda v: not v.strip(), options.timeout)
            except Exception as caught:
                errors.append(type(caught).__name__)
        try:
            for kind, ns, name, uid in resources:
                policy.wait_for("SAN resource cleanup", lambda k=kind, s=ns, n=name: ca_gate.read(kube, *args(k, s, n),
                    "--ignore-not-found=true", "-o", "name"), lambda v: not v.strip(), options.timeout)
            if baseline() != before:
                raise AssertionError("Retained mesh/echo baseline changed")
            record("echo-baseline-after", policy.wait_for("retained echo ADS", lambda: smoke.rpc(kube, method="inspect"), policy.ready, options.timeout))
            value = smoke.rpc(kube)
            record("echo-business-after", value)
            if not policy.successful(value, 1):
                raise AssertionError("Retained echo business failed")
        except Exception as caught:
            errors.append(type(caught).__name__)
        if errors:
            report["status"], report["cleanup_errors"] = "cleanup_failed", errors
            if failure is None:
                failure = RuntimeError("SAN fixture cleanup failed")
        else:
            report["cleaned_up"] = True
            if failure is None:
                report["status"] = "passed"
        save()
    if failure is not None:
        raise failure
    print("Actual Istio destination SAN rejection/recovery passed; resources removed", flush=True)


if __name__ == "__main__":
    main()
