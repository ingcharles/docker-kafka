package com.facturacion.inventario.infrastructure.security;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.commons.security.TokenPrincipal;
import com.facturacion.inventario.domain.repository.CategoriaRepository;
import com.facturacion.inventario.domain.repository.ImagenItemRepository;
import com.facturacion.inventario.domain.repository.ItemRepository;

/**
 * Autorización por empresa en la frontera REST: una api-key solo opera sobre ítems (y sus imágenes y kardex) y
 * categorías de su propio RUC. Un recurso de otra empresa responde igual que uno inexistente (404), para no revelar que existe.
 * <p>
 * Está separada de los servicios de aplicación a propósito: ellos no conocen al llamante (también los usan los
 * consumidores de Kafka) y la regla de "quién puede ver qué" queda en un solo lugar.
 */
@ApplicationScoped
public class AccesoEmpresa {

    private final TokenPrincipal tokenPrincipal;
    private final ItemRepository itemRepository;
    private final ImagenItemRepository imagenRepository;
    private final CategoriaRepository categoriaRepository;

    @Inject
    public AccesoEmpresa(TokenPrincipal tokenPrincipal, ItemRepository itemRepository,
            ImagenItemRepository imagenRepository, CategoriaRepository categoriaRepository) {
        this.tokenPrincipal = tokenPrincipal;
        this.itemRepository = itemRepository;
        this.imagenRepository = imagenRepository;
        this.categoriaRepository = categoriaRepository;
    }

    /** Falla con 404 si el ítem no existe o es de otra empresa. */
    @WithSession
    public Uni<Void> exigirItem(UUID itemId) {
        return itemRepository.findById(itemId)
            .onItem().transformToUni(item -> item != null && tokenPrincipal.esDeLaEmpresa(item.getRuc())
                ? Uni.createFrom().voidItem()
                : Uni.createFrom().failure(new NotFoundException("Ítem no encontrado: " + itemId)));
    }

    /** Falla con 404 si la categoría no existe o es de otra empresa. */
    @WithSession
    public Uni<Void> exigirCategoria(UUID categoriaId) {
        return categoriaRepository.findById(categoriaId)
            .onItem().transformToUni(categoria -> categoria != null && tokenPrincipal.esDeLaEmpresa(categoria.getRuc())
                ? Uni.createFrom().voidItem()
                : Uni.createFrom().failure(new NotFoundException("Categoría no encontrada: " + categoriaId)));
    }

    /** Como {@link #exigirCategoria}, pero para operaciones que modifican: además exige rol ADMIN u OPERADOR. */
    public Uni<Void> exigirCategoriaEscritura(UUID categoriaId) {
        tokenPrincipal.exigirEscritura();
        return exigirCategoria(categoriaId);
    }

    /** Como {@link #exigirItem}, pero para operaciones que modifican: además exige rol ADMIN u OPERADOR. */
    public Uni<Void> exigirItemEscritura(UUID itemId) {
        tokenPrincipal.exigirEscritura();
        return exigirItem(itemId);
    }

    /** Como {@link #exigirImagen}, pero para operaciones que modifican: además exige rol ADMIN u OPERADOR. */
    public Uni<Void> exigirImagenEscritura(UUID imagenId) {
        tokenPrincipal.exigirEscritura();
        return exigirImagen(imagenId);
    }

    /** Falla con 404 si la imagen no existe o su ítem es de otra empresa. */
    @WithSession
    public Uni<Void> exigirImagen(UUID imagenId) {
        return imagenRepository.findById(imagenId)
            .onItem().transformToUni(imagen -> imagen == null
                ? Uni.createFrom().failure(new NotFoundException("Imagen no encontrada: " + imagenId))
                : exigirItem(imagen.getItemId())
                    .onFailure(NotFoundException.class)
                    .transform(e -> new NotFoundException("Imagen no encontrada: " + imagenId)));
    }
}
