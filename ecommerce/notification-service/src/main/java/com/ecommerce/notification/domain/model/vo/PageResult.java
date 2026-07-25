package com.ecommerce.notification.domain.model.vo;

import java.util.List;

public record PageResult<T>(List<T> items, long total) {
}
