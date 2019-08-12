package com.sat.revolut.dao;

import com.sat.revolut.domain.Account;

import java.math.BigDecimal;
import java.sql.SQLException;

public class AccountServiceImpl implements AccountService {

    AccountDAO accountDAO;

    @Override
    public void create(Long accountId, BigDecimal initialBalance) throws SQLException {
        accountDAO.create(accountId,initialBalance);
    }

    @Override
    public void update(Long accountId, BigDecimal newBalance) throws SQLException {
        accountDAO.update(accountId,newBalance);
    }

    @Override
    public Account get(Long accountId) throws SQLException {
        return accountDAO.get(accountId);
    }

    @Override
    public AccountDAO getAccountDAO() {
        return accountDAO;
    }

    @Override
    public void setAccountDAO(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }
}
