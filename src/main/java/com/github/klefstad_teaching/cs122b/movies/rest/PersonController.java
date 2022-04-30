package com.github.klefstad_teaching.cs122b.movies.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.klefstad_teaching.cs122b.core.error.ResultError;
import com.github.klefstad_teaching.cs122b.core.security.JWTManager;
import com.github.klefstad_teaching.cs122b.movies.model.response.PersonSearchResponse;
import com.github.klefstad_teaching.cs122b.movies.repo.MovieRepo;
import com.github.klefstad_teaching.cs122b.movies.repo.entity.PersonQuery;
import com.github.klefstad_teaching.cs122b.movies.repo.entity.PersonSearchModel;
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
public class PersonController
{
    private final MovieRepo repo;

    @Autowired
    public PersonController(MovieRepo repo)
    {
        this.repo = repo;
    }

    @GetMapping("/person/search")
    public ResponseEntity<PersonSearchResponse> personSearch(@AuthenticationPrincipal SignedJWT user, PersonQuery personQuery) throws JsonProcessingException, ParseException {
        List<String> roles = user.getJWTClaimsSet().getStringListClaim(JWTManager.CLAIM_ROLES);
        System.out.println(Arrays.toString(roles.toArray()));
        boolean showHidden = false;
        if (roles.contains("ADMIN") || roles.contains("EMPLOYEE"))
        {
            showHidden = true;
        }

        PersonSearchResponse response = new PersonSearchResponse();
        PersonSearchModel[] person = repo.searchPerson(personQuery, showHidden);
        response.setPersons(person);
        response.setResult(MoviesResults.PERSONS_FOUND_WITHIN_SEARCH);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/person/{personId}")
    public ResponseEntity<PersonSearchResponse> personGetByPersonId(@PathVariable Long personId, @AuthenticationPrincipal SignedJWT user) throws ParseException, JsonProcessingException {
        List<String> roles = user.getJWTClaimsSet().getStringListClaim(JWTManager.CLAIM_ROLES);
        System.out.println(Arrays.toString(roles.toArray()));
        boolean showHidden = false;
        if (roles.contains("ADMIN") || roles.contains("EMPLOYEE"))
        {
            showHidden = true;
        }
        PersonSearchResponse response = new PersonSearchResponse();

        try {
            response.setPerson(repo.getPersonByPersonId(personId, showHidden));
        } catch (EmptyResultDataAccessException e) {
            throw new ResultError(MoviesResults.NO_PERSON_WITH_ID_FOUND);
        }
        response.setResult(MoviesResults.PERSON_WITH_ID_FOUND);
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }
}
