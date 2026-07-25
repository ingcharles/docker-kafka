package com.ecommerce.shipping.domain.model.vo;

import java.util.List;

public record PageResult<T>(List<T> items, long total) {
}
