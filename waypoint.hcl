project = "spring-boot-app-1.0"

app "quarkus-app" {
  labels = {
    "service" = "spring-boot-app",
    "env"     = "dev"
  }

  build {
    use "exec" {
      command = ["sh", "-c", <<EOT
        docker pull pyrodocker1/social-activity-service:latest #for now it is latest, since i didn't tag anything
        docker pull pyrodocker1/api-gateway:latest
        docker pull pyrodocker1/comment-like-service:latest
        docker pull pyrodocker1/image-service:latest
        docker pull pyrodocker1/user-service:latest
        docker pull postgres:15-alpine
        docker pull redis:7.2-alpine
        docker pull confluentinc/cp-kafka:7.8.0
        docker pull confluentinc/cp-zookeeper:7.8.0
        docker pull mongo:6.0
        docker pull prom/prometheus:v3.6.0
        docker pull grafana/grafana:main

        if ! kind get clusters | grep -q desktop; then
          kind create cluster --name desktop
        fi

        kind load docker-image postgres:14-alpine --name desktop
        kind load docker-image redis:7.2-alpine --name desktop
        kind load docker-image confluentinc/cp-kafka:7.8.0 --name desktop
        kind load docker-image confluentinc/cp-zookeeper:7.8.0 --name desktop
        kind load docker-image mongo:6.0 --name desktop
        kind load docker-image prom/prometheus:v3.6.0 --name desktop
        kind load docker-image grafana/grafana:main --name desktop
        kind load docker-image pyrodocker1/social-activity-service:latest --name desktop
        kind load docker-image pyrodocker1/api-gateway:latest --name desktop
        kind load docker-image pyrodocker1/comment-like-service:latest --name desktop
        kind load docker-image docker pull pyrodocker1/image-service:latest --name desktop
        kind load docker-image pyrodocker1/pyrodocker1/user-service:latest --name desktop
      EOT
      ]
    }
  }

  deploy {
    use "exec" {
      command = ["sh", "-c", <<EOT
        echo "Deploying all Kubernetes resources in correct order..."

        echo "Step 1: Applying base cluster resources..."
        kubectl apply -f ./k8s/namespace.yaml
        kubectl apply -f ./k8s/rbac.yaml
        kubectl apply -f ./k8s/service-accounts.yaml
        kubectl apply -f ./k8s/secrets.yaml

        echo "Step 2: Applying limits and policies..."
        kubectl apply -f ./k8s/limit-range.yaml
        kubectl apply -f ./k8s/network-policies.yaml

        echo "Step 3: Applying storage systems..."
        kubectl apply -f ./k8s/mongodb-ss.yaml
        kubectl apply -f ./k8s/postgres-ss.yaml
        kubectl apply -f ./k8s/redis-k8s.yaml
        kubectl apply -f ./k8s/kafka-k8s.yaml
        kubectl apply -f ./k8s/localstack-k8s.yaml

        echo "Waiting for storage systems to be ready..."
        kubectl wait --for=condition=ready pod -l app=postgres --timeout=180s
        kubectl wait --for=condition=ready pod -l app=redis --timeout=120s
        kubectl wait --for=condition=ready pod -l app=mongodb --timeout=180s

        echo "Step 4: Applying monitoring..."
        kubectl apply -f ./k8s/prometheus-k8s.yaml
        kubectl apply -f ./k8s/grafana-k8s.yaml

        echo "Step 5: Applying business services..."
        kubectl apply -f ./k8s/user-service-k8s.yaml
        kubectl apply -f ./k8s/image-service-k8s.yaml
        kubectl apply -f ./k8s/activity-service-k8s.yaml
        kubectl apply -f ./k8s/comment-like-service-k8s.yaml

        echo "Step 6: Applying gateway and frontend..."
        kubectl apply -f ./k8s/api-gateway-k8s.yaml
        kubectl apply -f ./k8s/front-k8s.yaml

        echo "Step 7: Applying ingress and availability policies..."
        if ! kubectl get namespace ingress-nginx > /dev/null 2>&1; then
          kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
          kubectl wait --namespace ingress-nginx \
            --for=condition=ready pod \
            --selector=app.kubernetes.io/component=controller \
            --timeout=120s
        fi

        kubectl apply -f ./k8s/social-service-ingress.yaml
        kubectl apply -f ./k8s/social-services-pdb.yaml
        kubectl apply -f ./k8s/social-services-hpa.yaml

        #echo "Step 8: Applying backups..."
        #kubectl apply -f ./k8s/backups.yaml

        echo "Waiting for main services to be ready..."
        kubectl wait --for=condition=ready pod -l app=user-service --timeout=120s
        kubectl wait --for=condition=ready pod -l app=api-gateway --timeout=120s
        
        kubectl get pods,svc,ingress -n social-service
      EOT
      ]
    }
  }
}
