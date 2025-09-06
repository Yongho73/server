package com.noah.api.app.person.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.noah.api.app.person.entity.Person;
import com.noah.api.app.person.service.mapper.PersonMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PersonService {
		
    private final PersonMapper mapper;
    
    /**
     * 전체 Person 목록 조회
     * - 캐시명: "persons:list"
     * - key: "all" (항상 같은 목록이면 고정키로)
     * - null 이거나 비어있는 결과는 캐싱하지 않음
     */
    @Cacheable(cacheNames = "persons:list", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<Person> getPersonList(Person person) {
        return mapper.selectPerson(person);
    }
}
