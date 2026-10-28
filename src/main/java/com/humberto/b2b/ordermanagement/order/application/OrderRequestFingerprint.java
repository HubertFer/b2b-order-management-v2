package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.domain.OrderItem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

final class OrderRequestFingerprint {

    private OrderRequestFingerprint() {
    }

    static String of(UUID partnerId, List<OrderItem> items) {
        Objects.requireNonNull(partnerId, "partnerId must not be null");
        Objects.requireNonNull(items, "items must not be null");
        String canonical = partnerId + "|" + items.stream()
                .map(item -> item.productId() + ":" + item.quantity() + ":"
                        + item.unitPrice().stripTrailingZeros().toPlainString())
                .sorted()
                .collect(Collectors.joining(";"));
        return sha256(canonical);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
