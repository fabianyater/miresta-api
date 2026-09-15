package com.miresta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
@EnableCaching
public class MirestaApplication {

    public static void main(String[] args) {
        // El endpoint SSE de stock (StockEventController) depende del SecurityContext
        // durante el redispatch async del servlet — con la estrategia por defecto
        // (ThreadLocal) ese contexto no cruza al segundo hilo y Spring Security lo
        // re-autoriza como anónimo, fallando con AuthorizationDeniedException. Con
        // MODE_INHERITABLETHREADLOCAL sí se propaga. Debe fijarse antes de que arranque
        // el contexto de Spring.
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        SpringApplication.run(MirestaApplication.class, args);
    }

}
