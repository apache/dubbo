#!/bin/bash

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的信息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 清理现有部署
cleanup_existing_deployments() {
    print_info "清理现有的Dubbo部署..."
    
    # 删除现有的deployments
    print_info "删除现有的deployments..."
    kubectl delete deployment dubbo-demo-xds-provider -n dubbo-proxyless --ignore-not-found=true
    kubectl delete deployment dubbo-demo-xds-provider-v1 -n dubbo-proxyless --ignore-not-found=true
    kubectl delete deployment dubbo-demo-xds-provider-v2 -n dubbo-proxyless --ignore-not-found=true
    kubectl delete deployment dubbo-demo-xds-consumer -n dubbo-proxyless --ignore-not-found=true
    
    # 删除现有的services
    print_info "删除现有的services..."
    kubectl delete service dubbo-demo-xds-provider -n dubbo-proxyless --ignore-not-found=true
    kubectl delete service dubbo-demo-xds-provider-v1 -n dubbo-proxyless --ignore-not-found=true
    kubectl delete service dubbo-demo-xds-provider-v2 -n dubbo-proxyless --ignore-not-found=true
    kubectl delete service dubbo-demo-xds-consumer -n dubbo-proxyless --ignore-not-found=true
    
    # 删除现有的VirtualService和DestinationRule
    print_info "删除现有的Istio配置..."
    kubectl delete virtualservice dubbo-demo-xds-provider -n dubbo-proxyless --ignore-not-found=true
    kubectl delete destinationrule dubbo-demo-xds-provider -n dubbo-proxyless --ignore-not-found=true
    
    # 等待Pod完全删除
    print_info "等待Pod完全删除..."
    kubectl wait --for=delete pods -l app=dubbo-demo-xds-provider -n dubbo-proxyless --timeout=120s || true
    kubectl wait --for=delete pods -l app=dubbo-demo-xds-consumer -n dubbo-proxyless --timeout=120s || true
    
    print_success "清理完成"
}

# 检查清理结果
check_cleanup_status() {
    print_info "检查清理状态..."
    
    # 检查是否还有相关的Pod
    REMAINING_PODS=$(kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-provider -o name 2>/dev/null | wc -l)
    REMAINING_CONSUMER_PODS=$(kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-consumer -o name 2>/dev/null | wc -l)
    
    if [ "$REMAINING_PODS" -eq 0 ] && [ "$REMAINING_CONSUMER_PODS" -eq 0 ]; then
        print_success "所有相关Pod已删除"
    else
        print_warning "仍有 $REMAINING_PODS 个Provider Pod和 $REMAINING_CONSUMER_PODS 个Consumer Pod存在"
        print_info "当前Pod状态:"
        kubectl get pods -n dubbo-proxyless -o wide
    fi
}

# 主函数
main() {
    echo "=========================================="
    echo "    Dubbo 部署清理脚本"
    echo "=========================================="
    echo
    
    print_info "当前Pod状态:"
    kubectl get pods -n dubbo-proxyless -o wide
    echo
    
    cleanup_existing_deployments
    echo
    
    check_cleanup_status
    echo
    
    print_success "清理完成！现在可以重新部署了。"
    echo
    print_info "重新部署命令:"
    echo "  ./deploy-provider.sh   # 部署Provider"
    echo "  ./deploy-consumer.sh   # 部署Consumer"
    echo "  ./deploy-all.sh        # 一键部署所有组件"
}

# 执行主函数
main "$@" 