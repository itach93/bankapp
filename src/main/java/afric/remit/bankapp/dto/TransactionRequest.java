package afric.remit.bankapp.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequest {
    private String accountNumber;
    private BigDecimal amount;

    // Getters and Setters
    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}