# Dubbo-demo-xds

### Prepare Environment (Optional)
**To run this example, you need to have a kubernetes cluster with istio installed.**
* If you don't have a k8s cluster, we recommend using docker desktop to get started. It has an embedded k8s cluster.
  * [install docker desktop](https://www.docker.com/products/docker-desktop/)
  * After installing it, you need to enable kubernetes in `settings/Kubernetes`.

* Then, install istio following [installation guide](https://istio.io/latest/docs/setup/getting-started/)
  * Use `kubectl get pods -n istio-system` to check if istio is installed correctly.

* If you are not using docker desktop, you need to install docker to build and manage image.

## Remote Deployment
Run the following command to deploy pre-prepared images:

```shell
kubectl apply -f
```

## Local Development
If you have code changed locally and want to deploy it to remote cluster, follow the instructions below to learn how to build and deploy from source code.

### Deploy Example
> Use `docker run -d -p 5000:5000 --name local-registry registry:2` to enable local image repository.

**When you have completed the above steps:**
* Run `chmod 777 ./start.sh ./update.sh`
* Then, use `./update.sh` to deploy example to Kubernetes.
  * Every time you change the code, you need to run `./update.sh` again to synchronize the changes to Kubernetes.

### Start debugging
* Every time you run ./update.sh, it will start port forward to demo containers. So you can use `Remote Debug` in your IDE to start debugging directly.

* You can also simply use ./port_forward.sh to start port forward.

> Consumer service debug port: 31000
>
> Provider service debug port: 31001
