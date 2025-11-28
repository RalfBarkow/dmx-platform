# Implementation Plan: DMX Health & dmx-dita Readiness

Goal: make the environment test green by ensuring DMX 5.3.5 reports health and dmx-dita resolves/executes a minimal DITA job.

## Steps
- Verify correct health endpoint and adjust test script if needed (`/dmx/version` vs `/dmx/api/version`); confirm 200 when DMX is up.
- Fix dmx-dita bundle resolution: add/combine Jackson dependencies (com.fasterxml.jackson.core >=2.13,<3) into the runner or embed in the plugin bundle; rebuild and redeploy the bundle.
- Re-run DMX start to confirm dmx-dita activates (no ClassNotFound/BundleException in logs) and its health endpoint responds.
- Execute the minimal DITA run via the env test script; confirm HTML output is produced in `test-output/dita/minimal/out/`.
- Capture results and keep the env test script/paths in sync with any endpoint or payload changes.
