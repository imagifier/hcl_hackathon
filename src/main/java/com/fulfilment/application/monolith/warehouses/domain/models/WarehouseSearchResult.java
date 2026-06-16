// src/main/java/com/fulfilment/application/monolith/warehouses/domain/models/WarehouseSearchResult.java
package com.fulfilment.application.monolith.warehouses.domain.models;

import java.util.List;

public class WarehouseSearchResult {

    public List<Warehouse> warehouses;
    public long totalCount;
    public int  page;
    public int  pageSize;
    public int  totalPages;

    public WarehouseSearchResult(List<Warehouse> warehouses, long totalCount,
                                 int page, int pageSize) {
        this.warehouses  = warehouses;
        this.totalCount  = totalCount;
        this.page        = page;
        this.pageSize    = pageSize;
        this.totalPages  = pageSize > 0 ? (int) Math.ceil((double) totalCount / pageSize) : 0;
    }

}
