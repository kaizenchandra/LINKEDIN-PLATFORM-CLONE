#!/usr/bin/env bash
# Restarts the local emulator; run after localstack-up and outside the main browser suite.
set -euo pipefail
cd "$(dirname "$0")/../apps/web"
npx playwright test --config playwright.operations.config.ts
