package com.smart.common.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** HS256 签名密钥（Base64 编码，至少 256 位） */
    private String secret = "c21hcnRMb2dpc3RpY3MtandrLXNlY3JldC1rZXktZm9yLWhzMjU2LTIwMjY=";
    /** accessToken 有效期（秒），默认 2 小时 */
    private long accessTtl = 7200;
    /** refreshToken 有效期（秒），默认 7 天 */
    private long refreshTtl = 604800;
}
