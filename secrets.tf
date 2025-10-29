resource "random_password" "postgres_password" {
  length  = 16
  special = false
}

resource "random_password" "redis_password" {
  length = 16
  special = false
}

resource "random_password" "mongodb_password" {
  length = 16
  special = false
}

resource "kubernetes_secret" "postgres_credentials" {
  metadata {
    name      = "postgres-credentials"
    namespace = "social-network-db"
  }

  data = {
    username = "postgres"
    password = random_password.postgres_password.result
  }

  depends_on = [kubectl_manifest.namespaces]
}

resource "kubernetes_secret" "redis_credentials" {
  metadata {
    name      = "redis-credentials"
    namespace = "social-network-db"
  }

  data = {
    password = random_password.redis_password.result
  }

  depends_on = [kubectl_manifest.namespaces]
}

resource "kubernetes_secret" "mongodb_credentials" {
  metadata {
    name      = "mongodb-credentials"
    namespace = "social-network-db"
  }

  data = {
    username = "admin"
    password = random_password.mongodb_password.result
    database = "activity_db"  }

  depends_on = [kubectl_manifest.namespaces]
}

resource "kubernetes_secret" "s3_credentials" {
  metadata {
    name      = "aws-credentials"
    namespace = "social-network-db"
  }

  data = {
    access-key = "test"
    secret-key = "test"
  }

  depends_on = [kubectl_manifest.namespaces]
}

resource "kubernetes_secret" "kafka_credentials" {
  metadata {
    name      = "kafka-credentials"
    namespace = "social-network-db"
  }

  data = {
    broker-id = "1"
  }

  depends_on = [kubectl_manifest.namespaces]
}