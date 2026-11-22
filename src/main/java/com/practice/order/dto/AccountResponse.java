package com.practice.order.dto;

import com.practice.order.domain.Account;

public record AccountResponse(String accountId, String userId, long balance, long version) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getUserId(),
                account.getBalance().amount(),
                account.getVersion()
        );
    }
}
