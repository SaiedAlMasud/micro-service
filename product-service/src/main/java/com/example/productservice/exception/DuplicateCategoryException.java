package com.example.productservice.exception;

public class DuplicateCategoryException extends RuntimeException {

	public DuplicateCategoryException(String name) {
		super("Category with name '" + name + "' already exists");
	}

	public DuplicateCategoryException(String name, Throwable cause) {
		super("Category with name '" + name + "' already exists", cause);
	}
}
