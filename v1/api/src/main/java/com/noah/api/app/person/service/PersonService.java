package com.noah.api.app.person.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.noah.api.app.person.entity.Person;
import com.noah.api.app.person.service.mapper.PersonMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PersonService {
		
    private final PersonMapper mapper;
    
    public List<Person> getPersonList(Person person) {
        return mapper.selectPerson(person);
    }
}
