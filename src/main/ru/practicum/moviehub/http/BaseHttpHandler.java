package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        String json = GSON.toJson(body);

        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, json.getBytes().length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected void sendNoContent(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(204, -1);
    }
}
