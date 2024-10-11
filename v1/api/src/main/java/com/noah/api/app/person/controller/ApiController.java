package com.noah.api.app.person.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noah.api.app.person.entity.Person;
import com.noah.api.app.person.service.PersonService;
import com.noah.api.cmmn.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {
		
	private ApiResponse apiResponse;
	private PersonService service;
	
	@GetMapping("/items")
	public ResponseEntity<?> getItems() {
		log.info("items...called^^;;;");
		List<Person> result = service.getPersonList(null);	
		return ResponseEntity.ok(apiResponse.sendResponse(result));
	}

	@GetMapping("/item/{id}")
	public ResponseEntity<?> getItem(@PathVariable("id") String id) {
		return ResponseEntity.ok(apiResponse.sendResponse("get..." + id));
	}

	@PutMapping("/item/{id}")
	public ResponseEntity<?> modifyItem(@PathVariable("id") String id,  @RequestBody String newItem) {
		return ResponseEntity.ok(apiResponse.sendResponse("modify..." + id + " with data: " + newItem));
	}

	@DeleteMapping("/item/{id}")
	public ResponseEntity<?> removeItem(@PathVariable("id") String id) {
		return ResponseEntity.ok(apiResponse.sendResponse("remove..." + id));
	}
}
