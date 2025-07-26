#!/bin/bash

# Variable to control whether to delete Docker images
DELETE_IMAGES=true

# Define port numbers
CONSUMER_DEBUG_PORT=31000
CONSUMER_PORT=50050
PROVIDER_DEBUG_PORT=31001
PROVIDER_PORT=50051
ISTIO_PORT=15010

# Define operating system type (options: linux, windows, mac)
OS_TYPE="mac"  # Modify this variable to switch the operating system

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS_TYPE="linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    OS_TYPE="mac"
elif [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]] || [[ "$OSTYPE" == "nt" ]]; then
    OS_TYPE="windows"
else
    echo "Unsupported OS type: $OSTYPE"
    exit 1
fi

echo "Current OSTYPE: $OS_TYPE"

# Define the method to delete Docker images
function delete_docker_images() {
    if [ "$DELETE_IMAGES" = true ]; then
        echo "Deleting Docker images..."
        docker rmi localhost:5000/dubbo-demo-xds-consumer:latest || true
        docker rmi localhost:5000/dubbo-demo-xds-provider:latest || true
        docker rmi -f dubbo-demo-xds-consumer:latest || true
        docker rmi -f dubbo-demo-xds-provider:latest || true
        echo "Docker images deleted"
    else
        echo "Skipping deletion of Docker images"
    fi
}

# Stop processes based on port occupation
function stop_processes_by_port() {
    local ports=("$@")
    for port in "${ports[@]}"; do
        if [ "$OS_TYPE" = "linux" ] || [ "$OS_TYPE" = "mac" ]; then
            # Linux and macOS system commands
            pid=$(lsof -t -i:$port)
            if [ -n "$pid" ]; then
                echo "Killing process with PID $pid on port $port"
                kill -9 $pid || true
            else
                echo "No process found on port $port"
            fi
        elif [ "$OS_TYPE" = "windows" ]; then
            # Windows system commands
            pid=$(netstat -ano | findstr ":$port" | head -n 1 | awk '{print $5}' | tr -d '[:space:]')
            if [ -n "$pid" ] && [[ "$pid" =~ ^[0-9]+$ ]]; then
                echo "Killing process with PID $pid on port $port"
                taskkill //PID $pid //F || true
            else
                echo "No valid process found on port $port"
            fi
        else
            echo "Unsupported OS type: $OS_TYPE"
        fi
    done
}

# Stop port forwarding
stop_processes_by_port $CONSUMER_PORT $PROVIDER_PORT $ISTIO_PORT

## Delete Kubernetes deployments and services
# kubectl delete deployment dubbo-demo-xds-consumer dubbo-demo-xds-provider || true
# kubectl delete svc dubbo-demo-xds-consumer dubbo-demo-xds-provider || true

# Delete other resources defined in ./services.yaml
kubectl delete -f ./services.yaml || true

# Call the method to delete Docker images
delete_docker_images

echo "All services and resources have been stopped and deleted"
