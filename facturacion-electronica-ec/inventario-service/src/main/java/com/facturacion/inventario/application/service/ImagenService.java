package com.facturacion.inventario.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.inventario.application.dto.ImagenItemResponse;
import com.facturacion.inventario.application.mapper.ImagenItemResponseMapper;
import com.facturacion.inventario.domain.model.entity.ImagenItem;
import com.facturacion.inventario.domain.repository.ImagenItemRepository;
import com.facturacion.inventario.domain.repository.ItemRepository;
import com.facturacion.inventario.shared.exception.ImagenInvalidaException;

/**
 * Imágenes de productos y servicios. El contenido se guarda directamente en
 * Postgres ({@code bytea}) — decisión deliberada para esta versión: no
 * requiere levantar un componente de object storage nuevo (MinIO/S3) en el
 * stack, y para un catálogo de tamaño moderado (decenas o pocos cientos de
 * ítems, unas cuantas imágenes cada uno, cada una acotada a
 * {@link #TAMANO_MAXIMO_BYTES}) el costo en la base de datos es asumible.
 * <p>
 * <b>Cuándo migrar a object storage:</b> si el catálogo crece a miles de
 * ítems con múltiples imágenes de alta resolución cada uno, el tamaño de la
 * base de datos y el tiempo de los backups empiezan a dominar — en ese
 * punto, la migración natural es reemplazar la columna {@code contenido}
 * por una referencia (bucket + clave) a MinIO/S3, manteniendo esta misma
 * tabla como índice de metadata. El diseño ya está preparado para ese
 * cambio sin romper la API pública: {@link ImagenItemResponse} nunca
 * expone el contenido — solo metadata — así que el contrato REST no
 * cambia, solo la implementación de {@code infrastructure.persistence}.
 */
@ApplicationScoped
public class ImagenService {

    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024; // 5 MB por imagen
    private static final Set<String> TIPOS_MIME_PERMITIDOS = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif");

    private final ItemRepository itemRepository;
    private final ImagenItemRepository imagenRepository;
    private final ImagenItemResponseMapper mapper;

    @Inject
    public ImagenService(ItemRepository itemRepository, ImagenItemRepository imagenRepository,
            ImagenItemResponseMapper mapper) {
        this.itemRepository = itemRepository;
        this.imagenRepository = imagenRepository;
        this.mapper = mapper;
    }

    @WithTransaction
    public Uni<ImagenItemResponse> subirImagen(UUID itemId, byte[] contenido, String nombreArchivo, String tipoMime,
            boolean esPrincipal, int orden) {
        if (contenido == null || contenido.length == 0) {
            return Uni.createFrom().failure(new ImagenInvalidaException("El archivo de imagen está vacío"));
        }
        if (contenido.length > TAMANO_MAXIMO_BYTES) {
            return Uni.createFrom().failure(new ImagenInvalidaException(
                "La imagen supera el tamaño máximo permitido de " + (TAMANO_MAXIMO_BYTES / 1024 / 1024) + " MB"));
        }
        if (tipoMime == null || !TIPOS_MIME_PERMITIDOS.contains(tipoMime.toLowerCase())) {
            return Uni.createFrom().failure(new ImagenInvalidaException(
                "Tipo de imagen no soportado: " + tipoMime + " (permitidos: " + TIPOS_MIME_PERMITIDOS + ")"));
        }

        return itemRepository.findById(itemId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Ítem no encontrado: " + itemId))
            .onItem().transformToUni(item -> {
                Uni<Integer> desmarcar = esPrincipal
                    ? imagenRepository.desmarcarPrincipal(itemId)
                    : Uni.createFrom().item(0);
                return desmarcar.chain(ignorado -> {
                    ImagenItem imagen = new ImagenItem(UUID.randomUUID(), itemId, nombreArchivo, tipoMime,
                        contenido.length, contenido, esPrincipal, orden, Instant.now());
                    return imagenRepository.persist(imagen);
                });
            })
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<List<ImagenItemResponse>> listarPorItem(UUID itemId) {
        return imagenRepository.findByItemId(itemId)
            .map(imagenes -> imagenes.stream().map(mapper::toResponse).collect(Collectors.toList()));
    }

    @WithSession
    public Uni<ImagenItem> obtenerParaDescarga(UUID imagenId) {
        return imagenRepository.findById(imagenId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Imagen no encontrada: " + imagenId));
    }

    @WithTransaction
    public Uni<Void> eliminar(UUID imagenId) {
        return imagenRepository.deleteById(imagenId)
            .onItem().transformToUni(borrada -> Boolean.TRUE.equals(borrada)
                ? Uni.createFrom().voidItem()
                : Uni.createFrom().failure(new NotFoundException("Imagen no encontrada: " + imagenId)));
    }
}
