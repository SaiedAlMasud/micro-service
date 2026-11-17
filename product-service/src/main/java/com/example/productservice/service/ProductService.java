package com.example.productservice.service;

import com.example.productservice.dto.BulkProductResponse;
import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.entity.Category;
import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductStatus;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.repository.ProductRepository;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private final ProductRepository repository;
	private final CategoryService categoryService;

	public ProductService(ProductRepository repository, CategoryService categoryService) {
		this.repository = repository;
		this.categoryService = categoryService;
	}

	@Transactional(readOnly = true)
	public Page<ProductResponse> search(UUID categoryId, String query, Pageable pageable) {
		return repository.search(ProductStatus.ACTIVE, categoryId, normalize(query), pageable).map(ProductResponse::from);
	}

	@Transactional(readOnly = true)
	public ProductResponse findById(UUID id) {
		return ProductResponse.from(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id)));
	}

	@Transactional(readOnly = true)
	public BulkProductResponse findBulk(List<UUID> ids) {
		var products = repository.findAllByIdIn(ids);
		var found = new HashSet<>(products.stream().map(Product::getId).toList());
		var missing = ids.stream().filter(id -> !found.contains(id)).distinct().toList();
		return new BulkProductResponse(products.stream().map(ProductResponse::from).toList(), missing);
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		return save(request, null);
	}

	@Transactional
	public ProductResponse update(UUID id, ProductRequest request) {
		Product product = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
		return save(request, product);
	}

	@Transactional
	public void deactivate(UUID id) {
		Product product = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
		product.deactivate();
	}

	private ProductResponse save(ProductRequest request, Product product) {
		Category category = categoryService.getRequired(request.categoryId());
		if (product == null) {
			product = new Product(request.name().trim(), request.description(), category, request.brand(),
					request.price(), normalizeCurrency(request.currency()), request.stockQuantity(), request.imageUrl());
		}
		else {
			product.update(request.name().trim(), request.description(), category, request.brand(), request.price(),
					normalizeCurrency(request.currency()), request.stockQuantity(), request.imageUrl());
		}
		return ProductResponse.from(repository.save(product));
	}

	private String normalize(String query) {
		return query == null || query.isBlank() ? null : query.trim();
	}

	private String normalizeCurrency(String currency) {
		return currency == null || currency.isBlank() ? "BDT" : currency.toUpperCase();
	}
}
