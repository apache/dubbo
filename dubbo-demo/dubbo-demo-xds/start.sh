#!/bin/bash
set -e

# --- 1. Build all modules from project root ---
# This is the most critical step to ensure all our code changes are compiled.
echo "--- Building all Dubbo modules... ---"
pushd ../../
mvn clean install -DskipTests -Dspotless.check.skip=true
popd
echo "--- Build complete. ---"


# --- 2. Build and Push Docker Image for the Provider ---
BASE_DIR=$(pwd)
PROVIDER_DIR="$BASE_DIR/dubbo-demo-xds-provider"
PROVIDER_IMAGE="dubbo-demo-xds-provider:latest"
# You can change this to your own repository
REMOTE_REPO="crpi-ta376u07gp1439ff.cn-beijing.personal.cr.aliyuncs.com/test_xds/test_dubbo_provider"
REMOTE_TAG="latest"

echo "--- Building Docker image for $PROVIDER_IMAGE... ---"
cd "$PROVIDER_DIR"

# Find the JAR file created by the root build
JAR_NAME=$(basename $(find target -type f -name "dubbo-demo-xds*.jar"))
if [ -z "$JAR_NAME" ]; then
    echo "❌ ERROR: JAR file not found in target directory. Build might have failed."
    exit 1
fi
echo "Found JAR: $JAR_NAME"

# Build the Docker image
docker build \
  --build-arg ARTIFACT="target/${JAR_NAME}" \
  -f Dockerfile \
  -t "$PROVIDER_IMAGE" \
  .

echo "--- Pushing Docker image to remote repository... ---"
# Tag the image for the remote repository
docker tag "$PROVIDER_IMAGE" "${REMOTE_REPO}:${REMOTE_TAG}"

# Note: You might need to run 'docker login ...' manually first if the token expires
echo "Pushing to ${REMOTE_REPO}:${REMOTE_TAG}"
# Push the image
docker push "${REMOTE_REPO}:${REMOTE_TAG}"
echo "--- Image pushed successfully. ---"


# --- 3. Deploy to Kubernetes ---
echo "--- Deploying services to Kubernetes... ---"
cd "$BASE_DIR"
kubectl apply -f ./services.yaml
# Force a restart of the deployment to pull the new image
echo "--- Restarting deployment to apply changes... ---"
kubectl rollout restart deployment dubbo-demo-xds-provider -n dubbo-proxyless

echo "--- Deployment initiated. Use 'kubectl get pods -n dubbo-proxyless -w' to monitor status. ---"
