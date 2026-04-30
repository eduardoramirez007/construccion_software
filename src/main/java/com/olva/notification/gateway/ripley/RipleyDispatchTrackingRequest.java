package com.olva.notification.gateway.ripley;

public record RipleyDispatchTrackingRequest(
        String usuario,
        String clave,
        String bulto,
        String estado,
        String fecha_creacion,
        String fecha_actualizacion
) {
}
