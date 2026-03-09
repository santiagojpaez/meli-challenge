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
                "Producto no encontrado"
        );
    }

    public static ItemNotFoundException forCategory(String categoryId) {
        return new ItemNotFoundException(
                "Categoría no encontrada"
        );
    }
}
