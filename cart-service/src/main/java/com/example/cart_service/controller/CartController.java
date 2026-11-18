package com.example.cart_service.controller;

import com.example.cart_service.dto.AddToCartRequest;
import com.example.cart_service.dto.CartResponse;
import com.example.cart_service.dto.UpdateQuantityRequest;
import com.example.cart_service.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Shopping cart management endpoints")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get current user's active cart")
    public CartResponse getCart(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return cartService.getCart(userId);
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item to the cart")
    public CartResponse addItem(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddToCartRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return cartService.addItem(userId, request);
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update item quantity in the cart")
    public CartResponse updateQuantity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateQuantityRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return cartService.updateQuantity(userId, itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove an item from the cart")
    public void removeItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID itemId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        cartService.removeItem(userId, itemId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Clear the entire active cart")
    public void clearCart(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        cartService.clearCart(userId);
    }
}

