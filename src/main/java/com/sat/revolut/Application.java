package com.sat.revolut;

import com.sat.revolut.dao.AccountDAO;
import com.sat.revolut.dao.AccountJDBCDAOImpl;
import com.sat.revolut.dao.AccountService;
import com.sat.revolut.dao.AccountServiceImpl;
import com.sat.revolut.domain.Account;
import com.sat.revolut.handlers.AccountHandler;
import io.javalin.Javalin;

import java.math.BigDecimal;
import java.sql.SQLException;

public class Application {
    public static final String FROM_ACCOUNT_ID = "fromAccountId";
    public static final String TO_ACCOUNT_ID = "toAccountId";
    static final String PARAM_AMOUNT = "amount";
    public static final int PORT = 7142;

    public static void main(String[] args) throws SQLException {
        Javalin app = Javalin.create().start(PORT);
        System.out.println("\n\n  Example of exposed endpoints : \n" +
                "  1. PUT http://localhost:7000/account?accountId=3\n" +
                "  2. PUT http://localhost:7000/create?accountId=10&initialAmount=5000\n" +
                "  3. PUT http://localhost:7000/withdraw?accountId=1&amount=4598\n" +
                "  4. PUT http://localhost:7000/deposit?accountId=1&amount=5000\n" +
                "  5. PUT http://localhost:7000/transfer?amount=5000&fromAccountId=1&toAccountId=2\n" +
                "  6. GET http://localhost:7000/transfer?amount=5000&fromAccountId=1&toAccountId=2\n");
        AccountHandler accountHandler = new AccountHandler();
        AccountDAO accountJDBCDAO = new AccountJDBCDAOImpl();
        AccountService accountService = new AccountServiceImpl();
        accountService.setAccountDAO(accountJDBCDAO);

        accountHandler.setAccountService(accountService);

        app.put("/transfer",ctx -> {
            String amount = ctx.queryParam(PARAM_AMOUNT);
            String fromAccountId = ctx.queryParam(FROM_ACCOUNT_ID);
            String toAccountId = ctx.queryParam(TO_ACCOUNT_ID);
            try {
                String result = accountHandler.transferAmount(new BigDecimal(amount), Long.valueOf(fromAccountId), Long.valueOf(toAccountId));
                ctx.result(result);

            }catch(Exception exp){
                ctx.result(exp.getMessage());
            }

        });

        app.put("/deposit",ctx -> {
            String accountId = ctx.queryParam("accountId");
            String amount = ctx.queryParam("amount");
            try{
                accountHandler.deposit(new BigDecimal(amount),Long.valueOf(accountId));
                ctx.result("Amount deposited in account " + accountId + " " + "successfully!");
            } catch(Exception e){
                ctx.result(e.getMessage());
            }

        });

        // GET Left to be easily called from browser.
        app.get("/transfer", ctx -> {
            String amount = ctx.queryParam(PARAM_AMOUNT);
            String fromAccountId = ctx.queryParam(FROM_ACCOUNT_ID);
            String toAccountId = ctx.queryParam(TO_ACCOUNT_ID);
            try {
                String result = accountHandler.transferAmount(new BigDecimal(amount), Long.valueOf(fromAccountId), Long.valueOf(toAccountId));
                ctx.result(result);
            }catch(Exception exp){
                ctx.result(exp.getMessage());
            }
        });

        app.get("/account",ctx ->{
            String accountId = ctx.queryParam("accountId");
            try{
                Account account = accountHandler.getAccount(Long.valueOf(accountId));
                if(account.getAccountId() != null){
                    ctx.result(account.toString());
                }
            }catch(Exception e){
                ctx.result(e.getMessage());
            }
        });

        app.put("/withdraw",ctx -> {
            String accountId = ctx.queryParam("accountId");
            String amount = ctx.queryParam("amount");
            try{
                accountHandler.withDraw(new BigDecimal(amount),Long.valueOf(accountId));
                ctx.result("Withdraw from account " + accountId + " " + " completed successfully!");
            } catch(Exception e){
                ctx.result(e.getMessage());
            }

        });

        app.put("/create",ctx -> {
            String accountId = ctx.queryParam("accountId");
            String initialAmount = ctx.queryParam("initialAmount");
            try{
                accountHandler.createAccount(Long.valueOf(accountId),new BigDecimal(initialAmount));
                ctx.result("Account " + accountId + " " + " created successfully!");
            } catch(Exception e){
                ctx.result(e.getMessage());
            }

        });

    }


}
