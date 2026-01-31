package ru.practicum.moviehub.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;

import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

public class MoviesHandler extends BaseHttpHandler {
    private static final int MAX_MOVIE_TITLE_LENGTH = 100;
    private static final int MIN_MOVIE_YEAR = 1888;
    private static final int MAX_MOVIE_YEAR = LocalDate.now().getYear() + 1;

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String[] queryParts = ex.getRequestURI().getPath().split("/");
        if (queryParts[1].equals("movies")) {
            switch (ex.getRequestMethod()) {
                case "GET":
                    handleGet(ex, queryParts);
                    break;
                case "POST":
                    handlePost(ex);
                    break;
                case "DELETE":
                    handleDelete(ex, queryParts);
                    break;
                default:
                    sendJson(ex, 405, new ErrorResponse("Метод не поддерживается",
                        List.of("Допустимые методы: GET, POST, DELETE")));
                    break;
            }
        } else {
            sendJson(ex, 405, new ErrorResponse("Путь не поддерживается",
                List.of("Допустимый путь: /movies")));
        }
    }

    private void handleGet(HttpExchange ex, String[] queryParts) throws IOException {
        Map<String, String> queryParams = getQueryParams(ex);

        if (queryParts.length == 3) {
            try {
                Movie movie = store.getMovieById(Integer.parseInt(queryParts[2]));
                if (movie == null) {
                    sendJson(ex, 404, new ErrorResponse("Фильм не найден"));
                } else {
                    sendJson(ex, 200, movie);
                }
            } catch (NumberFormatException e) {
                sendJson(ex, 400, new ErrorResponse("Некорректный ID", List.of("ID должен быть числом")));
            }
        } else if (queryParams.isEmpty()) {
            sendJson(ex, 200, store.getAllMovies());
        } else if (queryParams.containsKey("year")) {
            try {
                sendJson(ex, 200, store.getMoviesByYear(Integer.parseInt(queryParams.get("year"))));
            } catch (NumberFormatException e) {
                sendJson(ex, 400, new ErrorResponse("Некорректный параметр запроса — 'year'",
                    List.of("year должен быть числом")));
            }
        } else {
            sendJson(ex, 400, new ErrorResponse("Некорректный параметр запроса — 'year'",
                List.of("Поддерживается только параметр year", "year должен быть числом")));
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        if (ex.getRequestHeaders().getFirst("Content-Type").equalsIgnoreCase("application/json; charset=UTF-8")) {
            String requestBody = new String(ex.getRequestBody().readAllBytes());
            JsonElement jsonElement = JsonParser.parseString(requestBody);
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String title = jsonObject.get("title").getAsString().trim();

                if (title.isEmpty()) {
                    sendJson(ex, 422, new ErrorResponse("Ошибка валидации",
                            List.of("Название не должно быть пустым")));
                } else if (title.length() > MAX_MOVIE_TITLE_LENGTH) {
                    sendJson(ex, 422, new ErrorResponse("Ошибка валидации",
                            List.of("Длина названия не должна превышать " + MAX_MOVIE_TITLE_LENGTH + " символов")));
                } else {
                    try {
                        int year = Integer.parseInt(jsonObject.get("year").getAsString().trim());
                        if (year < MIN_MOVIE_YEAR || year > MAX_MOVIE_YEAR) {
                            sendJson(ex, 422, new ErrorResponse("Ошибка валидации",
                                List.of("Год должен быть числом между " + MIN_MOVIE_YEAR + " и " + MAX_MOVIE_YEAR)));
                        } else {
                            sendJson(ex, 201, store.addMovie(title, year));
                        }
                    } catch (NumberFormatException e) {
                        sendJson(ex, 422, new ErrorResponse("Ошибка валидации",
                            List.of("Год должен быть числом между " + MIN_MOVIE_YEAR + " и " + MAX_MOVIE_YEAR)));
                    }
                }
            } else {
                sendJson(ex, 400, new ErrorResponse("Некорректный JSON",
                    List.of("Тело запроса должно быть корректным JSON")));
            }
        } else {
            sendJson(ex, 415, new ErrorResponse("Неподдерживаемый тип данных",
                List.of("Ожидается тип данных формата json")));
        }
    }

    private void handleDelete(HttpExchange ex, String[] queryParts) throws IOException {
        if (queryParts.length == 3) {
            try {
                if (store.deleteMovieById(Integer.parseInt(queryParts[2]))) {
                    sendNoContent(ex);
                } else {
                    sendJson(ex, 404, new ErrorResponse("Фильм не найден"));
                }
            } catch (NumberFormatException e) {
                sendJson(ex, 400, new ErrorResponse("Некорректный ID", List.of("ID должен быть числом")));
            }
        } else {
            sendJson(ex, 404, new ErrorResponse("Некорректный запрос"));
        }
    }

    private Map<String, String> getQueryParams(HttpExchange ex) {
        String query = ex.getRequestURI().getQuery();
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);

            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";

            result.put(key, value);
        }

        return result;
    }
}
