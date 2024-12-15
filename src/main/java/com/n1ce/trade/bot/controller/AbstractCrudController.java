package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.service.AbstractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

public abstract class AbstractCrudController<T> {

	private final AbstractService<T> service;

	public AbstractCrudController(AbstractService<T> service) {
		this.service = service;
	}

	@GetMapping
	public List<T> getAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public ResponseEntity<T> getById(@PathVariable Long id) {
		return ResponseEntity.ok(service.findById(id));
	}

	@PostMapping
	public ResponseEntity<T> create(@RequestBody T entity) {
		return ResponseEntity.ok(service.save(entity));
	}

	@PutMapping("/{id}")
	public ResponseEntity<T> update(@PathVariable Long id, @RequestBody T entity) {
		service.findById(id); // Проверка на существование
		return ResponseEntity.ok(service.save(entity));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}

