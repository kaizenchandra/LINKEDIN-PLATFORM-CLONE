#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v terraform >/dev/null || { echo 'Install Terraform >=1.10 first.' >&2; exit 1; }
if [[ ! -f .env ]]; then cp .env.example .env; chmod 600 .env; fi
compose=(docker compose --ansi never --progress plain --env-file .env -f infrastructure/local/compose.yml -f infrastructure/local/compose.media.yml --profile app "$@")
"${compose[@]}" up -d --wait --wait-timeout 180 localstack
terraform -chdir=infrastructure/terraform/localstack init -input=false
terraform -chdir=infrastructure/terraform/localstack validate
terraform -chdir=infrastructure/terraform/localstack apply -input=false -auto-approve
"${compose[@]}" up -d --build --wait --wait-timeout 180
printf 'Meridian with LocalStack S3 ready at http://localhost:3100\n'
