package com.github.klefstad_teaching.cs122b.movies.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.klefstad_teaching.cs122b.core.error.ResultError;
import com.github.klefstad_teaching.cs122b.core.security.JWTManager;
import com.github.klefstad_teaching.cs122b.movies.model.response.MovieGetByMovieIdResponse;
import com.github.klefstad_teaching.cs122b.movies.model.response.MovieSearchResponse;
import com.github.klefstad_teaching.cs122b.movies.model.response.PersonSearchResponse;
import com.github.klefstad_teaching.cs122b.movies.repo.MovieRepo;
import com.github.klefstad_teaching.cs122b.movies.repo.entity.*;
import com.github.klefstad_teaching.cs122b.movies.util.Validate;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.github.klefstad_teaching.cs122b.core.result.MoviesResults;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

@RestController
public class MovieController
{
    private final MovieRepo repo;
    private final Validate validate;

    @Autowired
    public MovieController(MovieRepo repo, Validate validate)
    {
        this.repo = repo;
        this.validate = validate;
    }

    @GetMapping("/movie/search")
    public ResponseEntity<MovieSearchResponse> movieSearch(@AuthenticationPrincipal SignedJWT user, MovieQuery movieQuery) throws JsonProcessingException, ParseException {
        List<String> roles = user.getJWTClaimsSet().getStringListClaim(JWTManager.CLAIM_ROLES);
        System.out.println(Arrays.toString(roles.toArray()));
        boolean showHidden = false;
        if (roles.contains("ADMIN") || roles.contains("EMPLOYEE"))
        {
            showHidden = true;
        }

        Movie[] movies = repo.searchMovies(movieQuery, showHidden);
        MovieSearchResponse response = new MovieSearchResponse();
        response.setMovies(movies);
        response.setResult(MoviesResults.MOVIES_FOUND_WITHIN_SEARCH);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/movie/search/person/{personId}")
    public ResponseEntity<MovieSearchResponse> movieSearchByPersonId(@PathVariable Long personId, @AuthenticationPrincipal SignedJWT user, MovieByPersonIdQuery movieByPersonIdQuery) throws ParseException, JsonProcessingException {
        List<String> roles = user.getJWTClaimsSet().getStringListClaim(JWTManager.CLAIM_ROLES);
        System.out.println(Arrays.toString(roles.toArray()));
        boolean showHidden = false;
        if (roles.contains("ADMIN") || roles.contains("EMPLOYEE"))
        {
            showHidden = true;
        }
        Movie[] movies = repo.searchMoviesByPersonId(personId, movieByPersonIdQuery, showHidden);
        MovieSearchResponse response = new MovieSearchResponse();
        response.setMovies(movies);
        response.setResult(MoviesResults.MOVIES_WITH_PERSON_ID_FOUND);
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<MovieGetByMovieIdResponse> movieGetByMovieId(@PathVariable Long movieId, @AuthenticationPrincipal SignedJWT user) throws ParseException, JsonProcessingException {
        List<String> roles = user.getJWTClaimsSet().getStringListClaim(JWTManager.CLAIM_ROLES);
        System.out.println(Arrays.toString(roles.toArray()));
        boolean showHidden = false;
        if (roles.contains("ADMIN") || roles.contains("EMPLOYEE"))
        {
            showHidden = true;
        }
        MovieGetByMovieIdResponse response = new MovieGetByMovieIdResponse();

        try {
            Movie movie = repo.getMovieDetail(movieId, showHidden);
            Genre[] genres = repo.getMovieGenres(movieId);
            Person[] persons = repo.getMoviePersons(movieId);
            response.setMovie(movie);
            response.setGenres(genres);
            response.setPersons(persons);
        } catch (EmptyResultDataAccessException e) {
            throw new ResultError(MoviesResults.NO_MOVIE_WITH_ID_FOUND);
        }


        response.setResult(MoviesResults.MOVIE_WITH_ID_FOUND);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }




}
