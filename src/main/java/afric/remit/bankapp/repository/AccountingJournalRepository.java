package afric.remit.bankapp.repository;

import afric.remit.bankapp.model.Account;
import afric.remit.bankapp.model.AccountingJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface AccountingJournalRepository extends JpaRepository<AccountingJournal, Long> {
    List<AccountingJournal> findByAccountOrderByTransactionDateDesc(Account account);
}