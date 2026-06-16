package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class StoreTransactionUpdateTest {

    @InjectMock
    LegacyStoreManagerGateway legacyGateway;

    @BeforeEach
    @Transactional
    public void cleanUp() {
        Store.deleteAll();
    }

    @Test
    public void testLegacySystemNotifiedAfterSuccessfulUpdate() throws InterruptedException {
        Mockito.reset(legacyGateway);

        // Create a store first
        int id = given()
                .contentType("application/json")
                .body("{\"name\": \"TxUpdateStore\", \"quantityProductsInStock\": 5}")
                .when().post("/store")
                .then()
                .statusCode(201)
                .extract().path("id");

        Mockito.reset(legacyGateway); // reset after create, focus on update

        // Perform the update
        given()
                .contentType("application/json")
                .body("{\"name\": \"TxUpdateStore Updated\", \"quantityProductsInStock\": 50}")
                .when().put("/store/" + id)
                .then()
                .statusCode(200);

        Thread.sleep(1000);

        verify(legacyGateway, times(1)).updateStoreOnLegacySystem(any(Store.class));
    }

    @Test
    public void testLegacySystemNotNotifiedAfterFailedUpdate() throws InterruptedException {
        Mockito.reset(legacyGateway);

        // Attempt to update a non-existent store
        given()
                .contentType("application/json")
                .body("{\"name\": \"Ghost\", \"quantityProductsInStock\": 1}")
                .when().put("/store/99999")
                .then()
                .statusCode(404);

        Thread.sleep(500);

        verify(legacyGateway, never()).updateStoreOnLegacySystem(any(Store.class));
    }
}
