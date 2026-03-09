package com.challenge.exception;

/**
 * IMPORTANTE: La comparación de productos solo tiene sentido cuando todos los items
 * pertenecen a categorías comparables. Comparar un smartphone con una cafetera
 * sería inconsistente: no existen atributos comunes con significado de negocio.
 * Esta validación evita respuestas confusas o datos incompletos en la tabla de comparación.
 */
public class CategoryMismatchException extends RuntimeException {

    public CategoryMismatchException(String message) {
        super(message);
    }

    public CategoryMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public static CategoryMismatchException forProducts(String category1, String category2) {
        return new CategoryMismatchException("Las categorías de los productos seleccionados no son comparables");
    }
}
