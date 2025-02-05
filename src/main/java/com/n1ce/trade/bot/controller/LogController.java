package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.Application;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
@Log4j2
public class LogController {

	private static final String LOG_FILE_PATH = "logs/binance-java-connector.log";
	private static final int MAX_LINES = 500;

	@GetMapping("/tail")
	public ResponseEntity<List<String>> getTailLogs() {
		File logFile = new File(LOG_FILE_PATH);
		if (!logFile.exists()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		List<String> lines = new ArrayList<>();
		try (ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)) {
			String line;
			int count = 0;
			while ((line = reader.readLine()) != null && count < MAX_LINES) {
				lines.add(line);
				count++;
			}
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
		Collections.reverse(lines);
		return ResponseEntity.ok(lines);
	}

	@GetMapping("/download")
	public ResponseEntity<Resource> downloadLogFile() {
		File logFile = new File(LOG_FILE_PATH);
		if (!logFile.exists()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		Resource resource = new FileSystemResource(logFile);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + logFile.getName());
		return ResponseEntity.ok()
				.headers(headers)
				.contentLength(logFile.length())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(resource);
	}
}