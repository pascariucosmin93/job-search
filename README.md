# Job Alerts & Tech Radar

GitOps-based microservices platform for collecting job alerts, authenticating users, sending notifications, and surfacing a tech radar dashboard.

## Services

- `job-service`: Spring Boot service exposing job alert data
- `auth-service`: Spring Boot service exposing auth status endpoints
- `notification-service`: Node.js service for notification delivery status
- `analytics-service`: Node.js service for technology radar metrics
- `frontend`: Next.js UI consuming the platform APIs

## Delivery Flow

1. Push to `main`
2. GitHub Actions runs tests and builds images
3. Images are pushed to GHCR with `${GITHUB_SHA}` tags
4. Workflow updates image tags in the separate GitOps repository
5. Argo CD detects the GitOps commit and syncs Kubernetes automatically

## Required GitHub Secrets

- `GHCR_USERNAME`
- `GHCR_TOKEN`
- `GITOPS_REPO`
- `GITOPS_REPO_TOKEN`
- `GITOPS_REPO_BRANCH` (optional, defaults to `main`)

## GitOps Repo

Kubernetes manifests and the Argo CD application are kept separately in `/home/cosmin/job-search-gitops` in this workspace. In GitHub Actions, `GITOPS_REPO` should point to the remote GitOps repository, and the `update-gitops` job rewrites each `deployment.yaml` image line to the new `${GITHUB_SHA}` tag before commit and push.
