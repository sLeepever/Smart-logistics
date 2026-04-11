package com.smart.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String realName;
    private String phone;
    /** admin / dispatcher / driver / customer */
    private String role;
    /** 1: 启用, 0: 禁用 */
    private Integer status;
}
