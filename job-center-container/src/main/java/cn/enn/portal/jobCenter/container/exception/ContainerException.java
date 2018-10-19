package cn.enn.portal.jobCenter.container.exception;

import org.springframework.http.HttpStatus;

public class ContainerException extends Exception {

    public ContainerException(String msg, HttpStatus statusCode) {
        super(msg);
        this.statusCode = statusCode;
    }

    public ContainerException(String msg, HttpStatus statusCode, Throwable throwable) {
        super(msg, throwable);
        this.statusCode = statusCode;
    }


    private HttpStatus statusCode;

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }
}
