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

# 检查必要的工具
check_prerequisites() {
    print_info "检查必要工具..."
    
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl 未安装或不在PATH中"
        exit 1
    fi
    
    if ! command -v docker &> /dev/null; then
        print_error "docker 未安装或不在PATH中"
        exit 1
    fi
    
    print_success "工具检查完成"
}

# 构建项目
build_project() {
    print_info "构建Dubbo项目..."
    
    # 切换到项目根目录
    pushd ../../ > /dev/null
    
    # 构建整个项目
    print_info "执行Maven构建..."
    mvn clean install -pl dubbo-demo/dubbo-demo-xds/dubbo-demo-xds-provider -am -DskipTests -Dspotless.check.skip=true
    
    if [ $? -eq 0 ]; then
        print_success "项目构建完成"
    else
        print_error "项目构建失败"
        popd > /dev/null
        exit 1
    fi
    
    popd > /dev/null
}

# 构建Docker镜像
build_docker_image() {
    print_info "构建Provider Docker镜像..."
    
    # 进入provider目录
    cd dubbo-demo-xds-provider
    
    # 查找构建的JAR文件
    JAR_NAME=$(find target -type f -name "dubbo-demo-xds*.jar" | head -1)
    if [ -z "$JAR_NAME" ]; then
        print_error "未找到JAR文件，构建可能失败"
        exit 1
    fi
    
    JAR_BASENAME=$(basename "$JAR_NAME")
    print_info "找到JAR文件: $JAR_BASENAME"
    
    # 构建Docker镜像
    PROVIDER_IMAGE="dubbo-demo-xds-provider:latest"
    print_info "构建镜像: $PROVIDER_IMAGE"
    
    docker build \
        --build-arg ARTIFACT="$JAR_NAME" \
        -f Dockerfile \
        -t "$PROVIDER_IMAGE" \
        .
    
    if [ $? -eq 0 ]; then
        print_success "Docker镜像构建完成: $PROVIDER_IMAGE"
    else
        print_error "Docker镜像构建失败"
        exit 1
    fi
    
    # 推送到远程仓库（可选）
    REMOTE_REPO="crpi-ta376u07gp1439ff.cn-beijing.personal.cr.aliyuncs.com/test_xds/test_dubbo_provider"
    REMOTE_TAG="latest"
    
    print_info "推送镜像到远程仓库..."
    docker tag "$PROVIDER_IMAGE" "${REMOTE_REPO}:${REMOTE_TAG}"
    docker push "${REMOTE_REPO}:${REMOTE_TAG}"
    
    if [ $? -eq 0 ]; then
        print_success "镜像推送完成: ${REMOTE_REPO}:${REMOTE_TAG}"
    else
        print_warning "镜像推送失败，将使用本地镜像"
    fi
    
    # 返回到demo目录
    cd ..
}

# 部署到Kubernetes
deploy_to_kubernetes() {
    print_info "部署Provider到Kubernetes..."
    
    # 检查命名空间是否存在
    if ! kubectl get namespace dubbo-proxyless &> /dev/null; then
        print_info "创建dubbo-proxyless命名空间..."
        kubectl create namespace dubbo-proxyless
        kubectl label namespace dubbo-proxyless istio-injection=enabled
    fi
    
    # 部署Provider v1
    print_info "部署Provider v1版本..."
    kubectl apply -f provider-v1.yaml
    
    if [ $? -eq 0 ]; then
        print_success "Provider v1部署配置已应用"
    else
        print_error "Provider v1部署失败"
        exit 1
    fi
    
    # 部署Provider v2
    print_info "部署Provider v2版本..."
    kubectl apply -f provider-v2.yaml
    
    if [ $? -eq 0 ]; then
        print_success "Provider v2部署配置已应用"
    else
        print_error "Provider v2部署失败"
        exit 1
    fi
    
    # 重启deployment以确保使用最新镜像
    print_info "重启Deployment以加载最新镜像..."
    kubectl rollout restart deployment dubbo-demo-xds-provider-v1 -n dubbo-proxyless
    kubectl rollout restart deployment dubbo-demo-xds-provider-v2 -n dubbo-proxyless
    
    print_success "部署命令已执行"
}

# 检查部署状态
check_deployment_status() {
    print_info "检查部署状态..."
    
    # 等待部署完成
    print_info "等待Provider v1部署完成..."
    kubectl rollout status deployment/dubbo-demo-xds-provider-v1 -n dubbo-proxyless --timeout=300s
    
    print_info "等待Provider v2部署完成..."
    kubectl rollout status deployment/dubbo-demo-xds-provider-v2 -n dubbo-proxyless --timeout=300s
    
    # 显示Pod状态
    print_info "当前Pod状态:"
    kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-provider
    
    # 显示Service状态
    print_info "当前Service状态:"
    kubectl get svc -n dubbo-proxyless dubbo-demo-xds-provider
    
    print_success "部署检查完成"
}

# 主函数
main() {
    echo "=========================================="
    echo "    Dubbo Provider 部署脚本"
    echo "=========================================="
    echo
    
    check_prerequisites
    echo
    
    build_project
    echo
    
    build_docker_image
    echo
    
    deploy_to_kubernetes
    echo
    
    check_deployment_status
    echo
    
    print_success "Provider部署完成！"
    echo
    print_info "监控命令:"
    echo "  kubectl get pods -n dubbo-proxyless -w"
    echo "  kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-provider-v1 -f"
    echo "  kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-provider-v2 -f"
}

# 执行主函数
main "$@" 