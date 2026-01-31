package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MoviesStore {
    private final Map<Integer, Movie> store;
    int lastId = 0;

    public MoviesStore() {
        store = new HashMap<>();
    }

    public List<Movie> getAllMovies() {
        return store.values().stream().toList();
    }

    public Movie addMovie(String title, int year) {
        int newId = lastId++;
        Movie newMovie = new Movie(newId, title, year);
        store.put(newId, newMovie);
        return newMovie;
    }

    public Movie getMovieById(int id) {
        return store.get(id);
    }

    public boolean deleteMovieById(int id) {
        return store.remove(id) != null;
    }

    public List<Movie> getMoviesByYear(int year) {
        return store.values().stream().filter(m -> m.getYear() == year).toList();
    }

    public void deleteAllMovies() {
        store.clear();
        lastId = 0;
    }
}
