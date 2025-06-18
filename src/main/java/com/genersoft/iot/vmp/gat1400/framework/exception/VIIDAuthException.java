package com.genersoft.iot.vmp.gat1400.framework.exception;

import org.springframework.security.core.AuthenticationException;

public class VIIDAuthException extends AuthenticationException {

    public VIIDAuthException(String msg) {
        super(msg);
    }
}
