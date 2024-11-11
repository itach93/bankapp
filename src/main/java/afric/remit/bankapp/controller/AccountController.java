package afric.remit.bankapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import afric.remit.bankapp.service.UserService;
import afric.remit.bankapp.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import afric.remit.bankapp.dto.TransactionRequest;
import org.springframework.security.core.Authentication;
import java.util.HashMap;

import afric.remit.bankapp.service.AccountService;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    
    @Autowired
    private AccountService accountService;
    
    @PostMapping("/credit")
    public ResponseEntity<?> credit(@RequestBody TransactionRequest request) {
        accountService.credit(request);
        return ResponseEntity.ok("Credit successful");
    }
    
    @PostMapping("/debit")
    public ResponseEntity<?> debit(@RequestBody TransactionRequest request) {
        accountService.debit(request);
        return ResponseEntity.ok("Debit successful");
    }
}