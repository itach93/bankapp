package afric.remit.bankapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import afric.remit.bankapp.service.UserService;
import afric.remit.bankapp.util.JwtUtil;
import afric.remit.bankapp.dto.RegisterRequest;
import afric.remit.bankapp.dto.LoginRequest;
import org.springframework.security.core.Authentication;
import java.util.HashMap;


@RestController
@RequestMapping("/api")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Authentication auth = userService.authenticate(request);
        String token = jwtUtil.generateToken(auth);
        return ResponseEntity.ok(new HashMap<String, String>() {{
            put("token", token);
        }});
    }
}