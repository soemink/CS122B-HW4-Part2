package com.github.klefstad_teaching.cs122b.movies.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.klefstad_teaching.cs122b.core.error.ResultError;
import com.github.klefstad_teaching.cs122b.core.result.MoviesResults;
import com.github.klefstad_teaching.cs122b.movies.model.response.MovieSearchResponse;
import com.github.klefstad_teaching.cs122b.movies.repo.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.Map;

@Component
public class MovieRepo
{
    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;

    @Autowired
    public MovieRepo(ObjectMapper objectMapper, NamedParameterJdbcTemplate template)
    {
        this.template = template;
        this.objectMapper = objectMapper;
    }

    public Movie[] searchMovies(MovieQuery movieQuery, boolean showHidden) throws JsonProcessingException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        String sql =
                "SELECT JSON_ARRAYAGG(JSON_OBJECT('id', s.id, 'title', s.title, 'year', s.year, 'director', s.name, 'rating', s.rating, 'backdropPath', s.backdrop_path, 'posterPath', s.poster_path, 'hidden', s.hidden)) " +
                "FROM (SELECT m.id, m.title, m.year, director.name, m.rating, m.backdrop_path, m.poster_path, m.hidden " +
                "FROM movies.movie m " +
                "JOIN movies.person director ON director.id = m.director_id ";
        if (movieQuery.getGenre() != null)
        {
            sql += "JOIN movies.movie_genre mg on mg.movie_id = m.id JOIN movies.genre g on g.id = mg.genre_id ";
        }
        boolean whereAdded = false;
        if (movieQuery.getTitle() != null)
        {
            if (whereAdded)
            {
                sql += "AND ";
            } else {
                sql += "WHERE ";
                whereAdded = true;
            }
            sql += "m.title LIKE :title ";
            String wildcardSearch = '%' + movieQuery.getTitle() + "%";
            source.addValue("title", wildcardSearch, Types.VARCHAR);
        }

        if (movieQuery.getGenre() != null)
        {
            if (whereAdded)
            {
                sql += "AND ";
            } else {
                sql += "WHERE ";
                whereAdded = true;
            }
            sql += "g.name LIKE :genre ";
            String wildcardSearch = '%' + movieQuery.getGenre() + "%";
            source.addValue("genre", wildcardSearch, Types.VARCHAR);
        }

        if (movieQuery.getDirector() != null)
        {
            if (whereAdded)
            {
                sql += "AND ";
            } else {
                sql += "WHERE ";
                whereAdded = true;
            }
            sql += "director.name LIKE :director ";
            String wildcardSearch = '%' + movieQuery.getDirector() + '%';
            source.addValue("director", wildcardSearch, Types.VARCHAR);
        }

        if (movieQuery.getYear() != null)
        {
            if (whereAdded)
            {
                sql += "AND ";
            } else {
                sql += "WHERE ";
                whereAdded = true;
            }
            sql += "m.year = :year ";
            source.addValue("year", movieQuery.getYear(), Types.INTEGER);
        }

        if (showHidden == false)
        {
            if (whereAdded)
            {
                sql += "AND ";
            } else {
                sql += "WHERE ";
                whereAdded = true;
            }
            sql += "m.hidden = 0 ";
        }

        sql += "ORDER BY ";
        if (movieQuery.getOrderBy() == null)
        {
            sql += "m.title ";
        } else {
            if (movieQuery.getOrderBy() != "title" && movieQuery.getOrderBy() != "rating" && movieQuery.getOrderBy() != "year") {
                throw new ResultError(MoviesResults.INVALID_ORDER_BY);
            } else {
                sql += "m." + movieQuery.getOrderBy() + " ";
            }
        }

        if (movieQuery.getDirection() == null)
        {
            sql += "ASC, m.id ";
        } else {
            if (movieQuery.getDirection() != "asc" && movieQuery.getDirection() != "desc") {
                throw new ResultError(MoviesResults.INVALID_DIRECTION);
            } else {
                sql += movieQuery.getDirection() + ", m.id ";
            }
        }

