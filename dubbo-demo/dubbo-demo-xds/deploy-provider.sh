#!/bin/bash

set -e

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored information
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

# Check required tools
check_prerequisites() {
    print_info "Checking required tools..."

    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi

    if ! command -v docker &> /dev/null; then
        print_error "docker is not installed or not in PATH"
        exit 1
    fi

    print_success "Tool check completed"
}

# Build project
build_project() {
    print_info "Building Dubbo project..."

    # Switch to project root directory
    pushd ../../ > /dev/null

    # Build the entire project
    print_info "Executing Maven build..."
    mvn clean install -pl dubbo-demo/dubbo-demo-xds/dubbo-demo-xds-provider -am -DskipTests -Dspotless.check.skip=true

    if [ $? -eq 0 ]; then
        print_success "Project build completed"
    else
        print_error "Project build failed"
        popd > /dev/null
        exit 1
    fi

    popd > /dev/null
}

# Build Docker image
build_docker_image() {
    print_info "Building Provider Docker image..."

    # Enter provider directory
    cd dubbo-demo-xds-provider

    # Find the built JAR file
    JAR_NAME=$(find target -type f -name "dubbo-demo-xds*.jar" | head -1)
    if [ -z "$JAR_NAME" ]; then
        print_error "JAR file not found, build may have failed"
        exit 1
    fi

    JAR_BASENAME=$(basename "$JAR_NAME")
    print_info "Found JAR file: $JAR_BASENAME"

    # Build Docker image
    PROVIDER_IMAGE="dubbo-demo-xds-provider:latest"
    print_info "Building image: $PROVIDER_IMAGE"

    docker build \
        --build-arg ARTIFACT="$JAR_NAME" \
        -f Dockerfile \
        -t "$PROVIDER_IMAGE" \
        .

    if [ $? -eq 0 ]; then
        print_success "Docker image build completed: $PROVIDER_IMAGE"
    else
        print_error "Docker image build failed"
        exit 1
    fi

    # Push to remote repository (optional)
    REMOTE_REPO="xxx"
    REMOTE_TAG="latest"

    print_info "Pushing image to remote repository..."
    docker tag "$PROVIDER_IMAGE" "${REMOTE_REPO}:${REMOTE_TAG}"
    docker push "${REMOTE_REPO}:${REMOTE_TAG}"

    if [ $? -eq 0 ]; then
        print_success "Image push completed: ${REMOTE_REPO}:${REMOTE_TAG}"
    else
        print_warning "Image push failed, will use local image"
    fi

    # Return to demo directory
    cd ..
}

# Deploy to Kubernetes
deploy_to_kubernetes() {
    print_info "Deploying Provider to Kubernetes..."

    # Check if namespace exists
    if ! kubectl get namespace dubbo-proxyless &> /dev/null; then
        print_info "Creating dubbo-proxyless namespace..."
        kubectl create namespace dubbo-proxyless
        kubectl label namespace dubbo-proxyless istio-injection=disable
    fi

    # Deploy Provider v1
    print_info "Deploying Provider v1 version..."
    kubectl apply -f provider-v1.yaml

    if [ $? -eq 0 ]; then
        print_success "Provider v1 deployment configuration applied"
    else
        print_error "Provider v1 deployment failed"
        exit 1
    fi

    # Deploy Provider v2
    print_info "Deploying Provider v2 version..."
    kubectl apply -f provider-v2.yaml

    if [ $? -eq 0 ]; then
        print_success "Provider v2 deployment configuration applied"
    else
        print_error "Provider v2 deployment failed"
        exit 1
    fi

    # Restart deployment to ensure latest image is used
    print_info "Restarting Deployment to load latest image..."
    kubectl rollout restart deployment dubbo-demo-xds-provider-v1 -n dubbo-proxyless
    kubectl rollout restart deployment dubbo-demo-xds-provider-v2 -n dubbo-proxyless

    print_success "Deployment command executed"
}

# Check deployment status
check_deployment_status() {
    print_info "Checking deployment status..."

    # Wait for deployment to complete
    print_info "Waiting for Provider v1 deployment to complete..."
    kubectl rollout status deployment/dubbo-demo-xds-provider-v1 -n dubbo-proxyless --timeout=300s

    print_info "Waiting for Provider v2 deployment to complete..."
    kubectl rollout status deployment/dubbo-demo-xds-provider-v2 -n dubbo-proxyless --timeout=300s

    # Show Pod status
    print_info "Current Pod status:"
    kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-provider

    # Show Service status
    print_info "Current Service status:"
    kubectl get svc -n dubbo-proxyless dubbo-demo-xds-provider

    print_success "Deployment check completed"
}

# Main function
main() {
    echo "=========================================="
    echo "    Dubbo Provider Deployment Script"
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

    print_success "Provider deployment completed!"
    echo
    print_info "Monitoring commands:"
    echo "  kubectl get pods -n dubbo-proxyless -w"
    echo "  kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-provider-v1 -f"
    echo "  kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-provider-v2 -f"
}

# Execute main function
main "$@"
