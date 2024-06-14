./startChatWithMe.sh
(cd traefik; ./startTraefik.sh)
while true; do
  echo "Checking if postgres pod is created..."
  if kubectl get pods | grep postgres; then
    break;
  fi
  sleep 1
done
echo "Waiting for all pods to be ready..."
kubectl wait pod --all --for=condition=Ready --timeout=300s
expect expectCreateTunnel.exp
watch -n 1 kubectl get pods
