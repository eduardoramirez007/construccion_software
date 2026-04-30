package com.olva.jobs;

import com.olva.service.RipleyAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RipleyStartupProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RipleyStartupProbe.class);

    private final RipleyAuthService ripleyAuthService;

    @Value("${integration.ripley.probe-on-startup:false}")
    private boolean probeOnStartup;

    public RipleyStartupProbe(RipleyAuthService ripleyAuthService) {
        this.ripleyAuthService = ripleyAuthService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!probeOnStartup) {
            return;
        }

        log.info("Iniciando prueba manual Ripley: solicitando access token...");
        String accessToken = ripleyAuthService.forceRefreshToken();
        log.info("Autenticacion exitosa en Ripley. Token obtenido con longitud={}", accessToken.length());
        log.info("Prueba manual Ripley completada. No se ejecutara la consulta de ordenes en este modo.");
    }
}
