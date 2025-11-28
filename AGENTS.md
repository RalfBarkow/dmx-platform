# Repository Guidelines

## Project Structure & Module Organization
- `modules/` contains the OSGi Java backend. Core platform pieces live here (`dmx-core`, `dmx-storage-neo4j`, `dmx-webservice`, etc.), along with optional plugins. Tests sit beside modules in `src/test/java`.
- `modules/dmx-webclient/src/main/js` holds the Vue 2 frontend; built assets land in `modules/dmx-webclient/src/main/resources`.
- `modules-external/` tracks external plugins as git submodules. Update via `git submodule update --remote` when aligning with upstream.
- `modules/dmx-distribution` assembles the runnable distribution; use `bundle-dev` to hot-deploy custom plugins during development. Local Neo4j data sits in `dmx-db/` and should stay uncommitted.

## Build, Test, and Development Commands
- Backend build: `mvn clean install` at repo root builds all modules and the distribution. Add `-DskipTests` when iterating quickly.
- Backend tests only: `mvn -Pall test -pl modules/dmx-test -am` runs the core suite; use `-f modules/dmx-test/pom.xml` if you prefer a single-module invocation without profiles.
- Frontend dev: from `modules/dmx-webclient`, run `npm install` once, then `npm run dev` for the webpack dev server (hot reload). `npm run build` produces production assets.
- Running the stack: after building, launch the packaged app via the platform script for your OS under `modules/dmx-distribution/script/` (e.g., `./dmx-mac.command`).

## Coding Style & Naming Conventions
- Java: 4-space indentation, braces on same line, and package prefix `systems.dmx`. Keep constants upper snake case and align with existing naming (`dmx.core.*`).
- Vue/JS: 2-space indentation, prefer single quotes, and PascalCase component filenames. Keep the `dmx-*` prefix for shared components and modules.
- Linting: ESLint is available; run `npx eslint modules/dmx-webclient/src/main/js` before committing frontend changes.

## Testing Guidelines
- Primary tests are JUnit-based under `modules/*/src/test/java`. Add focused module tests rather than relying on cross-module coupling.
- Name tests after the class under test (e.g., `TopicServiceTest`) and cover service and plugin lifecycle behaviors.
- For frontend work, add targeted unit or snapshot coverage where feasible, and manually verify critical flows (authentication, topic/association editing) before submitting.

## Commit & Pull Request Guidelines
- Follow the observed conventional style: `feat:`, `fix:`, `chore:` with an optional scope per module (e.g., `fix(dmx-webservice): handle empty payload`). Use the imperative mood and keep subjects under ~72 chars.
- PRs should include: a short summary, the primary module(s) touched, screenshots for UI changes, and test/build commands executed.
- Do not commit build outputs, local config, or data directories (`dmx-db/`, `target/`, `node_modules/`, `resources-build/`). Keep secrets out of config files; use local overrides instead.
