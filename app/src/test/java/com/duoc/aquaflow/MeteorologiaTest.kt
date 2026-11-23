package com.duoc.aquaflow

import com.duoc.aquaflow.data.remote.MeteorologiaApi
import com.duoc.aquaflow.data.remote.PronosticoMaritimo
import com.duoc.aquaflow.data.remote.RespuestaMaritima
import com.duoc.aquaflow.data.repository.MeteorologiaRepository
import com.duoc.aquaflow.data.repository.ResultadoMeteorologia
import com.duoc.aquaflow.model.CENTROS_MARITIMOS
import com.duoc.aquaflow.viewmodel.MeteorologiaViewModel
import com.google.gson.JsonSyntaxException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class MeteorologiaTest {
    private val centro = CENTROS_MARITIMOS[0]
    private val api = mockk<MeteorologiaApi>()
    private val pronostico = PronosticoMaritimo(1_790_000_000, 1.2, 8.0, 230.0, null)
    private val respuesta = RespuestaMaritima(-42.0, -73.0, pronostico)

    @Test fun consultaUsaCoordenadasDelCentroYSeparaHoraDelModeloYDeConsulta() = runTest {
        coEvery { api.consultar(centro.latitud, centro.longitud) } returns respuesta
        val resultado = MeteorologiaRepository(api, { true }, { 1_790_000_099_000 }).consultar(centro)
            as ResultadoMeteorologia.Disponible
        assertEquals(1_790_000_000_000L, resultado.condiciones.horaModelo)
        assertEquals(1_790_000_099_000L, resultado.condiciones.consultadaEn)
        assertEquals(1.2, resultado.condiciones.alturaOla)
        assertNull(resultado.condiciones.temperaturaSuperficial)
        coVerify(exactly = 1) { api.consultar(centro.latitud, centro.longitud) }
    }

    @Test fun sinConexionEvitaLaLlamadaYLaPerdidaDeRedDuranteLaConsultaSeDistingue() = runTest {
        var hayRed = false
        val repositorio = MeteorologiaRepository(api, { hayRed })
        assertEquals(ResultadoMeteorologia.SinConexion, repositorio.consultar(centro))
        coVerify(exactly = 0) { api.consultar(any(), any()) }
        hayRed = true
        coEvery { api.consultar(any(), any()) } coAnswers { hayRed = false; throw IOException() }
        assertEquals(ResultadoMeteorologia.SinConexion, repositorio.consultar(centro))
    }

    @Test fun distingueErroresDeServicioRespuestaInvalidaYAusenciaDeDatos() = runTest {
        val repositorio = MeteorologiaRepository(api, { true })
        coEvery { api.consultar(any(), any()) } throws HttpException(Response.error<String>(503, "".toResponseBody()))
        assertTrue((repositorio.consultar(centro) as ResultadoMeteorologia.Error).mensaje.contains("503"))
        coEvery { api.consultar(any(), any()) } throws IOException()
        assertTrue(repositorio.consultar(centro) is ResultadoMeteorologia.Error)
        coEvery { api.consultar(any(), any()) } throws JsonSyntaxException("respuesta inválida")
        assertTrue(repositorio.consultar(centro) is ResultadoMeteorologia.Error)
        val invalidas = listOf(respuesta.copy(latitude = null), respuesta.copy(longitude = null),
            respuesta.copy(current = null), respuesta.copy(current = pronostico.copy(time = null)),
            respuesta.copy(current = pronostico.copy(time = 0)),
            respuesta.copy(current = PronosticoMaritimo(1, null, null, null, null)))
        for (invalida in invalidas) {
            coEvery { api.consultar(any(), any()) } returns invalida
            assertTrue(repositorio.consultar(centro) is ResultadoMeteorologia.Error)
        }
    }

    @Test fun cambiaDeCentroCancelaLaConsultaAnteriorYPermiteReintentar() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val siguiente = CENTROS_MARITIMOS[1]
            val primera = CompletableDeferred<RespuestaMaritima>()
            coEvery { api.consultar(centro.latitud, centro.longitud) } coAnswers { primera.await() }
            coEvery { api.consultar(siguiente.latitud, siguiente.longitud) } returns respuesta
            val vm = MeteorologiaViewModel(MeteorologiaRepository(api, { true }))
            vm.actualizar()
            vm.seleccionar(centro.id)
            vm.actualizar()
            runCurrent()
            assertTrue(vm.ui.value.cargando)
            vm.seleccionar(siguiente.id)
            advanceUntilIdle()
            primera.complete(respuesta)
            assertEquals(siguiente, vm.ui.value.centro)
            assertTrue(vm.ui.value.resultado is ResultadoMeteorologia.Disponible)
            vm.seleccionar(siguiente.id)
            vm.actualizar()
            advanceUntilIdle()
            coVerify(exactly = 2) { api.consultar(siguiente.latitud, siguiente.longitud) }
            vm.seleccionar("")
            assertNull(vm.ui.value.centro)
            assertNull(vm.ui.value.resultado)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
