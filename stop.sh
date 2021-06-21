#!/bin/bash

echo "💣 Removing local Kubernetes cluster with KinD …"
kind get clusters | grep mongodb-operator-cluster && kind delete cluster --name mongodb-operator-cluster

echo "💣 Removing local Docker registry …"
docker ps | grep kind-registry && docker stop kind-registry
docker rm kind-registry
