package com.noah.api.app.person.service.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.noah.api.app.person.entity.Person;

@Mapper
public interface PersonMapper {
	List<Person> selectPerson(Person person);
}