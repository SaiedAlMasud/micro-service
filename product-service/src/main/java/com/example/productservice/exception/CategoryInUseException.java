package com.example.productservice.exception;

import java.util.UUID;

public class CategoryInUseException extends RuntimeException {

	public CategoryInUseException(UUID id) {
		super("Category with id " + id + " cannot be deleted while products reference it");
	}
}
