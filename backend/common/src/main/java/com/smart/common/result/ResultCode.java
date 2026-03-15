package com.smart.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证，请先登录"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "数据冲突"),
    UNPROCESSABLE(422, "业务逻辑错误"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务码：用户模块 (1xxx)
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "账号已被禁用"),
    USERNAME_DUPLICATE(1003, "用户名已存在"),
    PASSWORD_WRONG(1004, "用户名或密码错误"),
    TOKEN_EXPIRED(1005, "Token 已过期"),
    TOKEN_INVALID(1006, "Token 无效"),
    TOKEN_BLACKLISTED(1007, "Token 已被注销"),

    // 业务码：订单模块 (2xxx)
    ORDER_NOT_FOUND(2001, "订单不存在"),
    ORDER_STATUS_ILLEGAL(2002, "订单状态不允许此操作"),
    ORDER_ALREADY_DISPATCHED(2003, "订单已被调度，无法修改"),

    // 业务码：调度模块 (3xxx)
    DISPATCH_ORDER_LIMIT(3001, "单批次订单数量不能超过 50 个"),
    DISPATCH_NO_VEHICLE(3002, "没有可用的空闲车辆"),
    DISPATCH_PLAN_NOT_FOUND(3003, "调度方案不存在"),
    DISPATCH_PLAN_STATUS_ILLEGAL(3004, "调度方案状态不允许此操作"),
    ROUTE_NOT_FOUND(3005, "路线不存在"),
    VEHICLE_NOT_FOUND(3006, "车辆不存在"),

    // 业务码：跟踪模块 (4xxx)
    ROUTE_NOT_BELONG_TO_DRIVER(4001, "该路线不属于当前司机");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
