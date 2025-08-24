# Dubbo XDS (Proxyless) Demo

This demo showcases how to use Apache Dubbo in **Proxyless mode** with Istio service mesh. In proxyless mode, Dubbo applications directly communicate with the Istio control plane (istiod) to receive xDS configuration, eliminating the need for Envoy sidecars while still benefiting from service mesh capabilities like traffic management, security, and observability.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Environment Setup](#environment-setup)
- [Quick Start](#quick-start)
- [Traffic Management Examples](#traffic-management-examples)
- [Debugging](#debugging)

## Overview

### What is Dubbo Proxyless Mode?

Dubbo Proxyless mode allows Dubbo applications to:
- **Direct xDS Integration**: Connect directly to Istio's control plane without Envoy sidecars
- **Native Service Mesh**: Leverage Istio's traffic management, security policies, and observability
- **Reduced Resource Overhead**: Eliminate sidecar proxy resource consumption
- **Simplified Architecture**: Fewer moving parts in your service mesh deployment

## Prerequisites

### Required Software

1. **Kubernetes Cluster**
   - A Kubernetes cluster or local Kubernetes cluster (e.g., Minikube, Kind)

2. **Istio**
   - Istio control plane installed and configured

3. **kubectl** - Kubernetes command-line tool

4. **Docker** - For building container images

5. **Container Registry** - For storing and distributing Docker images

### Container Registry Configuration

The deployment scripts will build Docker images and push them to a container registry. You need to configure the registry settings:

1. **Update Registry URLs in Deployment Scripts**:

   Edit the following files to use your container registry:
   - `deploy-provider.sh` : Update `REMOTE_REPO` variable
   - `deploy-consumer.sh` : Update `REMOTE_REPO` variable



2. **Update Kubernetes Manifests**:

   Update the image references in the following files:
   - `provider-service.yaml` : Update the `image` field
   - `consumer-services.yaml` : Update the `image` field

   ```yaml
   # replace REMOTE_REPO and REMOTE_TAG with actual values
   image: REMOTE_REPO:REMOTE_TAG
   ```

3. **Configure Image Pull Secrets** (if using private registry):

   ```bash
   # Create Docker registry secret
   kubectl create secret docker-registry your-registry-secret \
     --docker-server=your-registry.com \
     --docker-username=your-username \
     --docker-password=your-password \
     --docker-email=your-email@example.com \
     -n dubbo-proxyless
   ```

   Then update the `imagePullSecrets` in the YAML files:
   ```yaml
   imagePullSecrets:
     - name: your-registry-secret
   ```

## Environment Setup

### Step 1: Install Kubernetes

#### Using Kind as Example

```bash
# Install Kind
# For details, see official website: https://kind.sigs.k8s.io/
# Example for Linux + amd64:
wget -O kind https://github.com/kubernetes-sigs/kind/releases/download/v0.23.0/kind-linux-amd64
chmod +x kind
mv kind /usr/bin/

# Create cluster with provided config
kind create cluster --name dubbo-proxyless --config=kind-config.yaml

# Verify installation
kubectl get nodes

# Expected output should show all nodes:
NAME                            STATUS   ROLES           AGE   VERSION
dubbo-proxyless-control-plane   Ready    control-plane   64s   v1.33.1
dubbo-proxyless-worker          Ready    <none>          53s   v1.33.1
dubbo-proxyless-worker2         Ready    <none>          53s   v1.33.1
```

### Step 2: Install Istio

#### Quick Installation

```bash
# Download Istio
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH=$PWD/bin:$PATH

# Install Istio with demo profile
istioctl install --set profile=demo -y
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

The demo provides automated deployment scripts that handle the entire process from building to deployment.

> **⚠️ Important**: Before running the deployment scripts, make sure you have configured your container registry settings as described in the [Prerequisites](#container-registry-configuration) section.

1. **Make Scripts Executable**:

```bash
chmod +x deploy-provider.sh deploy-consumer.sh
```

2. **Deploy Provider Service**:

```bash
# Deploy provider service (includes v1 and v2 versions)
./deploy-provider.sh
```

This script will:
- Build the Dubbo provider project using Maven
- Create Docker images for the provider
- Deploy both v1 and v2 versions to Kubernetes
- Verify the deployment status

3. **Deploy Consumer Service**:

```bash
# Deploy consumer service
./deploy-consumer.sh
```

This script will:
- Build the Dubbo consumer project using Maven
- Create Docker image for the consumer
- Deploy the consumer to Kubernetes
- Verify the deployment status

4. **Verify Deployment**:

```bash
# Check pod status
kubectl get pods -n dubbo-proxyless

# Check services
kubectl get svc -n dubbo-proxyless

# Expected output:
# NAME                        TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)
# dubbo-demo-xds-provider-v1  ClusterIP   10.96.xxx.xxx   <none>        50051/TCP,31001/TCP
# dubbo-demo-xds-provider-v2  ClusterIP   10.96.xxx.xxx   <none>        50051/TCP,31001/TCP
# dubbo-demo-xds-consumer     ClusterIP   10.96.xxx.xxx   <none>        50050/TCP,31000/TCP,8080/TCP
```

5. **Test the Application**:

```bash
# Monitor consumer logs to observe service calls
kubectl logs -n dubbo-proxyless -l app=dubbo-demo-xds-consumer -f
# Look for "doSayHello called with name" messages in the logs
```

## Traffic Management Examples
Deploy weight-based routing to distribute traffic between service versions:

### Example 1: Weight-based Traffic Splitting



```bash
# Apply weight-based virtual service
kubectl apply -f weight-virtualservice.yaml
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
kubectl apply -f header-matching-virtualservice.yaml
```

This configuration will route based on the user-type field in the request header, routing VIP users to version v2 and ordinary users to version v1.
```yaml
spec:
  hosts:
  - dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local
  http:
  - match:
    - headers:
        user-type:
          exact: "vip"
    timeout: 10s
    route:
    - destination:
        host: dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local
        subset: v2
      weight: 100
  - match:
    - headers:
        user-type:
          exact: "normal"
    timeout: 10s
    route:
    - destination:
        host: dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local
        subset: v1
      weight: 100
```

## Debugging

### Remote Debugging Setup

The deployment includes remote debugging capabilities, but debugging is disabled by default. To enable debugging, you need to modify the environment variables in the service YAML files.

1. **Enable Debugging**:

   Before deploying, modify the JAVA_TOOL_OPTIONS in the following files to include the debugging agent:
   - In `consumer-services.yaml`, find the `JAVA_TOOL_OPTIONS` environment variable and add `-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=31000` to enable consumer debugging
   - In `provider-service.yaml`, find the `JAVA_TOOL_OPTIONS` environment variable and add `-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=31001` to enable provider debugging

   For example, in `consumer-services.yaml`, change:
   ```yaml
   - name: JAVA_TOOL_OPTIONS
     value: "-Ddubbo.application.logger=slf4j -Dlogging.level.root=INFO -Dlogging.level.org.apache.dubbo=DEBUG"
   ```
   
   to:
   ```yaml
   - name: JAVA_TOOL_OPTIONS
     value: "-Ddubbo.application.logger=slf4j -Dlogging.level.root=INFO -Dlogging.level.org.apache.dubbo=DEBUG -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=31000"
   ```

2. **Debug Ports**:
   - Consumer service: `31000`
   - Provider service: `31001`

3. **Redeploy Services**:

   After modifying the YAML files, redeploy the services:
   ```bash
   ./deploy-consumer.sh
   ./deploy-provider.sh
   ```

4. **Start Port Forwarding**:

```bash
# Automatic port forwarding
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


### Monitoring and Observability

1. **Check Pod Logs**:

```bash
# Consumer logs
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-consumer -f

# Provider logs
kubectl logs -n dubbo-proxyless deployment/dubbo-demo-xds-provider -f
```


