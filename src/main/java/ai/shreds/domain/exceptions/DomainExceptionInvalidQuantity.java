package ai.shreds.domain.exceptions;

import java.math.BigDecimal;

public class DomainExceptionInvalidQuantity extends RuntimeException {
    
    private final BigDecimal quantity;
    
    public DomainExceptionInvalidQuantity(String message) {
        super(message);
        this.quantity = null;
    }
    
    public DomainExceptionInvalidQuantity(BigDecimal quantity, String reason) {
        super(String.format("Invalid quantity %s: %s", quantity, reason));
        this.quantity = quantity;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
}