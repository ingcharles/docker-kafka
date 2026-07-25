package com.facturacion.comprobantes.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.application.dto.ComprobanteResponse;
import com.facturacion.comprobantes.application.dto.CrearFacturaRequest;
import com.facturacion.comprobantes.application.dto.CrearGuiaRemisionRequest;
import com.facturacion.comprobantes.application.dto.CrearLiquidacionCompraRequest;
import com.facturacion.comprobantes.application.dto.CrearNotaCreditoRequest;
import com.facturacion.comprobantes.application.dto.CrearNotaDebitoRequest;
import com.facturacion.comprobantes.application.dto.CrearRetencionRequest;
import com.facturacion.comprobantes.application.dto.DestinatarioGuiaRemisionRequest;
import com.facturacion.comprobantes.application.dto.DetalleComprobanteRequest;
import com.facturacion.comprobantes.application.dto.DetalleGuiaRemisionRequest;
import com.facturacion.comprobantes.application.dto.MotivoNotaDebitoRequest;
import com.facturacion.comprobantes.application.dto.RetencionLineaRequest;
import com.facturacion.comprobantes.application.event.ActualizarComprobanteCommand;
import com.facturacion.comprobantes.application.event.ComprobanteAutorizadoEventMessage;
import com.facturacion.comprobantes.application.event.ComprobanteCreadoEventMessage;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.entity.DestinatarioGuiaRemision;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.entity.EmisorLocal;
import com.facturacion.comprobantes.domain.model.entity.ItemLocal;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.model.enums.TipoComprobante;
import com.facturacion.comprobantes.domain.model.vo.Impuesto;
import com.facturacion.comprobantes.domain.model.vo.PageResult;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.domain.repository.EmisorLocalRepository;
import com.facturacion.comprobantes.domain.repository.ItemLocalRepository;
import com.facturacion.comprobantes.domain.service.ClaveAccesoGenerador;
import com.facturacion.comprobantes.infrastructure.outbox.OutboxEventEntity;
import com.facturacion.comprobantes.infrastructure.outbox.OutboxEventRepository;
import com.facturacion.comprobantes.infrastructure.persistence.mapper.ComprobanteMapper;
import com.facturacion.comprobantes.infrastructure.ride.RideGenerador;
import com.facturacion.comprobantes.infrastructure.xml.GeneradorXmlComprobanteResolver;
import com.facturacion.comprobantes.shared.constant.MessagingConstants;
import com.facturacion.comprobantes.shared.exception.EmisorSinCertificadoException;
import com.facturacion.comprobantes.shared.exception.NotFoundException;
import com.facturacion.comprobantes.shared.responses.PagedResponse;

/**
 * Dueño del agregado Comprobante. Un método {@code crear*} por tipo de
 * comprobante (todos comparten {@link #emitir} para el flujo común: buscar
 * el emisor, obtener el siguiente secuencial de forma atómica, generar el
 * XML y dejarlo en el outbox); {@link #aplicarComando} aplica cada
 * transición de estado que llega del orquestador — es ahí, en
 * {@code MARCAR_AUTORIZADO}, donde se genera y se guarda el RIDE en PDF
 * (nunca antes: el RIDE debe mostrar el número/fecha de autorización real).
 * Ver {@code EstadoComprobante} para qué campo se llena en cada paso.
 */
