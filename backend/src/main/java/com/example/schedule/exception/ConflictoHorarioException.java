package com.example.schedule.exception;

import com.example.schedule.dto.ConflictoResponse;

public class ConflictoHorarioException extends RuntimeException {

    private final ConflictoResponse conflicto;

    public ConflictoHorarioException(ConflictoResponse conflicto) {
        super(conflicto.getDescripcion());
        this.conflicto = conflicto;
    }

    public ConflictoResponse getConflicto() {
        return conflicto;
    }
}
