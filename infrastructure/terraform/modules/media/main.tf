terraform {
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.69.1" }
  }
}
variable "bucket_name" { type = string }
variable "require_tls" {
  type    = bool
  default = true
}
resource "aws_s3_bucket" "media" {
  bucket        = var.bucket_name
  force_destroy = false
  tags          = { Application = "meridian", DataClass = "private-media" }
}
resource "aws_s3_bucket_public_access_block" "media" {
  bucket                  = aws_s3_bucket.media.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
resource "aws_s3_bucket_ownership_controls" "media" {
  bucket = aws_s3_bucket.media.id
  rule { object_ownership = "BucketOwnerEnforced" }
}
resource "aws_s3_bucket_server_side_encryption_configuration" "media" {
  bucket = aws_s3_bucket.media.id
  rule {
    apply_server_side_encryption_by_default { sse_algorithm = "AES256" }
  }
}
# Objects use immutable UUID keys. No historical versions are retained after garbage collection.
resource "aws_s3_bucket_lifecycle_configuration" "media" {
  bucket = aws_s3_bucket.media.id
  rule {
    id     = "abort-incomplete-uploads"
    status = "Enabled"
    filter { prefix = "profiles/" }
    abort_incomplete_multipart_upload { days_after_initiation = 1 }
  }
}
resource "aws_s3_bucket_policy" "tls" {
  count  = var.require_tls ? 1 : 0
  bucket = aws_s3_bucket.media.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [{
    Sid       = "DenyInsecureTransport", Effect = "Deny", Principal = "*", Action = "s3:*",
    Resource  = [aws_s3_bucket.media.arn, "${aws_s3_bucket.media.arn}/*"],
    Condition = { Bool = { "aws:SecureTransport" = "false" } }
  }] })
}
output "bucket_name" { value = aws_s3_bucket.media.id }
output "workload_policy" {
  description = "Attach to the backend workload role; no IAM user or static keys are created."
  value = jsonencode({ Version = "2012-10-17", Statement = [{
    Effect   = "Allow", Action = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"],
    Resource = "${aws_s3_bucket.media.arn}/profiles/*"
  }] })
}
