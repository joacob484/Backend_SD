### Removed Deployment Files

The following deployment files were removed as they were deemed redundant or unused:

1. `deploy.sh`
2. `deploy-zero-downtime.sh`
3. `deploy-bluegreen.sh`

These files were replaced by `.github/workflows/deploy.yml` and `cloudbuild-cloudrun.yaml` for streamlined deployment processes.

If any of these files are still required, please restore them from version control.