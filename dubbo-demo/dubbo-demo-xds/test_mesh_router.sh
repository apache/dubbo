#!/bin/bash

echo "=== Testing StandardMeshRuleRouter Behavior ==="

# 检查consumer pod的日志，特别关注StandardMeshRuleRouter
echo "Checking consumer logs for StandardMeshRuleRouter behavior..."
kubectl logs deployment/dubbo-demo-xds-consumer -n dubbo-proxyless --tail=200 | grep -E "(StandardMeshRuleRouter|MESH|mesh)" || echo "No mesh-related logs found"

echo ""
echo "=== Checking current invokers ==="
echo "Available invokers:"
kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-provider -o wide

echo ""
echo "=== Checking mesh configuration ==="
echo "Istio VirtualService:"
kubectl get virtualservice -n dubbo-proxyless -o yaml

echo ""
echo "=== Checking DestinationRule ==="
kubectl get destinationrule -n dubbo-proxyless -o yaml

echo ""
echo "=== Testing direct call to verify invokers ==="
echo "Testing call to v1 provider:"
kubectl exec -n dubbo-proxyless deployment/dubbo-demo-xds-consumer -- curl -s http://dubbo-demo-xds-provider-v1:31001/health || echo "v1 not accessible"

echo "Testing call to v2 provider:"
kubectl exec -n dubbo-proxyless deployment/dubbo-demo-xds-consumer -- curl -s http://dubbo-demo-xds-provider-v2:31001/health || echo "v2 not accessible" 