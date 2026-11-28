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

"""Normal Dubbo provider/consumer in the owned, real Istiod signing-CA fixture."""

import copy
import hashlib
import json
import pathlib
import subprocess
import tarfile
import tempfile
import time
import uuid


class SdkProbe:
    def __init__(self, kube, namespace, trust, bundle, app, metadata, create, owned, read,
                 record, policy, timeout, inflight=False):
        self.kube, self.namespace, self.trust = kube, namespace, trust
        self.bundle, self.app = bundle, app
        self.metadata, self.create, self.owned = metadata, create, owned
        self.read, self.record, self.policy, self.timeout = read, record, policy, timeout
        self.provider, self.consumer = "ca-sdk-server", "ca-sdk-client"
        self.host = "ca-sdk." + namespace + ".svc.cluster.local"
        self.principal = "spiffe://cluster.local/ns/" + namespace + "/sa/ca-sdk"
        self.clusters = ["outbound|7070||" + self.host]
        self.metadata_cluster = "outbound|7072||" + self.host
        self.targets = ["tri://" + self.host + ":7070", "dubbo://" + self.host + ":7071"]
        self.initial_containers, self.initial_processes = {}, {}
        self.previous = None
        self.inflight, self.held = inflight, {}
        self.hold_budget = 1200000

    def pod(self, name):
        value = self.owned("Pod", self.namespace, name)
        if any(c.get("restartCount", 0) for c in value.get("status", {}).get("containerStatuses", [])):
            raise AssertionError("Real CA SDK Pod restarted")
        return value

    @staticmethod
    def containers(pod):
        return {c["name"]: c["containerID"] for c in pod["status"]["containerStatuses"]}

    def exec_read(self, name, path):
        self.pod(name)
        return self.read(self.kube, "-n", self.namespace, "exec", name, "-c", "app", "--", "cat", path)

    def rpc(self, method="echo", protocol="tri", count=1, message="real-istiod-ca", key="native-test"):
        self.pod(self.consumer)
        token = uuid.uuid4().hex
        path = "/runtime/commands/" + token
        properties = ("method=" + method + "\nprotocol=" + protocol + "\ncount=" + str(count)
                      + "\nmessage=" + message + "\nkey=" + key + "\ntimeout=3000\n")
        try:
            result = subprocess.run(self.kube + ["-n", self.namespace, "exec", "-i", self.consumer,
                "-c", "app", "--", "sh", "-c", 'cat > "$1.tmp" && mv "$1.tmp" "$1.request"', "sh", path],
                input=properties, text=True, capture_output=True, timeout=60)
        except subprocess.TimeoutExpired:
            result = None
        if result is None or result.returncode:
            # Observe the original UUID after a lost response; never redispatch a business call.
            self.record("sdk-command-publish-observation", {"token": token, "method": method})
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            try:
                result = subprocess.run(self.kube + ["-n", self.namespace, "exec", self.consumer,
                    "-c", "app", "--", "cat", path + ".json"], text=True, capture_output=True, timeout=30)
            except subprocess.TimeoutExpired:
                result = None
            if result is not None and result.returncode == 0:
                return json.loads(result.stdout)
            if result is not None and not any(x in result.stderr.lower() for x in (
                    "no such file", "network is unreachable", "unexpected eof", "connection reset by peer",
                    "i/o timeout", "tls handshake timeout", "timeout awaiting response headers")):
                raise RuntimeError("Could not observe the real CA SDK command")
            time.sleep(1)
        raise TimeoutError("Real CA SDK command did not finish")

    def start(self):
        jar = self.bundle / "dubbo-xds-native-test-app-3.3.7-SNAPSHOT.jar"
        libraries = self.bundle / "lib"
        if not jar.is_file() or not libraries.is_dir():
            raise ValueError("Build the current native application bundle first")
        self.record("sdk-bundle", {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                                  for p in [jar] + sorted(libraries.glob("*.jar"))})
        self.create({"apiVersion": "v1", "kind": "ServiceAccount",
                     "metadata": self.metadata("ca-sdk", self.namespace)})
        self.create({"apiVersion": "v1", "kind": "Service", "metadata": self.metadata("ca-sdk", self.namespace),
                     "spec": {"selector": {"app": self.provider}, "ports": [
                         {"name": "grpc-triple", "port": 7070, "targetPort": 17070},
                         {"name": "tcp-dubbo", "port": 7071, "targetPort": 20880},
                         {"name": "grpc-metadata", "port": 7072, "targetPort": 17072}]}})
        self.create({"apiVersion": "security.istio.io/v1", "kind": "PeerAuthentication",
                     "metadata": self.metadata("ca-sdk", self.namespace),
                     "spec": {"selector": {"matchLabels": {"app": self.provider}}, "mtls": {"mode": "STRICT"},
                              "portLevelMtls": {"20880": {"mode": "DISABLE"}}}})
        self.create({"apiVersion": "security.istio.io/v1", "kind": "AuthorizationPolicy",
                     "metadata": self.metadata("ca-sdk", self.namespace),
                     "spec": {"selector": {"matchLabels": {"app": self.provider}}, "action": "ALLOW", "rules": [
                         {"from": [{"source": {"principals": [self.principal.removeprefix("spiffe://")]}}],
                          "to": [{"operation": {"ports": ["17070", "17072"]}}]}]}})
        self.create({"apiVersion": "networking.istio.io/v1", "kind": "DestinationRule",
                     "metadata": self.metadata("ca-sdk", self.namespace),
                     "spec": {"host": self.host, "trafficPolicy": {"portLevelSettings": [
                         {"port": {"number": port}, "tls": {"mode": mode}}
                         for port, mode in ((7070, "ISTIO_MUTUAL"), (7071, "DISABLE"), (7072, "ISTIO_MUTUAL"))]}}})
        with tempfile.TemporaryDirectory(prefix="dubbo-real-ca-sdk-") as temporary:
            archive = pathlib.Path(temporary) / "bundle.tgz"
            with tarfile.open(archive, "w:gz") as output:
                output.add(jar, arcname=jar.name)
                output.add(libraries, arcname="lib")
            for name in (self.provider, self.consumer):
                provider = name == self.provider
                role = "rotation-provider" if provider else "consumer"
                meta = self.metadata(name, self.namespace, {"app": name})
                meta["annotations"] = {"inject.istio.io/templates": "grpc-agent", "proxy.istio.io/config": json.dumps({
                    "holdApplicationUntilProxyStarts": True, "proxyMetadata": {"SECRET_TTL": "6m",
                        "SECRET_GRACE_PERIOD_RATIO": "0.5", "SECRET_GRACE_PERIOD_RATIO_JITTER": "0"}})}
                overrides = {"NATIVE_XDS_METADATA_PORT": "17072"} if provider else {
                    "NATIVE_XDS_METADATA_SERVICE_PORT": "7072", "NATIVE_XDS_METADATA_TTL": "10000",
                    "NATIVE_XDS_METADATA_REFRESH": "1000"}
                if self.inflight:
                    overrides.update({"NATIVE_XDS_STREAMS": "true", "NATIVE_XDS_HOLD_BUDGET_MS": str(self.hold_budget)})
                container = {"name": "app", "image": self.app["image"], "resources": copy.deepcopy(self.app["resources"]),
                    "env": [{"name": "JAVA_TOOL_OPTIONS", "value": "-XX:ActiveProcessorCount=2"}]
                           + [{"name": key, "value": value} for key, value in overrides.items()],
                    "command": ["sh", "-c", "while [ ! -f /runtime/bundle-ready ]; do sleep 1; done; "
                        + "java -jar /runtime/" + jar.name + " prepare-rotation unused /runtime && "
                        + "export GRPC_XDS_BOOTSTRAP=/runtime/rotation-bootstrap.json && exec java -jar /runtime/"
                        + jar.name + " " + role + " " + ("real-ca" if provider else self.host) + " /runtime"],
                    "readinessProbe": {"exec": {"command": ["test", "-f", "/runtime/"
                        + ("provider-ready" if provider else "consumer-ready")]}, "periodSeconds": 2, "failureThreshold": 90},
                    "volumeMounts": [{"name": "runtime", "mountPath": "/runtime"}]}
                self.create({"apiVersion": "v1", "kind": "Pod", "metadata": meta, "spec": {
                    "serviceAccountName": "ca-sdk", "restartPolicy": "Never", "containers": [container],
                    "volumes": [{"name": "runtime", "emptyDir": {}}]}})
            for name in (self.provider, self.consumer):
                print("Uploading real-CA SDK bundle to " + name, flush=True)
                self.read(self.kube, "-n", self.namespace, "wait", "--for=jsonpath={.status.phase}=Running", "pod/" + name,
                          "--timeout=" + str(self.timeout) + "s", timeout=self.timeout + 20)
                pod = self.pod(name)
                volume = next(v for v in pod["spec"]["volumes"] if v["name"] == "istiod-ca-cert")
                if volume.get("configMap", {}).get("name") != self.trust:
                    raise AssertionError("SDK Pod did not mount the isolated Istiod roots")
                agent = next(c for c in pod["spec"]["containers"] if c["name"] == "istio-proxy")
                env = {e["name"]: e.get("value") for e in agent["env"]}
                if any(env.get(k) != v for k, v in {"SECRET_TTL": "6m", "SECRET_GRACE_PERIOD_RATIO": "0.5",
                        "SECRET_GRACE_PERIOD_RATIO_JITTER": "0"}.items()):
                    raise AssertionError("SDK agent did not receive the rotation configuration")
                self.upload_bundle(name, archive)
                self.pod(name)
                self.policy.smoke.run(self.kube, "-n", self.namespace, "exec", name, "-c", "app", "--", "sh", "-c",
                    "tar xzf /runtime/bundle.tgz -C /runtime && rm /runtime/bundle.tgz && touch /runtime/bundle-ready")
                self.read(self.kube, "-n", self.namespace, "wait", "--for=condition=Ready", "pod/" + name,
                          "--timeout=" + str(self.timeout) + "s", timeout=self.timeout + 20)
                tuning = json.loads(self.exec_read(name, "/runtime/rotation-tuning.json"))
                if tuning.get("status") != "PREPARED" or not tuning.get("certificate_providers") or any(
                        v.get("test_refresh_interval") != "1s" for v in tuning["certificate_providers"].values()):
                    raise AssertionError("SDK certificate polling was not prepared")
                self.initial_containers[name] = self.containers(self.pod(name))
                self.record("sdk-bootstrap-tuning-" + name, tuning)

    def upload_bundle(self, name, archive):
        expected = hashlib.sha256(archive.read_bytes()).hexdigest()
        for attempt in range(4):
            self.pod(name)
            try:
                result = subprocess.run(self.kube + ["-n", self.namespace, "cp", str(archive),
                    name + ":/runtime/bundle.tgz", "-c", "app"], text=True, capture_output=True, timeout=180)
            except subprocess.TimeoutExpired:
                result = None
            self.pod(name)
            observed = subprocess.run(self.kube + ["-n", self.namespace, "exec", name, "-c", "app", "--",
                "sha256sum", "/runtime/bundle.tgz"], text=True, capture_output=True, timeout=45)
            complete = observed.returncode == 0 and observed.stdout.split()[0] == expected
            self.record("sdk-bundle-upload-" + name + "-" + str(attempt), {
                "archive_sha256": expected, "copy_return_code": result.returncode if result else None,
                "remote_hash_matches": complete})
            if complete:
                return
            # Only this exact owned file is rewritten. Observe full content before extraction;
            # a lost transfer response is not grounds to redispatch an RPC or CA mutation.
            if attempt < 3:
                time.sleep(2)
        raise RuntimeError("Owned native bundle upload did not produce a complete file")

    @staticmethod
    def certificate(snapshot, verified, expected, principal):
        values = list(snapshot.get("certificates", {}).values())
        if len(values) != 1:
            return False
        value = values[0]
        return (value.get("ready") is True and value.get("leaf_sha256") == verified["leaf_sha256"]
                and principal in value.get("uri_sans", []) and set(value.get("root_authorities_sha256", []))
                == set(expected["root_authorities_sha256"]))

    def sessions(self, snapshot, local, peer, cluster):
        return [c for c in snapshot.get("connections", []) if c.get("cluster") == cluster
                and c.get("connectivity") == "READY" and c.get("tls", {}).get("ready") is True
                and c["tls"].get("alpn") == "h2" and c["tls"].get("protocol") in ("TLSv1.2", "TLSv1.3")
                and c["tls"].get("local_leaf_sha256") == local["leaf_sha256"]
                and c["tls"].get("peer_leaf_sha256") == peer["leaf_sha256"]]

    def phase(self, ca, phase, rotation_timeout):
        expected = ca.public(phase)
        before = self.rpc("inspect") if self.previous else None
        started = time.monotonic()
        attempts = []
        def observe():
            public = {}
            for name in (self.provider, self.consumer):
                roots = self.exec_read(name, "/var/lib/istio/data/root-cert.pem").encode()
                if set(ca.fingerprints(roots)) != set(expected["root_authorities_sha256"]):
                    return None
                try:
                    public[name] = ca.verify_workload(self.exec_read(name, "/var/lib/istio/data/cert-chain.pem").encode(),
                                                    phase, self.principal)
                except (AssertionError, RuntimeError):
                    return None
            server = json.loads(self.exec_read(self.provider, "/runtime/rotation-observation.json"))
            client = self.rpc("certificates")
            for name, snapshot in ((self.provider, server), (self.consumer, client)):
                if not self.certificate(snapshot, public[name], expected, self.principal):
                    return None
                if self.initial_processes and snapshot["process_start_ms"] != self.initial_processes[name]:
                    raise AssertionError("SDK JVM restarted during real CA rotation")
                if self.previous and snapshot.get("security_epoch", 0) <= self.previous[name].get("security_epoch", 0):
                    return None
            call = self.rpc()
            inspected = self.rpc("inspect")
            client = self.rpc("certificates")
            attempts.append({"provider_leaf": public[self.provider]["leaf_sha256"],
                             "consumer_leaf": public[self.consumer]["leaf_sha256"], "business": call,
                             "target_metadata": inspected.get("targets", {})})
            if not self.policy.successful(call, 1) or call.get("values") != ["real-ca:real-istiod-ca"]:
                return None
            if not self.certificate(client, public[self.consumer], expected, self.principal):
                return None
            if inspected.get("connection") != "READY" or inspected.get("runtime_count") != 1:
                return None
            if inspected.get("accepted_endpoints", {}).get(self.metadata_cluster, {}).get("endpoint_count") != 1:
                return None
            for key in self.targets:
                state = inspected.get("targets", {}).get(key, {})
                if (state.get("discovered_endpoints") != 1 or state.get("available_instances") != 1
                        or not state.get("graph_ready") or state.get("metadata_failure_statuses")):
                    return None
                minimum = before.get("targets", {}).get(key, {}).get("metadata_successes", 0) + 1 if before else 1
                if state.get("metadata_successes", 0) < minimum:
                    return None
            if before and time.monotonic() - started < 10:
                return None
            sessions = {cluster: self.sessions(client, public[self.consumer], public[self.provider], cluster)
                        for cluster in self.clusters}
            # The normal point-to-point metadata fetcher destroys its temporary Invoker after
            # each read. Its separate STRICT listener and fresh successful reads prove the
            # metadata path; a later connection snapshot cannot retain that short-lived channel.
            if any(not v for v in sessions.values()):
                return None
            if self.previous:
                old_channels = {c["tls"].get("channel_id") for c in self.previous[self.consumer].get("connections", [])
                                if c.get("tls", {}).get("ready")}
                if any(c["tls"].get("channel_id") in old_channels for values in sessions.values() for c in values):
                    if not self.inflight:
                        raise AssertionError("Real CA phase reused a previous TLS session")
                    # Admitted A streams deliberately retain their old channel. Prove a fresh
                    # matching session for new calls without treating the retained channel as one.
                    sessions = {cluster: [c for c in values if c["tls"].get("channel_id") not in old_channels]
                                for cluster, values in sessions.items()}
                    if any(not values for values in sessions.values()):
                        return None
            return {"phase": phase, "verified_public_identities": public, "provider": server, "consumer": client,
                    "discovery": inspected, "sessions": sessions}
        try:
            checked = self.policy.wait_for("Dubbo real CA phase " + phase, observe, lambda v: v is not None, rotation_timeout)
        finally:
            self.record("sdk-" + phase + "-transition-observations", attempts)
        self.record("sdk-real-ca-" + phase, checked)
        for name, snapshot in ((self.provider, checked["provider"]), (self.consumer, checked["consumer"])):
            self.initial_processes.setdefault(name, snapshot["process_start_ms"])
            if self.containers(self.pod(name)) != self.initial_containers[name]:
                raise AssertionError("Real CA phase replaced an SDK container")
        for method, protocol, count in (("echo", "tri", 3), ("async", "tri", 2), ("stream", "tri", 1), ("echo", "dubbo", 3)):
            value = self.rpc(method, protocol, count)
            size = count * (3 if method == "stream" else 1)
            self.record("sdk-" + phase + "-business-" + method + "-" + protocol, value)
            values = (["real-ca:real-istiod-ca:" + str(i) for _ in range(count) for i in range(3)]
                      if method == "stream" else ["real-ca:real-istiod-ca"] * count)
            if not self.policy.successful(value, size) or value.get("values") != values:
                raise AssertionError("Real CA phase business sample failed")
        self.previous = {self.provider: checked["provider"], self.consumer: checked["consumer"]}
        if self.held:
            self.held_state("after-" + phase)
        print("Real Istiod CA consumed by Dubbo TLS/business/metadata: " + phase, flush=True)

    def finish(self):
        if self.held:
            self.finish_held_calls()
        self.record("sdk-same-processes-and-containers", {"process_start_ms": self.initial_processes,
                    "containers": {name: self.containers(self.pod(name)) for name in (self.provider, self.consumer)}})
        if any(self.containers(self.pod(name)) != before for name, before in self.initial_containers.items()):
            raise AssertionError("Real CA test replaced SDK containers")

    def begin_held_calls(self):
        suffix = uuid.uuid4().hex
        self.held = {kind: kind + "-" + suffix for kind in ("unary", "stream", "client-stream", "bidi")}
        for kind, token in self.held.items():
            value = self.rpc("hold-start", message=token, key=kind)
            self.record("real-ca-held-start-" + kind, value)
            if value.get("status") != "ACTIVE" or value.get("terminals") or value.get("budget_ms") != self.hold_budget:
                raise AssertionError("Real CA hold did not start with its declared fixed budget")
        self.held_state("before-overlap")

    def held_state(self, label, half_closed=False):
        for kind, token in self.held.items():
            def observe():
                return {"client": self.rpc("hold-status", message=token), "provider": json.loads(self.exec_read(
                    self.provider, "/runtime/holds/" + token + ".json"))}
            def ready(value):
                return len(value["client"].get("values", [])) == (2 if kind == "bidi" and half_closed
                    else 1 if kind in ("stream", "bidi") else 0)
            value = self.policy.wait_for("held " + kind + " " + label, observe, ready, 30)
            client, provider = value["client"], value["provider"]
            self.record("real-ca-held-" + label + "-" + kind, value)
            if (client.get("status") != "ACTIVE" or client.get("terminals") or client.get("budget_ms") != self.hold_budget
                    or provider.get("status") != "ACTIVE" or provider.get("attempts") != 1 or provider.get("cancelled")
                    or provider.get("budget_ms") != self.hold_budget):
                raise AssertionError("Actual CA transition interrupted or redispatched a held RPC")
            if kind in ("client-stream", "bidi") and (provider.get("request_messages") != (
                    ["hold:" + token, token + ":1"] if half_closed else ["hold:" + token])
                    or provider.get("half_closed") is not half_closed):
                raise AssertionError("Actual CA transition changed the held input stream")
        inspected = self.rpc("inspect")
        self.record("real-ca-held-admissions-" + label, inspected)
        if inspected.get("accepted_clusters", {}).get(self.clusters[0], {}).get("active_requests") != 4:
            raise AssertionError("Actual CA transition released held admissions early")

    def finish_held_calls(self):
        for kind in ("client-stream", "bidi"):
            value = self.rpc("hold-send", message=self.held[kind])
            self.record("real-ca-held-send-under-B-only-" + kind, value)
            if value.get("status") != "ACTIVE" or value.get("terminals") or value.get("half_closed") is not True:
                raise AssertionError("Existing A input stream could not continue under B-only trust")
        self.held_state("input-under-B-only", half_closed=True)
        self.pod(self.provider)
        self.policy.smoke.run(self.kube, "-n", self.namespace, "exec", self.provider, "-c", "app", "--", "touch",
                             *("/runtime/holds/" + token + ".release" for token in self.held.values()))
        self.record("real-ca-held-all-releases-published", {"tokens": self.held, "budget_ms": self.hold_budget})
        for kind, token in self.held.items():
            client = self.policy.wait_for("held completion " + kind, lambda: self.rpc("hold-status", message=token),
                                         lambda v: v.get("status") != "ACTIVE", 30)
            provider = json.loads(self.exec_read(self.provider, "/runtime/holds/" + token + ".json"))
            expected = (["real-ca:hold:" + token] if kind == "unary" else ["real-ca:hold:" + token + ":collected=2"]
                        if kind == "client-stream" else ["real-ca:hold:" + token + ":" + str(i)
                        for i in range(2 if kind == "bidi" else 3)])
            self.record("real-ca-held-completed-" + kind, {"client": client, "provider": provider})
            if (client.get("status") != "OK" or client.get("terminals") != 1 or client.get("values") != expected
                    or provider.get("status") != "RELEASED" or provider.get("attempts") != 1 or provider.get("cancelled")
                    or not 0 <= client.get("completion_elapsed_ms", -1) <= self.hold_budget
                    or client.get("completed_at_ms", 0) < client.get("started_at_ms", 1)):
                raise AssertionError("Held RPC did not complete once inside its original declared budget")
            self.rpc("hold-forget", message=token)
        value = self.rpc("inspect")
        self.record("real-ca-held-admissions-released", value)
        if value["accepted_clusters"][self.clusters[0]]["active_requests"] != 0:
            raise AssertionError("Completed real CA holds leaked admission")
