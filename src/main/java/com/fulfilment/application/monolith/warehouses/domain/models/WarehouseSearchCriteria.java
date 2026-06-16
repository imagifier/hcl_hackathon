// src/main/java/com/fulfilment/application/monolith/warehouses/domain/models/WarehouseSearchCriteria.java
package com.fulfilment.application.monolith.warehouses.domain.models;

public class WarehouseSearchCriteria {

    public String location;
    public Integer minCapacity;
    public Integer maxCapacity;
    public String sortBy     = "createdAt";  // default
    public String sortOrder  = "asc";        // default
    public int    page       = 0;            // 0-indexed
    public int    pageSize   = 10;           // default

    // Clamp pageSize to allowed max
    public int effectivePageSize() {
        return Math.min(pageSize, 100);
    }
}
