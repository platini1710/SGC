package com.example.schedule.exception;

public class SchedulingException extends RuntimeException {

    private final String detalle;

    public SchedulingException(String detalle) {
        super(detalle);
        this.detalle = detalle;
    }

    public String getDetalle() {
        return detalle;
    }
}
