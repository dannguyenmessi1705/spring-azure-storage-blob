package com.didan.azure.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.didan.azure.entity.Users;
import com.didan.azure.exception.AzureBlobStorageException;
import com.didan.azure.payload.ResponseData;
import com.didan.azure.service.AzureBlobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
public class AzureController {

	@Autowired
	private AzureBlobService azureBlobService;
	@Autowired
	private HttpServletResponse response;

	@Operation(summary = "Upload files",
			description = "Chose files to upload to Azure Blob Storage",
			security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> uploadMany(@ModelAttribute MultipartFile[] files) {
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
	@Operation(summary = "Share file with user",
			description = "Enter the filename and the username to share the file with",
			security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping(value = "/share", produces = MediaType.APPLICATION_JSON_VALUE)
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

	// Delete file
	@Operation(summary = "Delete file",
			description = "Enter the filename to delete",
			security = @SecurityRequirement(name = "bearerAuth"))
	@DeleteMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> deleteFile(@RequestParam String filename) {
		ResponseData payload = new ResponseData();
		Map<String, String> data = new HashMap<>();
		try{
			if (azureBlobService.deleteFile(filename)) {
				payload.setDescription("Delete file successful");
				data.put("filename", filename);
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

	// Delete many files
	@Operation(summary = "Delete many files",
			description = "Enter the filenames to delete. Example: filename1, filename2, filename3",
			security = @SecurityRequirement(name = "bearerAuth"))
	@DeleteMapping(value = "/delete/many", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> deleteManyFiles(@RequestParam String[] filenames) {
		ResponseData payload = new ResponseData();
		Map<String, String[]> data = new HashMap<>();
		try{
			if (azureBlobService.deleteManyFiles(filenames)) {
				payload.setDescription("Delete files successful");
				data.put("filenames", filenames);
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

	// Delete all files
	@Operation(summary = "Delete all files",
			description = "Delete all files in the container",
			security = @SecurityRequirement(name = "bearerAuth"))
	@DeleteMapping(value = "/delete/all", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> deleteAllFiles() {
		ResponseData payload = new ResponseData();
		try{
			if (azureBlobService.deleteAllFiles()) {
				payload.setDescription("Delete all files successful");
				payload.setData(null);
			}
			return new ResponseEntity<>(payload, HttpStatus.OK);
		} catch (Exception e){
			payload.setDescription(e.getMessage());
			payload.setStatusCode(500);
			payload.setSuccess(false);
			return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	// Download file
	@Operation(summary = "Download file",
			description = "Enter the filename to download the file",
			security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping(value = "/download", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> downloadFile(@RequestParam String filename) {
		ResponseData payload = new ResponseData();
		try {
			String ext = filename.split("\\.")[1];
			// Set the appropriate response headers and content type read file on the browser
			HttpHeaders headers = new HttpHeaders();
			if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
				headers.setContentType(MediaType.IMAGE_JPEG);
			} else {
				headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			}
			// set content disposition inline to display file on the browser
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
			headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + filename);

			// Set the response body and return it
			ByteArrayResource resource = new ByteArrayResource(azureBlobService.downloadFile(filename));
			if (resource == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			return ResponseEntity.ok().headers(headers).contentLength(resource.contentLength()).body(resource);
		} catch (Exception e) {
			payload.setDescription(e.getMessage());
			payload.setStatusCode(500);
			payload.setSuccess(false);
			return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	// List files
	@Operation(summary = "Get all files you have access to",
			description = "List all files in the container",
			security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> listFiles() {
		ResponseData payload = new ResponseData();
		try{
			Map<String, Map<String, String>> files = azureBlobService.listFiles();
			if (files != null) {
				payload.setDescription("List files successful");
				payload.setData(files);
			}
			return new ResponseEntity<>(payload, HttpStatus.OK);
		} catch (Exception e){
			payload.setDescription(e.getMessage());
			payload.setStatusCode(500);
			payload.setSuccess(false);
			return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
		}
	}
}

