import { iteraciones } from "./password.js";

const salSinUsuario = new Uint8Array(16);
const verificadorSinUsuario = new Uint8Array(32);

function json(body, status = 200, headers = {}) {
    return Response.json(body, {
        status,
        headers: { "Cache-Control": "no-store", ...headers },
    });
}

function credenciales(request) {
    const header = request.headers.get("Authorization") ?? "";
    if (header.length > 384) return null;
    const encoded = /^Basic ([A-Za-z0-9+/]+={0,2})$/i.exec(header);
    if (!encoded) return null;
    try {
        const valor = atob(encoded[1]);
        const separador = valor.indexOf(":");
        if (separador < 1 || separador === valor.length - 1) return null;
        return { nombre: valor.slice(0, separador).toLowerCase(), clave: valor.slice(separador + 1) };
    } catch {
        return null;
    }
}

async function autenticar(request, env) {
    const acceso = credenciales(request);
    const rechazar = () => json({ error: "credenciales_invalidas" }, 401);
    if (!acceso) return rechazar();

    const ip = request.headers.get("CF-Connecting-IP") ?? "local";
    const limite = await env.INGRESOS.limit({ key: ip });
    if (!limite.success) {
        return json({ error: "demasiados_intentos" }, 429, { "Retry-After": "60" });
    }

    const cuenta = await env.DB.prepare(
        "SELECT nombre, rol, sal, verificador FROM usuarios WHERE nombre = ? AND activo = 1"
    ).bind(acceso.nombre).first();
    const sal = cuenta ? Uint8Array.fromBase64(cuenta.sal) : salSinUsuario;
    const esperado = cuenta ? Uint8Array.fromBase64(cuenta.verificador) : verificadorSinUsuario;
    const material = await crypto.subtle.importKey(
        "raw", new TextEncoder().encode(acceso.clave), "PBKDF2", false, ["deriveBits"]
    );
    const verificador = await crypto.subtle.deriveBits(
        { name: "PBKDF2", hash: "SHA-256", iterations: iteraciones, salt: sal }, material, 256
    );
    if (!crypto.subtle.timingSafeEqual(verificador, esperado) || !cuenta) return rechazar();
    return json({ nombre: cuenta.nombre, rol: cuenta.rol });
}

export default {
    async fetch(request, env) {
        const url = new URL(request.url);
        if (url.protocol !== "https:") return json({ error: "https_requerido" }, 400);
        if (url.pathname !== "/api/sesion") return json({ error: "no_encontrado" }, 404);
        if (request.method !== "GET") return json({ error: "metodo_no_permitido" }, 405, { Allow: "GET" });
        return autenticar(request, env);
    },
};
