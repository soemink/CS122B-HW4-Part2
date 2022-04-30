package com.github.klefstad_teaching.cs122b.movies.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.klefstad_teaching.cs122b.core.result.Result;
import com.github.klefstad_teaching.cs122b.movies.repo.entity.PersonSearchModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonSearchResponse {
    private Result result;
    private PersonSearchModel[] persons;
    private PersonSearchModel person;

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public PersonSearchModel[] getPersons() {
        return persons;
    }

    public void setPersons(PersonSearchModel[] persons) {
        this.persons = persons;
    }

    public PersonSearchModel getPerson() {
        return person;
    }

    public void setPerson(PersonSearchModel person) {
        this.person = person;
    }
}
