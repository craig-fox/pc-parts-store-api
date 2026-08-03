package nz.fox.craig.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.auth.dto.LoginRequest;
import nz.fox.craig.auth.dto.LoginResponse;
import nz.fox.craig.auth.service.AuthenticationService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }
}
