helm install traefik traefik/traefik -f values.yaml
kubectl apply -f traefik-ingress.yaml
(cd middleware; ./startMiddlewares.sh)
