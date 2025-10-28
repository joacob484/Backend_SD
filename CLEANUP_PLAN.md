# Backend Cleanup & Simplification Plan

This file summarizes safe, low-risk cleanup and modernization steps for the backend (`Back/Backend_SD`). The goal is to remove unnecessary checked-in artifacts, simplify Docker images for Cloud Run, and add lightweight CI checks.

Summary of recommended changes (non-breaking, staged):

1) Keep tracked source and remove build outputs (if tracked)
   - Confirm `target/` is ignored (present in `.gitignore` already). If any `target/` files are tracked, remove them using:
     - git rm --cached -r Back/Backend_SD/target/
     - commit and push the change.

2) Keep Maven wrapper but avoid large artifacts in repo
   - `.mvn/wrapper/maven-wrapper.jar` is intentionally tracked for reproducible builds. Keep it.

3) Prefer `Dockerfile.cloudrun` for Cloud Run builds
   - `Dockerfile.cloudrun` builds the JAR and launches `java -jar app.jar` without the `wait-for-services.sh` wrapper. Use this for Cloud Run to avoid startup-probe issues and to let Cloud Run healthchecks drive readiness.
   - Update CI/build steps to use `Dockerfile.cloudrun` when deploying to Cloud Run.

4) Keep `wait-for-services.sh` but simplify and use only for local / VM deployments
   - The `wait-for-services.sh` is useful for VM/docker-compose deployments but can block Cloud Run. Keep it, but ensure Cloud Run deploys use `Dockerfile.cloudrun`.

5) Add/verify `.dockerignore` and `.gitignore`
   - The repository already includes a good `.gitignore` and `.dockerignore`. Ensure your CI image builds use those and that image contexts are small.

6) Move obsolete deploy scripts to a `deploy/` folder (non-breaking)
   - Create `deploy/` and move `deploy-bluegreen.sh`, `deploy-zero-downtime.sh`, `deploy.sh` there, or archive them with a short README linking to the canonical deploy doc.

7) Add a simple smoke test for CI
   - Add a quick script `ci/smoke-actuator.sh` that calls `/actuator/health` after deploy. Wire it as a post-deploy step in CI (Cloud Build or GitHub Actions).

8) Run a build and tests in CI
   - Add a `cloudbuild-ci.yaml` that runs `mvn -DskipTests package` (or `mvn -DskipTests verify`), and run unit tests on PRs.

9) Frontend notes
   - The frontend `Front/FaltaUnoFront` looks healthy. Ensure `.dockerignore` excludes `node_modules` and `.next/` (already present).

Safe immediate actions I can take now (non-destructive):
- Create `deploy/README.md` describing deploy scripts and recommended usage.
- Create `ci/smoke-actuator.sh` and a sample Cloud Build step that runs it after a no-traffic deploy.
- Create a PR branch with `.github/workflows/ci.yml` or `cloudbuild-ci.yaml` (if you want CI changes applied).

If you want, I'll perform the following next steps automatically:
- Add `deploy/README.md` and move nothing (just create the README listing the existing deploy scripts and recommended migration). This is non-invasive.
- Add `ci/smoke-actuator.sh` in `Back/Backend_SD/ci/` and a suggested `cloudbuild-ci.yaml` in root.

Which immediate actions should I perform now? If you want me to apply changes, I can create the small helper files and a PR-ready branch, or just add the files here for you to review and commit.
