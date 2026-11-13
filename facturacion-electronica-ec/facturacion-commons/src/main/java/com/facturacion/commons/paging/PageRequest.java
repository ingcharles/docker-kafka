package com.facturacion.commons.paging;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

/** Parámetros de paginación de los listados ({@code ?page=0&limit=20}, máximo 200 por página). */
public class PageRequest {

    @Min(0)
    @QueryParam("page")
    @DefaultValue("0")
    private int page;

    @Min(1)
    @Max(200)
    @QueryParam("limit")
    @DefaultValue("20")
    private int limit;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
