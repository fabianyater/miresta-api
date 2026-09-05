package com.miresta.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

@Embeddable
public record Money(long amount) {

    public static final Money ZERO = new Money(0L);

    @JsonCreator
    public static Money of(long amount) {
        return new Money(amount);
    }

    public Money plus(Money other) {
        return new Money(this.amount + (other == null ? 0L : other.amount()));
    }

    public Money times(long quantity) {
        return new Money(this.amount * quantity);
    }

    public boolean isZero() {
        return amount == 0L;
    }

    @JsonValue
    public long amount() {
        return amount;
    }
}
