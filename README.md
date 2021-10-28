# options-responder
A SIP server that just responds to incoming OPTIONS requests.

## Building
This is a maven project. The source requires Java 11.
You can import this as a maven project into any IDE of choice and build it.
From the command-line, you can build it using `mvn clean` and `mvn install`.

## Deployment & Running
This SIP server is meant to be deployed in kubernetes.
You need :
- docker build tools that allows you to build container images.
- a working k8s cluster.

### Building the image
To build the container image:
- From a terminal go to the `docker/` directory under this project.
- Run `docker build --force-rm --tag options-responder:1.0.0 .`
  - Ensure the tag name unique.
- Push the image into your docker repository.

### Deployment into kubernetes
- Make sure you have a working k8s cluster.
- Make sure your k8s cluster has access to your docker repository.
  - If not, ensure you push the image into the docker repository used by your k8s cluster. For e.g. on minikube, you can use `minikube image load options-reposonder:1.0.0`.

- Kubernetes deployment manifest files are available under the directory `k8s/`.
- The application gets deployed as a Service with one pod behind it.
- Run - `kubectl apply -f ./options-responder.yaml` to deploy the SIP server into your k8s cluster.

