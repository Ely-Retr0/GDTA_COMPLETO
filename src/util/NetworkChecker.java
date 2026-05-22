package util;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * GDTA — Verificador de conectividad
 * Detecta si hay acceso a internet / APIs antes de intentar llamadas.
 */
public class NetworkChecker {

    private static final int TIMEOUT_MS = 4000;

    /** Verifica acceso a la API de Anthropic */
    public static boolean hayAccesoIA() {
        return ping("https://api.anthropic.com");
    }

    /** Verifica acceso general a internet */
    public static boolean hayInternet() {
        return ping("https://www.google.com");
    }

    private static boolean ping(String urlStr) {
        try {
            HttpURLConnection con = (HttpURLConnection)
                new URL(urlStr).openConnection();
            con.setConnectTimeout(TIMEOUT_MS);
            con.setReadTimeout(TIMEOUT_MS);
            con.setRequestMethod("HEAD");
            int code = con.getResponseCode();
            return code < 500;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Devuelve el estado de red como texto para mostrar en UI.
     */
    public static String getEstado() {
        if (!hayInternet())   return "SIN_INTERNET";
        if (!hayAccesoIA())   return "API_BLOQUEADA";
        return "OK";
    }
}
