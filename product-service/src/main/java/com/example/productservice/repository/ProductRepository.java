package com.example.productservice.repository;

import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	@Query("""
			select p from Product p
			where p.status = :status
			and (:categoryId is null or p.category.id = :categoryId)
			and (:query is null or lower(p.name) like lower(concat('%', :query, '%'))
				or lower(coalesce(p.brand, '')) like lower(concat('%', :query, '%')))
			""")
	Page<Product> search(@Param("status") ProductStatus status, @Param("categoryId") UUID categoryId,
			@Param("query") String query, Pageable pageable);

	List<Product> findAllByIdIn(Collection<UUID> ids);

	boolean existsByCategoryId(UUID categoryId);
}
