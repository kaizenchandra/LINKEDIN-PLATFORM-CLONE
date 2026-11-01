mock_provider "aws" {}
run "private_media" {
  command = plan
  variables { bucket_name = "meridian-test-media" }
  assert {
    condition     = aws_s3_bucket_public_access_block.media.block_public_acls && aws_s3_bucket_public_access_block.media.block_public_policy && aws_s3_bucket_public_access_block.media.ignore_public_acls && aws_s3_bucket_public_access_block.media.restrict_public_buckets
    error_message = "All public access controls must remain enabled."
  }
  assert {
    condition     = one(aws_s3_bucket_ownership_controls.media.rule).object_ownership == "BucketOwnerEnforced"
    error_message = "Object ACLs must be disabled."
  }
  assert {
    condition     = one(one(aws_s3_bucket_server_side_encryption_configuration.media.rule).apply_server_side_encryption_by_default).sse_algorithm == "AES256"
    error_message = "Default encryption must be configured."
  }
  assert {
    condition     = length(aws_s3_bucket_policy.tls) == 1 && !aws_s3_bucket.media.force_destroy
    error_message = "Production defaults require TLS and must protect non-empty buckets from destruction."
  }
}
run "local_emulator" {
  command = plan
  variables {
    bucket_name = "meridian-test-local-media"
    require_tls = false
  }
  assert {
    condition     = length(aws_s3_bucket_policy.tls) == 0 && aws_s3_bucket_public_access_block.media.block_public_policy
    error_message = "Local HTTP exception must not enable public access."
  }
}
