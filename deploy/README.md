# Deploy README

This folder contains helper scripts and CI configuration for deploying the backend to Cloud Run.

Recommended workflow
1. Produce or identify the image digest you want to deploy (example: `sha256:...`).
2. Run the Cloud Build retag job to copy the digest into the project Artifact Registry with a human-friendly tag:

   gcloud builds submit --config cloudbuild-retag.yaml --substitutions _IMAGE_DIGEST="sha256:...",_TARGET_TAG="deploy-waitfix-<short>"

3. Deploy the tagged image (no-traffic) and run the smoke test in CI. `cloudbuild-ci.yaml` shows a sample pipeline that:
   - deploys a no-traffic revision
   - runs `ci/smoke-actuator.sh` against the service URL
   - on success optionally promotes the revision to traffic

Notes
- There are two Dockerfiles in the repo. Use `Dockerfile.cloudrun` for Cloud Run (it starts the JVM directly). The production `Dockerfile` used for Kubernetes may include the `wait-for-services.sh` entrypoint; keep those separate.
- Secrets: prefer mapping explicit Secret Manager versions in Cloud Run (e.g. `your-secret:5`).
- The Cloud Build configs assume the Cloud Build service account has permissions to read/write Artifact Registry, deploy to Cloud Run, and access Secret Manager.
# Deploy scripts and recommendations

Note about local deploy scripts

The legacy local deploy scripts (`deploy.sh`, `deploy-bluegreen.sh`, `deploy-zero-downtime.sh`) were removed from the repository to reduce maintenance burden and avoid accidental use. These scripts were replaced by CI/CD workflows that run in a controlled environment.

Recommended approaches now:

- Use the GitHub Actions workflow: `.github/workflows/deploy.yml` (recommended for push->deploy flows).
- Use the Cloud Build pipelines: `cloudbuild-ci.yaml`, `cloudbuild-cloudrun.yaml` for Cloud Build deployments.
- For ad-hoc testing or local debugging, build images with `Dockerfile.cloudrun` (or run the application via your IDE/Maven) rather than running the legacy deploy scripts.

If you really need a local helper script for a custom environment, create a new script in `deploy/` and document its intended usage.
