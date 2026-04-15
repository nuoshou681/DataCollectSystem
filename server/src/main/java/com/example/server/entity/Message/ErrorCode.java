package com.example.server.entity.Message;

public class ErrorCode {
    public static final int SUCCESS = 0;
    public static final int PARAM_ERROR = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int SERVER_ERROR = 500;
    public static final int EMAIL_EXISTS = 1001;
    // ...其他业务错误码
    
}
