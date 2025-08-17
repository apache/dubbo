# Dubbo XDS (Proxyless) Demo

This demo showcases how to use Apache Dubbo in **Proxyless mode** with Istio service mesh. In proxyless mode, Dubbo applications directly communicate with the Istio control plane (istiod) to receive xDS configuration, eliminating the need for Envoy sidecars while still benefiting from service mesh capabilities like traffic management, security, and observability.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Environment Setup](#environment-setup)
- [Quick Start](#quick-start)
- [Configuration Details](#configuration-details)
- [Traffic Management Examples](#traffic-management-examples)
- [Local Development](#local-development)
- [Debugging](#debugging)
- [Troubleshooting](#troubleshooting)

## Overview

### What is Dubbo Proxyless Mode?

Dubbo Proxyless mode allows Dubbo applications to:
- **Direct xDS Integration**: Connect directly to Istio's control plane without Envoy sidecars
- **Native Service Mesh**: Leverage Istio's traffic management, security policies, and observability
- **Reduced Resource Overhead**: Eliminate sidecar proxy resource consumption
- **Simplified Architecture**: Fewer moving parts in your service mesh deployment

### Architecture

```
┌─────────────────┐    xDS Protocol    ┌─────────────────┐
│   Dubbo App     │◄──────────────────►│   Istio Control │
│  (Proxyless)    │                    │     Plane       │
└─────────────────┘                    └─────────────────┘
```

## Prerequisites

### Required Software

1. **Kubernetes Cluster** (v1.20+)
   - Kubernetes Cluster or a local Kubernetes cluster (e.g., Minikube, Kind)

2. **Istio**
   - Istio control plane installed and configured

3. **kubectl** - Kubernetes command-line tool

4. **Docker** - For building container images

## Environment Setup

### Step 1: Install Kubernetes

#### Option B: Kind (Lightweight alternative)

```bash
# Install Kind
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind

# Create cluster with provided config
kind create cluster --config=kind-config.yaml
```

### Step 2: Install Istio

#### Quick Installation

```bash
# Download Istio
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH=$PWD/bin:$PATH

# Install Istio with demo profile
istioctl install --set values.defaultRevision=default -y

# Enable automatic sidecar injection for default namespace (optional)
kubectl label namespace default istio-injection=enabled
```

#### Verify Istio Installation

```bash
# Check Istio system pods
kubectl get pods -n istio-system

# Expected output should show istiod and other Istio components running
```

### Step 3: Create Namespace

```bash
# Create dedicated namespace for the demo
kubectl create namespace dubbo-proxyless

# Label namespace for Istio (but disable sidecar injection for proxyless mode)
kubectl label namespace dubbo-proxyless istio-injection=disabled
```

## Quick Start

### Deploy the Demo Application

1. **Deploy Provider and Consumer Services**:

```bash
# Deploy provider service
kubectl apply -f services.yaml

# Deploy consumer service
kubectl apply -f consumer-services.yaml
```

2. **Verify Deployment**:

```bash
# Check pod status
kubectl get pods -n dubbo-proxyless

# Check services
kubectl get svc -n dubbo-proxyless

# Expected output:
# NAME                     TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)
# dubbo-demo-xds-provider  ClusterIP   10.96.xxx.xxx   <none>        50051/TCP,31001/TCP
# dubbo-demo-xds-consumer  ClusterIP   10.96.xxx.xxx   <none>        50050/TCP,31000/TCP,8080/TCP
```

3. **Test the Application**:

```bash
# Port forward to access consumer service
kubectl port-forward -n dubbo-proxyless svc/dubbo-demo-xds-consumer 8080:8080

# In another terminal, test the service
curl http://localhost:8080/hello?name=World
# Expected response: Hello World from provider
```

## Configuration Details

### Bootstrap Configuration

The key to Dubbo proxyless mode is the **bootstrap configuration** that tells Dubbo how to connect to the Istio control plane:

```json
{
  "xds_servers": [
    {
      "server_uri": "istiod.istio-system.svc:15010",
      "channel_creds": [
        {
          "type": "insecure"
        }
      ],
      "server_features": [
        "xds_v3"
      ]
    }
  ],
  "node": {
    "id": "sidecar~${INSTANCE_IP}~${POD_NAME}~${SERVICE_NAMESPACE}",
    "cluster": "dubbo-demo-xds-consumer",
    "metadata": {
      "CLUSTER_ID": "dubbo-demo-xds-consumer",
      "NAMESPACE": "dubbo-proxyless",
      "SERVICE_NAME": "dubbo-demo-xds-consumer",
      "SERVICE_VERSION": "v1",
      "SERVICE_NAMESPACE": "dubbo-proxyless"
    }
  }
}
```

### Key Configuration Elements

1. **xds_servers**: Defines the Istio control plane endpoint
   - `server_uri`: Istio discovery service address (istiod)
   - `channel_creds`: Authentication method (insecure for demo)
   - `server_features`: xDS protocol version

2. **node**: Identifies the application instance to Istio
   - `id`: Unique node identifier following Istio format
   - `cluster`: Service cluster name
   - `metadata`: Additional service metadata for Istio

### Environment Variables

The following environment variables are crucial for proxyless mode:

```yaml
env:
  - name: GRPC_XDS_BOOTSTRAP
    value: "/app/bootstrap.json"  # Path to bootstrap config
  - name: DUBBO_IP_TO_REGISTRY
    valueFrom:
      fieldRef:
        fieldPath: status.podIP    # Pod IP for service registration
```

### Istio Resources

#### DestinationRule

```yaml
apiVersion: networking.istio.io/v1
kind: DestinationRule
metadata:
  name: dubbo-demo-xds-provider-dr
  namespace: dubbo-proxyless
spec:
  host: dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local
  trafficPolicy:
    tls:
      mode: DISABLE  # Disable TLS for demo simplicity
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
```

#### VirtualService

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: dubbo-demo-xds-provider-vs
  namespace: dubbo-proxyless
spec:
  hosts:
  - dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local
  http:
  - route:
    - destination:
        host: dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local
        subset: v1
```

## Traffic Management Examples

### Example 1: Weight-based Traffic Splitting

Deploy weight-based routing to distribute traffic between service versions:

```bash
# Apply weight-based virtual service
kubectl apply -f weight-virtualservice.yaml

# Test with specific header
curl -H "test-scenario: weight-test" http://localhost:8080/hello?name=World
```

The configuration splits traffic 70% to v1 and 30% to v2:

```yaml
route:
- destination:
    host: dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local
    subset: v1
  weight: 70
- destination:
    host: dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local
    subset: v2
  weight: 30
```

### Example 2: Header-based Routing

```bash
# Apply header-based routing
kubectl apply -f header-virtualservice.yaml

# Test with version header
curl -H "version: v2" http://localhost:8080/hello?name=World
```

### Example 3: Fault Injection

```bash
# Apply fault injection rules
kubectl apply -f fault-virtualservice.yaml

# Test fault injection
curl -H "test-scenario: fault-test" http://localhost:8080/hello?name=World
```

### Example 4: Circuit Breaker

```bash
# Apply circuit breaker destination rule
kubectl apply -f circuit-breaker-dr.yaml
```

## Local Development

### Prerequisites for Local Development

1. **Docker** - For building container images
2. **Local Registry** (optional) - For storing images locally

```bash
# Start local Docker registry (optional)
docker run -d -p 5000:5000 --name local-registry registry:2
```

### Building and Deploying from Source

1. **Make Scripts Executable**:

```bash
chmod +x ./start.sh ./update.sh ./port_forward.sh
```

2. **Build and Deploy**:

```bash
# Build images and deploy to Kubernetes
./update.sh
```

The `update.sh` script performs the following actions:
- Builds Docker images for provider and consumer
- Tags images appropriately
- Applies Kubernetes manifests
- Sets up port forwarding for debugging

3. **Iterative Development**:

```bash
# After making code changes, redeploy with:
./update.sh

# This will rebuild images and update the deployment
```

### Project Structure

```
dubbo-demo-xds/
├── dubbo-demo-xds-interface/     # Shared interface definitions
├── dubbo-demo-xds-consumer/      # Consumer application
├── dubbo-demo-xds-provider/      # Provider application
├── services.yaml                 # Provider Kubernetes manifests
├── consumer-services.yaml        # Consumer Kubernetes manifests
├── weight-virtualservice.yaml    # Traffic management examples
├── header-virtualservice.yaml
├── fault-virtualservice.yaml
├── update.sh                     # Build and deploy script
├── start.sh                      # Initial setup script
└── port_forward.sh              # Port forwarding script
```

### Maven Configuration

The project uses Spring Boot with Dubbo dependencies:

```xml
<dependencies>
  <dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
  </dependency>
  <dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-xds</artifactId>
  </dependency>
  <dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-rpc-triple</artifactId>
  </dependency>
</dependencies>
```

## Debugging

### Remote Debugging Setup

The deployment includes remote debugging capabilities:

1. **Debug Ports**:
   - Consumer service: `31000`
   - Provider service: `31001`

2. **Start Port Forwarding**:

```bash
# Automatic port forwarding (included in update.sh)
./port_forward.sh

# Or manually:
kubectl port-forward -n dubbo-proxyless svc/dubbo-demo-xds-consumer 31000:31000 &
kubectl port-forward -n dubbo-proxyless svc/dubbo-demo-xds-provider 31001:31001 &
```

3. **IDE Configuration**:

**IntelliJ IDEA**:
- Go to `Run` → `Edit Configurations`
- Add new `Remote JVM Debug` configuration
- Set `Host: localhost`, `Port: 31000` (for consumer) or `31001` (for provider)
- Set `Use module classpath` to the appropriate module

**VS Code**:
```json
{
  "type": "java",
  "name": "Debug Consumer",
  "request": "attach",
  "hostName": "localhost",
  "port": 31000
}
```

### Logging Configuration

Enable detailed logging for troubleshooting:

```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: >-
      -Ddubbo.application.logger=slf4j
      -Dlogging.level.root=INFO
      -Dlogging.level.org.apache.dubbo=DEBUG
      -Dlogging.level.org.apache.dubbo.xds=TRACE
      -Dlogging.level.org.apache.dubbo.registry=TRACE
```

### Monitoring and Observability

1. **Check Pod Logs**:

```bash
# Consumer logs
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer -f

# Provider logs
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-provider -f
```

2. **Check xDS Configuration**:

```bash
# Verify xDS resources are being received
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer | grep -i xds
```

3. **Istio Configuration Dump**:

```bash
# Check Istio configuration for the service
istioctl proxy-config cluster dubbo-demo-xds-consumer-xxx -n dubbo-proxyless
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Bootstrap Configuration Not Found

**Error**: `GRPC_XDS_BOOTSTRAP file not found`

**Solution**:
```bash
# Verify bootstrap file is mounted correctly
kubectl exec -n dubbo-proxyless deployment/dubbo-demo-xds-consumer -- cat /app/bootstrap.json

# Check environment variable
kubectl exec -n dubbo-proxyless deployment/dubbo-demo-xds-consumer -- env | grep GRPC_XDS_BOOTSTRAP
```

#### 2. Cannot Connect to Istio Control Plane

**Error**: `Failed to connect to istiod`

**Solution**:
```bash
# Check istiod is running
kubectl get pods -n istio-system

# Verify network connectivity
kubectl exec -n dubbo-proxyless deployment/dubbo-demo-xds-consumer -- \
  nc -zv istiod.istio-system.svc 15010

# Check service discovery
kubectl get svc -n istio-system istiod
```

#### 3. Service Discovery Issues

**Error**: `No available providers`

**Solution**:
```bash
# Check service registration
kubectl get endpoints -n dubbo-proxyless

# Verify pod labels match service selector
kubectl get pods -n dubbo-proxyless --show-labels

# Check Dubbo registry logs
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-provider | grep -i registry
```

#### 4. xDS Configuration Not Applied

**Error**: Traffic management rules not working

**Solution**:
```bash
# Verify VirtualService and DestinationRule are applied
kubectl get virtualservice,destinationrule -n dubbo-proxyless

# Check Istio configuration status
kubectl describe virtualservice dubbo-demo-xds-provider-vs -n dubbo-proxyless

# Verify xDS updates in application logs
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer | grep -i "xds.*update"
```

#### 5. Port Conflicts

**Error**: `Port already in use`

**Solution**:
```bash
# Kill existing port forwards
pkill -f "kubectl port-forward"

# Check what's using the port
lsof -i :8080
lsof -i :31000
lsof -i :31001

# Use different ports if needed
kubectl port-forward -n dubbo-proxyless svc/dubbo-demo-xds-consumer 8081:8080
```

### Debug Commands

```bash
# Check all resources in namespace
kubectl get all -n dubbo-proxyless

# Describe problematic pods
kubectl describe pod -n dubbo-proxyless <pod-name>

# Check events
kubectl get events -n dubbo-proxyless --sort-by='.lastTimestamp'

# Verify Istio injection status
kubectl get namespace dubbo-proxyless -o yaml | grep istio

# Check Istio proxy status (if sidecar is accidentally injected)
kubectl exec -n dubbo-proxyless <pod-name> -c istio-proxy -- pilot-agent request GET stats/prometheus
```

### Performance Tuning

1. **JVM Options**:
```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: >-
      -Xms512m -Xmx1024m
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
      -Ddubbo.consumer.timeout=10000
      -Ddubbo.provider.timeout=10000
```

2. **Dubbo Configuration**:
```yaml
dubbo:
  consumer:
    timeout: 10000
    retries: 2
    loadbalance: roundrobin
  provider:
    timeout: 10000
    threads: 200
```

## Advanced Topics

### Security Configuration

1. **Enable mTLS**:
```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: dubbo-demo-mtls
  namespace: dubbo-proxyless
spec:
  mtls:
    mode: STRICT
```

2. **Authorization Policies**:
```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: dubbo-demo-authz
  namespace: dubbo-proxyless
spec:
  selector:
    matchLabels:
      app: dubbo-demo-xds-provider
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/dubbo-proxyless/sa/default"]
```

### Multi-Cluster Setup

For multi-cluster deployments, configure cross-cluster service discovery:

```bash
# Install Istio with multi-cluster configuration
istioctl install --set values.pilot.env.EXTERNAL_ISTIOD=true

# Configure cluster endpoints
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: istiod-gateway
  namespace: istio-system
spec:
  selector:
    istio: eastwestgateway
  servers:
  - port:
      number: 15010
      name: tls
      protocol: TLS
    tls:
      mode: PASSTHROUGH
    hosts:
    - "*"
EOF
```

## Best Practices

1. **Resource Management**:
   - Set appropriate CPU and memory limits
   - Use horizontal pod autoscaling for production
   - Monitor resource usage and adjust accordingly

2. **Configuration Management**:
   - Use ConfigMaps for bootstrap configuration
   - Implement proper secret management for production
   - Version your Istio configurations

3. **Monitoring**:
   - Enable Dubbo metrics collection
   - Use Istio's observability features
   - Implement proper logging strategies

4. **Testing**:
   - Test traffic management rules in staging
   - Implement chaos engineering practices
   - Use canary deployments for updates

## Conclusion

This demo demonstrates how to leverage Dubbo's proxyless mode with Istio for:

- **Simplified Architecture**: Direct xDS integration without sidecars
- **Advanced Traffic Management**: Weight-based routing, header-based routing, fault injection
- **Service Mesh Benefits**: Security, observability, and policy enforcement
- **Cloud-Native Integration**: Seamless Kubernetes and Istio integration

The proxyless mode is ideal for scenarios where:
- Resource efficiency is critical
- You want to reduce operational complexity
- Direct control plane integration is preferred
- Performance optimization is a priority

For production deployments, ensure proper security configurations, monitoring, and testing procedures are in place.

## References

- [Apache Dubbo Documentation](https://dubbo.apache.org/)
- [Istio Documentation](https://istio.io/latest/docs/)
- [xDS Protocol Specification](https://www.envoyproxy.io/docs/envoy/latest/api-docs/xds_protocol)
- [Kubernetes Documentation](https://kubernetes.io/docs/)

## Contributing

To contribute to this demo:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

For issues and questions, please use the [Apache Dubbo GitHub Issues](https://github.com/apache/dubbo/issues).
