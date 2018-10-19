package cn.enn.portal.jobCenter.container;

import cn.enn.portal.jobCenter.container.exception.ContainerException;
import cn.enn.portal.jobCenter.container.viewmodel.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> parameterErrorHandler(HttpServletRequest req, IllegalArgumentException e) {
        logError(req, e);
        return new ResponseEntity(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = ContainerException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> containerExceptionHandler(HttpServletRequest req, ContainerException e) {
        logError(req, e);
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), e.getStatusCode());
    }

    @ExceptionHandler(value = {Exception.class})
    @ResponseBody
    public ResponseEntity<ErrorResponse> othersErrorHandler(HttpServletRequest req, Exception e) throws Exception {
        logError(req, e);
        return new ResponseEntity<>(new ErrorResponse("请求失败:" + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void logError(HttpServletRequest req, Exception e) {
        String url = req.getRequestURI();
        logger.error("request error at " + req.getMethod() + ":" + url, e);
    }

}
