package com.noah.api.app.person.entity;

import org.apache.ibatis.type.Alias;

import lombok.Data;

@Data
@Alias("Person")
public class Person {
	private int id;
	private String name;
	private String email;
}
