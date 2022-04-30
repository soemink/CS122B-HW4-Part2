package com.github.klefstad_teaching.cs122b.movies.model.response;

import com.github.klefstad_teaching.cs122b.core.result.Result;
import com.github.klefstad_teaching.cs122b.movies.repo.entity.Genre;
import com.github.klefstad_teaching.cs122b.movies.repo.entity.Movie;
import com.github.klefstad_teaching.cs122b.movies.repo.entity.Person;

public class MovieGetByMovieIdResponse {
    private Result result;
    private Movie movie;
    private Genre[] genres;
    private Person[] persons;

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public Genre[] getGenres() {
        return genres;
    }

    public void setGenres(Genre[] genres) {
        this.genres = genres;
    }

    public Person[] getPersons() {
        return persons;
    }

    public void setPersons(Person[] persons) {
        this.persons = persons;
    }
}
