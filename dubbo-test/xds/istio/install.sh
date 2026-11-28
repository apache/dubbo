#!/usr/bin/env bash
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

set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: bash install.sh <kubeconfig> <official-istio-1.31.1-directory>" >&2
  exit 2
fi

test_kubeconfig=$1
istio_release_dir=$2
test_manifest_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
chart_dir="$istio_release_dir/manifests/charts"
wait_args=(--wait)
if [[ $(helm version --short) == v4.* ]]; then
  wait_args=(--wait=legacy)
fi

helm upgrade --install istio-base "$chart_dir/base" \
  --kubeconfig "$test_kubeconfig" --namespace istio-system \
  --create-namespace "${wait_args[@]}" --timeout 5m
helm upgrade --install istiod "$chart_dir/istio-control/istio-discovery" \
  --kubeconfig "$test_kubeconfig" --namespace istio-system \
  --values "$test_manifest_dir/istiod-values.yaml" \
  "${wait_args[@]}" --timeout 10m
kubectl --kubeconfig "$test_kubeconfig" rollout status \
  deployment/istiod --namespace istio-system --timeout=5m
kubectl --kubeconfig "$test_kubeconfig" apply \
  -f "$test_manifest_dir/grpc-echo.yaml"
kubectl --kubeconfig "$test_kubeconfig" rollout status \
  deployment/echo-v1 --namespace dubbo-xds-test --timeout=5m
kubectl --kubeconfig "$test_kubeconfig" rollout status \
  deployment/echo-v2 --namespace dubbo-xds-test --timeout=5m
kubectl --kubeconfig "$test_kubeconfig" rollout status \
  deployment/echo-client --namespace dubbo-xds-test --timeout=5m
python3 "$test_manifest_dir/verify-bootstrap.py" --kubeconfig "$test_kubeconfig"
