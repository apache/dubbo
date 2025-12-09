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

# Clean up existing deployments
cleanup_existing_deployments() {
    print_info "Cleaning up existing Dubbo deployments..."
    
    # Delete existing deployments
    print_info "Deleting existing deployments..."
    kubectl delete deployment dubbo-demo-xds-provider -n dubbo-proxyless --ignore-not-found=true
    kubectl delete deployment dubbo-demo-xds-provider-v1 -n dubbo-proxyless --ignore-not-found=true
    kubectl delete deployment dubbo-demo-xds-provider-v2 -n dubbo-proxyless --ignore-not-found=true
    kubectl delete deployment dubbo-demo-xds-consumer -n dubbo-proxyless --ignore-not-found=true
    
    # Delete existing services
    print_info "Deleting existing services..."
    kubectl delete service dubbo-demo-xds-provider -n dubbo-proxyless --ignore-not-found=true
    kubectl delete service dubbo-demo-xds-provider-v1 -n dubbo-proxyless --ignore-not-found=true
    kubectl delete service dubbo-demo-xds-provider-v2 -n dubbo-proxyless --ignore-not-found=true
    kubectl delete service dubbo-demo-xds-consumer -n dubbo-proxyless --ignore-not-found=true
    
    # Delete existing VirtualService and DestinationRule
    print_info "Deleting existing Istio configurations..."
    kubectl delete virtualservice dubbo-demo-xds-provider -n dubbo-proxyless --ignore-not-found=true
    kubectl delete destinationrule dubbo-demo-xds-provider -n dubbo-proxyless --ignore-not-found=true
    
    # Wait for Pods to be completely deleted
    print_info "Waiting for Pods to be completely deleted..."
    kubectl wait --for=delete pods -l app=dubbo-demo-xds-provider -n dubbo-proxyless --timeout=120s || true
    kubectl wait --for=delete pods -l app=dubbo-demo-xds-consumer -n dubbo-proxyless --timeout=120s || true
    
    print_success "Cleanup completed"
}

# Check cleanup results
check_cleanup_status() {
    print_info "Checking cleanup status..."
    
    # Check if there are still related Pods
    REMAINING_PODS=$(kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-provider -o name 2>/dev/null | wc -l)
    REMAINING_CONSUMER_PODS=$(kubectl get pods -n dubbo-proxyless -l app=dubbo-demo-xds-consumer -o name 2>/dev/null | wc -l)
    
    if [ "$REMAINING_PODS" -eq 0 ] && [ "$REMAINING_CONSUMER_PODS" -eq 0 ]; then
        print_success "All related Pods have been deleted"
    else
        print_warning "There are still $REMAINING_PODS Provider Pods and $REMAINING_CONSUMER_PODS Consumer Pods remaining"
        print_info "Current Pod status:"
        kubectl get pods -n dubbo-proxyless -o wide
    fi
}

# Main function
main() {
    echo "=========================================="
    echo "    Dubbo Deployment Cleanup Script"
    echo "=========================================="
    echo
    
    print_info "Current Pod status:"
    kubectl get pods -n dubbo-proxyless -o wide
    echo
    
    cleanup_existing_deployments
    echo
    
    check_cleanup_status
    echo
    
    print_success "Cleanup completed! You can now redeploy."
    echo
    print_info "Redeployment commands:"
    echo "  ./deploy-provider.sh   # Deploy Provider"
    echo "  ./deploy-consumer.sh   # Deploy Consumer"
    echo "  ./deploy-all.sh        # Deploy all components with one click"
}

# Execute main function
main "$@"