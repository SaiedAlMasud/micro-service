package com.example.productservice.repository;

import com.example.productservice.entity.Category;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	boolean existsByNameIgnoreCase(String name);
}
