package com.facturacion.comprobantes.application.service;

import java.time.LocalDate;
import java.util.function.BiFunction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Vertx;

import com.facturacion.comprobantes.application.dto.ComprobanteResponse;
import com.facturacion.comprobantes.application.dto.CrearFacturaRequest;
import com.facturacion.comprobantes.application.dto.CrearGuiaRemisionRequest;
import com.facturacion.comprobantes.application.dto.CrearLiquidacionCompraRequest;
import com.facturacion.comprobantes.application.dto.CrearNotaCreditoRequest;
import com.facturacion.comprobantes.application.dto.CrearNotaDebitoRequest;
import com.facturacion.comprobantes.application.dto.CrearRetencionRequest;
import com.facturacion.comprobantes.application.dto.EmpresaRequest;
import com.facturacion.comprobantes.application.service.construccion.ConstructorFactura;
import com.facturacion.comprobantes.application.service.construccion.ConstructorGuiaRemision;
import com.facturacion.comprobantes.application.service.construccion.ConstructorLiquidacionCompra;
import com.facturacion.comprobantes.application.service.construccion.ConstructorNotaCredito;
import com.facturacion.comprobantes.application.service.construccion.ConstructorNotaDebito;
import com.facturacion.comprobantes.application.service.construccion.ConstructorRetencion;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;
import com.facturacion.comprobantes.domain.repository.CatalogoSriRepository;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.domain.repository.ConfiguracionEmpresaLocalRepository;
import com.facturacion.comprobantes.domain.repository.EstablecimientoLocalRepository;
import com.facturacion.comprobantes.domain.repository.PuntoEmisionLocalRepository;
import com.facturacion.comprobantes.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.comprobantes.infrastructure.persistence.mapper.ComprobanteMapper;
import com.facturacion.comprobantes.infrastructure.xml.GeneradorXmlComprobanteResolver;
import com.facturacion.comprobantes.infrastructure.xml.XsdValidador;
import com.facturacion.comprobantes.shared.constant.ZonaHoraria;
import com.facturacion.comprobantes.shared.error.CodigoError;
import com.facturacion.commons.exception.ErrorNegocioException;

/**
 * Emisión de comprobantes: un método {@code crear*} por tipo, todos sobre el flujo común {@link #emitir} (validar
 * establecimiento y punto de emisión, reservar el siguiente secuencial de forma atómica, armar el agregado, generar
 * y validar el XML y dejarlo en el outbox). Esta clase solo orquesta ese flujo; el armado de cada tipo vive en su
 * {@code Constructor*} (paquete {@code construccion}), las consultas en {@link ComprobanteConsultaService}, las
 * transiciones que llegan de la saga en {@link ActualizacionComprobanteService} y los eventos en
 * {@link EventosComprobante}.
 * <p>
 * Este servicio no mantiene copia local de la Empresa de firma-electronica-service: cada request de creación trae
 * inline los datos de la empresa (identificada por {@code ruc}, no por un id interno de otro servicio). Ver
 * ARQUITECTURA.md, "Empresa sin ID".
 */
@ApplicationScoped
public class ComprobanteService {

    private final ComprobanteRepository comprobanteRepository;
    private final EstablecimientoLocalRepository establecimientoLocalRepository;
    private final PuntoEmisionLocalRepository puntoEmisionLocalRepository;
    private final ConfiguracionEmpresaLocalRepository configuracionEmpresaLocalRepository;
    private final GeneradorXmlComprobanteResolver xmlResolver;
    private final XsdValidador xsdValidador;
    private final CatalogoSriRepository catalogoSriRepository;
    private final ComprobanteMapper mapper;
    private final OutboxEventRepository outboxRepository;
    private final EventosComprobante eventos;
    private final ConstructorFactura constructorFactura;
    private final ConstructorNotaCredito constructorNotaCredito;
    private final ConstructorNotaDebito constructorNotaDebito;
    private final ConstructorGuiaRemision constructorGuiaRemision;
    private final ConstructorRetencion constructorRetencion;
    private final ConstructorLiquidacionCompra constructorLiquidacionCompra;

