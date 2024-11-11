package afric.remit.bankapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import afric.remit.bankapp.model.User;
import afric.remit.bankapp.repository.UserRepository;
import afric.remit.bankapp.dto.TransactionRequest;
import afric.remit.bankapp.model.Account;
import afric.remit.bankapp.model.AccountingJournal;
import afric.remit.bankapp.model.TransactionType;
import afric.remit.bankapp.repository.AccountRepository;
import afric.remit.bankapp.repository.AccountingJournalRepository;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AccountService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private AccountingJournalRepository journalRepository;
    
    @Transactional
    public void credit(TransactionRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
            .orElseThrow(() -> new RuntimeException("Account not found"));
            
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);
        
        AccountingJournal journal = new AccountingJournal();
        journal.setAccount(account);
        journal.setAmount(request.getAmount());
        journal.setTransactionDate(LocalDateTime.now());
        journal.setType(TransactionType.CREDIT);
        journalRepository.save(journal);
    }
    
    @Transactional
    public void debit(TransactionRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
            .orElseThrow(() -> new RuntimeException("Account not found"));
            
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        
        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);
        
        AccountingJournal journal = new AccountingJournal();
        journal.setAccount(account);
        journal.setAmount(request.getAmount());
        journal.setTransactionDate(LocalDateTime.now());
        journal.setType(TransactionType.DEBIT);
        journalRepository.save(journal);
    }
}