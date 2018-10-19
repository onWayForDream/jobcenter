package cn.enn.portal.jobCenter.container.exception;

import cn.enn.portal.jobCenter.container.exception.ContainerException;
import org.springframework.http.HttpStatus;

public class FieldRequiredException extends ContainerException {
    public FieldRequiredException(String filed) {
        super("field " + filed + " is required", HttpStatus.BAD_REQUEST);
    }
}
