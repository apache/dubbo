#!/bin/bash
set -e

# --- 1. Check if we need to build the consumer module ---
CONSUMER_DIR="$(pwd)/dubbo-demo-xds-consumer"
JAR_FILE=$(find $CONSUMER_DIR/target -name "*.jar" -type f | grep -v "sources\|javadoc\|tests" | head -1)

# Force rebuild
echo "--- Force rebuilding dubbo-demo-xds-consumer and its dependencies... ---"
pushd ../../

# Build the consumer module and all its dependencies automatically
echo "Building consumer module and dependencies..."
mvn clean install -pl dubbo-demo/dubbo-demo-xds/dubbo-demo-xds-consumer -am -DskipTests -Dspotless.check.skip=true

popd
echo "--- Build complete. ---"

# --- 2. Build and Push Docker Image for the Consumer ---
BASE_DIR=$(pwd)
CONSUMER_IMAGE="dubbo-demo-xds-consumer:latest"
# You can change this to your own repository
REMOTE_REPO="crpi-ta376u07gp1439ff.cn-beijing.personal.cr.aliyuncs.com/test_xds/test_dubbo_consumer"
REMOTE_TAG="latest"

echo "--- Building Docker image for $CONSUMER_IMAGE... ---"
cd $CONSUMER_DIR

# Find the JAR file again (in case it was just built)
JAR_FILE=$(find target -name "*.jar" -type f | grep -v "sources\|javadoc\|tests" | head -1)
if [ -z "$JAR_FILE" ]; then
  echo "Error: Could not find JAR file in $CONSUMER_DIR/target"
  exit 1
fi

echo "Found JAR: $(basename $JAR_FILE)"

# Build the Docker image
docker build -t $CONSUMER_IMAGE --build-arg ARTIFACT=$(basename $JAR_FILE) .
docker tag $CONSUMER_IMAGE $REMOTE_REPO:$REMOTE_TAG
docker push $REMOTE_REPO:$REMOTE_TAG

echo "--- Docker image built and pushed: $REMOTE_REPO:$REMOTE_TAG ---"

# --- 3. Deploy to Kubernetes ---
cd $BASE_DIR
echo "--- Deploying to Kubernetes... ---"

kubectl apply -f consumer-services.yaml

echo "--- Deployment complete. ---"
echo "--- You can check the status with: kubectl get pods -n dubbo-proxyless ---" 