    @Inject
    public ComprobanteService(ComprobanteRepository comprobanteRepository,
            EstablecimientoLocalRepository establecimientoLocalRepository,
            PuntoEmisionLocalRepository puntoEmisionLocalRepository,
            ConfiguracionEmpresaLocalRepository configuracionEmpresaLocalRepository,
            GeneradorXmlComprobanteResolver xmlResolver, XsdValidador xsdValidador,
            CatalogoSriRepository catalogoSriRepository, ComprobanteMapper mapper,
            OutboxEventRepository outboxRepository, EventosComprobante eventos,
            ConstructorFactura constructorFactura, ConstructorNotaCredito constructorNotaCredito,
            ConstructorNotaDebito constructorNotaDebito, ConstructorGuiaRemision constructorGuiaRemision,
            ConstructorRetencion constructorRetencion, ConstructorLiquidacionCompra constructorLiquidacionCompra) {
        this.comprobanteRepository = comprobanteRepository;
        this.establecimientoLocalRepository = establecimientoLocalRepository;
        this.puntoEmisionLocalRepository = puntoEmisionLocalRepository;
        this.configuracionEmpresaLocalRepository = configuracionEmpresaLocalRepository;
        this.xmlResolver = xmlResolver;
        this.xsdValidador = xsdValidador;
        this.catalogoSriRepository = catalogoSriRepository;
        this.mapper = mapper;
        this.outboxRepository = outboxRepository;
        this.eventos = eventos;
        this.constructorFactura = constructorFactura;
        this.constructorNotaCredito = constructorNotaCredito;
        this.constructorNotaDebito = constructorNotaDebito;
        this.constructorGuiaRemision = constructorGuiaRemision;
        this.constructorRetencion = constructorRetencion;
        this.constructorLiquidacionCompra = constructorLiquidacionCompra;
    }

    @WithTransaction
    public Uni<ComprobanteResponse> crearFactura(@Valid CrearFacturaRequest request) {
        return emitir(request.getEmpresa(), TipoComprobante.FACTURA,
            (secuencial, catalogos) -> constructorFactura.construir(request, secuencial, catalogos));
    }

    @WithTransaction
    public Uni<ComprobanteResponse> crearNotaCredito(@Valid CrearNotaCreditoRequest request) {
        return emitir(request.getEmpresa(), TipoComprobante.NOTA_CREDITO,
            (secuencial, catalogos) -> constructorNotaCredito.construir(request, secuencial, catalogos));
    }

    @WithTransaction
    public Uni<ComprobanteResponse> crearNotaDebito(@Valid CrearNotaDebitoRequest request) {
        return emitir(request.getEmpresa(), TipoComprobante.NOTA_DEBITO,
            (secuencial, catalogos) -> constructorNotaDebito.construir(request, secuencial, catalogos));
    }

    @WithTransaction
    public Uni<ComprobanteResponse> crearGuiaRemision(@Valid CrearGuiaRemisionRequest request) {
        return emitir(request.getEmpresa(), TipoComprobante.GUIA_REMISION,
            (secuencial, catalogos) -> constructorGuiaRemision.construir(request, secuencial, catalogos));
    }

    @WithTransaction
    public Uni<ComprobanteResponse> crearRetencion(@Valid CrearRetencionRequest request) {
        return emitir(request.getEmpresa(), TipoComprobante.RETENCION,
            (secuencial, catalogos) -> constructorRetencion.construir(request, secuencial, catalogos));
    }

    @WithTransaction
    public Uni<ComprobanteResponse> crearLiquidacionCompra(@Valid CrearLiquidacionCompraRequest request) {
        return emitir(request.getEmpresa(), TipoComprobante.LIQUIDACION_COMPRA,
            (secuencial, catalogos) -> constructorLiquidacionCompra.construir(request, secuencial, catalogos));
    }

    /** Lo que necesita el constructor de cada tipo de comprobante además del request. */
    private record SecuencialYCatalogos(long secuencial, CatalogosSriAFecha catalogos) {
    }

    /** Fecha de emisión en hora de Ecuador (UTC-5), la que usa el SRI y contra la que rigen las vigencias de los catálogos. */
    private static LocalDate fechaEmision() {
        return LocalDate.now(ZonaHoraria.ECUADOR);
    }

