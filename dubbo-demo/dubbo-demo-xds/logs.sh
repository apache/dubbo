#!bash

echo "Starting logs consumer"
kubectl get pods -n default
POD_NAME=$(kubectl get pods -n default | grep dubbo-demo-xds-consumer | awk '{print $1}')
echo $POD_NAME
kubectl logs $POD_NAME -n default


echo "Starting logs provider"
kubectl get pods -n default
POD_NAME=$(kubectl get pods -n default | grep dubbo-demo-xds-provider | awk '{print $1}')
echo $POD_NAME
kubectl logs $POD_NAME -n default
