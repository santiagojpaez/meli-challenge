package com.challenge.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Indicador de salud personalizado. Ejemplo de extensión del actuator para
 * evidenciar condiciones propias de la API y facilitar el mantenimiento y 
 * el diagnóstico en producción.
 */
@Component
public class ActuatorHealthConfig implements HealthIndicator {

    private static final String COMPONENT = "meli-challenge-api";

    @Override
    public Health health() {
        return Health.up()
                .withDetail("component", COMPONENT)
                .withDetail("message", "API operativa; listo para ampliar con checks de dependencias")
                .build();
    }
}
