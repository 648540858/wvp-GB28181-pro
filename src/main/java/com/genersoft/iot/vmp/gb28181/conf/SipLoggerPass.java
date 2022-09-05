package com.genersoft.iot.vmp.gb28181.conf;

import gov.nist.core.StackLogger;

import java.util.Properties;

/**
 * sip日志格式化
 */
public class SipLoggerPass implements StackLogger {

    @Override
    public void logStackTrace() {

    }

    @Override
    public void logStackTrace(int traceLevel) {

    }

    @Override
    public int getLineCount() {
        return 0;
    }

    @Override
    public void logException(Throwable ex) {

    }

    @Override
    public void logDebug(String message) {

    }

    @Override
    public void logDebug(String message, Exception ex) {

    }

    @Override
    public void logTrace(String message) {

    }

    @Override
    public void logFatalError(String message) {

    }

    @Override
    public void logError(String message) {

    }

    @Override
    public boolean isLoggingEnabled() {
        return false;
    }

    @Override
    public boolean isLoggingEnabled(int logLevel) {
        return false;
    }

    @Override
    public void logError(String message, Exception ex) {

    }

    @Override
    public void logWarning(String string) {

    }

    @Override
    public void logInfo(String string) {

    }

    @Override
    public void disableLogging() {

    }

    @Override
    public void enableLogging() {

    }

    @Override
    public void setBuildTimeStamp(String buildTimeStamp) {

    }

    @Override
    public void setStackProperties(Properties stackProperties) {

    }

    @Override
    public String getLoggerName() {
        return null;
    }
}
