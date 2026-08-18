package com.dewevrei.aikanban.auth;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Component;

@Component
public class SessionFailureClassifier {

    public boolean isSessionStoreFailure(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof DataAccessResourceFailureException || current instanceof QueryTimeoutException
                    || current.getClass().getName().contains("redis")
                    || current.getClass().getName().contains("Redis")) {
                return true;
            }
        }
        return false;
    }
}
