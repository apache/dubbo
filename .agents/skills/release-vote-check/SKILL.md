---
name: release-vote-check
description: Verify an Apache Dubbo release candidate before voting on dev@dubbo.apache.org - checksums, signatures, source package vs. tag, binaries, license headers, LICENSE/NOTICE, build - and draft the vote reply. Use when asked to check, verify or vote on a Dubbo release (a "[VOTE] Release Apache Dubbo x.y.z" email or an ATR vote page).
---

# Release vote check

Checks a Dubbo release candidate against the ASF release policy and drafts the vote reply.
Never cast the vote: the voter replies on the mailing list or votes on ATR.

## Inputs

Take these from the vote email:

- Version, for example `3.3.7`.
- Source commit, the "recorded source commit" line.
- Artifact location. ATR votes use `https://releases.apache.org/download/path/dubbo/<version>`.
  Older votes use `https://dist.apache.org/repos/dist/dev/dubbo/<version>`.

## Steps

1. Run the script from the repository root. It writes only under the work directory
   (default `${TMPDIR:-/tmp}/dubbo-<version>-rc`) and uses a temporary GnuPG home.

   ```bash
   .agents/skills/release-vote-check/scripts/verify-rc.sh -c <commit> <version>
   # other location:   -u https://dist.apache.org/repos/dist/dev/dubbo/<version>
   # build as well:    -b   (about 5 minutes; tests skipped)
   ```

   The script checks:

   - sha512 and GPG signature of every package, with keys from `https://downloads.apache.org/dubbo/KEYS`
   - the tag commit equals the vote commit
   - the source package equals the tag tree, except the files the source assembly excludes
     (`.mvn`, `mvnw*`, `build`, `.gitignore`)
   - the version in `pom.xml`, empty directories, binary files
   - ASF headers; a file without one must be listed in `.licenserc.yaml` and named in `LICENSE`
   - `LICENSE` and `NOTICE` in each package, and the `NOTICE` year

2. Read every `[FAIL]` and `[WARN]` line yourself before you report it.
   For a header-exempt file, open its header and the matching `LICENSE` section.

3. Check what the script cannot:

   - `LICENSE` and `NOTICE` text: third-party code in the package has its license listed in `LICENSE`.
     `NOTICE` carries only required attributions, not license text.
   - Dependency licenses, when dependencies changed since the last release:

     ```bash
     mvn license:add-third-party -Dlicense.useMissingFile
     find . -name THIRD-PARTY.txt | xargs grep -E 'GPL|General Public License' \
       | grep -v Apache | grep -v MIT | grep -v CDDL
     ```

   - Unit tests (`mvn clean test`), only when the voter asks; the full suite runs for a long time.

4. Compare each finding with the previous release (`git show dubbo-<previous>:<path>`).
   A problem that already shipped is usually a non-blocking note for the next release.

## Judging findings

- Blocking (`-1`): bad checksum or signature, a signing key not in `KEYS`, source package content that
  differs from the tag, compiled code in the source package, a file whose license is incompatible or missing
  from `LICENSE`, or a build failure.
- Non-blocking (`+1` with notes): an old `NOTICE` year, stray empty directories, an incomplete
  third-party entry in `LICENSE` that already shipped.
- No `DISCLAIMER` is needed: Dubbo is a top-level project, not an incubator project.

## Vote reply

Report the result to the voter first, then offer a reply they can send:

```
+1 (non-binding)

I checked:
- Checksums and signatures are valid
- Source package matches tag <version> (<short commit>)
- No binary files in the source package
- LICENSE and NOTICE exist
- Built from source successfully with JDK <n>

Minor, non-blocking:
- <finding>
```

Use `(binding)` only for Dubbo PMC members.

## References

- https://www.apache.org/legal/release-policy.html
- https://cn.dubbo.apache.org/zh-cn/contact/committer/release-guide/java-sdk/
