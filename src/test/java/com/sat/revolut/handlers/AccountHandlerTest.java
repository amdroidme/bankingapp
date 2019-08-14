package com.sat.revolut.handlers;

import com.sat.revolut.dao.AccountJDBCDAOImpl;
import com.sat.revolut.dao.AccountServiceImpl;
import com.sat.revolut.domain.Account;
import com.sat.revolut.exception.*;
import com.sat.revolut.handlers.AccountHandler;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountHandlerTest {

    AccountHandler accountHandler;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @BeforeAll
    public void setUp() {
        try{
        accountHandler = new AccountHandler();
        AccountJDBCDAOImpl accountJDBCDAO = new AccountJDBCDAOImpl();
        AccountServiceImpl accountService = new AccountServiceImpl();

        accountService.setAccountDAO(accountJDBCDAO);
        accountHandler.setAccountService(accountService);
        }catch(SQLException e){
            logger.error("Exception while setting test : " + e.getMessage());
            Assertions.fail(e);
        }

    }

    @AfterEach
    void tearDown() {
        // Do nothing as of now.
    }

    @Test
    void transferAmount() {
        BigDecimal amount = new BigDecimal(1000);
        Long fromAccountId = 1l;
        Long toAccountId = 3l;
        BigDecimal currentFromBalance = null;
        BigDecimal currentToAccountBalance = null;

        BigDecimal newFromBalance = null;
        BigDecimal newToAccountBalance = null;
        try {
            currentFromBalance = accountHandler.getAccount(fromAccountId).getTotalBalance();
            currentToAccountBalance = accountHandler.getAccount(toAccountId).getTotalBalance();
            String result = accountHandler.transferAmount(amount,fromAccountId,toAccountId);

            newFromBalance = accountHandler.getAccount(fromAccountId).getTotalBalance();
            newToAccountBalance = accountHandler.getAccount(toAccountId).getTotalBalance();
        }catch(Exception e){
            Assertions.fail(e);
        }

        Assertions.assertTrue(currentFromBalance.equals(newFromBalance.add(amount)), "Account balance of account " + fromAccountId + " does not match after transaction");
        Assertions.assertTrue(currentToAccountBalance.equals(newToAccountBalance.subtract(amount)),"Account balance of account " + toAccountId + " does not match after transaction");

    }

    @Test
    public void transferAmountCheckInvalidValues() throws Exception {
        Assertions.assertThrows(InvalidAmountException.class, () -> {
                accountHandler.transferAmount(new BigDecimal("-1"), 1l, 2l);
        },"Validation of negative amount");

        Assertions.assertThrows(NoAccountFoundException.class, () -> {
                accountHandler.transferAmount(new BigDecimal("1000"), -1l, 2l);

        },"Validation of from account ID");

        Assertions.assertThrows(NoAccountFoundException.class, () -> {
                accountHandler.transferAmount(new BigDecimal("1000"), 1l, -2l);

        },"Validation of to account ID");

        Assertions.assertThrows(LowBalanceException.class, () -> {
                accountHandler.transferAmount(new BigDecimal("100000"), 1l, 2l);

        },"Validation of amount exceeding available balance");

        Assertions.assertThrows(InvalidAccountNumberException.class, () -> {
            accountHandler.transferAmount(new BigDecimal("100"), 1l, 1l);

        },"Both Account number are same.");
    }

    @Test
    void deposit() {
        BigDecimal amount = new BigDecimal("1000");
        Long accountId = 1l;
        BigDecimal oldBalance = null;
        BigDecimal newBalance = null;

        try {
            oldBalance = accountHandler.getAccount(accountId).getTotalBalance();
            accountHandler.deposit(amount, accountId);
            newBalance = accountHandler.getAccount(accountId).getTotalBalance();
        }catch(Exception ex){
            Assertions.fail(ex);
        }

        Assertions.assertTrue(newBalance.equals(oldBalance.add(amount)));

        Assertions.assertThrows(NoAccountFoundException.class, () -> {
                accountHandler.deposit(new BigDecimal("100"), -1l);
        },"Deposit account is invalid");

        Assertions.assertThrows(InvalidAmountException.class, () -> {
                accountHandler.deposit(new BigDecimal("-100"), accountId);

        },"Deposit amount is invalid");
    }

    @Test
    void withDraw() {
        BigDecimal amount = new BigDecimal("1000");
        Long accountId = 1l;
        BigDecimal oldBalance = null;
        BigDecimal newBalance = null;

        try {
            oldBalance = accountHandler.getAccount(accountId).getTotalBalance();
            accountHandler.withDraw(amount, accountId);
            newBalance = accountHandler.getAccount(accountId).getTotalBalance();
        }catch(Exception ex){
            Assertions.fail(ex);
        }

        Assertions.assertTrue(newBalance.equals(oldBalance.subtract(amount)));

        Assertions.assertThrows(NoAccountFoundException.class, () -> {
                accountHandler.withDraw(new BigDecimal("100"), -1l);

        },"Deposit account is invalid");

        Assertions.assertThrows(InvalidAmountException.class, () -> {
                accountHandler.withDraw(new BigDecimal("-100"), accountId);

        },"Deposit amount is invalid");

        Assertions.assertThrows(LowBalanceException.class, () -> {
            accountHandler.withDraw(new BigDecimal("10000000"), accountId);
        });
    }

    @Test
    public void createAccountTest(){
        BigDecimal initialAmount = new BigDecimal("1000");
        Long accountId = 123l;

        try{
            accountHandler.createAccount(accountId,initialAmount);

            Assertions.assertNotNull(accountHandler.getAccount(accountId));
            Assertions.assertTrue(accountHandler.getAccount(accountId).toString().contains(accountId+""));
            Assertions.assertTrue(accountHandler.getAccount(accountId).toString().contains(initialAmount.toPlainString()+""));
        }catch(Exception e){
            Assertions.fail("Account creation failed : ",e);
        }

        Assertions.assertThrows(AccountAlreadyExistException.class , () ->{
            accountHandler.createAccount(accountId,initialAmount);
        });

        Assertions.assertThrows(InvalidAccountNumberException.class , () ->{
            accountHandler.createAccount(-100l,initialAmount);
        });
    }

    @Test
    public void threadSleepTestWhileCleanup(){
        Thread thread1 = new Thread( () -> {
            logger.info("Running thread name : " + Thread.currentThread().getName());
            try{
                BigDecimal initialAmount = new BigDecimal("1000");
                for(long accountIdAuto = 1000l ; accountIdAuto < 3500l; accountIdAuto++){
                    accountHandler.createAccount(accountIdAuto,initialAmount);
                    Assertions.assertNotNull(accountHandler.getAccount(accountIdAuto));
                }

            }catch(Exception e){
                Assertions.fail("Account creation failed : ",e);
            }
        });

        Thread thread2 = new Thread( () -> {
            logger.info("Running thread name : " + Thread.currentThread().getName());
            try{
                BigDecimal initialAmount = new BigDecimal("1000");
                for(long accountIdAuto = 11000l ; accountIdAuto < 14000l; accountIdAuto++){
                    accountHandler.createAccount(accountIdAuto,initialAmount);
                    Assertions.assertNotNull(accountHandler.getAccount(accountIdAuto));
                }

            }catch(Exception e){
                Assertions.fail("Account creation failed : ",e);
            }
        });

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        }catch (InterruptedException e) {
            Assertions.fail(e);
        }
    }

    @Test
    void getAccount() {
        Long accountId = 1l;
        try{
            Account account = accountHandler.getAccount(accountId);
            Assertions.assertNotNull(account);
        }catch(SQLException | NoAccountFoundException e){
            Assertions.fail(e);
        }

        Assertions.assertThrows(NoAccountFoundException.class, () -> {
           accountHandler.getAccount(-1l);
        });
    }

    @Test
    void setAccountService() {
    }
}