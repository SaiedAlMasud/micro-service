package com.example.productservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.example.productservice.dto.CategoryRequest;
import com.example.productservice.entity.Category;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.CategoryRepository;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.service.CategoryService;
import com.example.productservice.service.ProductService;

class ProductServiceApplicationTests {

	@Test
	void bulkLookupReturnsMissingIdsWithoutHidingInactiveProducts() {
		ProductRepository repository = Mockito.mock(ProductRepository.class);
		CategoryService categories = Mockito.mock(CategoryService.class);
		UUID existingId = UUID.randomUUID();
		UUID missingId = UUID.randomUUID();
		Product existing = Mockito.mock(Product.class);
		Category category = Mockito.mock(Category.class);
		Mockito.when(existing.getId()).thenReturn(existingId);
		Mockito.when(existing.getCategory()).thenReturn(category);
		Mockito.when(category.getId()).thenReturn(UUID.randomUUID());
		Mockito.when(existing.getName()).thenReturn("Phone X");
		Mockito.when(existing.getPrice()).thenReturn(BigDecimal.TEN);
		Mockito.when(existing.getCurrency()).thenReturn("BDT");
		Mockito.when(repository.findAllByIdIn(java.util.List.of(existingId, missingId)))
				.thenReturn(java.util.List.of(existing));

		ProductService service = new ProductService(repository, categories);
		var result = service.findBulk(java.util.List.of(existingId, missingId));

		assertEquals(1, result.items().size());
		assertEquals(java.util.List.of(missingId), result.missingIds());
	}

	@Test
	void categoryDeleteRejectsCategoriesUsedByProducts() {
		CategoryRepository categories = Mockito.mock(CategoryRepository.class);
		ProductRepository products = Mockito.mock(ProductRepository.class);
		UUID categoryId = UUID.randomUUID();
		Category category = Mockito.mock(Category.class);
		Mockito.when(categories.findById(categoryId)).thenReturn(java.util.Optional.of(category));
		Mockito.when(products.existsByCategoryId(categoryId)).thenReturn(true);

		CategoryService service = new CategoryService(categories, products);

		org.junit.jupiter.api.Assertions.assertThrows(
				com.example.productservice.exception.CategoryInUseException.class,
				() -> service.delete(categoryId));
		Mockito.verify(categories, Mockito.never()).delete(category);
	}

	@Test
	void categoryUpdateRejectsDuplicateName() {
		CategoryRepository categories = Mockito.mock(CategoryRepository.class);
		ProductRepository products = Mockito.mock(ProductRepository.class);
		UUID categoryId = UUID.randomUUID();
		Category category = Mockito.mock(Category.class);
		Mockito.when(categories.findById(categoryId)).thenReturn(java.util.Optional.of(category));
		Mockito.when(category.getName()).thenReturn("Phones");
		Mockito.when(categories.existsByNameIgnoreCase("Laptops")).thenReturn(true);

		CategoryService service = new CategoryService(categories, products);

		org.junit.jupiter.api.Assertions.assertThrows(
				com.example.productservice.exception.DuplicateCategoryException.class,
				() -> service.update(categoryId, new CategoryRequest("Laptops", null)));
	}

}
