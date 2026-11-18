package com.example.cart_service.repository;

import com.example.cart_service.entity.Cart;
import com.example.cart_service.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.userId = :userId AND c.status = :status")
    Optional<Cart> findWithItemsByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") CartStatus status);
}

