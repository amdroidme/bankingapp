package com.sat.revolut.handlers;

import com.sat.revolut.exception.*;
import com.sat.revolut.dao.AccountService;
import com.sat.revolut.domain.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;


public class AccountHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ConcurrentHashMap<Long, ReentrantReadWriteLock> mapOfLocks = new ConcurrentHashMap<>();
    private AccountService accountService = null;

    private boolean lockCleanUpInProgress = Boolean.FALSE;

    private static final int MAX_ALLOWED_IN_MEMORY_LOCKS = 100; // Value depends on available memory. Intentionally kept less.



    public String transferAmount(BigDecimal amount, Long fromAccountId, Long toAccountId) throws Exception {
        validateInputAmount(amount);
        if(!isAccountExist(fromAccountId) || !isAccountExist(toAccountId)){
            throw new NoAccountFoundException("One of Account does not exist");
        }

        String transactionId = Thread.currentThread().hashCode() + "_" +System.currentTimeMillis() + "_" + fromAccountId + "_" + toAccountId;
        logger.info("Initiating transaction : " + transactionId);

        ReadWriteLock fromAccountLock = getLockTobeAcquired(fromAccountId);
        ReadWriteLock toAccountLock = getLockTobeAcquired(toAccountId);

        fromAccountLock.writeLock().lock();
        toAccountLock.writeLock().lock();
        try {
            logger.info("Acquired locks for transaction : " + transactionId);


            BigDecimal fromAccountBalance = getBalance(fromAccountId);

            if(fromAccountBalance.compareTo(amount) < 0){
                logger.info("Transaction : " + transactionId + " failed. Low balance in account " + fromAccountId);
                throw new LowBalanceException("Transaction Id : " + transactionId + " failed with error : " + "Low balance in account " + fromAccountId);
            }

            BigDecimal toAccountBalance = getBalance(toAccountId);

            fromAccountBalance = fromAccountBalance.subtract(amount);
            toAccountBalance = toAccountBalance.add(amount);

            updateBalance(fromAccountBalance, fromAccountId);
            updateBalance(toAccountBalance, toAccountId);

            logger.info("Transaction : " + transactionId + " is completed");
        }finally {
            fromAccountLock.writeLock().unlock();
            toAccountLock.writeLock().unlock();
            logger.info("released locks for transaction : " + transactionId);
        }
        return "Transaction : " + transactionId + " is completed";
    }

    public void deposit(BigDecimal amount,Long accountId) throws Exception{
        validateInputAmount(amount);
        if(!isAccountExist(accountId)){
            throw new NoAccountFoundException("Account does not exist");
        }

        String transactionId = Thread.currentThread().hashCode() + "_deposit_" + System.currentTimeMillis() + "_" + accountId;
        logger.info("Initiating deposit transaction " + transactionId);
        ReadWriteLock lock = getLockTobeAcquired(accountId);
        lock.writeLock().lock();
        try{
            logger.info("Acquired lock for transaction " + transactionId);
            BigDecimal currentBalance = getBalance(accountId);
            BigDecimal newBalance = currentBalance.add(amount);

            updateBalance(newBalance,accountId);

            logger.info("Transaction " + transactionId + " completed") ;
        } finally{
            lock.writeLock().unlock();
            logger.info("Released lock for transaction " + transactionId);
        }
    }

    public void withDraw(BigDecimal amount,Long accountId) throws Exception{
        validateInputAmount(amount);
        if(!isAccountExist(accountId)){
            throw new NoAccountFoundException("Account does not exist");
        }
        String transactionId = Thread.currentThread().hashCode() + "_withdraw_" + System.currentTimeMillis() + "_" + accountId;
        logger.info("Initiating deposit transaction " + transactionId);
        ReadWriteLock lock = getLockTobeAcquired(accountId);
        lock.writeLock().lock();
        try{
            logger.info("Acquired lock for transaction " + transactionId);
            BigDecimal currentBalance = getBalance(accountId);
            if(currentBalance.compareTo(amount) < 0){
                logger.info("Transaction : " + transactionId + " failed. Low balance in account " + accountId);
                throw new LowBalanceException("Transaction : " + transactionId + " failed. Low balance in account " + accountId);
            }
            BigDecimal newBalance = currentBalance.subtract(amount);

            updateBalance(newBalance,accountId);

            logger.info("Transaction " + transactionId + " completed") ;
        }finally{
            lock.writeLock().unlock();
            logger.info("Released lock for transaction " + transactionId);
        }
    }

    public Account getAccount(Long accountId) throws SQLException, NoAccountFoundException {
        if(!isAccountExist(accountId)){
            throw new NoAccountFoundException("No account with exist id : " + accountId);
        }

        ReadWriteLock lock = getLockTobeAcquired(accountId);


        lock.readLock().lock();
        try{
            return accountService.get(accountId);
        }finally{
            lock.readLock().unlock();
        }
    }

    public void createAccount(Long accountId, BigDecimal initialBalance) throws InvalidAmountException, InvalidAccountNumberException, AccountAlreadyExistException,SQLException {
        validateInputAmount(initialBalance);

        if(accountId <= 01){
            throw new InvalidAccountNumberException("Account number is invalid");
        }

        if(isAccountExist(accountId)){
            throw new AccountAlreadyExistException("Account already exist");
        }

        ReadWriteLock lock = getLockTobeAcquired(accountId);
        lock.writeLock().lock();
        try{
            // Check again , to avoid parallel threads creating same account.
            if(isAccountExist(accountId)){
                throw new AccountAlreadyExistException("Account already exist");
            }

            accountService.create(accountId,initialBalance);
        logger.info("Account created successfully!!");
        }finally {
            lock.writeLock().unlock();
        }
    }



    private void updateBalance(BigDecimal balance, Long accountId) throws SQLException {
        accountService.update(accountId,balance);
    }

    private BigDecimal getBalance(Long accountId) throws SQLException {
        Account account = accountService.get(accountId);
        return account.getTotalBalance();
    }

    private void validateInputAmount(BigDecimal amount) throws InvalidAmountException{
        if(amount.compareTo(new BigDecimal("1")) <= 0){
            throw new InvalidAmountException("Invalid amount. Amount should always be greater than One.");
        }
    }

    private void cleanUpUnusedLocks(){
        try{
            logger.info("Unused lock clean up in progress , current size " + mapOfLocks.size() + " max allowed size : " + MAX_ALLOWED_IN_MEMORY_LOCKS);
            lockCleanUpInProgress = true;
            mapOfLocks.forEachKey(0, new Consumer<Long>() {
                @Override
                public void accept(Long accountId) {
                    ReentrantReadWriteLock lock = mapOfLocks.get(accountId);
                    if(lock.writeLock().getHoldCount() == 0){
                        mapOfLocks.remove(accountId);
                        logger.info("Unused lock removed for accountId : " + accountId);
                    }
                }
            });

        }finally {
            logger.info("Unused lock clean state , current size " + mapOfLocks.size() + " max allowed size : " + MAX_ALLOWED_IN_MEMORY_LOCKS);
            lockCleanUpInProgress = false;
        }
    }



    private ReentrantReadWriteLock getLockTobeAcquired(Long accountId){
        // Like stop the world scenario, Force all incoming threads accessing locks to sleep, so that clean up can be performed efficiently.
        // Sleep will be executed. Only latency will be seen, will NOT hung application, as cleanup will be very fast.
        while(lockCleanUpInProgress){
            try{
                logger.info(Thread.currentThread().getName() + " %s is going to sleep to help clean up of locks.");
                Thread.sleep(100);
            }catch(InterruptedException e) {
                logger.error(Thread.currentThread().getName() + "is interrupted while sleeping.");
            }
        }

        if(mapOfLocks.size() > MAX_ALLOWED_IN_MEMORY_LOCKS){
            synchronized (mapOfLocks){
                if(mapOfLocks.size() > MAX_ALLOWED_IN_MEMORY_LOCKS){
                    cleanUpUnusedLocks();
                }
            }
        }

        // Compute if lock for particular account is missing, else return existing lock.
        // This will help in account level locking for read , write and update operation.
        return mapOfLocks.computeIfAbsent(accountId, lock -> new ReentrantReadWriteLock());

    }

    private boolean isAccountExist(Long accountId) throws SQLException {
        return accountService.get(accountId) != null ? Boolean.TRUE : Boolean.FALSE ;
    }


    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }



}
