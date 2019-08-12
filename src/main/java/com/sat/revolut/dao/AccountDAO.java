package com.sat.revolut.dao;

import com.sat.revolut.domain.Account;

import java.math.BigDecimal;
import java.sql.SQLException;

public interface AccountDAO {
    void create(Long accountId, BigDecimal initialBalance) throws SQLException;
    void update(Long accountId, BigDecimal newBalance) throws SQLException;
    Account get(Long accountId) throws SQLException;
}
