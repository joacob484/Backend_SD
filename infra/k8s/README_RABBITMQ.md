RabbitMQ on GKE (Autopilot) — deploy guide

Prerequisites (local machine):
- gcloud CLI installed and authenticated to project `master-might-274420`.
- kubectl (and gke-gcloud-auth-plugin) installed. If missing on Windows PowerShell:
  - Install gke plugin: `gcloud components install gke-gcloud-auth-plugin`
  - Or follow: https://cloud.google.com/kubernetes-engine/docs/how-to/cluster-access-for-kubectl#install_plugin
- Helm installed (e.g., `choco install kubernetes-helm -y` or via https://helm.sh).

High-level steps (PowerShell commands):

1) Get cluster credentials

    gcloud container clusters get-credentials faltauno-rabbitmq --region=us-central1 --project=master-might-274420

2) Add Helm repo and update

    helm repo add bitnami https://charts.bitnami.com/bitnami
    helm repo update

3) Fetch RabbitMQ password from Secret Manager (we will use version 5 created earlier)

    $pw = gcloud secrets versions access 5 --secret=rabbitmq-password --project=master-might-274420

4) Install/upgrade RabbitMQ via Helm

    helm upgrade --install rabbitmq bitnami/rabbitmq \
      --namespace rabbitmq --create-namespace \
      --set auth.username=faltauno \
      --set auth.password=$pw \
      --set persistence.size=20Gi \
      --set service.type=LoadBalancer \
      --set service.annotations.'cloud.google.com/load-balancer-type'="Internal" \
      --values infra/k8s/rabbitmq-values.yaml

Notes:
- The chart will create a LoadBalancer service. On GCP, we set the annotation to request an internal LB; this makes the broker reachable by internal IPs only.
- After Helm completes, get the internal IP:

    kubectl get svc rabbitmq -n rabbitmq -o jsonpath='{.status.loadBalancer.ingress[0].ip}'

- Use that IP to update your Cloud Run `SPRING_RABBITMQ_HOST` environment variable and bind the `rabbitmq-password` secret version you created (version 5). Example:

    gcloud run deploy faltauno-backend \
      --region=us-central1 --project=master-might-274420 \
      --no-traffic \
      --update-secrets=SPRING_RABBITMQ_PASSWORD=rabbitmq-password:5 \
      --set-env-vars SPRING_RABBITMQ_HOST=<INTERNAL_IP>,SPRING_RABBITMQ_PORT=5672,APP_RABBIT_ENABLED=true

- Validate: tail Cloud Run logs and RabbitMQ pod logs. Ensure you see successful "user 'faltauno' authenticated" messages and that Spring consumers no longer fail with ACCESS_REFUSED.

Next steps (recommended):
- Once validated, promote the new Cloud Run revision to traffic and then decommission RabbitMQ on the VM.
- Consider setting up a Kubernetes Ingress, Network Endpoint Group, or DNS record for stable hostnames.
- For production, enable TLS for AMQP or use a private service mesh / mTLS if needed.