    /**
     * Flujo común a los 6 tipos: valida que el establecimiento y el punto de
     * emisión estén realmente registrados y activos para este ruc (ver
     * {@code EstablecimientoLocal}/{@code PuntoEmisionLocal} — antes de esto
     * cualquier string llegaba tal cual, sin gobernanza), reserva el
     * siguiente secuencial de forma atómica, arma el agregado (delegado a
     * {@code constructor}), genera el XML sin firmar y lo persiste junto con
     * el evento {@code ComprobanteCreado} en el outbox.
     */
    private Uni<ComprobanteResponse> emitir(EmpresaRequest empresa, TipoComprobante tipo,
            BiFunction<Long, CatalogosSriAFecha, Uni<Comprobante>> constructor) {
        io.vertx.core.Context callingContext = Vertx.currentContext();
        return validarEstablecimientoYPuntoEmision(empresa)
            // Catálogos del SRI (tablas propias, leídas con entidades, sin caché) a la fecha de emisión: se cargan
            // antes de reservar el secuencial, y en cadena (no en paralelo) por la misma razón que abajo.
            .onItem().transformToUni(ignorado -> catalogoSriRepository.cargar(fechaEmision()))
            .onItem().transformToUni(catalogos -> comprobanteRepository
                .siguienteSecuencial(empresa.getRuc(), tipo, empresa.getEstablecimiento(), empresa.getPuntoEmision())
                .map(secuencial -> new SecuencialYCatalogos(secuencial, catalogos)))
            .onItem().transformToUni(par -> constructor.apply(par.secuencial(), par.catalogos())
                // Secuencial a propósito (no Uni.combine): dos consultas Hibernate
                // Reactive concurrentes sobre la misma sesión reactiva rompen el
                // procesamiento del result set ("Illegal pop() with non-matching
                // JdbcValuesSourceProcessingState") — hay que encadenarlas, no
                // correrlas en paralelo, aunque sean independientes entre sí.
                .onItem().transformToUni(comprobante ->
                    configuracionEmpresaLocalRepository.findByRucAndClave(empresa.getRuc(), "sri.proveedor.ruc")
                        .onItem().transformToUni(configuracionProveedor -> {
                            // Anexo 26 (Res. NAC-DGERCGC26-00000027) — RUC del proveedor de
                            // software/servicio de facturación electrónica, white-label por
                            // empresa; null si no está configurada, sin afectar el XML.
                            comprobante.setProveedorRuc(
                                configuracionProveedor == null ? null : configuracionProveedor.getValor());
                            // El XML se genera ya con la clave de acceso, los totales y el detalle
                            // completos — es este texto el que queda guardado en xml_generado y el
                            // que viaja, sin cambios, hasta firma-electronica-service.
                            comprobante.setXmlGenerado(xmlResolver.generar(comprobante));
                            // Validar contra el XSD real del SRI (última versión, ver
                            // XsdValidador) es CPU-bound/bloqueante — se corre en el
                            // worker pool y se vuelve al contexto de Vert.x antes de
                            // tocar el repositorio, mismo patrón que
                            // CertificadoService.validarYExtraerMetadata. Un XML mal
                            // formado nunca debe llegar a firma-electronica-service.
                            return Uni.createFrom().item(() -> {
                                    xsdValidador.validar(tipo, comprobante.getXmlGenerado());
                                    return comprobante;
                                })
                                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                                .emitOn(runnable -> callingContext.runOnContext(ignored -> runnable.run()))
                                .onItem().transformToUni(validado -> comprobanteRepository.persist(validado)
                                    .chain(guardado -> outboxRepository.persist(eventos.creado(guardado))
                                        .replaceWith(guardado))
                                    .map(mapper::toResponse));
                        })));
    }

    /**
     * Antes de reservar un secuencial, confirma que el establecimiento esté
     * registrado y activo para este ruc, y luego que el punto de emisión lo
     * esté para ese establecimiento — en ese orden, para que el mensaje de
     * error sea el más específico posible. Falla con {@link ErrorNegocioException}
     * (→ 400 vía el mapper existente).
     */
    private Uni<Void> validarEstablecimientoYPuntoEmision(EmpresaRequest empresa) {
        return establecimientoLocalRepository.findByRucAndCodigo(empresa.getRuc(), empresa.getEstablecimiento())
            .onItem().transformToUni(establecimientoLocal -> {
                if (establecimientoLocal == null || !establecimientoLocal.isActivo()) {
                    return Uni.createFrom().failure(new ErrorNegocioException(CodigoError.ESTABLECIMIENTO_NO_REGISTRADO,
                        empresa.getEstablecimiento(), empresa.getRuc()));
                }
                return puntoEmisionLocalRepository.findByRucAndEstablecimientoCodigoAndCodigo(empresa.getRuc(),
                    empresa.getEstablecimiento(), empresa.getPuntoEmision());
            })
            .onItem().transformToUni(puntoEmisionLocal -> {
                if (puntoEmisionLocal == null || !puntoEmisionLocal.isActivo()) {
                    return Uni.createFrom().failure(new ErrorNegocioException(CodigoError.PUNTO_EMISION_NO_REGISTRADO,
                        empresa.getPuntoEmision(), empresa.getEstablecimiento(), empresa.getRuc()));
                }
                return Uni.createFrom().voidItem();
            });
    }
}
