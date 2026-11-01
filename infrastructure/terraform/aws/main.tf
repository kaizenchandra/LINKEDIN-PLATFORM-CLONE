terraform {
  required_version = ">= 1.10, < 2.0"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.69.1" }
  }
}
variable "account_id" {
  type = string
  validation {
    condition     = can(regex("^[0-9]{12}$", var.account_id))
    error_message = "Specify the explicitly authorized AWS account ID."
  }
}
variable "region" { type = string }
variable "media_bucket_name" { type = string }
provider "aws" {
  region              = var.region
  allowed_account_ids = [var.account_id]
  # Uses the standard credential chain/workload identity. Never use local dummy credentials here.
}
module "media" {
  source      = "../modules/media"
  bucket_name = var.media_bucket_name
}
output "media_bucket" { value = module.media.bucket_name }
output "backend_workload_policy" { value = module.media.workload_policy }
