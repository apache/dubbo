#!bash
#Run this script to start port-forwarding

CONSUMER_DEBUG_PORT=31000
CONSUMER_PORT=50050
PROVIDER_DEBUG_PORT=31001
PROVIDER_PORT=50051

kubectl port-forward $(kubectl get pods -n istio-system | grep istiod | awk '{print $1}') 15010:15010 -n istio-system &
PID1=$!
kubectl port-forward deployment/dubbo-demo-xds-consumer $CONSUMER_DEBUG_PORT:$CONSUMER_DEBUG_PORT $CONSUMER_PORT:$CONSUMER_PORT &
PID2=$!
kubectl port-forward deployment/dubbo-demo-xds-provider $PROVIDER_DEBUG_PORT:$PROVIDER_DEBUG_PORT $PROVIDER_PORT:$PROVIDER_PORT &
PID3=$!

wait $PID1
wait $PID2
wait $PID3
