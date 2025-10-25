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

  images = {
    user_service         = "pyrodocker1/user-service:${var.app_version}"
    image_service        = "pyrodocker1/image-service:${var.app_version}"
    comment_like_service = "pyrodocker1/comment-like-service:${var.app_version}"
    activity_service     = "pyrodocker1/social-activity-service:${var.app_version}"
    api_gateway          = "pyrodocker1/api-gateway:${var.app_version}"
    frontend             = "pyrodocker1/photo-sharing-frontend:${var.app_version}"
  }
}

resource "kubernetes_namespace" "social_network" {
  for_each = local.namespaces

  metadata {
    name = each.value
    labels = {
      name = each.value
      environment = var.environment
    }
  }
}

resource "random_password" "postgres_password" {
  length  = 16
  special = false
}

resource "kubernetes_secret" "postgres_credentials" {
  metadata {
    name      = "postgres-credentials"
    namespace = kubernetes_namespace.social_network["db"].metadata[0].name
  }

  data = {
    username = base64encode("postgres")
    password = base64encode(random_password.postgres_password.result)
  }
}

resource "kubernetes_secret" "aws_credentials" {
  metadata {
    name      = "aws-credentials"
    namespace = kubernetes_namespace.social_network["db"].metadata[0].name
  }

  data = {
    access-key = base64encode("test")
    secret-key = base64encode("test")
  }
}

resource "kubernetes_secret" "kafka_credentials" {
  metadata {
    name      = "kafka-credentials"
    namespace = kubernetes_namespace.social_network["db"].metadata[0].name
  }

  data = {
    broker-id = base64encode("1")
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
  yaml_body = templatefile("${path.module}/k8s/postgres-ss.yaml", {
    namespace = local.namespaces.db
  })
  depends_on = [kubectl_manifest.namespaces, kubernetes_secret.postgres_credentials]
}

resource "kubectl_manifest" "mongodb" {
  yaml_body = templatefile("${path.module}/k8s/mongodb-ss.yaml", {
    namespace = local.namespaces.db
  })
  depends_on = [kubectl_manifest.namespaces]
}

resource "kubectl_manifest" "redis" {
  yaml_body = templatefile("${path.module}/k8s/redis-deployment.yaml", {
    namespace = local.namespaces.db
  })
  depends_on = [kubectl_manifest.namespaces]
}

resource "kubectl_manifest" "kafka" {
  yaml_body = templatefile("${path.module}/k8s/kafka-deployment.yaml", {
    namespace = local.namespaces.db
  })
  depends_on = [kubectl_manifest.namespaces, kubernetes_secret.kafka_credentials]
}

resource "kubectl_manifest" "localstack" {
  yaml_body = templatefile("${path.module}/k8s/localstack-deployment.yaml", {
    namespace = local.namespaces.db
  })
  depends_on = [kubectl_manifest.namespaces]
}

resource "kubectl_manifest" "user_service" {
  yaml_body = templatefile("${path.module}/k8s/user-service-deployment.yaml", {
    namespace = local.namespaces.users
    image     = local.images.user_service
  })
  depends_on = [kubectl_manifest.postgres, kubectl_manifest.redis]
}

resource "kubectl_manifest" "image_service" {
  yaml_body = templatefile("${path.module}/k8s/image-service-deployment.yaml", {
    namespace = local.namespaces.images
    image     = local.images.image_service
  })
  depends_on = [kubectl_manifest.postgres, kubectl_manifest.redis, kubectl_manifest.localstack]
}

resource "kubectl_manifest" "comment_like_service" {
  yaml_body = templatefile("${path.module}/k8s/comment-like-service-deployment.yaml", {
    namespace = local.namespaces.comments
    image     = local.images.comment_like_service
  })
  depends_on = [kubectl_manifest.postgres, kubectl_manifest.redis, kubectl_manifest.kafka]
}

resource "kubectl_manifest" "activity_service" {
  yaml_body = templatefile("${path.module}/k8s/activity-service-deployment.yaml", {
    namespace = local.namespaces.activities
    image     = local.images.activity_service
  })
  depends_on = [kubectl_manifest.mongodb, kubectl_manifest.kafka]
}

resource "kubectl_manifest" "api_gateway" {
  yaml_body = templatefile("${path.module}/k8s/api-gateway-deployment.yaml", {
    namespace = local.namespaces.gateway
    image     = local.images.api_gateway
  })
  depends_on = [
    kubectl_manifest.user_service,
    kubectl_manifest.image_service,
    kubectl_manifest.comment_like_service,
    kubectl_manifest.activity_service
  ]
}

resource "kubectl_manifest" "frontend" {
  yaml_body = templatefile("${path.module}/k8s/front-deployment.yaml", {
    namespace = local.namespaces.gateway
    image     = local.images.frontend
  })
  depends_on = [kubectl_manifest.api_gateway]
}

resource "kubectl_manifest" "prometheus" {
  yaml_body = templatefile("${path.module}/k8s/prometheus-deployment.yaml", {
    namespace = local.namespaces.monitoring
  })
  depends_on = [kubectl_manifest.namespaces]
}

resource "kubectl_manifest" "grafana" {
  yaml_body = templatefile("${path.module}/k8s/grafana-deployment.yaml", {
    namespace = local.namespaces.monitoring
  })
  depends_on = [kubectl_manifest.prometheus]
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

resource "helm_release" "nginx_ingress" {
  name       = "nginx-ingress"
  repository = "https://kubernetes.github.io/ingress-nginx"
  chart      = "ingress-nginx"
  version    = "4.8.3"
  namespace  = "ingress-nginx"

  create_namespace = true

  set {
    name  = "controller.service.type"
    value = "LoadBalancer"
  }
}

resource "kubectl_manifest" "ingress" {
  yaml_body = templatefile("${path.module}/k8s/social-service-ingress.yaml", {
    gateway_namespace    = local.namespaces.gateway
    monitoring_namespace = local.namespaces.monitoring
  })
  depends_on = [helm_release.nginx_ingress, kubectl_manifest.api_gateway, kubectl_manifest.grafana]
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

resource "kubectl_manifest" "backups" {
  yaml_body = templatefile("${path.module}/k8s/backups.yaml", {
    namespace = local.namespaces.db
  })
  depends_on = [kubectl_manifest.postgres, kubectl_manifest.mongodb, kubectl_manifest.redis]
}

output "application_urls" {
  description = "URLs для доступа к приложению"
  value = {
    frontend   = "http://social-network.local"
    api        = "http://social-network.local/api"
    monitoring = "http://monitor.social-network.local"
    grafana    = "http://monitor.social-network.local"
    prometheus = "http://monitor.social-network.local/prometheus"
  }
}

output "database_info" {
  description = "Информация о базах данных"
  value = {
    postgres_host = "postgres.${local.namespaces.db}"
    redis_host    = "redis.${local.namespaces.db}"
    mongodb_host  = "mongodb.${local.namespaces.db}"
    kafka_host    = "kafka.${local.namespaces.db}"
  }
  sensitive = false
}

output "namespaces" {
  description = "Созданные неймспейсы"
  value       = local.namespaces
}