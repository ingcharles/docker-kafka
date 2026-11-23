package com.facturacion.identidad.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.identidad.domain.model.entity.ApiKey;
import com.facturacion.identidad.domain.port.RevocacionApiKeysPublicador;
import com.facturacion.identidad.domain.repository.ApiKeyRepository;

/** Revocar una api-key la marca y avisa a los demás servicios; es idempotente y no toca keys ajenas ni vencidas. */
class ApiKeyServiceRevocacionTest {

    private final UUID usuario = UUID.randomUUID();
    private final FakeApiKeys repositorio = new FakeApiKeys();
    private final FakePublicador publicador = new FakePublicador();
    private final ApiKeyService servicio =
        new ApiKeyService(repositorio, null, null, null, null, null, publicador, 365);

    private ApiKey key(UUID dueno, String ruc, Instant vence, boolean revocada) {
        ApiKey k = new ApiKey();
        k.setId(UUID.randomUUID());
        k.setUsuarioId(dueno);
        k.setRuc(ruc);
        k.setJti(UUID.randomUUID().toString());
        k.setFechaExpiracion(vence);
        k.setRevocada(revocada);
        repositorio.guardados.put(k.getId(), k);
        return k;
    }

    private static <T> T esperar(Uni<T> uni) {
        return uni.await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void testDeberiaRevocarUnaKeyVigenteYPublicarSuJtiConSuVencimiento() {
        Instant vence = Instant.now().plusSeconds(3600);
        ApiKey k = key(usuario, "1722039953001", vence, false);

        esperar(servicio.revocar(usuario, k.getId()));

        assertEquals(List.of(k.getId()), repositorio.revocadas);
        assertEquals(1, publicador.avisos.size());
        assertEquals(k.getJti(), publicador.avisos.get(0).jti());
        assertEquals(vence, publicador.avisos.get(0).venceEn());
    }

    @Test
    void testDeberiaReenviarElAvisoSinMarcarDeNuevoAlRevocarDosVeces() {
        ApiKey k = key(usuario, "1722039953001", Instant.now().plusSeconds(3600), true);

        esperar(servicio.revocar(usuario, k.getId()));

        assertTrue(repositorio.revocadas.isEmpty(), "ya estaba marcada");
        assertEquals(1, publicador.avisos.size(), "por si el aviso anterior no llegó");
    }

    @Test
    void testDeberiaTratarUnaKeyAjenaComoInexistenteSinPublicarNada() {
        ApiKey ajena = key(UUID.randomUUID(), "1722039953001", Instant.now().plusSeconds(3600), false);

        assertThrows(NotFoundException.class, () -> esperar(servicio.revocar(usuario, ajena.getId())));
        assertThrows(NotFoundException.class, () -> esperar(servicio.revocar(usuario, UUID.randomUUID())));

        assertTrue(repositorio.revocadas.isEmpty());
        assertTrue(publicador.avisos.isEmpty());
    }

    @Test
    void testDeberiaMarcarUnaKeyYaVencidaSinPublicarAviso() {
        ApiKey vencida = key(usuario, "1722039953001", Instant.now().minusSeconds(60), false);

        esperar(servicio.revocar(usuario, vencida.getId()));

        assertEquals(List.of(vencida.getId()), repositorio.revocadas);
        assertTrue(publicador.avisos.isEmpty());
    }

    @Test
    void testDeberiaRevocarTodasLasKeysVigentesDeEseRucYSoloDeEseAlQuitarElAcceso() {
        Instant vence = Instant.now().plusSeconds(3600);
        ApiKey a = key(usuario, "1722039953001", vence, false);
        ApiKey b = key(usuario, "1722039953001", vence, false);
        ApiKey otroRuc = key(usuario, "0190072002001", vence, false);
        ApiKey yaRevocada = key(usuario, "1722039953001", vence, true);

        esperar(servicio.revocarDeUsuarioEnRuc(usuario, "1722039953001"));

        assertEquals(2, repositorio.revocadas.size());
        assertTrue(repositorio.revocadas.containsAll(List.of(a.getId(), b.getId())));
        assertEquals(2, publicador.avisos.size());
        assertTrue(!repositorio.revocadas.contains(otroRuc.getId()));
        assertTrue(!repositorio.revocadas.contains(yaRevocada.getId()));
    }

    @Test
    void testDeberiaPropagarElErrorParaRevertirLaTransaccionSiElAvisoNoSale() {
        ApiKey k = key(usuario, "1722039953001", Instant.now().plusSeconds(3600), false);
        publicador.falla = true;

        CompletionException e = assertThrows(CompletionException.class,
            () -> servicio.revocar(usuario, k.getId()).subscribeAsCompletionStage().join());

        assertTrue(e.getCause() instanceof IllegalStateException);
    }

    private static final class FakeApiKeys implements ApiKeyRepository {
        final Map<UUID, ApiKey> guardados = new HashMap<>();
        final List<UUID> revocadas = new ArrayList<>();

        @Override
        public Uni<ApiKey> persist(ApiKey apiKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Uni<ApiKey> findById(UUID id) {
            return Uni.createFrom().item(guardados.get(id));
        }

        @Override
        public Uni<List<ApiKey>> findByUsuarioId(UUID usuarioId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Uni<Boolean> existsByUsuarioIdAndNombre(UUID usuarioId, String nombre) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Uni<Integer> revocar(UUID id, UUID usuarioId) {
            revocadas.add(id);
            return Uni.createFrom().item(1);
        }

        @Override
        public Uni<List<ApiKey>> findVigentesByUsuarioIdAndRuc(UUID usuarioId, String ruc) {
            List<ApiKey> vigentes = new ArrayList<>();
            for (ApiKey k : guardados.values()) {
                if (k.getUsuarioId().equals(usuarioId) && k.getRuc().equals(ruc) && !k.isRevocada()
                        && k.getFechaExpiracion().isAfter(Instant.now())) {
                    vigentes.add(k);
                }
            }
            return Uni.createFrom().item(vigentes);
        }
    }

    private record Aviso(String jti, Instant venceEn) {
    }

    private static final class FakePublicador implements RevocacionApiKeysPublicador {
        final List<Aviso> avisos = new ArrayList<>();
        boolean falla;

        @Override
        public Uni<Void> publicar(String jti, Instant venceEn) {
            if (falla) {
                return Uni.createFrom().failure(new IllegalStateException("broker caído"));
            }
            avisos.add(new Aviso(jti, venceEn));
            return Uni.createFrom().voidItem();
        }
    }
}
