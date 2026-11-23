package com.duoc.aquaflow.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aquaflow-test;DB_CLOSE_DELAY=-1",
        "AQUAFLOW_SUPERVISOR_PIN=pin-supervisor-prueba",
        "AQUAFLOW_JEFE_PIN=pin-jefe-prueba"
})
@AutoConfigureMockMvc
class FaenasApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Faenas faenas;

    @Test
    void compruebaCredencialesYRellenaElRolDesdeElServidor() throws Exception {
        mvc.perform(get("/api/sesion")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sesion").headers(cuenta("jefe", "incorrecto")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sesion").headers(supervisor()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("SUPERVISOR"));
        mvc.perform(get("/api/sesion").headers(jefe()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("JEFE"));
    }

    @Test
    void supervisorGuardaConIdEstableYJefaturaConsultaLaMismaApertura() throws Exception {
        UUID id = UUID.randomUUID();
        String url = "/api/faenas/" + id;
        ObjectNode apertura = datos().put("supervisorACargo", "otro-supervisor");

        mvc.perform(put(url).headers(supervisor()).contentType(MediaType.APPLICATION_JSON).content(apertura.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supervisorACargo").value("supervisor"));

        apertura.put("hora", "08:15");
        mvc.perform(put(url).headers(supervisor()).contentType(MediaType.APPLICATION_JSON).content(apertura.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));

        mvc.perform(get(url).headers(jefe()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.datos.hora").value("08:15"));
        mvc.perform(get("/api/faenas").headers(jefe()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(id.toString())));
        mvc.perform(delete(url).headers(supervisor())).andExpect(status().isNoContent());
        mvc.perform(get(url).headers(jefe())).andExpect(status().isNotFound());
    }

    @Test
    void jefaturaSoloLeeYUnSupervisorNoAccedeAAperturasAjenas() throws Exception {
        UUID id = UUID.randomUUID();
        String url = "/api/faenas/" + id;
        faenas.guardar(id, "otro-supervisor", json.treeToValue(datos(), AperturaDatos.class));
        mvc.perform(put(url).headers(jefe()).contentType(MediaType.APPLICATION_JSON).content(datos().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(delete(url).headers(jefe())).andExpect(status().isForbidden());
        mvc.perform(get(url).headers(jefe())).andExpect(status().isOk());
        mvc.perform(get(url).headers(supervisor())).andExpect(status().isNotFound());
        mvc.perform(put(url).headers(supervisor()).contentType(MediaType.APPLICATION_JSON).content(datos().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(delete(url).headers(supervisor())).andExpect(status().isNotFound());
    }

    @Test
    void validaVigenciasYCondicionesAunqueSeLlameDirectamenteALaApi() throws Exception {
        var sinTest = datos().put("edadBuzo", 46);
        mvc.perform(post("/api/faenas").headers(supervisor()).contentType(MediaType.APPLICATION_JSON)
                        .content(sinTest.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Los buzos mayores de 45 años requieren un test de esfuerzo vigente."));

        var examenVencido = datos().put("vigenciaExamenPreocupacional", "01/01/2026");
        mvc.perform(post("/api/faenas").headers(supervisor()).contentType(MediaType.APPLICATION_JSON)
                        .content(examenVencido.toString()))
                .andExpect(status().isUnprocessableEntity());

        var sinRescate = datos().put("buzoEmergenciaDisponible", false);
        mvc.perform(post("/api/faenas").headers(supervisor()).contentType(MediaType.APPLICATION_JSON)
                        .content(sinRescate.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rutaHistoricaNoCreaCierresSinValidarYRegistraElBloqueo() throws Exception {
        UUID id = UUID.randomUUID();
        faenas.guardar(id, "supervisor", json.treeToValue(datos(), AperturaDatos.class));
        String ruta = "/api/faenas/" + id + "/cierre-historico";
        String cierre = """
                {"cerradoEn":1795110000000,"tiempoFondoMinutos":25,"profundidadMaximaMetros":18,
                "descensoMaximo":25,"ascensoMaximo":10,"origen":"CENSUS"}
                """;
        mvc.perform(put(ruta).headers(jefe()).contentType(MediaType.APPLICATION_JSON).content(cierre))
                .andExpect(status().isForbidden());
        mvc.perform(put(ruta).headers(supervisor()).contentType(MediaType.APPLICATION_JSON).content(cierre))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/faenas/" + id + "/bloqueos").headers(jefe()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].operacion").value("CIERRE"))
                .andExpect(jsonPath("$[0].responsable").value("supervisor"))
                .andExpect(jsonPath("$[0].registradaEn").exists());
    }

    @Test
    void swaggerDescribeLasRutasYPermiteAutorizarUnaCuenta() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.cuenta.scheme").value("basic"))
                .andExpect(jsonPath("$.paths['/api/faenas'].post").exists())
                .andExpect(jsonPath("$.paths['/api/faenas/{id}'].get").exists());
    }

    private ObjectNode datos() throws Exception {
        JsonNode valores = json.readTree("""
                {
                  "centroCultivo": "Centro ficticio",
                  "centroId": "CENTRO-1",
                  "buzoAsignado": "Buzo ficticio",
                  "buzoId": "BUZO-1",
                  "fecha": "19/11/2026",
                  "hora": "08:00",
                  "edadBuzo": 45,
                  "imcBuzo": 24.0,
                  "puntajeCalbuco": 80,
                  "examenPreocupacionalVigente": true,
                  "vigenciaExamenPreocupacional": "31/12/2026",
                  "buzoEmergenciaDisponible": true,
                  "condicionesListas": true,
                  "embarcacionLista": true,
                  "comunicacionesOperativas": true
                }
                """);
        return (ObjectNode) valores;
    }

    private static HttpHeaders cuenta(String usuario, String clave) {
        var headers = new HttpHeaders();
        headers.setBasicAuth(usuario, clave);
        return headers;
    }

    private static HttpHeaders supervisor() {
        return cuenta("supervisor", "pin-supervisor-prueba");
    }

    private static HttpHeaders jefe() {
        return cuenta("jefe", "pin-jefe-prueba");
    }
}
