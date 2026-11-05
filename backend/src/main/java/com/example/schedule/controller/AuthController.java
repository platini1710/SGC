package com.example.schedule.controller;

import com.example.schedule.dto.FaceEnrollRequest;
import com.example.schedule.dto.FaceLoginRequest;
import com.example.schedule.dto.LoginRequest;
import com.example.schedule.dto.LoginResponse;
import com.example.schedule.dto.PasswordCodeRequest;
import com.example.schedule.dto.PasswordResetRequest;
import com.example.schedule.model.Usuario;
import com.example.schedule.service.AuthService;
import com.example.schedule.service.FaceAuthService;
import com.example.schedule.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final FaceAuthService faceAuthService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Público: genera y "envía" el código OTP al correo del profesor (RF-01.3).
    @PostMapping("/pwd/code")
    public ResponseEntity<Map<String, Object>> solicitarCodigo(@Valid @RequestBody PasswordCodeRequest request) {
        PasswordResetService.CodeResult r = passwordResetService.solicitarCodigo(request.getEmail());
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("message", r.mensaje());
        if (r.debugCode() != null) {
            body.put("debugCode", r.debugCode()); // solo en modo demo (schedule.otp.expose-code)
        }
        return ResponseEntity.ok(body);
    }

    // Público: valida el código y asienta la nueva contraseña (RF-01.3).
    @PostMapping("/pwd/reset")
    public ResponseEntity<Map<String, Object>> confirmarCambio(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.confirmarCambio(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("updated", true));
    }

    // Público: login por rostro (usuario + descriptor facial).
    @PostMapping("/face-login")
    public ResponseEntity<LoginResponse> faceLogin(@Valid @RequestBody FaceLoginRequest request) {
        return ResponseEntity.ok(faceAuthService.login(request));
    }

    // Autenticado: registra/reemplaza el rostro del usuario actual (exige contraseña y consentimiento).
    @PostMapping("/face/enroll")
    public ResponseEntity<Map<String, Object>> enrolar(@AuthenticationPrincipal Usuario usuario,
                                                       @Valid @RequestBody FaceEnrollRequest request) {
        faceAuthService.registrar(usuario.getUsername(), request);
        return ResponseEntity.ok(Map.of("registrado", true));
    }

    // Autenticado: indica si el usuario actual tiene un rostro registrado.
    @GetMapping("/face/status")
    public ResponseEntity<Map<String, Object>> estado(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(Map.of("registrado", faceAuthService.tieneRostro(usuario.getUsername())));
    }

    // Autenticado: elimina el rostro registrado del usuario actual.
    @DeleteMapping("/face")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Usuario usuario) {
        faceAuthService.eliminar(usuario.getUsername());
        return ResponseEntity.noContent().build();
    }
}
