#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

echo "=========================================="
echo "    Provider 配置验证脚本"
echo "=========================================="
echo

# 检查Pod是否运行
print_info "检查Provider Pod状态..."
kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-provider

echo
print_info "检查Provider v1环境变量..."
V1_POD=$(kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-provider,version=v1 -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)

if [ -n "$V1_POD" ]; then
    print_success "找到Provider v1 Pod: $V1_POD"
    
    echo
    print_info "检查POD_NAME环境变量..."
    kubectl exec -n dubbo-proxyless "$V1_POD" -- printenv POD_NAME || print_error "无法获取POD_NAME"
    
    echo
    print_info "检查JAVA_TOOL_OPTIONS..."
    kubectl exec -n dubbo-proxyless "$V1_POD" -- printenv JAVA_TOOL_OPTIONS || print_error "无法获取JAVA_TOOL_OPTIONS"
    
    echo
    print_info "检查Java系统属性service.version..."
    kubectl exec -n dubbo-proxyless "$V1_POD" -- sh -c 'jps -v | grep dubbo-demo' || print_warning "无法通过jps查看系统属性"
    
    echo
    print_info "检查Java进程信息..."
    kubectl exec -n dubbo-proxyless "$V1_POD" -- ps aux | grep java || print_warning "无法查看Java进程"
    
else
    print_error "未找到Provider v1 Pod"
fi

echo
print_info "检查Provider v2环境变量..."
V2_POD=$(kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-provider,version=v2 -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)

if [ -n "$V2_POD" ]; then
    print_success "找到Provider v2 Pod: $V2_POD"
    
    echo
    print_info "检查POD_NAME环境变量..."
    kubectl exec -n dubbo-proxyless "$V2_POD" -- printenv POD_NAME || print_error "无法获取POD_NAME"
    
    echo
    print_info "检查JAVA_TOOL_OPTIONS..."
    kubectl exec -n dubbo-proxyless "$V2_POD" -- printenv JAVA_TOOL_OPTIONS || print_error "无法获取JAVA_TOOL_OPTIONS"
    
else
    print_error "未找到Provider v2 Pod"
fi

echo
print_info "检查最近的Provider日志..."
if [ -n "$V1_POD" ]; then
    echo "=== Provider v1 日志 ==="
    kubectl logs -n dubbo-proxyless "$V1_POD" --tail=10 | grep -E "(version|pod|Hello)" || print_warning "未找到相关日志"
fi

if [ -n "$V2_POD" ]; then
    echo "=== Provider v2 日志 ==="
    kubectl logs -n dubbo-proxyless "$V2_POD" --tail=10 | grep -E "(version|pod|Hello)" || print_warning "未找到相关日志"
fi

echo
print_success "配置检查完成" 