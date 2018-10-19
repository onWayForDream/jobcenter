package cn.enn.portal.jobCenter.core;

import cn.enn.portal.jobCenter.JobcenterLogger;
import org.slf4j.Logger;

import java.util.UUID;

public class JobcenterLoggerProxy implements JobcenterLogger {

    private Logger logger;
    private UUID runId;
    private String runIdStr;

    public JobcenterLoggerProxy(Logger logger, UUID runId) {
        this.logger = logger;
        this.runId = runId;
        this.runIdStr = "[" + runId.toString() + "]";
    }

    @Override
    public UUID getRunId() {
        return this.runId;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        logger.trace(this.runIdStr + s);
    }

    @Override
    public void trace(String s, Object o) {
        logger.trace(this.runIdStr + s, o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        logger.trace(this.runIdStr + s, o, o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        logger.trace(this.runIdStr + s, objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        logger.trace(this.runIdStr + s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        logger.debug(this.runIdStr + s);
    }

    @Override
    public void debug(String s, Object o) {
        logger.debug(this.runIdStr + s, o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        logger.debug(this.runIdStr + s, o, o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        logger.debug(this.runIdStr + s, objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        logger.debug(this.runIdStr + s, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        logger.info(this.runIdStr + s);
    }

    @Override
    public void info(String s, Object o) {
        logger.info(this.runIdStr + s, o);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        logger.info(this.runIdStr + s, o, o1);
    }

    @Override
    public void info(String s, Object... objects) {
        logger.info(this.runIdStr + s, objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        logger.info(this.runIdStr + s, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        logger.warn(this.runIdStr + s);
    }

    @Override
    public void warn(String s, Object o) {
        logger.warn(this.runIdStr + s, o);
    }

    @Override
    public void warn(String s, Object... objects) {
        logger.warn(this.runIdStr + s, objects);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        logger.warn(this.runIdStr + s, o, o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        logger.warn(this.runIdStr + s, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        logger.error(this.runIdStr + s);
    }

    @Override
    public void error(String s, Object o) {
        logger.error(this.runIdStr + s, o);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        logger.error(this.runIdStr + s, o, o1);
    }

    @Override
    public void error(String s, Object... objects) {
        logger.error(this.runIdStr + s, objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        logger.error(this.runIdStr + s, throwable);
    }
}
