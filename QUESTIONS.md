# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly.

What are your thoughts on the pros and cons of each approach? Which would you choose and why?

**Answer:**
```txt
OPENAPI-GENERATED APPROACH (Warehouse):
Pros:
- Single source of truth: API contract is explicitly defined and versioned
- Auto-generated documentation via Swagger/OpenAPI tooling
- Strong contract enforcement - changes to spec trigger code regeneration
- Consistency across endpoints - guaranteed structure and naming conventions
- Easy API versioning and evolution - spec changes propagate to generated code
- Better for team coordination - non-developers can understand API contracts
- Client code generation for consumers (SDKs, etc.)
- Reduces human error in endpoint definitions

Cons:
- Generated code is often boilerplate-heavy and hard to customize
- Learning curve for teams unfamiliar with OpenAPI tooling
- Integration complexity - generator must integrate with build process
- Regeneration can overwrite manual customizations (requires discipline)
- Slower iteration during development - update spec, regenerate, test cycle
- Generated code may not match team's coding patterns/preferences
- Tight coupling between spec and implementation

HAND-CODED APPROACH (Product, Store):
Pros:
- Full flexibility and control over implementation details
- Faster iteration - direct code changes without regeneration
- Easier customization for business logic
- No generator overhead or build complexity
- Simpler for small APIs or rapidly changing specifications
- Aligns implementation with team's coding patterns

Cons:
- Manual sync between code and documentation (documentation often trails reality)
- API contract can drift from documentation
- Inconsistent patterns across endpoints without discipline
- Harder for new team members to understand API structure
- Requires hand-written documentation that needs maintenance
- No client SDK generation - clients must write their own integration
- More prone to human error in endpoint design

MY RECOMMENDATION:
I would choose OpenAPI-generated approach for production systems, with these qualifications:

1. **Use OpenAPI as the single source of truth** - your API contract is your most important artifact
2. **Generate server stubs** - use generated interfaces/base classes but implement business logic separately
3. **Version your specifications** - treat API specs like code (git, reviews, etc.)
4. **Use custom templates** - many OpenAPI generators support templates to align generated code with team patterns
5. **Establish regeneration discipline** - periodically regenerate, review changes, and merge carefully

Rationale: As a system grows, the OpenAPI approach scales better. It enables:
- Better documentation consistency
- Easier client integration and SDKs
- Contract-first development (catch API design issues early)
- Team communication through specs
- Reduced API surface drift

For the Warehouse API: The generated approach was the right choice. For Product/Store: Could be refactored to OpenAPI for consistency, especially if these APIs are exposed to external clients or will have versioning requirements.

For initial prototypes or internal APIs with low change frequency: Hand-coded is acceptable, but should transition to OpenAPI-generated as the system matures.
```

---

## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project?

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
TESTING ANALYSIS:
Given this project's domain (order fulfillment, inventory, transactions), I'd prioritize tests in this order:

TIER 1 - CRITICAL (Must Have):
1. **Use Case Unit Tests** (ArchiveWarehouseUseCaseTest, ReplaceWarehouseUseCaseTest, CreateWarehouseUseCase)
   - Why: Business logic is the core value - validation bugs are expensive
   - Coverage: All validations, edge cases, error paths
   - Time investment: Moderate, high ROI
   - These tests are fast, deterministic, and catch logic bugs early

2. **Database Constraint Tests** (WarehouseTestcontainersIT)
   - Why: Unique constraints, foreign keys prevent data corruption
   - Coverage: Unique constraint on businessUnitCode, capacity limits, stock validations
   - Time investment: Low
   - Saves debugging production issues

3. **Happy Path Integration Tests** (WarehouseEndpointIT)
   - Why: Validates end-to-end flow and REST contracts
   - Coverage: Create, Archive, Replace operations via REST
   - Time investment: Medium
   - Ensures API works as designed

TIER 2 - IMPORTANT (Should Have):
4. **Concurrency Tests** (WarehouseConcurrencyIT)
   - Why: Race conditions cause intermittent data corruption
   - Coverage: Concurrent creations with duplicates, optimistic locking
   - Time investment: Medium (complex setup)
   - Prevents hard-to-debug production bugs

5. **Transaction Boundary Tests** (StoreTransactionIntegrationTest, StoreEventObserverTest)
   - Why: Ensures legacy system updates only after commit
   - Coverage: Event firing, transaction lifecycle
   - Time investment: Medium
   - Critical for external integrations

6. **Parameterized Tests**
   - Why: Test multiple scenarios without duplicating test code
   - Coverage: Different location types, capacity ranges, stock levels
   - Time investment: Low setup, high coverage
   - Finds edge cases systematically

TIER 3 - NICE TO HAVE (Time Permitting):
7. **Performance Tests** - Load testing with multiple concurrent operations
8. **Negative Flow Integration Tests** - Error handling via REST (400, 404, 409 responses)
9. **API Contract Tests** - Validate OpenAPI spec matches implementation

MY RECOMMENDED TEST DISTRIBUTION:
- Unit Tests: 50% of effort - Most efficient for catching bugs
- Integration Tests: 35% (happy paths + concurrency)
- Contract/Documentation Tests: 15%

ENSURING EFFECTIVE COVERAGE OVER TIME:

1. **Metrics & Monitoring**
   - Track code coverage (target 80%+ for critical paths, 60%+ overall)
   - Monitor test execution time (alert if growing - indicates parallelization issues)
   - Track flaky test rate (target: <1% failures unrelated to code changes)

2. **Test Maintenance**
   - Schedule monthly review of test patterns (consolidate duplicates)
   - Update tests when business rules change (tests = documentation)
   - Remove tests that consistently pass without adding value

3. **Continuous Improvement**
   - Add tests for each bug discovered (prevent regressions)
   - Don't increase test count blindly - focus on coverage gaps
   - Regular team retrospectives on test effectiveness

4. **Testing in Pipeline**
   - Fast tests (unit) run on every commit
   - Slower tests (integration) run on pull requests
   - Full suite (including concurrency) runs before merge
   - Nightly runs of performance tests

5. **Test Observability**
   - Clear test names describing what and why (e.g., "testDuplicateWarehouseCodeFailsDueToUniqueConstraint")
   - Good assertions with meaningful messages
   - Structure: Arrange → Act → Assert (AAA pattern)

SPECIFIC FOR THIS PROJECT:

Since we're dealing with transactions and concurrency:
1. **Prioritize concurrency tests** - even though they're complex, data corruption is the most expensive failure
2. **Strong integration tests** for transaction boundaries - external integrations often fail in subtle ways
3. **Use testcontainers** for real database testing - mocks won't catch database-specific issues
4. **Focus on business rule validation** - these are the core value and change frequently

ESTIMATED TIME ALLOCATION:
- Archive operation: 30 minutes unit tests + 20 minutes integration
- Replace operation: 30 minutes unit tests + 20 minutes integration
- Create operation (including concurrency): 40 minutes unit + 40 minutes concurrency tests
- Store/Product integration: 30 minutes transaction tests
- Performance baseline: 30 minutes
Total: ~4-5 hours, leaving buffer for issues found during testing

This approach maximizes bug prevention with limited time.
```
