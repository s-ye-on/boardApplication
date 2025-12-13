# Repository Guidelines
- 모든 답변은 한국어로 한다.
- 코드 수정 시 항상 나에게 먼저 묻고 수정한다.
- 코드를 어떻게 수정할건지 항상 먼저 보여줄 것.

## Project Structure & Module Organization
- Backend is Spring Boot (Java 21) with Gradle wrapper. Main code lives in `src/main/java/me/boardApp` with domain-focused packages (`domain`, `auth`, `authorization`, `filter`, `idempotency`, `config`, `global`, `temp`).
- Web assets: Thymeleaf templates in `src/main/resources/templates`, static content in `src/main/resources/static`. Application config is `src/main/resources/application.yml`. Architecture and security notes are under `src/main/resources/*.md`.
- Tests are in `src/test/java/me/boardApp`, mirrored by domain (`board`, `post`, `comment`, `user`, `domain`).

## Build, Test, and Development Commands
- `./gradlew bootRun` — start the app locally with H2 runtime; reload config via `application.yml`.
- `./gradlew clean build` — full compile, run unit/integration tests, and produce the executable JAR in `build/libs`.
- `./gradlew test` — run the test suite only; prefer before PRs for faster feedback.
- `./gradlew check` — run validation tasks (linting from plugins, tests).

## Coding Style & Naming Conventions
- Use Java 21 features where helpful (records, sealed types). Prefer Spring idioms and constructor injection.
- Indentation: 4 spaces; package names lowercase; classes UpperCamelCase; methods/fields lowerCamelCase; constants UPPER_SNAKE_CASE.
- DTOs: requests live in shared `global` request hierarchy; responses stay within their domain package. Map entities to responses via static factory methods.
- Lombok is available; keep it for boilerplate only. Align with existing security/JWT utilities under `auth` and `authorization`.

## Testing Guidelines
- Framework: JUnit 5 via `spring-boot-starter-test`; security tests can use `spring-security-test`.
- Mirror production package structure under `src/test/java/me/boardApp/...`. Name test classes with `*Test`.
- Aim for scenario-focused tests around JWT auth, authorization filters, and domain services. Add integration tests for controller flows when touching request/response contracts.
- Prefer H2-backed tests; keep external dependencies stubbed or mocked.

## Commit & Pull Request Guidelines
- Follow existing Conventional Commit style (`feat: ...`, `fix: ...`, `chore: ...`, `docs: ...`). Keep messages imperative and scoped.
- Before opening a PR: run `./gradlew test`, ensure new endpoints/configuration are documented in `src/main/resources/*.md` if applicable, and update templates/static assets when UI changes.
- PRs should include a brief summary, testing notes, and linked issues. Screenshots or request/response examples help for API or UI work.

## Security & Configuration Tips
- Secrets stay out of `application.yml`; rely on environment variables or a local override file. Do not commit tokens or keys.
- JWT-related changes should mirror existing patterns in `auth` and `authorization`, including expiration handling noted in recent commits. Keep filters and exception handling aligned with global config.
