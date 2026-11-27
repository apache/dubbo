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

"""Test-only CA inputs for native SDK rollover; never rotates the cluster's signing CA."""

import hashlib
import pathlib
import shutil
import ssl
import subprocess
import tempfile
import time


class RootMaterial:
    def __init__(self, principal):
        self.temporary = tempfile.TemporaryDirectory(prefix="dubbo-xds-test-roots-")
        self.directory = pathlib.Path(self.temporary.name)
        self.openssl = shutil.which("openssl") or "/usr/bin/openssl"
        self.principal = principal
        self.certificates = {}
        self.roots = {}
        try:
            self.generate()
        except Exception:
            self.close()
            raise

    def execute(self, *arguments):
        value = subprocess.run([self.openssl, *map(str, arguments)], capture_output=True, timeout=30)
        if value.returncode:
            # Commands write keys to private temporary files, never to stdout or the report.
            raise RuntimeError("Could not generate/validate test CA input")

    def generate(self):
        ca_config = self.directory / "ca.cnf"
        ca_config.write_text("[req]\ndistinguished_name=dn\n[dn]\n[ca]\nbasicConstraints=critical,CA:true\n"
                             "keyUsage=critical,keyCertSign,cRLSign\nsubjectKeyIdentifier=hash\n")
        leaf_config = self.directory / "leaf.cnf"
        leaf_config.write_text("[leaf]\nbasicConstraints=critical,CA:false\n"
                               "keyUsage=critical,digitalSignature,keyEncipherment\n"
                               "extendedKeyUsage=serverAuth,clientAuth\nsubjectKeyIdentifier=hash\n"
                               "authorityKeyIdentifier=keyid,issuer\nsubjectAltName=URI:" + self.principal + "\n")
        for label in ("A", "B"):
            ca_key, ca_cert = self.directory / (label + ".key"), self.directory / (label + ".crt")
            self.execute("req", "-new", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
                         "-subj", "/CN=dubbo-xds-test-root-" + label, "-config", ca_config,
                         "-extensions", "ca", "-keyout", ca_key, "-out", ca_cert)
            self.roots[label] = ca_cert.read_bytes()
            for index, role in enumerate(("provider", "consumer")):
                name = label + "-" + role
                key, csr, cert = [self.directory / (name + suffix) for suffix in (".key", ".csr", ".crt")]
                self.execute("req", "-new", "-newkey", "rsa:2048", "-nodes", "-subj", "/CN=dubbo-xds-test-" + role,
                             "-keyout", key, "-out", csr)
                self.execute("x509", "-req", "-in", csr, "-CA", ca_cert, "-CAkey", ca_key,
                             "-set_serial", str((1 if label == "A" else 3) + index), "-days", "1",
                             "-extfile", leaf_config, "-extensions", "leaf", "-out", cert)
                self.execute("verify", "-CAfile", ca_cert, cert)
                key.chmod(0o600)
                self.certificates[(label, role)] = {"cert.pem": cert.read_bytes(), "key.pem": key.read_bytes()}
            ca_key.chmod(0o600)

    @staticmethod
    def fingerprint(pem):
        return hashlib.sha256(ssl.PEM_cert_to_DER_cert(pem.decode("ascii"))).hexdigest()

    def expected(self, phase, role):
        label = "A" if phase in ("initial", "overlap") else "B"
        roots = ["A"] if phase == "initial" else (["B"] if phase == "final" else ["A", "B"])
        return {"leaf_sha256": self.fingerprint(self.certificates[(label, role)]["cert.pem"]),
                "root_authorities_sha256": [self.fingerprint(self.roots[x]) for x in roots]}

    def publish(self, kube, namespace, pod, role, phase):
        label = "A" if phase in ("initial", "overlap") else "B"
        roots = self.roots["A"] if phase == "initial" else (self.roots["B"] if phase == "final" else self.roots["A"] + self.roots["B"])
        files = {"root.pem": roots}
        if phase in ("initial", "migrated"):
            files = {**self.certificates[(label, role)], **files}
        for name, data in files.items():
            for attempt in range(3):
                value = subprocess.run(kube + ["-n", namespace, "exec", "-i", pod, "-c", "app", "--", "sh", "-c",
                    'umask 077; mkdir -p /runtime/root-rotation && cat > "$1.tmp" && mv "$1.tmp" "$1"',
                    "sh", "/runtime/root-rotation/" + name], input=data, capture_output=True, timeout=45)
                if value.returncode == 0:
                    break
                if not any(x in value.stderr.decode("utf-8", "replace").lower() for x in ("connection reset by peer", "unexpected eof")):
                    raise RuntimeError("Could not publish test certificate input")
                # Rewriting these exact files is idempotent; this is not an RPC redispatch.
                time.sleep(1)
            else:
                raise RuntimeError("Test certificate file publication did not recover")

    def close(self):
        self.temporary.cleanup()