        sql += "LIMIT ";
        Integer limit;
        if (movieQuery.getLimit() == null)
        {
            sql += "10 ";
            limit = 10;
        } else {
            if (movieQuery.getLimit() != 10 && movieQuery.getLimit() != 25 && movieQuery.getLimit()!=50 && movieQuery.getLimit() != 100)
            {
                throw new ResultError(MoviesResults.INVALID_LIMIT);
            } else {
                sql += movieQuery.getLimit() + " ";
                limit = movieQuery.getLimit();
            }
        }

        sql += "OFFSET ";
        if (movieQuery.getPage() == null) {
            sql += "0) s;";
        } else {
            if (((movieQuery.getPage() - 1) * limit) < 1)
            {
                throw new ResultError(MoviesResults.INVALID_PAGE);
            }
            else {
                sql += String.valueOf((movieQuery.getPage() - 1) * limit);
                sql += ") s;";

            }
        }

        System.out.println(sql);

        String jsonArrayString = this.template.queryForObject(sql, source, (rs,rowNum)-> rs.getString(1));

        if (jsonArrayString == null) {
            throw new ResultError(MoviesResults.NO_MOVIES_FOUND_WITHIN_SEARCH);
        }

        return objectMapper.readValue(jsonArrayString, Movie[].class);

    }

    public Movie[] searchMoviesByPersonId(Long personId, MovieByPersonIdQuery movieByPersonIdQuery, boolean showHidden) throws JsonProcessingException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        String sql =
                "SELECT JSON_ARRAYAGG(JSON_OBJECT('id', s.id, 'title', s.title, 'year', s.year, 'director', s.director_name, 'rating', s.rating, 'backdropPath', s.backdrop_path, 'posterPath', s.poster_path, 'hidden', s.hidden)) " +
                "FROM (SELECT m.id, m.title, m.year, director.name AS director_name, m.rating, m.backdrop_path, m.poster_path, m.hidden " +
                "FROM movies.movie_person p " +
                "JOIN movies.movie m ON m.id = p.movie_id " +
                "JOIN movies.person director ON director.id = m.director_id " +
                "WHERE p.person_id = :personId ";

        source.addValue("personId", personId, Types.INTEGER);

        if (showHidden == false)
        {
            sql += "AND m.hidden = 0 ";
        }

        sql += "ORDER BY ";
        if (movieByPersonIdQuery.getOrderBy() == null)
        {
            sql += "m.title ";
        } else {
            if (movieByPersonIdQuery.getOrderBy() != "title" && movieByPersonIdQuery.getOrderBy() != "rating" && movieByPersonIdQuery.getOrderBy() != "year") {
                throw new ResultError(MoviesResults.INVALID_ORDER_BY);
            } else {
                sql += "m." + movieByPersonIdQuery.getOrderBy() + " ";
            }
        }

        if (movieByPersonIdQuery.getDirection() == null)
        {
            sql += "ASC, m.id ";
        } else {
            if (movieByPersonIdQuery.getDirection() != "asc" && movieByPersonIdQuery.getDirection() != "desc") {
                throw new ResultError(MoviesResults.INVALID_DIRECTION);
            } else {
                sql += movieByPersonIdQuery.getDirection() + ", m.id ";
            }
        }

        sql += "LIMIT ";
        Integer limit;
        if (movieByPersonIdQuery.getLimit() == null)
        {
            sql += "10 ";
            limit = 10;
        } else {
            if (movieByPersonIdQuery.getLimit() != 10 && movieByPersonIdQuery.getLimit() != 25 && movieByPersonIdQuery.getLimit()!=50 && movieByPersonIdQuery.getLimit() != 100)
            {
                throw new ResultError(MoviesResults.INVALID_LIMIT);
            } else {
                sql += movieByPersonIdQuery.getLimit() + " ";
                limit = movieByPersonIdQuery.getLimit();
            }
        }

        sql += "OFFSET ";
        if (movieByPersonIdQuery.getPage() == null) {
            sql += "0) s;";
        } else {
            if (((movieByPersonIdQuery.getPage() - 1) * limit) < 1)
            {
                throw new ResultError(MoviesResults.INVALID_PAGE);
            }
            else {
                sql += String.valueOf((movieByPersonIdQuery.getPage() - 1) * limit);
                sql += ") s;";

            }
        }
        String jsonArrayString = this.template.queryForObject(sql, source, (rs,rowNum)-> rs.getString(1));
        if (jsonArrayString == null) {
            throw new ResultError(MoviesResults.NO_MOVIES_WITH_PERSON_ID_FOUND);
        }

        return objectMapper.readValue(jsonArrayString, Movie[].class);
    }

    public Movie getMovieDetail(Long movieId, boolean showHidden) throws JsonProcessingException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        String sql =
                "SELECT JSON_OBJECT('id', m.id, 'title', m.title, 'year', m.year, 'director', director.name, 'rating', m.rating, 'numVotes', m.num_votes, 'budget', m.budget, 'revenue', m.revenue, 'overview', m.overview, 'backdropPath', m.backdrop_path, 'posterPath', m.poster_path, 'hidden', m.hidden) " +
                        "FROM movies.movie m " +
                        "JOIN movies.person director ON director.id = m.director_id " +
                        "WHERE m.id = :movieId ";
        source.addValue("movieId", movieId, Types.INTEGER);
        if (showHidden == false)
        {
            sql += "AND m.hidden = 0 ";
        }
        String jsonArrayString = this.template.queryForObject(sql, source, (rs, rowNum)-> rs.getString(1));
        if (jsonArrayString == null) {
            throw new ResultError(MoviesResults.NO_MOVIE_WITH_ID_FOUND);
        }
