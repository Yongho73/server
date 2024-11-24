package com.noah.api.app.person.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
	
	@Value("${spring.servlet.multipart.location}")
    private String uploadPath;
	
	@PostMapping("/down/{id}")
    public ResponseEntity<Resource> down(@PathVariable("id") String id) {
        try {
          
            String fleName = "test.zip";
            String outPath = uploadPath + "/" + fleName;

            log.debug("다운로드 호출=[{}]", outPath);

            // 파일을 임시 디렉토리에 생성
            Path path = Paths.get(outPath);

            // 엑셀 파일 다운로드를 위한 Resource 객체 생성
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            // Content-Disposition 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(fleName, "utf-8"));
            
            // 생성된 파일 삭제
            //File file = new File(outPath);
            //file.delete();

            // OK 상태코드와 함께 리소스와 헤더 반환
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile[] file) {
        try {

            log.debug("파일개수 : {}", file.length);            
            String uploadFile = "";
            String uuidFile = "";

            if (file.length > 0) {

                for (int i = 0; i < file.length; i++) {

                    String fileName = file[i].getOriginalFilename();
                    String fileExt = fileName.substring(fileName.lastIndexOf("."));

                    log.debug("UPLOAD_LOCATION : {}", uploadPath);
                    log.debug("파일 이름 : {}", fileName);
                    log.debug("파일 확장자 : {}", fileExt);
                    log.debug("파일 크기 : {}", file[i].getSize());

                    uuidFile = UUID.randomUUID().toString().replaceAll("-", "") + fileExt;

                    log.debug("UUID 파일명 : {}", uuidFile);

                    uploadFile = uploadPath +"/"+ uuidFile;

                    log.debug("업로드 파일 : {}", uploadFile);

                    try {
                        if (file[i].isEmpty()) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload the file.");
                        }

                        if (!fileExt.equals(".xlsx")) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload the file.");
                        }

                        File Folder = new File(uploadFile);
                        if ( !Folder.exists() ) {
                            try {
                                Folder.mkdirs();
                                log.debug("폴더가 생성되었습니다.");
                            } catch (Exception e) {
                                e.getStackTrace();
                            }
                        }

                        Path destinationFile = Paths.get(uploadFile);
                        try (InputStream inputStream = file[i].getInputStream()) {
                            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload the file.");
                    }
                }
            }

            return ResponseEntity.status(HttpStatus.OK).body("");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload the file.");
        }
    }
	 
}
