package com.example.productservice.service;

import com.example.productservice.dto.CategoryRequest;
import com.example.productservice.dto.CategoryResponse;
import com.example.productservice.entity.Category;
import com.example.productservice.exception.DuplicateCategoryException;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.repository.CategoryRepository;
import com.example.productservice.repository.ProductRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

	private final CategoryRepository repository;
	private final ProductRepository productRepository;

	public CategoryService(CategoryRepository repository, ProductRepository productRepository) {
		this.repository = repository;
		this.productRepository = productRepository;
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll() {
		return repository.findAll().stream().map(CategoryResponse::from).toList();
	}

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		if (repository.existsByNameIgnoreCase(request.name().trim())) {
			throw new DuplicateCategoryException(request.name());
		}
		try {
			return CategoryResponse.from(repository.save(new Category(request.name().trim(), request.description())));
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateCategoryException(request.name(), exception);
		}
	}

	@Transactional(readOnly = true)
	public Category getRequired(UUID id) {
		return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
	}

	@Transactional
	public CategoryResponse update(UUID id, CategoryRequest request) {
		Category category = getRequired(id);
		if (!category.getName().equalsIgnoreCase(request.name().trim())
				&& repository.existsByNameIgnoreCase(request.name().trim())) {
			throw new DuplicateCategoryException(request.name());
		}
		category.setName(request.name().trim());
		category.setDescription(request.description());
		return CategoryResponse.from(repository.save(category));
	}

	@Transactional
	public void delete(UUID id) {
		Category category = getRequired(id);
		if (productRepository.existsByCategoryId(id)) {
			throw new com.example.productservice.exception.CategoryInUseException(id);
		}
		repository.delete(category);
	}
}