//        System.out.println(jsonArrayString);

        return objectMapper.readValue(jsonArrayString, Movie.class);
    }

    public Genre[] getMovieGenres(Long movieId) throws JsonProcessingException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        String sql =
                "SELECT JSON_ARRAYAGG(JSON_OBJECT('id', s.id, 'name', s.name)) " +
                        "FROM (SELECT DISTINCT g.id, g.name " +
                        "FROM movies.genre g " +
                        "JOIN movies.movie_genre mg ON mg.genre_id = g.id " +
                        "JOIN movies.movie m ON m.id = mg.movie_id " +
                        "WHERE m.id = :movieId " +
                        "ORDER BY g.name) s";
        source.addValue("movieId", movieId, Types.INTEGER);
//        if (showHidden == false)
//        {
//            sql += "AND m.hidden = 0 ";
//        }
        String jsonArrayString = this.template.queryForObject(sql, source, (rs, rowNum)-> rs.getString(1));
        if (jsonArrayString == null) {
            throw new ResultError(MoviesResults.NO_MOVIE_WITH_ID_FOUND);
        }
        return objectMapper.readValue(jsonArrayString, Genre[].class);
    }

    public Person[] getMoviePersons(Long movieId) throws JsonProcessingException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        String sql =
                "SELECT JSON_ARRAYAGG(JSON_OBJECT('id', s.id,'name', s.name)) " +
                        "FROM (SELECT DISTINCT p.id, p.name, p.popularity " +
                        "FROM movies.person p " +
                        "JOIN movies.movie_person mp ON mp.person_id = p.id " +
                        "JOIN movies.movie m ON m.id = mp.movie_id " +
                        "WHERE m.id = :movieId " +
                        "ORDER BY p.popularity DESC, p.id) s";
        source.addValue("movieId", movieId, Types.INTEGER);
