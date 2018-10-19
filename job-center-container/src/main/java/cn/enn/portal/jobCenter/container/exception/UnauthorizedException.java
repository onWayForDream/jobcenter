package cn.enn.portal.jobCenter.container.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ContainerException {
    public UnauthorizedException() {
        super("access deny", HttpStatus.UNAUTHORIZED);
    }

}
