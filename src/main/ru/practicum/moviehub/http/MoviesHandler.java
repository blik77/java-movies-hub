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

    public static final String ERROR_400_WRONG_YEAR = "Некорректный параметр запроса — 'year'";
    public static final String ERROR_400_WRONG_YEAR_DESCRIPTION_1 = "Поддерживается только параметр year";
    public static final String ERROR_400_WRONG_YEAR_DESCRIPTION_2 = "year должен быть числом";
    public static final String ERROR_400_INCORRECT_ID = "Некорректный ID";
    public static final String ERROR_400_INCORRECT_ID_DESCRIPTION = "ID должен быть числом";
    public static final String ERROR_400_INCORRECT_JSON = "Некорректный JSON";
    public static final String ERROR_400_INCORRECT_JSON_DESCRIPTION = "Тело запроса должно быть корректным JSON";
    public static final String ERROR_404_MOVIE_NOT_FOUND = "Фильм не найден";
    public static final String ERROR_404_INCORRECT_QUERY = "Некорректный запрос";
    public static final String ERROR_405_WRONG_METHOD = "Метод не поддерживается";
    public static final String ERROR_405_WRONG_METHOD_DESCRIPTION = "Допустимые методы: GET, POST, DELETE";
    public static final String ERROR_405_WRONG_PATH = "Путь не поддерживается";
    public static final String ERROR_405_WRONG_PATH_DESCRIPTION = "Допустимый путь: /movies";
    public static final String ERROR_415_UNSUPPORTED_DATA_TYPE = "Неподдерживаемый тип данных";
    public static final String ERROR_415_UNSUPPORTED_DATA_TYPE_DESCRIPTION = "Ожидается тип данных формата json";
    public static final String ERROR_422_VALIDATION = "Ошибка валидации";
    public static final String ERROR_422_VALIDATION_TITLE_EMPTY = "Название не должно быть пустым";
    public static final String ERROR_422_VALIDATION_TITLE_LONG = "Длина названия не должна превышать " + MAX_MOVIE_TITLE_LENGTH + " символов";
    public static final String ERROR_422_VALIDATION_YEAR = "Год должен быть числом между " + MIN_MOVIE_YEAR + " и " + MAX_MOVIE_YEAR;

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
                    sendJson(ex, 405, new ErrorResponse(ERROR_405_WRONG_METHOD,
                        List.of(ERROR_405_WRONG_METHOD_DESCRIPTION)));
                    break;
            }
        } else {
            sendJson(ex, 405, new ErrorResponse(ERROR_405_WRONG_PATH,
                List.of(ERROR_405_WRONG_PATH_DESCRIPTION)));
        }
    }

    private void handleGet(HttpExchange ex, String[] queryParts) throws IOException {
        Map<String, String> queryParams = getQueryParams(ex);

        if (queryParts.length == 3) {
            getMovieById(ex, queryParts[2]);
        } else if (queryParams.isEmpty()) {
            sendJson(ex, 200, store.getAllMovies());
        } else if (queryParams.containsKey("year")) {
            getMoviesByYear(ex, queryParams.get("year"));
        } else {
            sendJson(ex, 400, new ErrorResponse(ERROR_400_WRONG_YEAR,
                List.of(ERROR_400_WRONG_YEAR_DESCRIPTION_1, ERROR_400_WRONG_YEAR_DESCRIPTION_2)));
        }
    }

    private void getMovieById(HttpExchange ex, String id) throws IOException {
        try {
            Movie movie = store.getMovieById(Integer.parseInt(id));
            if (movie == null) {
                sendJson(ex, 404, new ErrorResponse(ERROR_404_MOVIE_NOT_FOUND));
            } else {
                sendJson(ex, 200, movie);
            }
        } catch (NumberFormatException e) {
            sendJson(ex, 400, new ErrorResponse(ERROR_400_INCORRECT_ID,
                List.of(ERROR_400_INCORRECT_ID_DESCRIPTION)));
        }
    }

    private void getMoviesByYear(HttpExchange ex, String year) throws IOException {
        try {
            sendJson(ex, 200, store.getMoviesByYear(Integer.parseInt(year)));
        } catch (NumberFormatException e) {
            sendJson(ex, 400, new ErrorResponse(ERROR_400_WRONG_YEAR,
                List.of(ERROR_400_WRONG_YEAR_DESCRIPTION_2)));
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        if (ex.getRequestHeaders().getFirst("Content-Type") == null ||
                !ex.getRequestHeaders().getFirst("Content-Type").equalsIgnoreCase("application/json; charset=UTF-8")) {
            sendJson(ex, 415, new ErrorResponse(ERROR_415_UNSUPPORTED_DATA_TYPE,
                List.of(ERROR_415_UNSUPPORTED_DATA_TYPE_DESCRIPTION)));
        } else {
            String requestBody = new String(ex.getRequestBody().readAllBytes());
            JsonElement jsonElement = JsonParser.parseString(requestBody);
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String title = jsonObject.get("title").getAsString().trim();

                if (title.isEmpty()) {
                    sendJson(ex, 422, new ErrorResponse(ERROR_422_VALIDATION,
                            List.of(ERROR_422_VALIDATION_TITLE_EMPTY)));
                } else if (title.length() > MAX_MOVIE_TITLE_LENGTH) {
                    sendJson(ex, 422, new ErrorResponse(ERROR_422_VALIDATION,
                            List.of(ERROR_422_VALIDATION_TITLE_LONG)));
                } else {
                    try {
                        int year = Integer.parseInt(jsonObject.get("year").getAsString().trim());
                        if (year < MIN_MOVIE_YEAR || year > MAX_MOVIE_YEAR) {
                            sendJson(ex, 422, new ErrorResponse(ERROR_422_VALIDATION,
                                List.of(ERROR_422_VALIDATION_YEAR)));
                        } else {
                            sendJson(ex, 201, store.addMovie(title, year));
                        }
                    } catch (NumberFormatException e) {
                        sendJson(ex, 422, new ErrorResponse(ERROR_422_VALIDATION,
                            List.of(ERROR_422_VALIDATION_YEAR)));
                    }
                }
            } else {
                sendJson(ex, 400, new ErrorResponse(ERROR_400_INCORRECT_JSON,
                    List.of(ERROR_400_INCORRECT_JSON_DESCRIPTION)));
            }
        }
    }

    private void handleDelete(HttpExchange ex, String[] queryParts) throws IOException {
        if (queryParts.length == 3) {
            try {
                if (store.deleteMovieById(Integer.parseInt(queryParts[2]))) {
                    sendNoContent(ex);
                } else {
                    sendJson(ex, 404, new ErrorResponse(ERROR_404_MOVIE_NOT_FOUND));
                }
            } catch (NumberFormatException e) {
                sendJson(ex, 400, new ErrorResponse(ERROR_400_INCORRECT_ID,
                    List.of(ERROR_400_INCORRECT_ID_DESCRIPTION)));
            }
        } else {
            sendJson(ex, 404, new ErrorResponse(ERROR_404_INCORRECT_QUERY));
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
