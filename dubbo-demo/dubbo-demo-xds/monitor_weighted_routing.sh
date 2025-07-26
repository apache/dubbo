#!/bin/bash

echo "监控加权路由分布..."
echo "预期分布: v1 ~20%, v2 ~80%"
echo "================================"

POD_NAME=$(kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-consumer -o jsonpath='{.items[0].metadata.name}')

while true; do
    echo -n "$(date '+%H:%M:%S') - "
    
    # 获取最新的统计数据
    stats=$(kubectl logs -n dubbo-proxyless $POD_NAME 2>/dev/null | grep "Selected weighted cluster" | grep -o "v[12]" | sort | uniq -c)
    
    if [ -n "$stats" ]; then
        v1_count=$(echo "$stats" | grep "v1" | awk '{print $1}' || echo "0")
        v2_count=$(echo "$stats" | grep "v2" | awk '{print $1}' || echo "0")
        
        [ -z "$v1_count" ] && v1_count=0
        [ -z "$v2_count" ] && v2_count=0
        
        total=$((v1_count + v2_count))
        
        if [ $total -gt 0 ]; then
            v1_percent=$(( v1_count * 100 / total ))
            v2_percent=$(( v2_count * 100 / total ))
            echo "v1: ${v1_count}次(${v1_percent}%) | v2: ${v2_count}次(${v2_percent}%) | 总计: ${total}次"
        else
            echo "暂无数据"
        fi
    else
        echo "暂无数据"
    fi
    
    sleep 5
done 