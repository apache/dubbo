#!bash

# docker run -d -p 5000:5000 --name local-registry registry:2  #Use this to enable docker local repository

BASE_DIR=$(pwd)
SKIP_PACKAGE=true

echo BaseDir: $BASE_DIR

function package(){
    if [ $SKIP_PACKAGE = false ]; then
         mvn spotless:apply
         mvn clean package
    fi
}

cd $BASE_DIR/dubbo-demo-xds-consumer
package
JAR_NAME=$(basename $( find $(pwd)/target -type f -name "dubbo-demo-xds*.jar") )
echo JarName: $JAR_NAME
docker build --build-arg ARTIFACT=${JAR_NAME} -t dubbo-demo-xds-consumer:latest .
docker tag dubbo-demo-xds-consumer:latest localhost:5000/dubbo-demo-xds-consumer:latest
docker push localhost:5000/dubbo-demo-xds-consumer

cd $BASE_DIR/dubbo-demo-xds-provider
package
JAR_NAME=$(basename $(find $(pwd)/target -type f -name "dubbo-demo-xds*.jar") )
echo jarname: $JAR_NAME
docker build --build-arg ARTIFACT=${JAR_NAME} -t dubbo-demo-xds-provider:latest .
docker tag dubbo-demo-xds-provider:latest localhost:5000/dubbo-demo-xds-provider:latest
docker push localhost:5000/dubbo-demo-xds-provider

echo $(curl http://localhost:5000/v2/_catalog)

cd $BASE_DIR
kubectl apply -f ./services.yaml
kubectl rollout restart deployment dubbo-demo-xds-provider dubbo-demo-xds-consumer

sleep 5
sh ./port_forward.sh


