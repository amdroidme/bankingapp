package com.sat.revolut.handlers;

import com.sat.revolut.exception.*;
import com.sat.revolut.dao.AccountService;
import com.sat.revolut.domain.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class AccountHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ConcurrentHashMap<Long, ReentrantReadWriteLock> mapOfLocks = new ConcurrentHashMap<>();
    private AccountService accountService = null;

    private boolean lockCleanUpInProgress = Boolean.FALSE;

    private static final int MAX_ALLOWED_IN_MEMORY_LOCKS = 100; // Value depends on available memory. Intentionally kept less.



    public String transferAmount(BigDecimal amount, Long fromAccountId, Long toAccountId) throws NoAccountFoundException, InvalidAccountNumberException, SQLException, InvalidAmountException, RetriesExceededException, InterruptedException {
        if(fromAccountId.equals(toAccountId)){
            throw new InvalidAccountNumberException("From and To account ID same");
        }

        validateInputAmount(amount);

        if(!isAccountExist(fromAccountId) || !isAccountExist(toAccountId)){
            throw new NoAccountFoundException("One of Account does not exist");
        }


        String transactionId = Thread.currentThread().hashCode() + "_" +System.currentTimeMillis() + "_" + fromAccountId + "_" + toAccountId;
        if(logger.isInfoEnabled()){
            logger.info(MessageFormat.format("Initiating transaction : {0}" , transactionId));
        }

        ReadWriteLock fromAccountLock = getLockTobeAcquired(fromAccountId);
        ReadWriteLock toAccountLock = getLockTobeAcquired(toAccountId);

        int retires = 0;
        while(!isBothLockAcquired(fromAccountLock,toAccountLock)){
            retires++;
            if(retires > 100){
                throw new RetriesExceededException("Unable to acquire lock after 100 tries.");
            }
        }


        try {

            BigDecimal fromAccountBalance = getBalance(fromAccountId);

            if(fromAccountBalance.compareTo(amount) < 0){
                throw new LowBalanceException("Transaction Id : " + transactionId + " failed with error : " + "Low balance in account " + fromAccountId);
            }

            BigDecimal toAccountBalance = getBalance(toAccountId);

            fromAccountBalance = fromAccountBalance.subtract(amount);
            toAccountBalance = toAccountBalance.add(amount);

            updateBalance(fromAccountBalance, fromAccountId);
            updateBalance(toAccountBalance, toAccountId);
        }finally {
            fromAccountLock.writeLock().unlock();
            toAccountLock.writeLock().unlock();
        }
        return "Transaction : " + transactionId + " is completed";
    }

    private boolean isBothLockAcquired(ReadWriteLock lock1 , ReadWriteLock lock2){
        boolean lock2Acq = false;
        boolean lock1Acq = false;
        try {
            lock2Acq = lock2.writeLock().tryLock();
            lock1Acq = lock1.writeLock().tryLock();

        }finally{
            if(!(lock1Acq && lock2Acq)){
                if(lock1Acq){
                   lock1.writeLock().unlock();
                }
                if(lock2Acq){
                    lock2.writeLock().unlock();
                }
            }
        }
        return lock1Acq && lock2Acq;
    }

    public void deposit(BigDecimal amount,Long accountId) throws InvalidAmountException,NoAccountFoundException,SQLException,InterruptedException{
        validateInputAmount(amount);
        if(!isAccountExist(accountId)){
            throw new NoAccountFoundException("Account does not exist");
        }

        ReadWriteLock lock = getLockTobeAcquired(accountId);
        lock.writeLock().lock();
        try{
            BigDecimal currentBalance = getBalance(accountId);
            BigDecimal newBalance = currentBalance.add(amount);

            updateBalance(newBalance,accountId);
        } finally{
            lock.writeLock().unlock();
        }
    }

    public void withDraw(BigDecimal amount,Long accountId) throws InvalidAmountException,NoAccountFoundException,SQLException,InterruptedException{
        validateInputAmount(amount);
        if(!isAccountExist(accountId)){
            throw new NoAccountFoundException("Account does not exist");
        }
        String transactionId = Thread.currentThread().hashCode() + "_withdraw_" + System.currentTimeMillis() + "_" + accountId;
        ReadWriteLock lock = getLockTobeAcquired(accountId);
        lock.writeLock().lock();
        try{
            BigDecimal currentBalance = getBalance(accountId);
            if(currentBalance.compareTo(amount) < 0){
                throw new LowBalanceException("Transaction : " + transactionId + " failed. Low balance in account " + accountId);
            }
            BigDecimal newBalance = currentBalance.subtract(amount);

            updateBalance(newBalance,accountId);
        }finally{
            lock.writeLock().unlock();
        }
    }

    public Account getAccount(Long accountId) throws SQLException, NoAccountFoundException,InterruptedException {
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

    public void createAccount(Long accountId, BigDecimal initialBalance) throws InvalidAmountException, InvalidAccountNumberException, AccountAlreadyExistException,SQLException,InterruptedException {
        validateInputAmount(initialBalance);

        if(accountId <= 0){
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
            if(logger.isInfoEnabled()) {
                logger.info(MessageFormat.format("Unused lock clean up in progress , current size {1} max allowed size : {0}" , MAX_ALLOWED_IN_MEMORY_LOCKS,mapOfLocks.size()));
            }
            lockCleanUpInProgress = true;
            mapOfLocks.forEachKey(0, accountId -> {
                    ReentrantReadWriteLock lock = mapOfLocks.get(accountId);
                    if(lock.writeLock().getHoldCount() == 0){
                        mapOfLocks.remove(accountId);
                    }
            });

        }finally {
            if(logger.isInfoEnabled()) {
                logger.info(MessageFormat.format("Unused lock clean state , current size {1} max allowed size : {0}", MAX_ALLOWED_IN_MEMORY_LOCKS, mapOfLocks.size()));
            }
            lockCleanUpInProgress = false;
        }
    }



    private ReentrantReadWriteLock getLockTobeAcquired(Long accountId) throws InterruptedException {
        // Like stop the world scenario, Force all incoming threads accessing locks to sleep, so that clean up can be performed efficiently.
        // Sleep will be executed. Only latency will be seen, will NOT hung application, as cleanup will be very fast.
        while(lockCleanUpInProgress){
            try{
                if(logger.isInfoEnabled()) {
                    logger.info(MessageFormat.format("{0} is going to sleep to help clean up of locks.", Thread.currentThread().getName()));
                }
                Thread.sleep(100);
            }catch(InterruptedException e) {
                logger.error(MessageFormat.format( " {0} is interrupted while sleeping.",Thread.currentThread().getName()));
                throw e;
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
