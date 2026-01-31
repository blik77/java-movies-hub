package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

public class MovieHubApp {
    private static final String MOVIES_PATH = "/movies";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        final MoviesServer server = new MoviesServer(new MoviesStore(), MOVIES_PATH, SERVER_PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}
