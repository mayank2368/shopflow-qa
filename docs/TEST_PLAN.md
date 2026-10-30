# ShopFlow — Test Plan

## 1. Purpose & scope

This document describes the functional end-to-end test strategy for **ShopFlow**,
a small e-commerce web application. The goal is to verify, automatically and
repeatably, that the features a customer and an administrator rely on behave
correctly through the real UI and the underlying API.

In scope:

- Authentication and role-based access (customer vs. admin, locked accounts)
- Product catalogue: listing, search, sorting, category filtering, pagination, detail
- Shopping cart: add, update quantity, remove, badge/count
- Checkout: multi-step wizard, field validation, totals (subtotal + 19% tax), order confirmation
- Account: view and edit profile with validation
- Admin: product create/delete with validation and access control
- API contract for the above (auth, roles, validation, catalogue)

Out of scope: payment processing, email, performance/load (covered by the
separate shop performance-testing project), security scanning, visual
regression.

## 2. Test approach

| Decision | Choice | Why |
|---|---|---|
| Stack | Java 17, Selenium 4, TestNG, Maven | Industry-standard, matches the roles targeted |
| Design | Page Object Model + component objects | Locators live in one place; tests read as behaviour |
| Test data | Reset + seed via REST API before each test | Deterministic, isolated, fast — no leftover state |
| Waiting | Explicit waits only | No `Thread.sleep`, no implicit waits — stable and fast |
| Isolation | Fresh browser (ThreadLocal) per test | Parallel-safe; tests never share session or cookies |
| Flakiness | One automatic retry via `RetryAnalyzer` | Absorbs rare infra blips without hiding real failures |
| Reporting | Allure (steps, severity, screenshots on failure) | Readable results for humans and CI |
| Layers | UI E2E **and** API tests | API tests pin the contract fast; UI tests prove the journey |

### Levels of testing

- **API tests** (`ApiTests`) exercise the backend directly — faster and not
  subject to UI timing. They cover auth, roles, validation and the catalogue.
- **UI end-to-end tests** drive the browser through complete user journeys,
  using the API only to *arrange* state (e.g. seed a cart) so each test spends
  its time on the behaviour under test.

## 3. Environments

| Setting | Local default | CI | Grid |
|---|---|---|---|
| App URL | `http://localhost:5000` | localhost on the runner | `host.docker.internal` |
| Browser | Chrome (headless) | Chrome + Firefox matrix | Chrome/Firefox nodes |
| Driver | Selenium Manager (auto) | Selenium Manager (auto) | RemoteWebDriver |

Configuration precedence: **system property → environment variable →
`config.properties`**. So any value can be overridden on the command line
(`-DbaseUrl=…`, `-Dbrowser=firefox`, `-Dheadless=false`) without editing files.

## 4. Entry / exit criteria

- **Entry:** the app starts and `/api/health` returns 200; the test-data reset
  hook is enabled.
- **Exit (pass):** the smoke suite is green on every push; the regression suite
  is green on both browsers before a release; the Allure report shows no
  unexplained failures.

## 5. Test suites

| Suite | Content | When it runs |
|---|---|---|
| `suites/smoke.xml` | Critical happy paths (group `smoke`) | Every push — fast gate |
| `suites/regression.xml` | All functional tests (group `regression`) | PRs and nightly |
| `suites/parallel.xml` | Regression, 4 methods in parallel | On demand / Grid |

## 6. Test cases

See the `@Test` descriptions in `src/test/java/com/shopflow/tests`. Each test is
tagged with an id (TC-xx for UI, API-xx for API), an Allure `@Story`, and a
`@Severity`. Highlights:

- **Login/roles:** valid login, six invalid/locked variants (data-driven),
  logout, admin sees admin nav, customer does not.
- **Products:** full first page, four sort orders (data-driven) each asserted
  actually sorted, search match + empty state, four category filters
  (data-driven), pagination with no overlap between pages, product detail.
- **Cart:** add from list and from detail, accumulation, seeded cart, quantity
  change updates subtotal, remove empties cart and clears the badge.
- **Checkout:** full purchase returns an `ORD-…` id and empties the cart,
  totals = subtotal + 19% tax, three missing-field validations (data-driven),
  cancel preserves the cart.
- **Account:** edit persists across reload, invalid email rejected, empty name
  rejected.
- **Admin:** create adds a row, delete removes a row, empty name rejected,
  customer is denied the admin area.
- **API:** token on valid login, 403 locked, 401 wrong password, catalogue
  count, category filter, checkout 400 on missing field, 403 for non-admin on
  admin endpoint, 201 for admin.

## 7. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Flaky waits on SPA re-render | Staleness waits on the re-rendered element; explicit waits everywhere |
| Shared state between tests | API reset before each test + fresh browser |
| Driver/browser version drift | Selenium Manager resolves drivers at runtime |
| Timing-only failures masking bugs | Exactly one retry, logged; repeated failures still fail the build |

## 8. Reporting & triage

Allure is generated in CI and published to GitHub Pages. Failures attach a
screenshot and the failing step. Defects are logged using
`docs/BUG_REPORT_TEMPLATE.md`.
