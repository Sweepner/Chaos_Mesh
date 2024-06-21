(cd metrics; kubectl apply -f components.yaml)
while true; do
  echo "Checking if metrics pod is created..."
  if kubectl get pods -n kube-system | grep metrics; then
    break;
  fi
  sleep 1
done
minikube dashboard
minikube addons enable metrics-server
(cd traefik; kubectl apply -f traefik-ingress-dashboard.yaml)
kubectl create secret generic kubernetes-dashboard-auth --from-file=auth --namespace=kubernetes-dashboard
