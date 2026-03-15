package com.smart.user.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.user.entity.User;
import com.smart.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 演示数据初始化器
 * 仅在 users 表为空时执行，避免重复插入
 * 演示账号密码统一为 Demo@1234
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)
        );
        if (count > 0) {
            log.info("[DataInitializer] users 表已有数据（{}条），跳过演示数据初始化", count);
            return;
        }

        log.info("[DataInitializer] 初始化演示用户数据...");
        String encodedPassword = passwordEncoder.encode("Demo@1234");

        List<User> demoUsers = List.of(
                buildUser(1L, "admin",       encodedPassword, "管理员",   "13900000001", "admin"),
                buildUser(2L, "dispatcher01",encodedPassword, "张调度",   "13900000002", "dispatcher"),
                buildUser(3L, "driver001",   encodedPassword, "李司机",   "13900000003", "driver"),
                buildUser(4L, "driver002",   encodedPassword, "王司机",   "13900000004", "driver")
        );

        for (User user : demoUsers) {
            userMapper.insert(user);
        }

        log.info("[DataInitializer] 演示用户初始化完成，共插入 {} 条记录", demoUsers.size());
        log.info("[DataInitializer] 演示账号：admin / dispatcher01 / driver001 / driver002，密码统一：Demo@1234");
    }

    private User buildUser(Long id, String username, String password,
                           String realName, String phone, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus(1);
        user.setDeleted(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
