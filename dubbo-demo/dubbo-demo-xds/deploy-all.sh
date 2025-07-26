#!/bin/bash

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 打印带颜色的信息
print_title() {
    echo -e "${CYAN}$1${NC}"
}

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

# 显示脚本用法
show_usage() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  --skip-provider    跳过Provider部署"
    echo "  --skip-consumer    跳过Consumer部署"
    echo "  --skip-routing     跳过流量路由配置"
    echo "  --help             显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                 # 部署所有组件"
    echo "  $0 --skip-provider # 只部署Consumer和流量规则"
    echo "  $0 --skip-routing  # 只部署Provider和Consumer"
}

# 解析命令行参数
SKIP_PROVIDER=false
SKIP_CONSUMER=false
SKIP_ROUTING=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-provider)
            SKIP_PROVIDER=true
            shift
            ;;
        --skip-consumer)
            SKIP_CONSUMER=true
            shift
            ;;
        --skip-routing)
            SKIP_ROUTING=true
            shift
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            print_error "未知选项: $1"
            show_usage
            exit 1
            ;;
    esac
done

# 检查必要工具
check_prerequisites() {
    print_info "检查部署环境..."
    
    local missing_tools=()
    
    if ! command -v kubectl &> /dev/null; then
        missing_tools+=("kubectl")
    fi
    
    if ! command -v docker &> /dev/null; then
        missing_tools+=("docker")
    fi
    
    if ! command -v mvn &> /dev/null; then
        missing_tools+=("maven")
    fi
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        print_error "缺少必要工具: ${missing_tools[*]}"
        print_info "请先安装这些工具后再运行脚本"
        exit 1
    fi
    
    # 检查Kubernetes连接
    if ! kubectl cluster-info &> /dev/null; then
        print_error "无法连接到Kubernetes集群"
        print_info "请确保kubectl已正确配置并可以访问集群"
        exit 1
    fi
    
    # 检查Istio
    if ! kubectl get namespace istio-system &> /dev/null; then
        print_warning "未检测到Istio，请确保已安装Istio"
    fi
    
    print_success "环境检查通过"
}

# 准备命名空间
prepare_namespace() {
    print_info "准备Kubernetes命名空间..."
    
    if ! kubectl get namespace dubbo-proxyless &> /dev/null; then
        print_info "创建dubbo-proxyless命名空间..."
        kubectl create namespace dubbo-proxyless
    fi
    
    # 启用Istio注入
    kubectl label namespace dubbo-proxyless istio-injection=enabled --overwrite
    
    print_success "命名空间准备完成"
}

# 部署Provider
deploy_provider() {
    if [ "$SKIP_PROVIDER" = true ]; then
        print_warning "跳过Provider部署"
        return 0
    fi
    
    print_title "开始部署Provider..."
    
    if [ -f "./deploy-provider.sh" ]; then
        chmod +x ./deploy-provider.sh
        ./deploy-provider.sh
    else
        print_error "未找到deploy-provider.sh脚本"
        exit 1
    fi
    
    print_success "Provider部署完成"
}

# 部署Consumer
deploy_consumer() {
    if [ "$SKIP_CONSUMER" = true ]; then
        print_warning "跳过Consumer部署"
        return 0
    fi
    
    print_title "开始部署Consumer..."
    
    if [ -f "./deploy-consumer.sh" ]; then
        chmod +x ./deploy-consumer.sh
        ./deploy-consumer.sh
    else
        print_error "未找到deploy-consumer.sh脚本"
        exit 1
    fi
    
    print_success "Consumer部署完成"
}

# 配置流量路由
configure_traffic_routing() {
    if [ "$SKIP_ROUTING" = true ]; then
        print_warning "跳过流量路由配置"
        return 0
    fi
    
    print_title "配置流量路由规则..."
    
    if [ -f "./traffic-rules.yaml" ]; then
        print_info "应用加权路由配置..."
        kubectl apply -f traffic-rules.yaml
        
        if [ $? -eq 0 ]; then
            print_success "流量路由规则配置完成"
        else
            print_error "流量路由规则配置失败"
            exit 1
        fi
    else
        print_error "未找到traffic-rules.yaml文件"
        exit 1
    fi
}

# 验证部署
verify_deployment() {
    print_title "验证部署状态..."
    
    # 检查所有Pod状态
    print_info "检查Pod状态..."
    kubectl get pods -n dubbo-proxyless
    
    # 检查Service状态
    print_info "检查Service状态..."
    kubectl get svc -n dubbo-proxyless
    
    # 检查Istio配置
    if ! [ "$SKIP_ROUTING" = true ]; then
        print_info "检查Istio配置..."
        kubectl get virtualservice -n dubbo-proxyless
        kubectl get destinationrule -n dubbo-proxyless
    fi
    
    print_success "部署验证完成"
}

# 显示后续操作指南
show_next_steps() {
    print_title "部署完成！"
    echo
    print_info "后续操作指南:"
    echo
    echo "1. 监控Pod状态:"
    echo "   kubectl get pods -n dubbo-proxyless -w"
    echo
    echo "2. 查看Consumer日志:"
    echo "   kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer -f"
    echo
    echo "3. 查看Provider日志:"
    echo "   kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-provider-v1 -f"
    echo "   kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-provider-v2 -f"
    echo
    
    if ! [ "$SKIP_ROUTING" = true ]; then
        echo "4. 监控加权路由分布:"
        echo "   ./monitor_weighted_routing.sh"
        echo
        echo "5. 验证路由效果:"
        echo "   # 预期: v1 ~20%, v2 ~80%"
        echo "   kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer | grep 'Selected weighted cluster' | grep -o 'v[12]' | sort | uniq -c"
        echo
    fi
    
    echo "6. 调试命令:"
    echo "   # 端口转发用于调试"
    echo "   kubectl port-forward -n dubbo-proxyless deployment/dubbo-demo-xds-consumer 31000:31000"
    echo
    echo "7. 清理部署:"
    echo "   kubectl delete namespace dubbo-proxyless"
}

# 主函数
main() {
    echo "================================================"
    echo "    Dubbo Proxyless 一键部署脚本"
    echo "================================================"
    echo
    
    print_info "部署配置:"
    echo "  Provider: $([ "$SKIP_PROVIDER" = true ] && echo "跳过" || echo "部署")"
    echo "  Consumer: $([ "$SKIP_CONSUMER" = true ] && echo "跳过" || echo "部署")"
    echo "  路由规则: $([ "$SKIP_ROUTING" = true ] && echo "跳过" || echo "配置")"
    echo
    
    # 执行部署流程
    check_prerequisites
    echo
    
    prepare_namespace
    echo
    
    deploy_provider
    echo
    
    deploy_consumer
    echo
    
    configure_traffic_routing
    echo
    
    verify_deployment
    echo
    
    show_next_steps
}

# 执行主函数
main "$@" 