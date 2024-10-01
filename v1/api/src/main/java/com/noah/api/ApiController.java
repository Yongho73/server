package com.noah.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noah.api.cmmn.ApiResponse;

@RestController
@RequestMapping("/api")
public class ApiController {
		
	private ApiResponse apiResponse;
	
	public ApiController() {
		this.apiResponse = new ApiResponse();
	}

	@GetMapping("/items")
	public ResponseEntity<?> getItems() {		
		List<String> result = new ArrayList<String>();
		result.add("item one");
		result.add("item two");		
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
