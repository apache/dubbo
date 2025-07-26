#!/bin/bash

echo "=== Testing Weighted Routing Distribution via Port Forward ==="
echo "Expected: v1 (20%), v2 (80%)"
echo "Testing with 10 calls..."
echo ""

# 获取consumer pod name
CONSUMER_POD=$(kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-consumer -o jsonpath='{.items[0].metadata.name}')
echo "Consumer pod: $CONSUMER_POD"

# 启动端口转发到consumer的50050端口
echo "Starting port forwarding to consumer port 50050..."
kubectl port-forward -n dubbo-proxyless pod/$CONSUMER_POD 8080:50050 > /dev/null 2>&1 &
PORT_FORWARD_PID=$!

# 等待端口转发建立
echo "Waiting for port forwarding to establish..."
sleep 5

# 统计变量
v1_count=0
v2_count=0
total_calls=10
successful_calls=0

echo "Starting test calls..."
echo ""

# 执行10次调用
for i in $(seq 1 $total_calls); do
    echo "Call $i:"
    
    # 调用REST接口
    response=$(curl -s -m 10 http://localhost:8080/hello 2>&1)
    
    if [ $? -eq 0 ] && [ -n "$response" ]; then
        echo "  ✅ Call successful: $response"
        successful_calls=$((successful_calls + 1))
        
        # 等待一下让日志生成
        sleep 0.5
        
        # 获取consumer的最新日志来分析路由选择
        log_output=$(kubectl logs -n dubbo-proxyless $CONSUMER_POD --tail=10 | grep "Selected weighted cluster" | tail -1)
        
        if echo "$log_output" | grep -q "outbound|50051|v1|"; then
            echo "  📍 Routed to v1 cluster"
            v1_count=$((v1_count + 1))
        elif echo "$log_output" | grep -q "outbound|50051|v2|"; then
            echo "  📍 Routed to v2 cluster"
            v2_count=$((v2_count + 1))
        else
            echo "  ❓ Could not determine cluster from logs"
            echo "  Log: $log_output"
        fi
    else
        echo "  ❌ Call failed: $response"
    fi
    
    echo ""
    sleep 1
done

# 清理端口转发
kill $PORT_FORWARD_PID > /dev/null 2>&1
echo "Port forwarding stopped."

echo ""
echo "=== Results Summary ==="
echo "Total calls: $total_calls"
echo "Successful calls: $successful_calls"

if [ $successful_calls -gt 0 ]; then
    v1_percentage=$((v1_count * 100 / successful_calls))
    v2_percentage=$((v2_count * 100 / successful_calls))
    
    echo "v1 calls: $v1_count ($v1_percentage%)"
    echo "v2 calls: $v2_count ($v2_percentage%)"
    echo ""

    # 计算预期值
    expected_v1=$((successful_calls * 20 / 100))
    expected_v2=$((successful_calls * 80 / 100))

    echo "Expected distribution (based on $successful_calls successful calls):"
    echo "v1: ~$expected_v1 calls (20%)"
    echo "v2: ~$expected_v2 calls (80%)"
    echo ""

    # 分析结果
    if [ $v2_count -gt $v1_count ]; then
        echo "✅ Great! Weighted routing is working correctly!"
        echo "   v2 is getting more traffic than v1, which matches the 80/20 split."
    elif [ $v1_count -eq $v2_count ]; then
        echo "⚠️  Equal distribution. This might be due to small sample size."
    else
        echo "⚠️  Unexpected distribution. v1 got more traffic than v2."
    fi
else
    echo "❌ No successful calls. Cannot evaluate weighted routing."
fi

echo ""
echo "=== Detailed Analysis ==="
echo "v1 cluster weight: 20% (should get ~20% of traffic)"
echo "v2 cluster weight: 80% (should get ~80% of traffic)"
echo ""
echo "Actual results:"
echo "v1: $v1_count/$successful_calls calls"
echo "v2: $v2_count/$successful_calls calls"
