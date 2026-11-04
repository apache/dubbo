#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Verifies an Apache Dubbo release candidate. Writes only under the work directory.

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: verify-rc.sh [options] <version>

Options:
  -c <sha>   Source commit from the vote email (default: commit of tag <version>)
  -u <url>   Base URL of the artifacts
             (default: https://releases.apache.org/download/path/dubbo/<version>)
  -w <dir>   Work directory (default: ${TMPDIR:-/tmp}/dubbo-<version>-rc)
  -b         Also build the source package (mvn package, checkstyle on, tests skipped)
  -h         Show this help
EOF
}

COMMIT=""
BASE_URL=""
WORK=""
BUILD=0
while getopts "c:u:w:bh" opt; do
  case "${opt}" in
    c) COMMIT="${OPTARG}" ;;
    u) BASE_URL="${OPTARG}" ;;
    w) WORK="${OPTARG}" ;;
    b) BUILD=1 ;;
    h) usage; exit 0 ;;
    *) usage; exit 2 ;;
  esac
done
shift $((OPTIND - 1))
[ $# -eq 1 ] || { usage; exit 2; }

VERSION="$1"
BASE_URL="${BASE_URL:-https://releases.apache.org/download/path/dubbo/${VERSION}}"
WORK="${WORK:-${TMPDIR:-/tmp}/dubbo-${VERSION}-rc}"
KEYS_URL="https://downloads.apache.org/dubbo/KEYS"
REPO_URL="https://github.com/apache/dubbo.git"
SRC_NAME="apache-dubbo-${VERSION}-src"
BIN_NAME="apache-dubbo-${VERSION}-bin"

FAILS=0
WARNS=0
pass() { echo "[PASS] $*"; }
warn() { echo "[WARN] $*"; WARNS=$((WARNS + 1)); }
fail() { echo "[FAIL] $*"; FAILS=$((FAILS + 1)); }
section() { echo; echo "== $*"; }

sha512_check() {
  if command -v sha512sum >/dev/null 2>&1; then
    sha512sum -c "$1"
  else
    shasum -a 512 -c "$1"
  fi
}

mkdir -p "${WORK}/dl"
cd "${WORK}"

section "Download"
HAS_BIN=0
for f in "${SRC_NAME}.zip" "${SRC_NAME}.zip.asc" "${SRC_NAME}.zip.sha512"; do
  curl -sSfL -o "dl/${f}" "${BASE_URL}/${f}"
done
if curl -sSfL -o "dl/${BIN_NAME}.zip" "${BASE_URL}/${BIN_NAME}.zip" 2>/dev/null; then
  HAS_BIN=1
  for f in "${BIN_NAME}.zip.asc" "${BIN_NAME}.zip.sha512"; do
    curl -sSfL -o "dl/${f}" "${BASE_URL}/${f}"
  done
else
  rm -f "dl/${BIN_NAME}.zip"
  echo "No binary package at ${BASE_URL}"
fi
curl -sSfL -o KEYS "${KEYS_URL}"
ls -l dl

PKGS=("${SRC_NAME}")
[ "${HAS_BIN}" -eq 1 ] && PKGS+=("${BIN_NAME}")

section "Checksums"
for p in "${PKGS[@]}"; do
  if (cd dl && sha512_check "${p}.zip.sha512" >/dev/null 2>&1); then
    pass "sha512 ${p}.zip"
  else
    fail "sha512 ${p}.zip"
  fi
done

section "Signatures"
export GNUPGHOME="${WORK}/gnupg"
rm -rf "${GNUPGHOME}"
mkdir -m 700 "${GNUPGHOME}"
gpg --batch --quiet --import KEYS >/dev/null 2>&1
for p in "${PKGS[@]}"; do
  if out=$(gpg --batch --status-fd 1 --verify "dl/${p}.zip.asc" "dl/${p}.zip" 2>/dev/null) \
    && grep -q '^\[GNUPG:\] GOODSIG' <<<"${out}"; then
    pass "signature ${p}.zip: $(grep '^\[GNUPG:\] GOODSIG' <<<"${out}" | cut -d' ' -f4-)"
  else
    fail "signature ${p}.zip"
  fi
done

section "Unpack"
rm -rf src bin
mkdir src bin
unzip -q "dl/${SRC_NAME}.zip" -d src
[ "${HAS_BIN}" -eq 1 ] && unzip -q "dl/${BIN_NAME}.zip" -d bin
S="${WORK}/src/${SRC_NAME}"
[ -d "${S}" ] || { fail "source package does not unpack to ${SRC_NAME}/"; exit 1; }
pass "unpacked to ${S}"

section "Source package vs. tag ${VERSION}"
rm -rf tag
git clone -q --depth 1 --branch "${VERSION}" "${REPO_URL}" tag 2>/dev/null \
  || git clone -q --depth 1 --branch "dubbo-${VERSION}" "${REPO_URL}" tag
TAG_SHA=$(git -C tag rev-parse HEAD)
if [ -z "${COMMIT}" ] || [ "${TAG_SHA}" = "${COMMIT}" ]; then
  pass "tag commit ${TAG_SHA}"
else
  fail "tag commit ${TAG_SHA} differs from vote commit ${COMMIT}"
fi
rm -rf tag/.git
# The source assembly (dubbo-distribution/dubbo-apache-release) excludes these on purpose.
DIFF=$(diff -rq tag "${S}" \
  | grep -vE "^Only in tag: (\.mvn|mvnw|mvnw\.cmd|build|\.gitignore)$" \
  | grep -vE "^Only in tag(/[^:]*)?: \.gitignore$" \
  | while IFS= read -r line; do
      if [[ "${line}" =~ ^Only\ in\ ([^:]+):\ (.+)$ ]] \
        && [ -d "${BASH_REMATCH[1]}/${BASH_REMATCH[2]}" ] \
        && [ -z "$(ls -A "${BASH_REMATCH[1]}/${BASH_REMATCH[2]}")" ]; then
        continue
      fi
      echo "${line}"
    done || true)
if [ -z "${DIFF}" ]; then
  pass "source package matches the tag"
else
  fail "source package differs from the tag:"
  echo "${DIFF}"
fi
if grep -q "<version>${VERSION}</version>" "${S}/pom.xml" \
  || grep -q "<revision>${VERSION}</revision>" "${S}/pom.xml"; then
  pass "pom.xml version ${VERSION}"
else
  fail "pom.xml does not declare version ${VERSION}"
fi
EMPTY=$(cd "${S}" && find . -type d -empty)
if [ -z "${EMPTY}" ]; then
  pass "no empty directories"
else
  warn "empty directories in the source package:"
  echo "${EMPTY}"
fi

section "Binary files in the source package"
BINARIES=$(cd "${S}" && find . -type f -print0 | xargs -0 file --mime-encoding | grep -E ': binary$' || true)
if [ -z "${BINARIES}" ]; then
  pass "no binary files"
else
  fail "binary files found:"
  echo "${BINARIES}"
fi

section "ASF license headers"
HEADER_RE='Licensed to the Apache Software Foundation|www\.apache\.org/licenses/LICENSE-2\.0'
EXEMPT=$(sed -nE "s/^ *- '([^*?]+\.[a-z]+)'$/\1/p" "${S}/.licenserc.yaml" 2>/dev/null || true)
MISSING=""
THIRD_PARTY=""
while IFS= read -r -d '' f; do
  f="${f#./}"
  grep -qE "${HEADER_RE}" "${S}/${f}" && continue
  if grep -qxF "${f}" <<<"${EXEMPT}"; then
    THIRD_PARTY+="${f}"$'\n'
  else
    MISSING+="${f}"$'\n'
  fi
done < <(cd "${S}" && find . -type f \
  \( -name '*.java' -o -name '*.kt' -o -name '*.scala' -o -name '*.groovy' -o -name '*.xml' \
     -o -name '*.yml' -o -name '*.yaml' -o -name '*.js' -o -name '*.ts' -o -name '*.py' -o -name '*.go' \
     -o -name 'Dockerfile*' \) \
  -not -path './.github/*' -not -path './codestyle/*' -not -path '*/resources/META-INF/*' \
  -not -path '*/src/test/resources/*' -print0)
if [ -z "${MISSING}" ]; then
  pass "all checked source files have an ASF header or a .licenserc.yaml exemption"
else
  fail "files without an ASF header and not exempt in .licenserc.yaml:"
  printf '%s' "${MISSING}"
fi

section "Third-party files vs. LICENSE"
NOT_IN_LICENSE=""
while IFS= read -r f; do
  [ -n "${f}" ] || continue
  cls=$(basename "${f}" | sed 's/\.[^.]*$//')
  pkg=$(dirname "${f}" | sed -nE 's|.*/src/main/java/||p' | tr '/' '.')
  if grep -qF "${cls}" "${S}/LICENSE" || { [ -n "${pkg}" ] && grep -qF "${pkg}" "${S}/LICENSE"; }; then
    continue
  fi
  NOT_IN_LICENSE+="${f}"$'\n'
done <<<"${THIRD_PARTY}"
if [ -z "${NOT_IN_LICENSE}" ]; then
  pass "every header-exempt third-party file is named in LICENSE"
else
  warn "header-exempt files whose class or package is not named in LICENSE (read the file header and LICENSE):"
  printf '%s' "${NOT_IN_LICENSE}"
fi

section "LICENSE and NOTICE"
for p in "${PKGS[@]}"; do
  d=src
  [ "${p}" = "${BIN_NAME}" ] && d=bin
  for f in LICENSE NOTICE; do
    if [ -s "${d}/${p}/${f}" ]; then pass "${p}/${f} exists"; else fail "${p}/${f} missing"; fi
  done
done
YEAR=$(date +%Y)
if grep -qE "Copyright [0-9-]*${YEAR} The Apache Software Foundation" "${S}/NOTICE"; then
  pass "NOTICE year includes ${YEAR}"
else
  warn "NOTICE year: $(grep -m1 'The Apache Software Foundation' "${S}/NOTICE")"
fi
if [ "${HAS_BIN}" -eq 1 ]; then
  echo "Binary package content:"
  (cd bin && find . -type f | sort)
fi

if [ "${BUILD}" -eq 1 ]; then
  section "Build from source"
  if (cd "${S}" && mvn -B clean package -Pskip-spotless,checkstyle -DskipTests >"${WORK}/build.log" 2>&1); then
    pass "build succeeded (log: ${WORK}/build.log)"
  else
    fail "build failed (log: ${WORK}/build.log)"
  fi
fi

section "Summary"
echo "work directory: ${WORK}"
echo "failures: ${FAILS}, warnings: ${WARNS}"
[ "${FAILS}" -eq 0 ]
