package com.practice.order.config;

import com.practice.order.domain.Account;
import com.practice.order.domain.Money;
import com.practice.order.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedAccounts(AccountRepository accountRepository) {
        return args -> {
            accountRepository.save(new Account("acct-user-1", "user-1", new Money(1_000_000)));
            accountRepository.save(new Account("acct-user-2", "user-2", new Money(500_000)));
        };
    }
}
