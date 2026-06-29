# Threat Model

This project maintains a living threat model at [`docs/threat-model.md`](docs/threat-model.md).

The threat model describes:

- The implicit contract between Apache Dubbo and its downstream users
- Trust boundaries, adversary model, and assumptions about the deployment environment
- Security properties Dubbo provides (conditional on configuration)
- Security properties Dubbo explicitly does *not* provide
- Downstream responsibilities for integrators
- Known misuse patterns and recurring false positives
- Triage dispositions for classifying vulnerability reports

## For vulnerability reporters

Before reporting, consult the threat model to determine whether the finding:

- Violates a claimed security property (§8) — report to [security@dubbo.apache.org](mailto:security@dubbo.apache.org)
- Falls under an out-of-scope or by-design disclaimer (§3, §9) — will be closed citing the threat model
- Matches a known non-finding (§11a) — already documented as a false positive

## For automated security tools

The threat model includes a machine-readable companion structure (§15) and a triage disposition table (§13) that can be used to automate report classification.

Key triage dispositions:

| Disposition | When to apply |
|-------------|--------------|
| `VALID` | Violates §8 property via in-scope adversary |
| `OUT-OF-MODEL: trusted-input` | Requires attacker control of trusted registry/config |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires excluded attacker capability |
| `BY-DESIGN: property-disclaimed` | Concerns property explicitly disclaimed in §9 |
| `KNOWN-NON-FINDING` | Matches documented false positive in §11a |
