terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "3.5.0"
    }
  }
}

provider "google" {
  credentials = file(var.credentials_file)
  project     = var.project
  region      = "us-central1"
  zone        = "us-central1-c"
}


data "archive_file" "function_archive" {
  type        = "zip"
  source_dir  = "../function/dist"
  output_path = "../dist/function.zip"
}

resource "google_storage_bucket" "bucket" {
  name          = "tototoshi-scala-hello"
  location      = "US"
  storage_class = "STANDARD"
}

resource "google_storage_bucket_object" "packages" {
  name   = "packages/functions.${data.archive_file.function_archive.output_md5}.zip"
  bucket = google_storage_bucket.bucket.name
  source = data.archive_file.function_archive.output_path
}

resource "google_pubsub_topic" "scala_hello_request" {
  name = var.topic_name_request
}

resource "google_pubsub_topic" "scala_hello_response" {
  name = var.topic_name_response
}

resource "google_pubsub_subscription" "scala_hello_response" {
  name  = var.subscription_name_response
  topic = google_pubsub_topic.scala_hello_response.name
}

resource "google_cloudfunctions_function" "function" {
  name                  = "scala-hello"
  description           = "Hello-World in Scala"
  runtime               = "java11"
  source_archive_bucket = google_storage_bucket.bucket.name
  source_archive_object = google_storage_bucket_object.packages.name
  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = google_pubsub_topic.scala_hello_request.name
  }
  entry_point = "com.example.function.Hello"

  environment_variables = {
    GCP_PROJECT_ID                       = var.project
    GCP_CLOUD_PUBSUB_TOPIC_NAME_RESPONSE = var.topic_name_response
  }
}
