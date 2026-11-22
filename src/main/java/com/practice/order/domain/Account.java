package com.practice.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false, length = 64)
    private String accountId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "balance", nullable = false)
    private long balance;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Account() {
    }

    public Account(String accountId, String userId, Money balance) {
        this.accountId = accountId;
        this.userId = userId;
        this.balance = balance.amount();
    }

    public void withdraw(Money amount) {
        this.balance = new Money(this.balance).minus(amount).amount();
    }

    public void deposit(Money amount) {
        this.balance = new Money(this.balance).plus(amount).amount();
    }

    public String getAccountId() {
        return accountId;
    }

    public String getUserId() {
        return userId;
    }

    public Money getBalance() {
        return new Money(balance);
    }

    public long getVersion() {
        return version;
    }
}
