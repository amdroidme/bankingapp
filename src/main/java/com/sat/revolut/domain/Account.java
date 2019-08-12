package com.sat.revolut.domain;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class Account {

    Long accountId;

    BigDecimal totalBalance;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    @Override
    public String toString() {
        StringBuilder accountDetails = new StringBuilder();
        accountDetails.append("Account Id : " + accountId); accountDetails.append("\n");
        accountDetails.append("Total balance : " + totalBalance);
        return accountDetails.toString();
    }
}
