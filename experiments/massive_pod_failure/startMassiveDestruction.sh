while true; do
  kubectl apply -f massive_pod_failure.yaml
  sleep  5
  if  kubectl get PodChaos -n chaos-mesh | grep pod-kill-all; then
    kubectl delete PodChaos pod-kill-all -n chaos-mesh
  fi
done

