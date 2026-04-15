# Job Alerts & Tech Radar

Production-ready GitOps microservices platform. Developers push code; GitHub Actions builds, tests, and publishes images; Argo CD automatically deploys to Kubernetes — no manual `kubectl apply` anywhere in the chain.

## Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                     Application Repo (this repo)                     │
│  job-service  auth-service  notification-service  analytics-service  │
│  frontend                                                            │
└────────────────────────────┬─────────────────────────────────────────┘
                             │ git push → main
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                       GitHub Actions CI                              │
│  1. mvn test / npm test (parallel matrix)                            │
│  2. docker buildx → ghcr.io/<owner>/<service>:<sha>                 │
│  3. clone gitops repo → sed image tag → commit & push               │
└────────────────────────────┬─────────────────────────────────────────┘
                             │ bumps base/<service>/deployment.yaml
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│               GitOps Repo  (job-platform-gitops)                     │
│  base/                                                               │
│  ├── kustomization.yaml                                              │
│  ├── job-service/deployment.yaml   ← image tag bumped by CI         │
│  ├── auth-service/deployment.yaml                                    │
│  ├── notification-service/deployment.yaml                            │
│  ├── analytics-service/deployment.yaml                               │
│  └── frontend/deployment.yaml                                        │
└────────────────────────────┬─────────────────────────────────────────┘
                             │ Argo CD polls / webhook
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│              Argo CD  (auto-sync, prune, selfHeal)                   │
│  kubectl apply -k base/  →  namespace: jobs-platform                 │
└──────────────────────────────────────────────────────────────────────┘
```

## Services

| Service | Stack | Port | Health endpoints |
|---|---|---|---|
| job-service | Spring Boot 3 + Actuator | 8080 | `/actuator/health/liveness` `/actuator/health/readiness` |
| auth-service | Spring Boot 3 + Actuator | 8080 | `/actuator/health/liveness` `/actuator/health/readiness` |
| notification-service | Node.js 22 + Express | 8080 | `/health` `/ready` |
| analytics-service | Node.js 22 + Express | 8080 | `/health` `/ready` |
| frontend | Next.js 15 (standalone) | 3000 | `/` |

## Repositories

| Repo | Purpose |
|---|---|
| `job-platform` (this) | Application source, Dockerfiles, GitHub Actions |
| `job-platform-gitops` | Kubernetes manifests, Kustomize config, Argo CD Application |

## CI/CD Pipeline

### Trigger
Any push to `main` (or manual `workflow_dispatch`).

### Jobs

```
test-java ──┐
            ├──► build-and-push ──► update-gitops
test-node ──┘
```

1. **test-java** — `mvn -B test` for `job-service` and `auth-service` in parallel
2. **test-node** — `npm test` for `notification-service`, `analytics-service`, `frontend` in parallel
3. **build-and-push** — `docker buildx` → GHCR tagged `:${GITHUB_SHA}`. Uses `GITHUB_TOKEN` (no manual registry secret). Docker layer cache stored in GitHub Actions cache per service.
4. **update-gitops** — clones gitops repo, replaces `image:` in each `deployment.yaml` via `sed`, commits and pushes.

### Secrets required

| Secret | Value |
|---|---|
| `GITOPS_REPO` | `owner/job-platform-gitops` |
| `GITOPS_PUSH_USER` | GitHub username used for the gitops commit identity |
| `GITOPS_PUSH_TOKEN` | PAT with `repo` scope on the gitops repo |
| `GITOPS_REPO_BRANCH` | (optional) defaults to `main` |

`GITHUB_TOKEN` is used automatically for GHCR — no additional registry secret needed.

## Kubernetes

**Namespace:** `jobs-platform`  
**Tool:** Kustomize (`base/kustomization.yaml` in the gitops repo)

Every Deployment has:
- `replicas: 2`
- `livenessProbe`, `readinessProbe`, and `startupProbe`
- CPU and memory `requests` and `limits`
- PostgreSQL credentials injected from the `postgres-credentials` Secret

### PostgreSQL env vars

```
POSTGRES_HOST=postgres.postgres.svc.cluster.local
POSTGRES_DB=<jobs|auth>
POSTGRES_USER=<from Secret>
POSTGRES_PASSWORD=<from Secret>
```

**Do not commit real credentials.** Use [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) or [External Secrets Operator](https://external-secrets.io/) in production.

## Argo CD bootstrap

Edit `argocd/application.yaml` in the gitops repo — replace `YOUR_GITHUB_USERNAME` with your GitHub username — then apply once:

```bash
kubectl apply -f argocd/application.yaml
```

After that every CI push triggers an automatic sync. No further manual intervention.

## Image tag format

```
ghcr.io/<github-owner>/<service>:<40-char-git-sha>
```

Example:
```
ghcr.io/myorg/job-service:a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2
```

## Local development

```bash
# Java services
cd job-service && mvn spring-boot:run

# Node services
cd notification-service && npm install && npm start

# Frontend
cd frontend && npm install && npm run dev
```
