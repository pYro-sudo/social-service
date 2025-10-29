terraform {
  required_version = ">= 1.0"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }

    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }

    kubectl = {
      source  = "gavinbunney/kubectl"
      version = "~> 1.14"
    }

    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}

provider "kubectl" {
  config_path = "~/.kube/config"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "app_version" {
  type    = string
  default = "latest"
}

locals {
  namespaces = {
    db         = "social-network-db"
    users      = "social-network-users"
    images     = "social-network-images"
    comments   = "social-network-comments"
    activities = "social-network-activities"
    gateway    = "social-network-gateway"
    monitoring = "monitoring"
  }
}

resource "kubectl_manifest" "namespaces" {
  yaml_body = file("${path.module}/k8s/namespace.yaml")
}

resource "kubectl_manifest" "service_accounts" {
  yaml_body = file("${path.module}/k8s/service-accounts.yaml")
  depends_on = [kubectl_manifest.namespaces]
}

resource "kubectl_manifest" "rbac" {
  yaml_body = file("${path.module}/k8s/rbac.yaml")
  depends_on = [kubectl_manifest.service_accounts]
}

resource "kubectl_manifest" "limit_range" {
  yaml_body = file("${path.module}/k8s/limit-range.yaml")
  depends_on = [kubectl_manifest.namespaces]
}

resource "kubectl_manifest" "postgres" {
  yaml_body = file("${path.module}/k8s/postgres-ss.yaml")
  depends_on = [kubectl_manifest.namespaces, kubernetes_secret.postgres_credentials]
}

resource "kubectl_manifest" "mongodb" {
  yaml_body = file("${path.module}/k8s/mongodb-ss.yaml")
  depends_on = [kubectl_manifest.namespaces, kubernetes_secret.mongodb_credentials]
}

resource "kubectl_manifest" "redis" {
  yaml_body = file("${path.module}/k8s/redis-deployment.yaml")
  depends_on = [kubectl_manifest.namespaces, kubernetes_secret.redis_credentials]
}

resource "kubectl_manifest" "kafka" {
  yaml_body = file("${path.module}/k8s/kafka-deployment.yaml")
  depends_on = [kubectl_manifest.namespaces, kubernetes_secret.kafka_credentials]
}

resource "kubectl_manifest" "localstack" {
  yaml_body = file("${path.module}/k8s/localstack-deployment.yaml")
  depends_on = [kubectl_manifest.namespaces, kubernetes_secret.s3_credentials]
}

resource "kubectl_manifest" "user_service" {
  yaml_body = file("${path.module}/k8s/user-service-deployment.yaml")
  depends_on = [
    kubectl_manifest.postgres,
    kubectl_manifest.redis,
    kubectl_manifest.service_accounts,
    kubernetes_secret.redis_credentials
  ]
}

resource "kubectl_manifest" "image_service" {
  yaml_body = file("${path.module}/k8s/image-service-deployment.yaml")
  depends_on = [
    kubectl_manifest.postgres,
    kubectl_manifest.redis,
    kubectl_manifest.localstack,
    kubectl_manifest.service_accounts,
    kubernetes_secret.redis_credentials
  ]
}

resource "kubectl_manifest" "comment_like_service" {
  yaml_body = file("${path.module}/k8s/comment-like-service-deployment.yaml")
  depends_on = [
    kubectl_manifest.postgres,
    kubectl_manifest.redis,
    kubectl_manifest.kafka,
    kubectl_manifest.service_accounts,
    kubernetes_secret.redis_credentials
  ]
}

resource "kubectl_manifest" "activity_service" {
  yaml_body = file("${path.module}/k8s/activity-service-deployment.yaml")
  depends_on = [
    kubectl_manifest.mongodb,
    kubectl_manifest.kafka,
    kubectl_manifest.service_accounts,
    kubernetes_secret.mongodb_credentials
  ]
}

resource "kubectl_manifest" "api_gateway" {
  yaml_body = file("${path.module}/k8s/api-gateway-deployment.yaml")
  depends_on = [
    kubectl_manifest.user_service,
    kubectl_manifest.image_service,
    kubectl_manifest.comment_like_service,
    kubectl_manifest.activity_service,
    kubectl_manifest.service_accounts,
    kubernetes_secret.redis_credentials
  ]
}

resource "kubectl_manifest" "frontend" {
  yaml_body = file("${path.module}/k8s/front-deployment.yaml")
  depends_on = [kubectl_manifest.api_gateway]
}

resource "kubectl_manifest" "prometheus" {
  yaml_body = file("${path.module}/k8s/prometheus-deployment.yaml")
  depends_on = [kubectl_manifest.namespaces]
}

resource "kubectl_manifest" "grafana" {
  yaml_body = file("${path.module}/k8s/grafana-deployment.yaml")
  depends_on = [kubectl_manifest.prometheus]
}

resource "helm_release" "nginx_ingress" {
  name       = "nginx-ingress"
  repository = "https://kubernetes.github.io/ingress-nginx"
  chart      = "ingress-nginx"
  version    = "4.8.3"
  namespace  = "ingress-nginx"

  create_namespace = true

  set {
    name  = "controller.service.type"
    value = "NodePort"
  }

  set {
    name  = "controller.service.nodePorts.http"
    value = "30080"
  }

  set {
    name  = "controller.service.nodePorts.https"
    value = "30443"
  }
}

resource "kubectl_manifest" "ingress" {
  yaml_body = file("${path.module}/k8s/social-service-ingress.yaml")
  depends_on = [
    helm_release.nginx_ingress,
    kubectl_manifest.api_gateway,
    kubectl_manifest.grafana
  ]
}

resource "kubectl_manifest" "hpa" {
  yaml_body = file("${path.module}/k8s/social-services-hpa.yaml")
  depends_on = [
    kubectl_manifest.user_service,
    kubectl_manifest.image_service,
    kubectl_manifest.comment_like_service,
    kubectl_manifest.activity_service,
    kubectl_manifest.api_gateway
  ]
}

resource "kubectl_manifest" "pdb" {
  yaml_body = file("${path.module}/k8s/social-services-pdb.yaml")
  depends_on = [
    kubectl_manifest.user_service,
    kubectl_manifest.image_service,
    kubectl_manifest.comment_like_service,
    kubectl_manifest.activity_service,
    kubectl_manifest.api_gateway
  ]
}

resource "kubectl_manifest" "network_policies" {
  yaml_body = file("${path.module}/k8s/network-policies.yaml")
  depends_on = [
    kubectl_manifest.user_service,
    kubectl_manifest.image_service,
    kubectl_manifest.comment_like_service,
    kubectl_manifest.activity_service,
    kubectl_manifest.api_gateway
  ]
}

resource "kubectl_manifest" "backups" {
  yaml_body = file("${path.module}/k8s/backups.yaml")
  depends_on = [
    kubectl_manifest.postgres,
    kubectl_manifest.mongodb,
    kubectl_manifest.redis
  ]
}

output "application_urls" {
  description = "URLs для доступа к приложению"
  value = {
    frontend   = "http://social-network.local:30080"
    api        = "http://social-network.local:30080/api"
    monitoring = "http://monitor.social-network.local:30080"
    grafana    = "http://monitor.social-network.local:30080"
    prometheus = "http://monitor.social-network.local:30080/prometheus"
  }
}

output "database_info" {
  description = "Информация о базах данных"
  value = {
    postgres_host = "postgres.${local.namespaces.db}.svc.cluster.local"
    redis_host    = "redis.${local.namespaces.db}.svc.cluster.local"
    mongodb_host  = "mongodb.${local.namespaces.db}.svc.cluster.local"
    kafka_host    = "kafka.${local.namespaces.db}.svc.cluster.local"
  }
}

output "postgres_password" {
  description = "Пароль PostgreSQL"
  value       = random_password.postgres_password.result
  sensitive   = true
}