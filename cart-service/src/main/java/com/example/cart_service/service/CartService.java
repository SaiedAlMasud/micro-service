package com.example.cart_service.service;

import com.example.cart_service.client.ProductClient;
import com.example.cart_service.dto.*;
import com.example.cart_service.entity.Cart;
import com.example.cart_service.entity.CartItem;
import com.example.cart_service.entity.CartStatus;
import com.example.cart_service.exception.InsufficientStockException;
import com.example.cart_service.exception.ProductNotFoundException;
import com.example.cart_service.exception.ResourceNotFoundException;
import com.example.cart_service.exception.ServiceUnavailableException;
import com.example.cart_service.repository.CartItemRepository;
import com.example.cart_service.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductClient productClient) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productClient = productClient;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        Cart cart = cartRepository.findWithItemsByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> new Cart(null, userId));

        return buildCartResponse(cart);
    }

    public CartResponse addItem(UUID userId, AddToCartRequest request) {
        ProductDto product = productClient.getProduct(request.productId())
                .orElseThrow(() -> new ProductNotFoundException("Product with id " + request.productId() + " not found"));

        if (!"ACTIVE".equalsIgnoreCase(product.status())) {
            throw new ProductNotFoundException("Product with id " + request.productId() + " is inactive");
        }

        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart newCart = new Cart(UUID.randomUUID(), userId);
                    return cartRepository.save(newCart);
                });

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.productId());

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.quantity();

            if (product.stockQuantity() == null || product.stockQuantity() < newQuantity) {
                throw new InsufficientStockException("Insufficient stock for product " + request.productId()
                        + ". Available: " + product.stockQuantity() + ", requested: " + newQuantity);
            }

            existingItem.setQuantity(newQuantity);
            existingItem.setUnitPrice(product.price());
            existingItem.setProductName(product.name());
            cartItemRepository.save(existingItem);
        } else {
            if (product.stockQuantity() == null || product.stockQuantity() < request.quantity()) {
                throw new InsufficientStockException("Insufficient stock for product " + request.productId()
                        + ". Available: " + product.stockQuantity() + ", requested: " + request.quantity());
            }

            CartItem newItem = new CartItem(
                    UUID.randomUUID(),
                    cart,
                    product.id(),
                    product.name(),
                    product.price(),
                    request.quantity()
            );
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return getCart(userId);
    }

    public CartResponse updateQuantity(UUID userId, UUID itemId, UpdateQuantityRequest request) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .filter(i -> i.getCart().getId().equals(cart.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cart item with id " + itemId + " not found"));

        ProductDto product = productClient.getProduct(item.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product with id " + item.getProductId() + " not found"));

        if (!"ACTIVE".equalsIgnoreCase(product.status())) {
            throw new ProductNotFoundException("Product with id " + item.getProductId() + " is inactive");
        }

        if (product.stockQuantity() == null || product.stockQuantity() < request.quantity()) {
            throw new InsufficientStockException("Insufficient stock for product " + item.getProductId()
                    + ". Available: " + product.stockQuantity() + ", requested: " + request.quantity());
        }

        item.setQuantity(request.quantity());
        item.setUnitPrice(product.price());
        item.setProductName(product.name());
        cartItemRepository.save(item);

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return getCart(userId);
    }

    public void removeItem(UUID userId, UUID itemId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .filter(i -> i.getCart().getId().equals(cart.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cart item with id " + itemId + " not found"));

        cart.removeItem(item);
        cartItemRepository.delete(item);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    public void clearCart(UUID userId) {
        Optional<Cart> cartOpt = cartRepository.findWithItemsByUserIdAndStatus(userId, CartStatus.ACTIVE);
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            cart.getItems().clear();
            cart.setUpdatedAt(Instant.now());
            cartRepository.save(cart);
        }
    }

    private CartResponse buildCartResponse(Cart cart) {
        if (cart.getId() == null || cart.getItems().isEmpty()) {
            return new CartResponse(
                    cart.getId(),
                    cart.getUserId(),
                    cart.getStatus() != null ? cart.getStatus() : CartStatus.ACTIVE,
                    List.of(),
                    BigDecimal.ZERO.setScale(2),
                    cart.getCreatedAt(),
                    cart.getUpdatedAt()
            );
        }

        List<UUID> productIds = cart.getItems().stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();

        Map<UUID, ProductDto> productMap = new HashMap<>();
        Set<UUID> missingIds = new HashSet<>();

        try {
            BulkProductResponse bulkResponse = productClient.getBulkProducts(productIds);
            if (bulkResponse != null) {
                if (bulkResponse.items() != null) {
                    productMap = bulkResponse.items().stream()
                            .collect(Collectors.toMap(ProductDto::id, Function.identity(), (a, b) -> a));
                }
                if (bulkResponse.missingIds() != null) {
                    missingIds.addAll(bulkResponse.missingIds());
                }
            }
        } catch (ServiceUnavailableException ex) {
            log.warn("Product Service unavailable while viewing cart. Falling back to snapshot prices.", ex);
        }

        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            BigDecimal unitPrice = item.getUnitPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(subtotal);

            ProductDto currentProduct = productMap.get(item.getProductId());
            boolean isMissing = missingIds.contains(item.getProductId());
            boolean isAvailable = !isMissing && (currentProduct == null || "ACTIVE".equalsIgnoreCase(currentProduct.status()));

            boolean priceChanged = false;
            BigDecimal currentPrice = unitPrice;

            if (currentProduct != null && currentProduct.price() != null) {
                currentPrice = currentProduct.price();
                priceChanged = currentProduct.price().compareTo(unitPrice) != 0;
            }

            itemResponses.add(new CartItemResponse(
                    item.getId(),
                    item.getProductId(),
                    item.getProductName(),
                    unitPrice,
                    item.getQuantity(),
                    subtotal,
                    item.getAddedAt(),
                    priceChanged,
                    currentPrice,
                    isAvailable
            ));
        }

        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                cart.getStatus(),
                itemResponses,
                total.setScale(2),
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }
}

