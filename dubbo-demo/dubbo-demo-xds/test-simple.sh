#!/bin/bash

# 简单的加权路由测试
NAMESPACE="dubbo-proxyless"
SERVICE="dubbo-demo-xds-consumer"
PORT="8080"
ENDPOINT="/hello"
NUM_CALLS=10

echo "开始测试加权路由（$NUM_CALLS 次调用）..."
echo "预期分布：v1 (~20%) vs v2 (~80%)"
echo "========================================"

# 启动端口转发
echo "启动端口转发..."
kubectl port-forward -n $NAMESPACE svc/$SERVICE 8080:8080 &
PORT_FORWARD_PID=$!

# 等待端口转发就绪
sleep 3

# 计数器
v1_count=0
v2_count=0
failed_count=0
unknown_count=0

# 进行测试调用
for i in $(seq 1 $NUM_CALLS); do
    echo -n "调用 $i: "
    
    # 使用curl直接访问本地转发的端口
    response=$(curl -s --connect-timeout 5 "http://localhost:8080$ENDPOINT" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$response" ]; then
        echo "响应: $response"
        
        # 检查版本
        if echo "$response" | grep -q "v1"; then
            v1_count=$((v1_count + 1))
        elif echo "$response" | grep -q "v2"; then
            v2_count=$((v2_count + 1))
        else
            unknown_count=$((unknown_count + 1))
        fi
    else
        echo "失败"
        failed_count=$((failed_count + 1))
    fi
    
    sleep 1
done

# 停止端口转发
kill $PORT_FORWARD_PID 2>/dev/null

echo ""
echo "========================================"
echo "测试结果汇总："
echo "========================================"

total_success=$((v1_count + v2_count + unknown_count))

echo "v1版本: $v1_count 次调用"
echo "v2版本: $v2_count 次调用"
echo "未知版本: $unknown_count 次调用"
echo "失败: $failed_count 次调用"
echo ""
echo "成功调用总数: $total_success/$NUM_CALLS"

# 分析结果
if [ $total_success -gt 0 ]; then
    echo ""
    echo "分布分析："
    echo "预期: v1=20%, v2=80%"
    
    if [ $total_success -gt 0 ]; then
        v1_percentage=$(echo "scale=1; $v1_count * 100 / $total_success" | bc -l 2>/dev/null || echo "N/A")
        v2_percentage=$(echo "scale=1; $v2_count * 100 / $total_success" | bc -l 2>/dev/null || echo "N/A")
        echo "实际: v1=${v1_percentage}%, v2=${v2_percentage}%"
    fi
    
    # 检查分布是否合理
    if [ "$v1_count" -gt 0 ] && [ "$v2_count" -gt 0 ]; then
        echo "✓ 两个版本都接收到了流量（加权路由正常工作）"
    elif [ "$v1_count" -gt 0 ] || [ "$v2_count" -gt 0 ]; then
        echo "⚠ 只有一个版本接收到流量（可能存在问题或样本太小）"
    else
        echo "✗ 没有检测到版本特定的流量"
    fi
fi 