@ApplicationScoped
public class ComprobanteService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ComprobanteRepository comprobanteRepository;
    private final EmisorLocalRepository emisorLocalRepository;
    private final ItemLocalRepository itemLocalRepository;
    private final GeneradorXmlComprobanteResolver xmlResolver;
    private final RideGenerador rideGenerador;
    private final ComprobanteMapper mapper;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public ComprobanteService(ComprobanteRepository comprobanteRepository, EmisorLocalRepository emisorLocalRepository,
            ItemLocalRepository itemLocalRepository, GeneradorXmlComprobanteResolver xmlResolver,
            RideGenerador rideGenerador, ComprobanteMapper mapper, OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.comprobanteRepository = comprobanteRepository;
        this.emisorLocalRepository = emisorLocalRepository;
        this.itemLocalRepository = itemLocalRepository;
        this.xmlResolver = xmlResolver;
        this.rideGenerador = rideGenerador;
        this.mapper = mapper;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @WithTransaction
    public Uni<ComprobanteResponse> crearFactura(@Valid CrearFacturaRequest request) {
        return emitir(request.getEmisorId(), TipoComprobante.FACTURA,
            (emisor, secuencial) -> construirFactura(emisor, request, secuencial));
    }

    @WithTransaction
    public Uni<ComprobanteResponse> crearNotaCredito(@Valid CrearNotaCreditoRequest request) {
        return emitir(request.getEmisorId(), TipoComprobante.NOTA_CREDITO,
            (emisor, secuencial) -> construirNotaCredito(emisor, request, secuencial));
    }

    @WithTransaction
    public Uni<ComprobanteResponse> crearNotaDebito(@Valid CrearNotaDebitoRequest request) {
        return emitir(request.getEmisorId(), TipoComprobante.NOTA_DEBITO,
            (emisor, secuencial) -> construirNotaDebito(emisor, request, secuencial));
    }

    @WithTransaction
    public Uni<ComprobanteResponse> crearGuiaRemision(@Valid CrearGuiaRemisionRequest request) {
        return emitir(request.getEmisorId(), TipoComprobante.GUIA_REMISION,
            (emisor, secuencial) -> construirGuiaRemision(emisor, request, secuencial));
    }

    @WithTransaction
    public Uni<ComprobanteResponse> crearRetencion(@Valid CrearRetencionRequest request) {
        return emitir(request.getEmisorId(), TipoComprobante.RETENCION,
            (emisor, secuencial) -> construirRetencion(emisor, request, secuencial));
    }

    @WithTransaction
    public Uni<ComprobanteResponse> crearLiquidacionCompra(@Valid CrearLiquidacionCompraRequest request) {
        return emitir(request.getEmisorId(), TipoComprobante.LIQUIDACION_COMPRA,
            (emisor, secuencial) -> construirLiquidacionCompra(emisor, request, secuencial));
    }

    /**
     * Flujo común a los 6 tipos: busca el emisor, reserva el siguiente
     * secuencial de forma atómica para esa combinación
     * emisor+tipo+establecimiento+puntoEmision, arma el agregado (delegado a
     * {@code constructor}), genera el XML sin firmar y lo persiste junto con
     * el evento {@code ComprobanteCreado} en el outbox.
     */
    private Uni<ComprobanteResponse> emitir(UUID emisorId, TipoComprobante tipo,
            BiFunction<EmisorLocal, Long, Uni<Comprobante>> constructor) {
        return emisorLocalRepository.findById(emisorId)
            .onItem().ifNull().failWith(() -> new EmisorSinCertificadoException(emisorId))
            .onItem().transformToUni(emisor -> comprobanteRepository
                .siguienteSecuencial(emisor.getId(), tipo, emisor.getEstablecimientoDefecto(),
                    emisor.getPuntoEmisionDefecto())
                .onItem().transformToUni(secuencial -> constructor.apply(emisor, secuencial)
                    .onItem().transformToUni(comprobante -> {
                        // El XML se genera ya con la clave de acceso, los totales y el detalle
                        // completos — es este texto el que queda guardado en xml_generado y el
                        // que viaja, sin cambios, hasta firma-electronica-service.
                        comprobante.setXmlGenerado(xmlResolver.generar(comprobante));
                        return comprobanteRepository.persist(comprobante)
                            .chain(guardado -> outboxRepository.persist(construirEventoCreado(guardado))
                                .replaceWith(guardado))
                            .map(mapper::toResponse);
                    })));
    }

    /** Encabezado común a los 6 tipos (clave de acceso, snapshot del emisor, estado inicial). */
    private Comprobante construirEncabezado(EmisorLocal emisor, TipoComprobante tipo, long secuencial, Instant ahora) {
        String claveAcceso = ClaveAccesoGenerador.generar(ClaveAccesoGenerador.hoyEnEcuador(), tipo, emisor.getRuc(),
            emisor.getAmbiente(), emisor.getEstablecimientoDefecto(), emisor.getPuntoEmisionDefecto(), secuencial);

        Comprobante comprobante = new Comprobante();
        comprobante.setId(UUID.randomUUID());
        comprobante.setTipoComprobante(tipo);
        comprobante.setClaveAcceso(claveAcceso);
        comprobante.setAmbiente(emisor.getAmbiente());
        comprobante.setEstablecimiento(emisor.getEstablecimientoDefecto());
        comprobante.setPuntoEmision(emisor.getPuntoEmisionDefecto());
        comprobante.setSecuencial(String.format("%09d", secuencial));
        comprobante.setEmisorId(emisor.getId());
        comprobante.setEmisorRuc(emisor.getRuc());
        comprobante.setEmisorRazonSocial(emisor.getRazonSocial());
        comprobante.setEmisorNombreComercial(emisor.getNombreComercial());
        comprobante.setEmisorDireccionMatriz(emisor.getDireccionMatriz());
        comprobante.setDireccionEstablecimiento(emisor.getDireccionMatriz());
        comprobante.setEmisorObligadoContabilidad(emisor.isObligadoContabilidad());
        comprobante.setEmisorContribuyenteEspecial(emisor.isContribuyenteEspecial());
        comprobante.setEmisorNumeroContribuyenteEspecial(emisor.getNumeroContribuyenteEspecial());
        comprobante.setEmisorEsGranContribuyente(emisor.isEsGranContribuyente());
        comprobante.setEmisorNumeroResolucionGranContribuyente(emisor.getNumeroResolucionGranContribuyente());
        comprobante.setFechaEmision(ahora);
        comprobante.setEstado(EstadoComprobante.BORRADOR);
        comprobante.setCreadoEn(ahora);
        comprobante.setActualizadoEn(ahora);
        return comprobante;
    }

    private Uni<Comprobante> construirFactura(EmisorLocal emisor, CrearFacturaRequest request, long secuencial) {
        Instant ahora = Instant.now();
        Comprobante comprobante = construirEncabezado(emisor, TipoComprobante.FACTURA, secuencial, ahora);

        return cargarItemsParaAutoImpuestos(request.getDetalles())
            .map(itemsPorId -> {
                List<DetalleComprobante> detalles = request.getDetalles().stream()
                    .map(det -> construirDetalle(det, itemsPorId))
                    .collect(Collectors.toList());
                aplicarTotalesDeDetalle(comprobante, detalles);

                comprobante.setCompradorTipoIdentificacion(request.getCompradorTipoIdentificacion());
                comprobante.setCompradorIdentificacion(request.getCompradorIdentificacion());
                comprobante.setCompradorRazonSocial(request.getCompradorRazonSocial());
                comprobante.setCompradorDireccion(request.getCompradorDireccion());
                comprobante.setCompradorCorreo(request.getCompradorCorreo());
                comprobante.setMoneda(request.getMoneda());
                comprobante.setDetalles(detalles);
                return comprobante;
            });
    }

    private Uni<Comprobante> construirNotaCredito(EmisorLocal emisor, CrearNotaCreditoRequest request,
            long secuencial) {
        Instant ahora = Instant.now();
        Comprobante comprobante = construirEncabezado(emisor, TipoComprobante.NOTA_CREDITO, secuencial, ahora);

        return cargarItemsParaAutoImpuestos(request.getDetalles())
            .map(itemsPorId -> {
                List<DetalleComprobante> detalles = request.getDetalles().stream()
                    .map(det -> construirDetalle(det, itemsPorId))
                    .collect(Collectors.toList());
                aplicarTotalesDeDetalle(comprobante, detalles);

                comprobante.setCompradorTipoIdentificacion(request.getCompradorTipoIdentificacion());
                comprobante.setCompradorIdentificacion(request.getCompradorIdentificacion());
                comprobante.setCompradorRazonSocial(request.getCompradorRazonSocial());
                comprobante.setCompradorDireccion(request.getCompradorDireccion());
                comprobante.setCompradorCorreo(request.getCompradorCorreo());
                comprobante.setMoneda(request.getMoneda());
                comprobante.setDetalles(detalles);
                comprobante.setDocModificadoTipo(request.getDocModificadoTipo());
                comprobante.setDocModificadoNumero(request.getDocModificadoNumero());
                comprobante.setDocModificadoFecha(parseFecha(request.getDocModificadoFecha()));
                comprobante.setMotivo(request.getMotivo());
                return comprobante;
            });
    }

    private Uni<Comprobante> construirNotaDebito(EmisorLocal emisor, CrearNotaDebitoRequest request,
            long secuencial) {
        Instant ahora = Instant.now();
        Comprobante comprobante = construirEncabezado(emisor, TipoComprobante.NOTA_DEBITO, secuencial, ahora);

        BigDecimal totalMotivos = request.getMotivos().stream()
            .map(MotivoNotaDebitoRequest::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // El bloque <impuestos> de Nota de Débito es único a nivel de
        // encabezado (no por motivo) — se calcula sobre la suma de los
        // motivos y se guarda solo en la primera línea, así el generador de
        // XML reutiliza la misma lógica de agrupación que Factura sin
        // cambios (ver XmlComprobanteUtil.agruparImpuestos).
        List<Impuesto> impuestosEncabezado = request.getImpuestos().stream()
            .map(impRequest -> new Impuesto(
                impRequest.getCodigo(),
                impRequest.getCodigoPorcentaje(),
                impRequest.getTarifa(),
                totalMotivos,
                totalMotivos.multiply(impRequest.getTarifa()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)))
            .collect(Collectors.toList());
        BigDecimal totalImpuestos = impuestosEncabezado.stream().map(Impuesto::valor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DetalleComprobante> detalles = new ArrayList<>();
        for (int i = 0; i < request.getMotivos().size(); i++) {
            MotivoNotaDebitoRequest motivoRequest = request.getMotivos().get(i);
            DetalleComprobante detalle = new DetalleComprobante();
            detalle.setId(UUID.randomUUID());
            detalle.setCodigoPrincipal("MOTIVO-" + (i + 1));
            detalle.setDescripcion(motivoRequest.getRazon());
            detalle.setCantidad(BigDecimal.ONE);
            detalle.setPrecioUnitario(motivoRequest.getValor());
            detalle.setDescuento(BigDecimal.ZERO);
            detalle.setImpuestos(i == 0 ? impuestosEncabezado : List.of());
            detalles.add(detalle);
        }

        comprobante.setCompradorTipoIdentificacion(request.getCompradorTipoIdentificacion());
        comprobante.setCompradorIdentificacion(request.getCompradorIdentificacion());
        comprobante.setCompradorRazonSocial(request.getCompradorRazonSocial());
        comprobante.setCompradorDireccion(request.getCompradorDireccion());
        comprobante.setCompradorCorreo(request.getCompradorCorreo());
        comprobante.setMoneda(request.getMoneda());
        comprobante.setDetalles(detalles);
        comprobante.setSubtotalSinImpuestos(totalMotivos);
        comprobante.setTotalDescuento(BigDecimal.ZERO);
        comprobante.setImporteTotal(totalMotivos.add(totalImpuestos));
        comprobante.setDocModificadoTipo(request.getDocModificadoTipo());
        comprobante.setDocModificadoNumero(request.getDocModificadoNumero());
        comprobante.setDocModificadoFecha(parseFecha(request.getDocModificadoFecha()));
        return Uni.createFrom().item(comprobante);
    }

    private Uni<Comprobante> construirGuiaRemision(EmisorLocal emisor, CrearGuiaRemisionRequest request,
            long secuencial) {
        Instant ahora = Instant.now();
        Comprobante comprobante = construirEncabezado(emisor, TipoComprobante.GUIA_REMISION, secuencial, ahora);

        List<DestinatarioGuiaRemision> destinatarios = new ArrayList<>();
        List<DetalleComprobante> detalles = new ArrayList<>();
        for (DestinatarioGuiaRemisionRequest destinoRequest : request.getDestinatarios()) {
            DestinatarioGuiaRemision destinatario = new DestinatarioGuiaRemision();
            destinatario.setId(UUID.randomUUID());
            destinatario.setIdentificacion(destinoRequest.getIdentificacion());
            destinatario.setRazonSocial(destinoRequest.getRazonSocial());
            destinatario.setDireccion(destinoRequest.getDireccion());
            destinatario.setMotivoTraslado(destinoRequest.getMotivoTraslado());
            destinatario.setDocAduaneroUnico(destinoRequest.getDocAduaneroUnico());
            destinatario.setCodEstabDestino(destinoRequest.getCodEstabDestino());
            destinatario.setRuta(destinoRequest.getRuta());
            destinatario.setCodDocSustento(destinoRequest.getCodDocSustento());
            destinatario.setNumDocSustento(destinoRequest.getNumDocSustento());
            destinatario.setNumAutDocSustento(destinoRequest.getNumAutDocSustento());
            destinatario.setFechaEmisionDocSustento(parseFecha(destinoRequest.getFechaEmisionDocSustento()));
            destinatarios.add(destinatario);

            for (DetalleGuiaRemisionRequest detalleRequest : destinoRequest.getDetalles()) {
                DetalleComprobante detalle = new DetalleComprobante();
                detalle.setId(UUID.randomUUID());
                detalle.setItemId(detalleRequest.getItemId());
                detalle.setCodigoPrincipal(detalleRequest.getCodigoPrincipal());
                detalle.setDescripcion(detalleRequest.getDescripcion());
                detalle.setCantidad(detalleRequest.getCantidad());
                detalle.setPrecioUnitario(BigDecimal.ZERO);
                detalle.setDescuento(BigDecimal.ZERO);
                detalle.setImpuestos(List.of());
                detalle.setDestinatarioId(destinatario.getId());
                detalles.add(detalle);
            }
        }

        comprobante.setDestinatarios(destinatarios);
        comprobante.setDetalles(detalles);
        comprobante.setSubtotalSinImpuestos(BigDecimal.ZERO);
        comprobante.setTotalDescuento(BigDecimal.ZERO);
        comprobante.setImporteTotal(BigDecimal.ZERO);
        comprobante.setMoneda("DOLAR");
        comprobante.setDirPartida(request.getDirPartida());
        comprobante.setTransportistaRazonSocial(request.getTransportistaRazonSocial());
        comprobante.setTransportistaTipoIdentificacion(request.getTransportistaTipoIdentificacion());
        comprobante.setTransportistaRuc(request.getTransportistaRuc());
        comprobante.setFechaIniTransporte(parseFecha(request.getFechaIniTransporte()));
        comprobante.setFechaFinTransporte(parseFecha(request.getFechaFinTransporte()));
        comprobante.setPlaca(request.getPlaca());
        return Uni.createFrom().item(comprobante);
    }

    private Uni<Comprobante> construirRetencion(EmisorLocal emisor, CrearRetencionRequest request, long secuencial) {
        Instant ahora = Instant.now();
        Comprobante comprobante = construirEncabezado(emisor, TipoComprobante.RETENCION, secuencial, ahora);

        List<DetalleComprobante> detalles = new ArrayList<>();
        BigDecimal totalBaseImponible = BigDecimal.ZERO;
        BigDecimal totalRetenido = BigDecimal.ZERO;
        for (int i = 0; i < request.getRetenciones().size(); i++) {
            RetencionLineaRequest lineaRequest = request.getRetenciones().get(i);
            Impuesto impuesto = new Impuesto(
                lineaRequest.getCodigo(),
                null,
                lineaRequest.getPorcentajeRetener(),
                lineaRequest.getBaseImponible(),
                lineaRequest.getValorRetenido(),
                lineaRequest.getCodigoRetencion(),
                lineaRequest.getDocSustentoCodigo(),
                lineaRequest.getDocSustentoNumero(),
                lineaRequest.getDocSustentoFecha());

            DetalleComprobante detalle = new DetalleComprobante();
            detalle.setId(UUID.randomUUID());
            detalle.setCodigoPrincipal("RET-" + (i + 1));
            detalle.setDescripcion("Retención " + lineaRequest.getCodigoRetencion() + " sobre "
                + lineaRequest.getDocSustentoNumero());
            detalle.setCantidad(BigDecimal.ONE);
            detalle.setPrecioUnitario(lineaRequest.getBaseImponible());
            detalle.setDescuento(BigDecimal.ZERO);
            detalle.setImpuestos(List.of(impuesto));
            detalles.add(detalle);

            totalBaseImponible = totalBaseImponible.add(lineaRequest.getBaseImponible());
            totalRetenido = totalRetenido.add(lineaRequest.getValorRetenido());
        }

        comprobante.setCompradorTipoIdentificacion(request.getSujetoRetenidoTipoIdentificacion());
        comprobante.setCompradorIdentificacion(request.getSujetoRetenidoIdentificacion());
        comprobante.setCompradorRazonSocial(request.getSujetoRetenidoRazonSocial());
        comprobante.setCompradorCorreo(request.getSujetoRetenidoCorreo());
        comprobante.setMoneda("DOLAR");
        comprobante.setDetalles(detalles);
        comprobante.setSubtotalSinImpuestos(totalBaseImponible);
        comprobante.setTotalDescuento(BigDecimal.ZERO);
        comprobante.setImporteTotal(totalRetenido);
        comprobante.setPeriodoFiscal(request.getPeriodoFiscal());
        return Uni.createFrom().item(comprobante);
    }

    private Uni<Comprobante> construirLiquidacionCompra(EmisorLocal emisor, CrearLiquidacionCompraRequest request,
            long secuencial) {
        Instant ahora = Instant.now();
        Comprobante comprobante = construirEncabezado(emisor, TipoComprobante.LIQUIDACION_COMPRA, secuencial, ahora);

        return cargarItemsParaAutoImpuestos(request.getDetalles())
            .map(itemsPorId -> {
                List<DetalleComprobante> detalles = request.getDetalles().stream()
                    .map(det -> construirDetalle(det, itemsPorId))
                    .collect(Collectors.toList());
                aplicarTotalesDeDetalle(comprobante, detalles);

                // El proveedor se guarda en las mismas columnas comprador_* — aquí
                // representa a quien vende, no a quien compra, pero es el mismo dato
                // desde el punto de vista del agregado (ver clase de la request).
                comprobante.setCompradorTipoIdentificacion(request.getProveedorTipoIdentificacion());
                comprobante.setCompradorIdentificacion(request.getProveedorIdentificacion());
                comprobante.setCompradorRazonSocial(request.getProveedorRazonSocial());
                comprobante.setCompradorDireccion(request.getProveedorDireccion());
                comprobante.setCompradorCorreo(request.getProveedorCorreo());
                comprobante.setMoneda(request.getMoneda());
                comprobante.setDetalles(detalles);
                return comprobante;
            });
    }

    private void aplicarTotalesDeDetalle(Comprobante comprobante, List<DetalleComprobante> detalles) {
        BigDecimal subtotal = detalles.stream().map(DetalleComprobante::precioTotalSinImpuesto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDescuento = detalles.stream()
            .map(d -> d.getDescuento() == null ? BigDecimal.ZERO : d.getDescuento())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalImpuestos = detalles.stream()
            .flatMap(d -> d.getImpuestos().stream())
            .map(Impuesto::valor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        comprobante.setSubtotalSinImpuestos(subtotal);
        comprobante.setTotalDescuento(totalDescuento);
        comprobante.setImporteTotal(subtotal.add(totalImpuestos));
    }

    private Instant parseFecha(String ddMmAaaa) {
        if (ddMmAaaa == null || ddMmAaaa.isBlank()) {
            return null;
        }
        return LocalDate.parse(ddMmAaaa, FORMATO_FECHA).atStartOfDay(ZoneOffset.of("-05:00")).toInstant();
    }

    /**
     * Aplica la transición de estado que indique el comando — ver
     * {@code MessagingConstants.COMANDO_*}. Es aquí, y solo aquí, donde se
     * decide qué XML/PDF se guarda en cada paso.
     */
    @WithTransaction
    public Uni<Void> aplicarComando(ActualizarComprobanteCommand command) {
        return comprobanteRepository.findById(command.comprobanteId())
            .onItem().ifNull().failWith(() ->
                new NotFoundException("Comprobante no encontrado: " + command.comprobanteId()))
            .onItem().transformToUni(comprobante -> {
                switch (command.tipoComando()) {
                    case MessagingConstants.COMANDO_MARCAR_FIRMADO -> {
                        comprobante.setEstado(EstadoComprobante.FIRMADO);
                        comprobante.setXmlFirmado(command.xmlFirmado());
                    }
                    case MessagingConstants.COMANDO_MARCAR_FIRMA_FALLIDA -> {
                        comprobante.setEstado(EstadoComprobante.FIRMA_FALLIDA);
                        comprobante.setMotivoRechazo(command.motivoRechazo());
                    }
                    case MessagingConstants.COMANDO_MARCAR_AUTORIZADO -> {
                        comprobante.setEstado(EstadoComprobante.AUTORIZADO);
                        comprobante.setXmlAutorizado(command.xmlAutorizado());
                        comprobante.setNumeroAutorizacion(command.numeroAutorizacion());
                        comprobante.setFechaAutorizacion(command.fechaAutorizacion());
                        // El RIDE se genera acá, no antes: recién ahora existen
                        // numeroAutorizacion/fechaAutorizacion reales para mostrar.
                        comprobante.setRidePdf(rideGenerador.generar(comprobante));
                    }
                    case MessagingConstants.COMANDO_MARCAR_NO_AUTORIZADO -> {
                        comprobante.setEstado(EstadoComprobante.NO_AUTORIZADO);
                        comprobante.setMotivoRechazo(command.motivoRechazo());
                    }
                    case MessagingConstants.COMANDO_MARCAR_NOTIFICADO ->
                        comprobante.setEstado(EstadoComprobante.NOTIFICADO);
                    default -> throw new IllegalArgumentException(
                        "Tipo de comando de comprobante desconocido: " + command.tipoComando());
                }
                return comprobanteRepository.update(comprobante)
                    .chain(guardado -> MessagingConstants.COMANDO_MARCAR_AUTORIZADO.equals(command.tipoComando())
                        ? publicarComprobanteAutorizadoSiHayCorreo(guardado)
                        : Uni.createFrom().voidItem());
            })
            .replaceWithVoid();
    }

    /**
     * notificacion-service no puede llamar de vuelta a este servicio (cero
     * REST entre servicios) ni regenerar el RIDE por su cuenta (no conoce el
     * resto del agregado) — así que el RIDE ya generado viaja embebido en
     * este evento, igual que {@code xmlGenerado} viaja en ComprobanteCreado.
     * Si el llamador nunca dio un correo, no hay nada que notificar — no es
     * un error, simplemente no se publica el evento.
     */
    private Uni<Void> publicarComprobanteAutorizadoSiHayCorreo(Comprobante comprobante) {
        if (comprobante.getCompradorCorreo() == null || comprobante.getCompradorCorreo().isBlank()) {
            return Uni.createFrom().voidItem();
        }
        ComprobanteAutorizadoEventMessage mensaje = new ComprobanteAutorizadoEventMessage(
            UUID.randomUUID(),
            comprobante.getId(),
            MessagingConstants.EVENT_TYPE_COMPROBANTE_AUTORIZADO,
            comprobante.getClaveAcceso(),
            comprobante.getTipoComprobante().name(),
            comprobante.getCompradorCorreo(),
            comprobante.getCompradorRazonSocial(),
            Base64.getEncoder().encodeToString(comprobante.getRidePdf()),
            comprobante.getXmlAutorizado(),
            Instant.now());
        OutboxEventEntity evento = OutboxEventEntity.of(comprobante.getId(),
            MessagingConstants.EVENT_TYPE_COMPROBANTE_AUTORIZADO, MessagingConstants.COMPROBANTE_EVENTS_TOPIC,
            writeValueAsString(mensaje));
        return outboxRepository.persist(evento).replaceWithVoid();
    }

    @WithSession
    public Uni<ComprobanteResponse> obtenerPorId(UUID id) {
        return comprobanteRepository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Comprobante no encontrado: " + id))
            .map(mapper::toResponse);
    }

    @WithSession
    public Uni<Comprobante> obtenerDominioPorId(UUID id) {
        return comprobanteRepository.findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Comprobante no encontrado: " + id));
    }

    @WithSession
    public Uni<PagedResponse<ComprobanteResponse>> listarPaginado(int page, int limit, EstadoComprobante estado,
            UUID emisorId) {
        return comprobanteRepository.findPaged(page, limit, estado, emisorId)
            .map((PageResult<Comprobante> pageResult) -> {
                List<ComprobanteResponse> items = pageResult.items().stream()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }

    /**
     * Trae de una sola vez (batch) la copia local de todos los ítems que
     * necesitarán derivar su impuesto automáticamente — es decir, las
     * líneas que no traen {@code impuestos} explícitos pero sí un
     * {@code itemId}. Si una línea ya trae impuestos, o no tiene itemId, no
     * se incluye en la consulta.
     */
    private Uni<Map<UUID, ItemLocal>> cargarItemsParaAutoImpuestos(List<DetalleComprobanteRequest> detalles) {
        Set<UUID> ids = detalles.stream()
            .filter(d -> (d.getImpuestos() == null || d.getImpuestos().isEmpty()) && d.getItemId() != null)
            .map(DetalleComprobanteRequest::getItemId)
            .collect(Collectors.toSet());
        return itemLocalRepository.findByIds(ids);
    }

    private DetalleComprobante construirDetalle(DetalleComprobanteRequest request, Map<UUID, ItemLocal> itemsPorId) {
        DetalleComprobante detalle = new DetalleComprobante();
        detalle.setId(UUID.randomUUID());
        detalle.setItemId(request.getItemId());
        detalle.setCodigoPrincipal(request.getCodigoPrincipal());
        detalle.setCodigoAuxiliar(request.getCodigoAuxiliar());
        detalle.setDescripcion(request.getDescripcion());
        detalle.setCantidad(request.getCantidad());
        detalle.setPrecioUnitario(request.getPrecioUnitario());
        detalle.setDescuento(request.getDescuento() == null ? BigDecimal.ZERO : request.getDescuento());

        BigDecimal baseImponible = detalle.precioTotalSinImpuesto();
        List<Impuesto> impuestos;
        if (request.getImpuestos() != null && !request.getImpuestos().isEmpty()) {
            impuestos = request.getImpuestos().stream()
                .map(impRequest -> new Impuesto(
                    impRequest.getCodigo(),
                    impRequest.getCodigoPorcentaje(),
                    impRequest.getTarifa(),
                    baseImponible,
                    baseImponible.multiply(impRequest.getTarifa())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
        } else {
            ItemLocal item = request.getItemId() == null ? null : itemsPorId.get(request.getItemId());
            if (item == null) {
                throw new IllegalArgumentException("La línea '" + request.getDescripcion() + "' no trae impuestos y "
                    + (request.getItemId() == null
                        ? "tampoco un itemId del que derivarlos."
                        : "el itemId " + request.getItemId() + " no está (todavía) en el catálogo local de "
                            + "inventario-service — puede que el evento ItemCreado no haya llegado aún, o que el "
                            + "ítem no exista."));
            }
            impuestos = construirImpuestosDesdeItem(item, baseImponible);
        }
        detalle.setImpuestos(impuestos);
        return detalle;
    }

    /**
     * Deriva el/los impuesto(s) de una línea a partir de la clasificación
     * tributaria del ítem en inventario-service — el caso normal es un solo
     * impuesto (IVA); si el ítem tiene ICE se agrega una segunda línea.
     * Simplificación deliberada: ambos se calculan sobre la misma
     * {@code baseImponible} (el precio de la línea); la ley ecuatoriana en
     * rigor calcula el IVA sobre base+ICE para ítems con ICE — afinar eso
     * queda fuera del alcance de esta versión de referencia (mismo criterio
     * que el bloque de reembolso omitido en Factura/Liquidación de Compra).
     */
    private List<Impuesto> construirImpuestosDesdeItem(ItemLocal item, BigDecimal baseImponible) {
        List<Impuesto> impuestos = new ArrayList<>();
        BigDecimal valorIva = baseImponible.multiply(item.getTarifaIva())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        impuestos.add(new Impuesto(item.getCodigoImpuesto(), item.getCodigoPorcentaje(), item.getTarifaIva(),
            baseImponible, valorIva));
        if (item.isTieneIce()) {
            BigDecimal valorIce = baseImponible.multiply(item.getTarifaIce())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            impuestos.add(new Impuesto("3", item.getCodigoPorcentajeIce(), item.getTarifaIce(), baseImponible,
                valorIce));
        }
        return impuestos;
    }

    private OutboxEventEntity construirEventoCreado(Comprobante comprobante) {
        ComprobanteCreadoEventMessage mensaje = new ComprobanteCreadoEventMessage(
            UUID.randomUUID(),
            comprobante.getId(),
            MessagingConstants.EVENT_TYPE_COMPROBANTE_CREADO,
            comprobante.getEmisorId(),
            comprobante.getClaveAcceso(),
            comprobante.getAmbiente().name(),
            comprobante.getXmlGenerado(),
            Instant.now());
        return OutboxEventEntity.of(comprobante.getId(), MessagingConstants.EVENT_TYPE_COMPROBANTE_CREADO,
            MessagingConstants.COMPROBANTE_EVENTS_TOPIC, writeValueAsString(mensaje));
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload del outbox", e);
        }
    }
}
