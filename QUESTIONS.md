# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Standardize on Repository + UseCase/Service and stop using Panache Active Record directly in resources.
Reason: Right now StoreResource uses Store.findById/listAll/persist directly, while products/warehouses use repositories. Mixing patterns makes behavior, testing, and transaction boundaries inconsistent.

Separate persistence entities from domain models everywhere.
Reason: Warehouses already map DbWarehouse -> domain Warehouse, but stores/products expose JPA entities directly. Consistent mapping avoids leaking DB concerns into API/use cases and makes schema changes safer.

Keep domain rules out of adapters/resources.
Reason: Validation/behavior should live in use cases; resources should map HTTP to commands/results only. This reduces duplication and makes unit testing much easier.

Introduce explicit repository query methods for business checks.
Reason: Current warehouse checks often load all rows and filter in memory. Purpose-built queries (counts, exists) are clearer and scale better.

Make write operations uniformly transactional at the application layer.
Reason: Consistent transaction handling across modules prevents subtle differences in flush/commit timing and integration side effects.

Effect: lower maintenance cost, easier testing/mocking, clearer architecture boundaries, and more predictable behavior.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
OpenAPI-first pros:

1. Contract is explicit and reviewable before implementation.
2. Better client generation and cross-team integration.
3. Reduces drift between docs and runtime API.
4. Good for governance/versioning.

OpenAPI-first cons:

1. Extra tooling and generation complexity.
2. Generated code can feel rigid/noisy.
3. Quick internal changes can be slower.

Code-first pros:

1. Faster iteration and simpler local development.
2. Less ceremony for small/internal endpoints.
3. Direct control over framework idioms.

Code-first cons:

1. Easy for implementation and docs to diverge.
2. Weaker contract discipline for consumers.
3. Harder to standardize error models and conventions.

My choice:

1. Use OpenAPI-first for public/integration-facing APIs.
2. Allow code-first for internal CRUD endpoints with low integration risk.
3. In this project, I’d align all three (Warehouse, Product, Store) to the same approach for consistency, preferably OpenAPI-first if these endpoints are consumed outside the service.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Prioritize by risk, not by layer.
First test the business-critical rules: warehouse create/replace/archive validations and store post-commit legacy sync timing. Those are most likely to regress and highest impact.

Focus on two test types first.

Fast unit tests for use cases (validation branches, constraint limits, replacement/archive rules).
Integration/API tests for endpoint behavior + persistence + transaction boundaries (HTTP status, payloads, DB effects).
Add a small contract test set for Warehouse OpenAPI.
Verify key request/response/error shapes match the spec to prevent drift between generated API and implementation.

Keep E2E tests minimal.
Only a few happy-path smoke tests across modules; avoid heavy full-stack suites as primary coverage due to cost/slowness.

Make coverage effective over time.

Gate PRs on critical-path tests.
Track mutation/branch coverage on use-case package, not just line coverage.
Add regression tests for every production bug.
Keep fixtures deterministic and small (import.sql + focused test data builders).
Run fast tests on every commit, slower integration suite in CI pipeline stages/nightly.
If I had to sequence quickly:

Warehouse use-case unit tests.
Warehouse endpoint integration tests.
Store transaction/after-commit integration tests.
Product CRUD regression tests.
```