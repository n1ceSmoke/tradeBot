package com.n1ce.trade.bot.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public abstract class AbstractService<T> {
	protected JpaRepository<T, Long> repository;

	protected AbstractService(JpaRepository<T, Long> repository) {
		this.repository = repository;
	}
	public List<T> findAll() {
		return repository.findAll();
	}

	public T findById(Long id) {
		return repository.findById(id).orElseThrow(() -> new RuntimeException("Strategy not found"));
	}

	public T save(T strategy) {
		return repository.save(strategy);
	}

	public void deleteById(Long id) {
		repository.deleteById(id);
	}
}
