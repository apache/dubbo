# Security Policy

## Supported Versions

Below is a table that shows versions that we accept security fixes.

| Version | Supported          |
|---------| ------------------ |
| 3.3.x   | :white_check_mark: |
| 3.2.x   | :white_check_mark: |
| 3.1.x   | :white_check_mark: |
| 3.0.x   | :x: |
| 2.7.x   | :x: |
| 2.6.x   | :x: |
| 2.5.x   | :x: |

## Reporting a Vulnerability

The Apache Software Foundation takes a rigorous standpoint in annihilating the security issues in its software projects. Apache Dubbo is highly sensitive and forthcoming to issues pertaining to its features and functionality.

If you have apprehensions regarding Dubbo's security or you discover vulnerability or potential threat, don’t hesitate to get in touch with the Apache Dubbo Security Team by dropping a mail at security@dubbo.apache.org. In the email, specify the description of the issue or potential threat. You are also urged to recommend the way to reproduce and replicate the issue. The Dubbo community will get back to you after assessing and analysing the findings.

PLEASE PAY ATTENTION to report the security issue on the security email before disclosing it on public domain.

## Vulnerability Handling

An overview of the vulnerability handling process is:

* The reporter reports the vulnerability privately to Apache.
* The appropriate project's security team works privately with the reporter to resolve the vulnerability.
* A new release of the Apache product concerned is made that includes the fix.
* The vulnerability is publicly announced.

A more detailed description of the process can be found [here](https://www.apache.org/security/committers.html).

## Threat Model

Apache Dubbo maintains a threat model at [`docs/threat-model.md`](docs/threat-model.md) that describes the project's security boundaries, adversary model, claimed and disclaimed security properties, and triage dispositions for vulnerability reports.

When reporting a vulnerability, consult the threat model first. Findings that violate claimed properties (§8) should be reported to security@dubbo.apache.org. Findings that fall under out-of-scope (§3) or by-design disclaimed properties (§9) will be closed citing the threat model.
