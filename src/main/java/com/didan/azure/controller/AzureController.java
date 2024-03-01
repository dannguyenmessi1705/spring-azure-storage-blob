package com.didan.azure.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.didan.azure.entity.Users;
import com.didan.azure.payload.ResponseData;
import com.didan.azure.service.AzureBlobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
public class AzureController {

	@Autowired
	private AzureBlobService azureBlobService;

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> uploadMany(@RequestParam MultipartFile[] files) {
		ResponseData payload = new ResponseData();
		Map<String, List<String>> response = new HashMap<>();
		try{
			List<String> fileNames = azureBlobService.upload(files);
			if (fileNames != null) {
				payload.setDescription("Upload files successful");
				response.put("fileNames", fileNames);
				payload.setData(response);
			}
			return new ResponseEntity<>(payload, HttpStatus.OK);
		} catch (Exception e){
			payload.setDescription(e.getMessage());
			payload.setStatusCode(500);
			payload.setSuccess(false);
			return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	// Share file with user
	@PostMapping(value = "/share", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> shareFile(@RequestParam String filename, @RequestParam String username) {
		ResponseData payload = new ResponseData();
		Map<String, String> data = new HashMap<>();
		try{
			if (azureBlobService.shareFile(filename, username)) {
				payload.setDescription("Share file successful");
				data.put("filename", filename);
				data.put("username", username);
				payload.setData(data);
			}
			return new ResponseEntity<>(payload, HttpStatus.OK);
		} catch (Exception e){
			payload.setDescription(e.getMessage());
			payload.setStatusCode(500);
			payload.setSuccess(false);
			return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

//	@PostMapping("/upload/many")
//	public ResponseEntity<List<String>> uploadMany(@RequestParam MultipartFile[] files) throws IOException {
//
//		List<String> fileNames = azureBlobAdapter.uploadMany(files);
//		return ResponseEntity.ok(fileNames);
//	}
//
//	@GetMapping("/list")
//	public ResponseEntity<List<String>> getAllBlobs() {
//
//		List<String> items = azureBlobAdapter.listBlobs();
//		return ResponseEntity.ok(items);
//	}
//
//	@DeleteMapping
//	public ResponseEntity<Boolean> delete(@RequestParam String fileName) {
//
//		azureBlobAdapter.deleteBlob(fileName);
//		return ResponseEntity.ok().build();
//	}
//
//
//
//	@GetMapping("/download")
//	public ResponseEntity<Resource> getFile(@RequestParam String fileName) throws URISyntaxException {
//
//		ByteArrayResource resource = new ByteArrayResource(azureBlobAdapter.getFile(fileName));
//
//		HttpHeaders headers = new HttpHeaders();
//		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
//
//		return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).headers(headers).body(resource);
//	}
}

