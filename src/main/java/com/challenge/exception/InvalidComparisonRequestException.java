package com.challenge.exception;

public class InvalidComparisonRequestException extends RuntimeException {

    public InvalidComparisonRequestException(String message) {
        super(message);
    }

    public InvalidComparisonRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InvalidComparisonRequestException emptyList() {
        return new InvalidComparisonRequestException(
                "La lista de IDs de productos no puede estar vacía. Indique al menos dos productos para comparar."
        );
    }

    public static InvalidComparisonRequestException singleItem() {
        return new InvalidComparisonRequestException(
                "La comparación requiere al menos dos productos. Indique dos o más IDs."
        );
    }

    public static InvalidComparisonRequestException moreThanFiveItems() {
        return new InvalidComparisonRequestException(
                "La comparación soporta un máximo de 5 productos. Indique 5 o menos IDs."
        );
    }

    public static InvalidComparisonRequestException sameProduct() {
        return new InvalidComparisonRequestException(
                "No se pueden comparar productos idénticos. Asegúrese de que los IDs sean diferentes."
        );
    }
}
