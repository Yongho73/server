package com.noah.api.app.person.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.noah.api.cmmn.ApiResponse;
import com.noah.api.cmmn.MessageService;

import io.micrometer.common.util.StringUtils;

@RestController
@RequestMapping("/api")
public class ApiController {
		
	private ApiResponse apiResponse = new ApiResponse();	
	
	@Autowired
    private MessageService activemq;
	
	public ApiController() {		
		//
	}

	@GetMapping("/items")
	public ResponseEntity<?> getItems(@RequestParam(value = "msg") String msg) {		
		List<String> result = new ArrayList<String>();
		
		String message = StringUtils.isBlank(msg) ? "no message" : msg;
		
		 // 메시지를 전송
        activemq.sendMessage(message);		
		
		result.add("item one");
		result.add("item two");
		result.add(message);
		 
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
