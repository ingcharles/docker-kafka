package com.facturacion.inventario.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "imagen_item")
public class ImagenItemEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "item_id", nullable = false)
    public UUID itemId;

    @Column(name = "nombre_archivo", nullable = false)
    public String nombreArchivo;

    @Column(name = "tipo_mime", nullable = false)
    public String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    public long tamanoBytes;

    // Sin @Lob a propósito: la columna es "bytea" (no un LOB real de SQL), y
    // el cliente reactivo de Postgres espera un byte[] plano — @Lob hace que
    // Hibernate lo envuelva en un BlobProxy, que el driver reactivo no sabe
    // coercionar a io.vertx.core.buffer.Buffer al hacer el bind del parámetro.
    @Column(nullable = false)
    public byte[] contenido;

    @Column(name = "es_principal", nullable = false)
    public boolean esPrincipal;

    @Column(nullable = false)
    public int orden;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;
}
