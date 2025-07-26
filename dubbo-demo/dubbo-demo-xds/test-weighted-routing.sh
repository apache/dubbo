#!/bin/bash

# Test weighted routing by making multiple HTTP calls
# Expected distribution: v1 (20%) vs v2 (80%)

NAMESPACE="dubbo-proxyless"
SERVICE="dubbo-demo-xds-consumer"
PORT="8080"
ENDPOINT="/hello"
NUM_CALLS=10

echo "Testing weighted routing with $NUM_CALLS calls..."
echo "Expected distribution: v1 (~20%) vs v2 (~80%)"
echo "=========================================="

# Get the service IP
SERVICE_IP=$(kubectl get svc -n $NAMESPACE $SERVICE -o jsonpath='{.spec.clusterIP}')
echo "Service IP: $SERVICE_IP"

# Counters
v1_count=0
v2_count=0
failed_count=0
unknown_count=0

# Make the calls
for i in $(seq 1 $NUM_CALLS); do
    echo -n "Call $i: "
    
    # Make HTTP request and capture response
    response=$(kubectl run test-client-$i --rm -i --restart=Never --image=curlimages/curl:latest -- \
        curl -s "http://$SERVICE_IP:$PORT$ENDPOINT" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$response" ]; then
        echo "Response: $response"
        
        # Try to extract version from response
        if echo "$response" | grep -q "v1"; then
            v1_count=$((v1_count + 1))
        elif echo "$response" | grep -q "v2"; then
            v2_count=$((v2_count + 1))
        else
            unknown_count=$((unknown_count + 1))
        fi
    else
        echo "FAILED"
        failed_count=$((failed_count + 1))
    fi
    
    sleep 1
done

echo ""
echo "=========================================="
echo "RESULTS SUMMARY:"
echo "=========================================="

total_success=$((v1_count + v2_count + unknown_count))

echo "v1: $v1_count calls"
echo "v2: $v2_count calls"
echo "unknown: $unknown_count calls"
echo "failed: $failed_count calls"
echo ""
echo "Total successful calls: $total_success/$NUM_CALLS"

# Expected vs actual analysis
if [ $total_success -gt 0 ]; then
    echo ""
    echo "Distribution Analysis:"
    echo "Expected: v1=20%, v2=80%"
    
    if [ $total_success -gt 0 ]; then
        v1_percentage=$(echo "scale=1; $v1_count * 100 / $total_success" | bc -l 2>/dev/null || echo "N/A")
        v2_percentage=$(echo "scale=1; $v2_count * 100 / $total_success" | bc -l 2>/dev/null || echo "N/A")
        echo "Actual:   v1=${v1_percentage}%, v2=${v2_percentage}%"
    fi
    
    # Check if distribution is reasonable
    if [ "$v1_count" -gt 0 ] && [ "$v2_count" -gt 0 ]; then
        echo "✓ Both versions are receiving traffic (weighted routing working)"
    elif [ "$v1_count" -gt 0 ] || [ "$v2_count" -gt 0 ]; then
        echo "⚠ Only one version is receiving traffic (may indicate issue or small sample)"
    else
        echo "✗ No version-specific traffic detected"
    fi
fi 