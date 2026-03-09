package com.challenge.exception;

public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(String message) {
        super(message);
    }

    public ItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ItemNotFoundException forProduct(String productId) {
        return new ItemNotFoundException(
                String.format("Producto con ID '%s' no encontrado.", productId)
        );
    }

    public static ItemNotFoundException forCategory(String categoryId) {
        return new ItemNotFoundException(
                String.format("Categoría con ID '%s' no encontrada.", categoryId)
        );
    }
}
