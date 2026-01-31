package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final int SERVER_PORT = 8080;
    private static final String BASE_PATH = "http://localhost:" + SERVER_PORT;
    private static final String MOVIES_PATH = "/movies";

    private static final String CT_JSON = "application/json; charset=UTF-8";

    private static final Gson GSON = new Gson();

    private static final MoviesStore STORE = new MoviesStore();

    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(STORE, MOVIES_PATH, SERVER_PORT);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        STORE.deleteAllMovies();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "Статус должен быть 200 (GET /movies)");
        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""),
            "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("[]", body, "Для пустого хранилища должен возвращаться []");
    }

    @Test
    void getMovies_whenHasMovies_returnsArray() throws Exception {
        STORE.addMovie("Рокки", 1976);
        STORE.addMovie("Рокки 2", 1979);
        STORE.addMovie("Рокки 3", 1982);
        STORE.addMovie("Рокки 4", 1985);
        STORE.addMovie("Рокки 5", 1990);
        STORE.addMovie("Рокки Бальбоа", 2006);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "Статус должен быть 200 (GET /movies)");

        String body = resp.body();
        assertTrue(body.contains("Рокки 4"));
        assertTrue(body.contains("Рокки Бальбоа"));
    }

    @Test
    void postMovies_withFullData_addAndReturnMovie() throws Exception {
        String requestBody = "{ \"title\": \"Рокки Бальбоа\", \"year\": 2006 }";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH))
                .headers("Content-type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""),
            "Content-Type должен содержать формат данных и кодировку");
        assertEquals(201, resp.statusCode(), "Статус должен быть 201 (POST /movies)");

        Movie movie = GSON.fromJson(resp.body().trim(), Movie.class);
        assertEquals("Рокки Бальбоа", movie.getTitle(), "Название должно совпадать");
        assertEquals(2006, movie.getYear(), "Год фильма должен совпадать");
        assertEquals(0, movie.getId(), "ID равен 0 (ноль)");
    }

    @Test
    void postMovies_withWrongNullTitle_returnsError() throws Exception {
        String requestBody = "{ \"title\": \"\", \"year\": 2006 }";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH))
                .headers("Content-type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""),
            "Content-Type должен содержать формат данных и кодировку");
        assertEquals(422, resp.statusCode(), "Статус должен быть 422 (POST /movies)");
        assertTrue(resp.body().contains("Название не должно быть пустым"));
    }

    @Test
    void postMovies_withWrongLargeTitle_returnsError() throws Exception {
        String requestBody = "{ \"title\": \"" + ("X".repeat(123)) + "\", \"year\": 2006 }";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH))
                .headers("Content-type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""),
            "Content-Type должен содержать формат данных и кодировку");
        assertEquals(422, resp.statusCode(), "Статус должен быть 422 (POST /movies)");
        assertTrue(resp.body().contains("Длина названия не должна превышать"));
    }

    @Test
    void postMovies_withLessAcceptableYear_returnsError() throws Exception {
        String requestBody = "{ \"title\": \"Рокки Бальбоа\", \"year\": 1006 }";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH))
                .headers("Content-type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""),
            "Content-Type должен содержать формат данных и кодировку");
        assertEquals(422, resp.statusCode(), "Статус должен быть 422 (POST /movies)");
        assertTrue(resp.body().contains("Год должен быть числом между"));
    }

    @Test
    void postMovies_withMoreAcceptableYear_returnsError() throws Exception {
        String requestBody = "{ \"title\": \"Рокки Бальбоа\", \"year\": 3006 }";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH))
                .headers("Content-type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""),
            "Content-Type должен содержать формат данных и кодировку");
        assertEquals(422, resp.statusCode(), "Статус должен быть 422 (POST /movies)");
        assertTrue(resp.body().contains("Год должен быть числом между"));
    }

    @Test
    void postMovies_withWrongCT_returnsError() throws Exception {
        String requestBody = "{ \"title\": \"Рокки Бальбоа\", \"year\": 2006 }";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH))
                .headers("Content-type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""),
                "Content-Type должен содержать формат данных и кодировку");
        assertEquals(415, resp.statusCode(), "Статус должен быть 415 (POST /movies)");
        assertTrue(resp.body().contains("Ожидается тип данных формата json"));
    }

    @Test
    void postMovie_withWrongFormatJson_returnsError() throws Exception {
        String requestBody = "XXX";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH))
                .headers("Content-type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""),
                "Content-Type должен содержать формат данных и кодировку");
        assertEquals(400, resp.statusCode(), "Статус должен быть 400 (POST /movies)");
        assertTrue(resp.body().contains("Тело запроса должно быть корректным JSON"));
    }

    @Test
    void getMovies_whenHasMovies_returnsElement() throws Exception {
        STORE.addMovie("Рокки Бальбоа", 2006);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH + "/0"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""),
            "Content-Type должен содержать формат данных и кодировку");
        assertEquals(200, resp.statusCode(), "Статус должен быть 200 (GET /movies/{id})");
        assertTrue(resp.body().contains("\"id\": 0"));
        assertTrue(resp.body().contains("Рокки Бальбоа"));
        assertTrue(resp.body().contains("\"year\": 2006"));
    }

    @Test
    void getMovies_whenNotHasMovies_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH + "/100"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "Статус должен быть 404 (GET /movies/{id})");
        assertTrue(resp.body().contains("Фильм не найден"));
    }

    @Test
    void getMovies_whenWrongId_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH + "/X"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "Статус должен быть 400 (GET /movies/{id})");
        assertTrue(resp.body().contains("Некорректный ID"));
    }

    @Test
    void deleteMovies_whenHasMovies() throws Exception {
        STORE.addMovie("Рокки Бальбоа", 2006);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH + "/0"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(), "Статус должен быть 204 (DELETE /movies/{id})");
        assertTrue(STORE.getAllMovies().isEmpty(), "Фильм должен быть удалён из хранилища");
    }

    @Test
    void deleteMovies_whenNotHasMovies_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH + "/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "Статус должен быть 404 (DELETE /movies/{id})");
        assertTrue(resp.body().contains("Фильм не найден"));
    }

    @Test
    void deleteMovies_whenWrongId_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH + "/X"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "Статус должен быть 400 (DELETE /movies/{id})");
        assertTrue(resp.body().contains("Некорректный ID"));
    }

    @Test
    void getMovies_whenNonIncludeYear_returnsArrayEmpty() throws Exception {
        STORE.addMovie("Рокки", 1976);
        STORE.addMovie("Рокки 2", 1979);
        STORE.addMovie("Рокки 3", 1982);
        STORE.addMovie("Рокки 4", 1985);
        STORE.addMovie("Рокки 5", 1990);
        STORE.addMovie("Рокки Бальбоа", 2006);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH + "?year=2026"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "Статус должен быть 200 (GET /movies?year=)");
        assertTrue(resp.body().contains("[]"));
    }

    @Test
    void getMovies_whenWrongYear_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH + "?year=XYZ"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "Статус должен быть 400 (GET /movies?year=)");
        assertTrue(resp.body().contains("year должен быть числом"));
    }

    @Test
    void getMovies_whenValidYear_returnsArray() throws Exception {
        STORE.addMovie("Рокки", 1976);
        STORE.addMovie("Рокки 2", 1979);
        STORE.addMovie("Рокки 3", 1982);
        STORE.addMovie("Рокки 4", 1985);
        STORE.addMovie("Рокки 5", 1990);
        STORE.addMovie("Рокки Бальбоа", 2006);
        STORE.addMovie("Рокки Бальбоа (дубль)", 2006);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH + "?year=2006"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CT_JSON, resp.headers().firstValue("Content-Type").orElse(""),
                "Content-Type должен содержать формат данных и кодировку");
        assertEquals(200, resp.statusCode(), "Статус должен быть 200 (GET /movies?year=)");
        List<Movie> movies = GSON.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());
        assertTrue(movies.stream().allMatch(m -> m.getYear() == 2006));
    }

    @Test
    void wrongMethod_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_PATH + MOVIES_PATH))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, resp.statusCode(), "Статус должен быть 405");
        assertTrue(resp.body().contains("Допустимые методы: GET, POST, DELETE"));
    }
}
