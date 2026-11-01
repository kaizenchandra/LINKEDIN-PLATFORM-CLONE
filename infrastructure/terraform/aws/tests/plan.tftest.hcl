mock_provider "aws" {}
run "offline_aws_plan" {
  command = plan
  variables {
    account_id        = "000000000000"
    region            = "us-east-1"
    media_bucket_name = "meridian-test-plan-media"
  }
}
