package com.facturacion.inventario.infrastructure.rest;

import java.net.URI;
import java.nio.file.Files;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Vertx;

import com.facturacion.inventario.application.dto.ImagenItemResponse;
import com.facturacion.inventario.application.dto.SubirImagenRequest;
import com.facturacion.inventario.application.service.ImagenService;
import com.facturacion.inventario.domain.model.entity.ImagenItem;

/**
 * Imágenes de productos y servicios. El contenido se guarda en Postgres
 * ({@code bytea}) en vez de un object storage (MinIO/S3) — ver la nota
 * completa en {@link ImagenService}. En resumen: para el tamaño de
 * catálogo que este servicio de referencia asume, evita levantar un
 * componente de infraestructura adicional en el stack; la API pública
 * (esta clase) no expone el contenido en ningún listado, así que migrar la
 * implementación después no rompe a los clientes.
 */
@Path("")
@Tag(name = "Imágenes", description = "Imágenes de productos y servicios")
public class ImagenResource {

    private final ImagenService imagenService;

    @Inject
    public ImagenResource(ImagenService imagenService) {
        this.imagenService = imagenService;
    }

    @POST
    @Path("/items/{id}/imagenes")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Subir una imagen para un ítem",
        description = "Máximo 5 MB, formatos JPEG/PNG/WEBP/GIF. Si esPrincipal=true, desmarca cualquier otra imagen principal del mismo ítem.")
    @APIResponse(responseCode = "201", description = "Imagen guardada")
    @APIResponse(responseCode = "400", description = "Imagen inválida (tamaño, tipo, o vacía)")
    @APIResponse(responseCode = "404", description = "Ítem no encontrado")
    public Uni<Response> subir(
            @Parameter(description = "Id del ítem", required = true) @PathParam("id") UUID id,
            @Valid @BeanParam SubirImagenRequest form, @Context UriInfo uriInfo) {
        io.vertx.core.Context callingContext = Vertx.currentContext();
        return Uni.createFrom().item(() -> leerArchivo(form))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
            .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
            .onItem().transformToUni(contenido -> imagenService.subirImagen(id, contenido,
                form.archivo.fileName(), form.archivo.contentType(), form.esPrincipal, form.orden))
            .map(response -> {
                URI location = uriInfo.getBaseUriBuilder()
                    .path("imagenes").path(response.getId().toString()).path("contenido").build();
                return Response.created(location).entity(response).build();
            });
    }

    @GET
    @Path("/items/{id}/imagenes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar las imágenes de un ítem", description = "Metadata solamente — ver .../contenido para los bytes.")
    @APIResponse(responseCode = "200", description = "Listado")
    public Uni<Response> listarPorItem(
            @Parameter(description = "Id del ítem", required = true) @PathParam("id") UUID id) {
        return imagenService.listarPorItem(id)
            .map((java.util.List<ImagenItemResponse> imagenes) -> Response.ok(imagenes).build());
    }

    @GET
    @Path("/imagenes/{id}/contenido")
    @Operation(summary = "Descargar el contenido binario de una imagen")
    @APIResponse(responseCode = "200", description = "Bytes de la imagen, con el Content-Type original")
    @APIResponse(responseCode = "404", description = "Imagen no encontrada")
    public Uni<Response> obtenerContenido(
            @Parameter(description = "Id de la imagen", required = true) @PathParam("id") UUID id) {
        return imagenService.obtenerParaDescarga(id)
            .map((ImagenItem imagen) -> Response.ok(imagen.getContenido())
                .type(imagen.getTipoMime())
                .header("Content-Disposition", "inline; filename=\"" + imagen.getNombreArchivo() + "\"")
                .build());
    }

    @DELETE
    @Path("/imagenes/{id}")
    @Operation(summary = "Eliminar una imagen")
    @APIResponse(responseCode = "204", description = "Eliminada")
    @APIResponse(responseCode = "404", description = "Imagen no encontrada")
    public Uni<Response> eliminar(
            @Parameter(description = "Id de la imagen", required = true) @PathParam("id") UUID id) {
        return imagenService.eliminar(id).map(ignorado -> Response.noContent().build());
    }

    private byte[] leerArchivo(SubirImagenRequest form) {
        try {
            return Files.readAllBytes(form.archivo.uploadedFile());
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el archivo de imagen subido", e);
        }
    }
}
