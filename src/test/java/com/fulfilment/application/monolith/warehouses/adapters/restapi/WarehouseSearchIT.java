// src/test/java/com/fulfilment/application/monolith/warehouses/adapters/restapi/WarehouseSearchIT.java
package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class WarehouseSearchIT {

    @Inject CreateWarehouseUseCase createWarehouseUseCase;
    @Inject WarehouseRepository    warehouseRepository;

    @BeforeEach
    @Transactional
    public void setup() {
        warehouseRepository.deleteAll();
    }

    // ── seed helper ─────────────────────────────────────────────────────────

    @Transactional
    void seed(String code, String location, int capacity, int stock, boolean archived) {
        Warehouse w = new Warehouse();
        w.businessUnitCode = code;
        w.location         = location;
        w.capacity         = capacity;
        w.stock            = stock;
        w.createdAt        = LocalDateTime.now();
        w.archivedAt       = archived ? LocalDateTime.now() : null;
        warehouseRepository.create(w);
    }

    // ── 1. No filters → returns all active warehouses ───────────────────────

    @Test
    public void testNoFilters_returnsAllActive() {
        seed("W-001", "AMSTERDAM-001", 80, 10, false);
        seed("W-002", "ZWOLLE-001",    30, 5,  false);
        seed("W-003", "AMSTERDAM-001", 60, 20, true);  // archived — must be excluded

        given()
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("totalCount",        is(2))
                .body("warehouses.size()", is(2));
    }

    // ── 2. Filter by location ────────────────────────────────────────────────

    @Test
    public void testFilterByLocation() {
        seed("W-A1", "AMSTERDAM-001", 80, 10, false);
        seed("W-A2", "AMSTERDAM-001", 60, 5,  false);
        seed("W-Z1", "ZWOLLE-001",    30, 3,  false);

        given()
                .queryParam("location", "AMSTERDAM-001")
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("totalCount",        is(2))
                .body("warehouses.size()", is(2))
                .body("warehouses.location", everyItem(is("AMSTERDAM-001")));
    }

    // ── 3. Filter by minCapacity ─────────────────────────────────────────────

    @Test
    public void testFilterByMinCapacity() {
        seed("W-S",  "AMSTERDAM-001", 40, 5,  false);
        seed("W-M",  "AMSTERDAM-001", 70, 10, false);
        seed("W-L",  "AMSTERDAM-001", 90, 20, false);

        given()
                .queryParam("minCapacity", 70)
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("totalCount",        is(2))
                .body("warehouses.capacity", everyItem(greaterThanOrEqualTo(70)));
    }

    // ── 4. Filter by maxCapacity ─────────────────────────────────────────────

    @Test
    public void testFilterByMaxCapacity() {
        seed("W-S", "AMSTERDAM-001", 40, 5,  false);
        seed("W-M", "AMSTERDAM-001", 70, 10, false);
        seed("W-L", "AMSTERDAM-001", 90, 20, false);

        given()
                .queryParam("maxCapacity", 70)
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("totalCount",        is(2))
                .body("warehouses.capacity", everyItem(lessThanOrEqualTo(70)));
    }

    // ── 5. Combined filters (AND logic) ──────────────────────────────────────

    @Test
    public void testCombinedFilters_locationAndCapacityRange() {
        seed("W-A-S", "AMSTERDAM-001", 40,  5,  false);
        seed("W-A-M", "AMSTERDAM-001", 70,  10, false);
        seed("W-A-L", "AMSTERDAM-001", 95,  20, false);
        seed("W-Z-M", "ZWOLLE-001",    35,  3,  false);

        given()
                .queryParam("location",    "AMSTERDAM-001")
                .queryParam("minCapacity", 50)
                .queryParam("maxCapacity", 80)
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("totalCount",        is(1))
                .body("warehouses[0].businessUnitCode", is("W-A-M"));
    }

    // ── 6. Archived warehouses excluded ──────────────────────────────────────

    @Test
    public void testArchivedWarehousesExcluded() {
        seed("W-ACTIVE",   "AMSTERDAM-001", 80, 10, false);
        seed("W-ARCHIVED", "AMSTERDAM-001", 80, 10, true);

        given()
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("totalCount",                       is(1))
                .body("warehouses[0].businessUnitCode",   is("W-ACTIVE"));
    }

    // ── 7. Sort by capacity ascending ────────────────────────────────────────

    @Test
    public void testSortByCapacityAscending() {
        seed("W-C60", "AMSTERDAM-001", 60, 5,  false);
        seed("W-C90", "AMSTERDAM-001", 90, 10, false);
        seed("W-C40", "AMSTERDAM-001", 40, 3,  false);

        given()
                .queryParam("sortBy",    "capacity")
                .queryParam("sortOrder", "asc")
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("warehouses[0].capacity", is(40))
                .body("warehouses[1].capacity", is(60))
                .body("warehouses[2].capacity", is(90));
    }

    // ── 8. Sort by capacity descending ───────────────────────────────────────

    @Test
    public void testSortByCapacityDescending() {
        seed("W-C60", "AMSTERDAM-001", 60, 5,  false);
        seed("W-C90", "AMSTERDAM-001", 90, 10, false);
        seed("W-C40", "AMSTERDAM-001", 40, 3,  false);

        given()
                .queryParam("sortBy",    "capacity")
                .queryParam("sortOrder", "desc")
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("warehouses[0].capacity", is(90))
                .body("warehouses[1].capacity", is(60))
                .body("warehouses[2].capacity", is(40));
    }

    // ── 9. Pagination ─────────────────────────────────────────────────────────

    @Test
    public void testPagination() {
        for (int i = 1; i <= 15; i++) {
            seed("W-PAG-" + i, "AMSTERDAM-001", 40 + i, i, false);
        }

        // page 0, size 5
        given()
                .queryParam("page",     0)
                .queryParam("pageSize", 5)
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("totalCount",        is(15))
                .body("totalPages",        is(3))
                .body("page",              is(0))
                .body("pageSize",          is(5))
                .body("warehouses.size()", is(5));

        // page 1, size 5
        given()
                .queryParam("page",     1)
                .queryParam("pageSize", 5)
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("warehouses.size()", is(5))
                .body("page",              is(1));

        // last page — 5 items
        given()
                .queryParam("page",     2)
                .queryParam("pageSize", 5)
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("warehouses.size()", is(5));
    }

    // ── 10. pageSize capped at 100 ────────────────────────────────────────────

    @Test
    public void testPageSizeCappedAt100() {
        given()
                .queryParam("pageSize", 999)
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("pageSize", is(100));
    }

    // ── 11. Invalid sortBy → 400 ──────────────────────────────────────────────

    @Test
    public void testInvalidSortBy_returns400() {
        given()
                .queryParam("sortBy", "invalidField")
                .when().get("/warehouse/search")
                .then()
                .statusCode(400)
                .body("error", containsString("Invalid sortBy"));
    }

    // ── 12. minCapacity > maxCapacity → 400 ──────────────────────────────────

    @Test
    public void testMinCapacityGreaterThanMax_returns400() {
        given()
                .queryParam("minCapacity", 100)
                .queryParam("maxCapacity", 50)
                .when().get("/warehouse/search")
                .then()
                .statusCode(400)
                .body("error", containsString("minCapacity cannot be greater than maxCapacity"));
    }

    // ── 13. Empty result set ──────────────────────────────────────────────────

    @Test
    public void testNoMatchingWarehouses_returnsEmptyList() {
        seed("W-001", "AMSTERDAM-001", 80, 10, false);

        given()
                .queryParam("location", "TILBURG-001")
                .when().get("/warehouse/search")
                .then()
                .statusCode(200)
                .body("totalCount",        is(0))
                .body("warehouses.size()", is(0))
                .body("totalPages",        is(0));
    }
}