//        if (showHidden == false)
//        {
//            sql += "AND m.hidden = 0 ";
//        }
        String jsonArrayString = this.template.queryForObject(sql, source, (rs, rowNum)-> rs.getString(1));
        if (jsonArrayString == null) {
            throw new ResultError(MoviesResults.NO_MOVIE_WITH_ID_FOUND);
        }
        return objectMapper.readValue(jsonArrayString, Person[].class);
    }

    public PersonSearchModel[]  searchPerson(PersonQuery personQuery, boolean showHidden) throws JsonProcessingException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        String sql =
                "SELECT JSON_ARRAYAGG(JSON_OBJECT('id', s.id, 'name', s.name, 'birthday', s.birthday, 'biography', s.biography, 'birthplace', s.birthplace, 'popularity', s.popularity, 'profilePath', s.profile_path))\n" +
                        "FROM (SELECT DISTINCT p.id, p.name, p.birthday, p.biography, p.birthplace, p.popularity, p.profile_path " +
                        "FROM movies.person p ";
        if (personQuery.getMovieTitle() != null) {
            sql += "JOIN movies.movie_person mp ON p.id = mp.person_id JOIN movies.movie m on m.id = mp.movie_id ";
        }
        boolean whereAdded = false;
        if (personQuery.getName() != null) {
            if (whereAdded) {
                sql += "AND ";
            } else{
                sql += "WHERE ";
                whereAdded = true;
            }
            sql += "p.name LIKE :name ";
            String wildCardSearch = "%" + personQuery.getName() + "%";
            source.addValue("name", wildCardSearch, Types.VARCHAR);
        }

        if (personQuery.getBirthday() != null) {
            if (whereAdded) {
                sql += "AND ";
            } else{
                sql += "WHERE ";
                whereAdded = true;
            }
            sql += "p.birthday = :birthday ";
            source.addValue("birthday", personQuery.getBirthday(), Types.VARCHAR);
        }

        if (personQuery.getMovieTitle() != null) {
            if (whereAdded) {
                sql += "AND ";
            } else{
                sql += "WHERE ";
                whereAdded = true;
            }
            sql += "m.title LIKE :title ";
            String wildCardSearch = "%" + personQuery.getMovieTitle() + "%";
            source.addValue("title", wildCardSearch, Types.VARCHAR);
        }

        if (showHidden == false)
        {
            if (whereAdded)
            {
                sql += "AND ";
            } else {
                sql += "WHERE ";
                whereAdded = true;
            }
            sql += "m.hidden = 0 ";
        }

        sql += "ORDER BY ";
        if (personQuery.getOrderBy() == null)
        {
            sql += "p.name ";
        } else {
            if (personQuery.getOrderBy() != "name" && personQuery.getOrderBy() != "popularity" && personQuery.getOrderBy() != "birthday") {
                throw new ResultError(MoviesResults.INVALID_ORDER_BY);
            } else {
                sql += "p." + personQuery.getOrderBy() + " ";
            }
        }

        if (personQuery.getDirection() == null)
        {
            sql += "ASC, p.id ";
        } else {
            if (personQuery.getDirection() != "asc" && personQuery.getDirection() != "desc") {
                throw new ResultError(MoviesResults.INVALID_DIRECTION);
            } else {
                sql += personQuery.getDirection() + ", p.id ";
            }
        }

        sql += "LIMIT ";
        Integer limit;
        if (personQuery.getLimit() == null)
        {
            sql += "10 ";
            limit = 10;
        } else {
            if (personQuery.getLimit() != 10 && personQuery.getLimit() != 25 && personQuery.getLimit()!=50 && personQuery.getLimit() != 100)
            {
                throw new ResultError(MoviesResults.INVALID_LIMIT);
            } else {
                sql += personQuery.getLimit() + " ";
                limit = personQuery.getLimit();
            }
        }

        sql += "OFFSET ";
        if (personQuery.getPage() == null) {
            sql += "0) s;";
        } else {
            if (((personQuery.getPage() - 1) * limit) < 1)
            {
                throw new ResultError(MoviesResults.INVALID_PAGE);
            }
            else {
                sql += String.valueOf((personQuery.getPage() - 1) * limit);
                sql += ") s;";

            }
        }

        String jsonArrayString = this.template.queryForObject(sql, source, (rs,rowNum)-> rs.getString(1));
        if (jsonArrayString == null) {
            throw new ResultError(MoviesResults.NO_PERSONS_FOUND_WITHIN_SEARCH);
        }

        return objectMapper.readValue(jsonArrayString, PersonSearchModel[].class);
    }

    public PersonSearchModel getPersonByPersonId(Long personId, boolean showHidden) throws JsonProcessingException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        String sql =
                "SELECT JSON_OBJECT('id', p.id, 'name', p.name, 'birthday', p.birthday, 'biography', p.biography, 'birthplace', p.birthplace, 'popularity', p.popularity, 'profilePath', p.profile_path) " +
                        "FROM movies.person p " +
                        "WHERE p.id = :id";
        source.addValue("id", personId, Types.INTEGER);
        if (showHidden == false)
        {
            sql += "AND m.hidden = 0 ";
        }
        String jsonArrayString = this.template.queryForObject(sql, source, (rs, rowNum)-> rs.getString(1));
        if (jsonArrayString == null) {
            throw new ResultError(MoviesResults.NO_PERSON_WITH_ID_FOUND);
        }
//        System.out.println(jsonArrayString);

        return objectMapper.readValue(jsonArrayString, PersonSearchModel.class);


    }

}
