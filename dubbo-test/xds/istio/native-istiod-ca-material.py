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

"""Private test root/intermediate inputs for an owned, real Istiod CA."""

import base64
import hashlib
import pathlib
import re
import shutil
import ssl
import subprocess
import tempfile


class SigningMaterial:
    def __init__(self, owner):
        if not re.fullmatch("[a-f0-9]{32}", owner):
            raise ValueError("Invalid CA fixture owner")
        self.temporary = tempfile.TemporaryDirectory(prefix="dubbo-xds-istiod-ca-")
        self.directory = pathlib.Path(self.temporary.name)
        self.openssl = shutil.which("openssl") or "/usr/bin/openssl"
        self.owner = owner
        try:
            self.generate()
        except BaseException:
            self.close()
            raise

    def execute(self, *arguments, input=None, expect_success=True):
        value = subprocess.run([self.openssl, *map(str, arguments)], input=input,
                               capture_output=True, timeout=30)
        if expect_success and value.returncode:
            raise RuntimeError("Could not generate or validate private Istiod CA inputs")
        return value

    def generate(self):
        config = self.directory / "ca.cnf"
        config.write_text("[req]\ndistinguished_name=dn\n[dn]\n"
                          "[root]\nbasicConstraints=critical,CA:true,pathlen:1\n"
                          "keyUsage=critical,keyCertSign,cRLSign\nsubjectKeyIdentifier=hash\n"
                          "[intermediate]\nbasicConstraints=critical,CA:true,pathlen:0\n"
                          "keyUsage=critical,keyCertSign,cRLSign\nsubjectKeyIdentifier=hash\n"
                          "authorityKeyIdentifier=keyid,issuer\n")
        for index, label in enumerate(("A", "B")):
            root_key, root_cert = (self.directory / (label + suffix) for suffix in ("-root.key", "-root.pem"))
            key, csr, cert = (self.directory / (label + suffix) for suffix in ("-ca.key", "-ca.csr", "-ca.pem"))
            self.execute("req", "-new", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
                         "-subj", "/CN=dubbo-istiod-root-" + label + "-" + self.owner[:10],
                         "-config", config, "-extensions", "root", "-keyout", root_key, "-out", root_cert)
            root_key.chmod(0o600)
            self.execute("req", "-new", "-newkey", "rsa:2048", "-nodes",
                         "-subj", "/CN=dubbo-istiod-intermediate-" + label + "-" + self.owner[:10],
                         "-keyout", key, "-out", csr)
            key.chmod(0o600)
            self.execute("x509", "-req", "-in", csr, "-CA", root_cert, "-CAkey", root_key,
                         "-set_serial", str(index + 1), "-days", "1", "-extfile", config,
                         "-extensions", "intermediate", "-out", cert)
            self.execute("verify", "-CAfile", root_cert, cert)

    @staticmethod
    def fingerprints(pem):
        blocks = re.findall(b"-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----", pem, re.DOTALL)
        if not blocks:
            raise ValueError("No public certificates observed")
        return [hashlib.sha256(ssl.PEM_cert_to_DER_cert(block.decode("ascii"))).hexdigest() for block in blocks]

    def roots(self, phase):
        labels = ["A"] if phase == "initial" else ["B"] if phase == "final" else ["A", "B"]
        if phase not in ("initial", "overlap", "migrated", "final"):
            raise ValueError("Unknown Istiod CA phase")
        return b"".join((self.directory / (label + "-root.pem")).read_bytes() for label in labels)

    def data(self, phase):
        signer = "A" if phase in ("initial", "overlap") else "B"
        certificate = (self.directory / (signer + "-ca.pem")).read_bytes()
        files = {"ca-cert.pem": certificate, "ca-key.pem": (self.directory / (signer + "-ca.key")).read_bytes(),
                 "root-cert.pem": self.roots(phase),
                 "cert-chain.pem": certificate + (self.directory / (signer + "-root.pem")).read_bytes()}
        return {key: base64.b64encode(value).decode("ascii") for key, value in files.items()}

    def public(self, phase):
        signer = "A" if phase in ("initial", "overlap") else "B"
        return {"phase": phase, "root_authorities_sha256": self.fingerprints(self.roots(phase)),
                "signer_sha256": self.fingerprints((self.directory / (signer + "-ca.pem")).read_bytes())[0]}

    def verify_workload(self, chain, phase, principal):
        signer = "A" if phase in ("initial", "overlap") else "B"
        # Only public workload certificates go into this temporary verification file.
        leaf = self.directory / "observed-leaf.pem"
        leaf.write_bytes(chain)
        roots = self.directory / "observed-roots.pem"
        roots.write_bytes(self.roots(phase))
        intermediate = self.directory / (signer + "-ca.pem")
        self.execute("verify", "-CAfile", roots, "-untrusted", intermediate, leaf)
        identity = self.execute("x509", "-in", leaf, "-noout", "-ext", "subjectAltName").stdout.decode("utf-8")
        if principal not in re.findall(r"URI:([^,\s]+)", identity):
            raise AssertionError("Istiod workload certificate has an unexpected identity")
        issuer = self.execute("x509", "-in", leaf, "-noout", "-issuer", "-nameopt", "RFC2253").stdout.decode("utf-8").strip()
        if issuer != "issuer=CN=dubbo-istiod-intermediate-" + signer + "-" + self.owner[:10]:
            raise AssertionError("Workload was not signed by the phase's Istiod intermediate")
        other = "B" if signer == "A" else "A"
        if self.execute("verify", "-CAfile", self.directory / (other + "-root.pem"),
                        "-untrusted", intermediate, leaf, expect_success=False).returncode == 0:
            raise AssertionError("Wrong root unexpectedly verified the workload")
        return {"leaf_sha256": self.fingerprints(chain)[0], "issuer": issuer, "uri_san": principal,
                "wrong_root_rejected": True}

    def close(self):
        self.temporary.cleanup()
