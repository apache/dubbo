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
    print_info "Building Dubbo Consumer project..."

    # Switch to project root directory
    pushd ../../ > /dev/null

    # Build the entire project
    print_info "Executing Maven build..."
    mvn clean install -pl dubbo-demo/dubbo-demo-xds/dubbo-demo-xds-consumer -am -DskipTests -Dspotless.check.skip=true

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
    print_info "Building Consumer Docker image..."

    # Enter consumer directory
    cd dubbo-demo-xds-consumer

    # Find the built JAR file
    JAR_NAME=$(find target -type f -name "dubbo-demo-xds*.jar" | head -1)
    if [ -z "$JAR_NAME" ]; then
        print_error "JAR file not found, build may have failed"
        exit 1
    fi

    JAR_BASENAME=$(basename "$JAR_NAME")
    print_info "Found JAR file: $JAR_BASENAME"

    # Build Docker image
    CONSUMER_IMAGE="dubbo-demo-xds-consumer:latest"
    print_info "Building image: $CONSUMER_IMAGE"

    # Copy the JAR file to current directory so Docker can find it
    cp "$JAR_NAME" .

    docker build \
        --build-arg ARTIFACT="$JAR_BASENAME" \
        -f Dockerfile \
        -t "$CONSUMER_IMAGE" \
        .

    # Clean up copied JAR file
    rm "$JAR_BASENAME"

    if [ $? -eq 0 ]; then
        print_success "Docker image build completed: $CONSUMER_IMAGE"
    else
        print_error "Docker image build failed"
        exit 1
    fi

    # Push to remote repository (optional)
    REMOTE_REPO=""
    REMOTE_TAG="latest"

    print_info "Pushing image to remote repository..."
    docker tag "$CONSUMER_IMAGE" "${REMOTE_REPO}:${REMOTE_TAG}"
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
    print_info "Deploying Consumer to Kubernetes..."

    # Check if namespace exists
    if ! kubectl get namespace dubbo-proxyless &> /dev/null; then
        print_info "Creating dubbo-proxyless namespace..."
        kubectl create namespace dubbo-proxyless
        kubectl label namespace dubbo-proxyless istio-injection=enabled
    fi

    # Deploy Consumer
    print_info "Deploying Consumer..."
    kubectl apply -f consumer-services.yaml

    if [ $? -eq 0 ]; then
        print_success "Consumer deployment configuration applied"
    else
        print_error "Consumer deployment failed"
        exit 1
    fi

    # Restart deployment to ensure latest image is used
    print_info "Restarting Deployment to load latest image..."
    kubectl rollout restart deployment dubbo-demo-xds-consumer -n dubbo-proxyless

    print_success "Deployment command executed"
}

# Check deployment status
check_deployment_status() {
    print_info "Checking deployment status..."

    # Wait for deployment to complete
    print_info "Waiting for Consumer deployment to complete..."
    kubectl rollout status deployment/dubbo-demo-xds-consumer -n dubbo-proxyless --timeout=300s

    # Show Pod status
    print_info "Current Pod status:"
    kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-consumer

    # Show Service status
    print_info "Current Service status:"
    kubectl get svc -n dubbo-proxyless dubbo-demo-xds-consumer

    print_success "Deployment check completed"
}

# Main function
main() {
    echo "=========================================="
    echo "    Dubbo Consumer Deployment Script"
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

    print_success "Consumer deployment completed!"
    echo
    print_info "Monitoring commands:"
    echo "  kubectl get pods -n dubbo-proxyless -w"
    echo "  kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer -f"
    echo "  ./monitor_weighted_routing.sh  # Monitor weighted routing"
}

# Execute main function
main "$@"
