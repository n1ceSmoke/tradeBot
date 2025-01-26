package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.dto.AbstractDTO;
import com.n1ce.trade.bot.mapper.MapperInterface;
import com.n1ce.trade.bot.service.AbstractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractCrudController<O, D extends AbstractDTO> {

	private final AbstractService<O> service;
	private final MapperInterface<O, D> mapper;

	public AbstractCrudController(AbstractService<O> service, MapperInterface<O, D> mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	public List<D> getAll() {
		return service.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
	}

	@GetMapping("/{id}")
	public ResponseEntity<D> getById(@PathVariable Long id) {
		return ResponseEntity.ok(mapper.toDto(service.findById(id)));
	}

	@PostMapping
	public ResponseEntity<D> create(@RequestBody D entity) {
		return ResponseEntity.ok(mapper.toDto(service.save(mapper.toEntity(entity))));
	}

	@PutMapping("/{id}")
	public ResponseEntity<D> update(@PathVariable Long id, @RequestBody D entity) {
		service.update(id, entity);
		return ResponseEntity.ok(mapper.toDto(service.findById(id)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}

