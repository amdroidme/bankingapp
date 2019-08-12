package com.sat.revolut.handlers;

import com.sat.revolut.Application;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.SQLException;

public class ApplicationTest {

    @Test
    public void applicationTest(){
        try{
            Application.main(null);
        }catch(SQLException e){
            Assertions.fail(e);
        }

        validateURL("http://localhost:7142/withdraw?accountId=3&amount=4598", "PUT");
        validateURL("http://localhost:7142/account?accountId=3","GET");
        validateURL("http://localhost:7142/create?accountId=10&initialAmount=5000","PUT");
        validateURL("http://localhost:7142/transfer?amount=5000&fromAccountId=1&toAccountId=2","PUT");
        validateURL("http://localhost:7142/deposit?accountId=1&amount=5000","PUT");
        validateURL("http://localhost:7142/transfer?amount=5000&fromAccountId=1&toAccountId=2","GET");

    }

    public void validateURL(String URL, String requestMethod){

        URL url = null;
        HttpURLConnection con = null;

        try {
            url = new URL(URL);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(requestMethod);

            int status = con.getResponseCode();

            Assertions.assertTrue(HttpURLConnection.HTTP_OK == status);

        } catch (ProtocolException  e) {
            Assertions.fail(e);
        } catch (MalformedURLException e) {
            Assertions.fail(e);
        } catch (IOException e) {
            Assertions.fail(e);
        } finally{
            con.disconnect();
        }
    }
}
