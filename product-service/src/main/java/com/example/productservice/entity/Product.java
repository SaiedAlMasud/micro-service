package com.example.productservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import java.time.LocalDateTime;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(length = 100)
	private String brand;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "stock_quantity", nullable = false)
	private int stockQuantity;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProductStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Product() {
	}

	public Product(String name, String description, Category category, String brand, BigDecimal price,
			String currency, int stockQuantity, String imageUrl) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.brand = brand;
		this.price = price;
		this.currency = currency;
		this.stockQuantity = stockQuantity;
		this.imageUrl = imageUrl;
		this.status = ProductStatus.ACTIVE;
	}

	public UUID getId() { return id; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public Category getCategory() { return category; }
	public String getBrand() { return brand; }
	public BigDecimal getPrice() { return price; }
	public String getCurrency() { return currency; }
	public int getStockQuantity() { return stockQuantity; }
	public String getImageUrl() { return imageUrl; }
	public ProductStatus getStatus() { return status; }
	public void update(String name, String description, Category category, String brand, BigDecimal price,
			String currency, int stockQuantity, String imageUrl) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.brand = brand;
		this.price = price;
		this.currency = currency;
		this.stockQuantity = stockQuantity;
		this.imageUrl = imageUrl;
	}
	public void deactivate() { this.status = ProductStatus.INACTIVE; }

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
