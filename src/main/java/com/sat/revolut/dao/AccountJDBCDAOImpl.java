package com.sat.revolut.dao;

import com.sat.revolut.domain.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;

public class AccountJDBCDAOImpl implements AccountDAO {

    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";


    public AccountJDBCDAOImpl() throws SQLException{
        insertWithStatement();
    }


    @Override
    public void create(Long accountId, BigDecimal initialBalance) throws SQLException {
        Connection connection = getDBConnection();
        try {
            Statement stmt = null;
            connection.setAutoCommit(true);
            stmt = connection.createStatement();

            stmt.execute("INSERT INTO Account(id, balance) VALUES(" + accountId + "," + initialBalance.toPlainString()+")");
        }finally {
            connection.close();
        }
    }

    @Override
    public void update(Long accountId, BigDecimal newBalance)  throws SQLException  {
        Connection connection = getDBConnection();
        try {
            Statement stmt = null;
            connection.setAutoCommit(true);
            stmt = connection.createStatement();
            stmt.execute("update Account set balance= " + newBalance + " where id=" + accountId);
        }finally {
            connection.close();
        }
    }

    @Override
    public Account get(Long accountId) throws SQLException {
        Account account = null;
        Connection connection = getDBConnection();
        try{
            Statement stmt = null;

            connection.setAutoCommit(true);
            stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("select * from Account where id=" + accountId);

            while (rs.next()) {
                account = new Account();
                account.setAccountId(rs.getLong("id"));
                account.setTotalBalance(new BigDecimal(rs.getString("balance")));
            }
        }finally{
            connection.close();
        }

        return account;
    }

    private static void insertWithStatement() throws SQLException {
        Connection connection = getDBConnection();
        Statement stmt = null;
        try {
            connection.setAutoCommit(true);
            stmt = connection.createStatement();
            stmt.execute("CREATE TABLE Account(id long primary key, balance varchar(255))");
            stmt.execute("INSERT INTO Account(id, balance) VALUES(1, 10000)");
            stmt.execute("INSERT INTO Account(id, balance) VALUES(2, 15000)");
            stmt.execute("INSERT INTO Account(id, balance) VALUES(3, 25000)");

            ResultSet rs = stmt.executeQuery("select * from Account");
            System.out.println("  Creating few sample accounts :\n");
            while (rs.next()) {
                System.out.print("  Account Id " + rs.getInt("id") + " Balance " + rs.getString("balance") + "\n");
            }
            stmt.close();
            connection.commit();
        } catch (SQLException e) {
            System.out.println("Exception Message " + e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
    }

    private static Connection getDBConnection() {
        Connection dbConnection = null;
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
            return dbConnection;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return dbConnection;
    }
}
