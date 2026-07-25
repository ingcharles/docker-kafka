package com.ecommerce.inventory.domain.repository;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

import com.ecommerce.inventory.domain.model.entity.Product;
import com.ecommerce.inventory.domain.model.vo.PageResult;

/**
 * Puerto (interfaz) para la persistencia de Product, siguiendo arquitectura
 * hexagonal. La implementación concreta (el adaptador) vive en
 * {@code infrastructure.persistence}, usando Panache.
 */
public interface ProductRepository {

    Uni<Product> persist(Product product);

    Uni<Product> findById(UUID id);

    Uni<PageResult<Product>> findPaged(int page, int limit);

    /**
     * Mueve de forma atómica la {@code quantity} indicada, de "disponible"
     * a "reservado", con un único {@code UPDATE} condicional
     * (con la cláusula {@code WHERE available_quantity >= quantity}) — esta
     * es la forma segura ante concurrencia de "verificar y reservar" sin que
     * dos órdenes distintas puedan entrar en una condición de carrera
     * compitiendo por el mismo stock. Es la base misma de por qué este
     * diseño no necesita ningún bloqueo (lock) manual a nivel de aplicación.
     *
     * @return true si la reserva se aplicó con éxito, false si no había stock suficiente
     */
    Uni<Boolean> tryReserve(UUID productId, int quantity);

    /** Mueve de forma atómica la {@code quantity} indicada, de vuelta de "reservado" a "disponible". */
    Uni<Void> release(UUID productId, int quantity);
}
