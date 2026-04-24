package com.example.mynewwork.service;

import com.example.mynewwork.exception.DuplicateEntityException;
import com.example.mynewwork.exception.EntityNotFoundException;
import com.example.mynewwork.model.entity.User;
import com.example.mynewwork.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户业务逻辑服务层
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    public Optional<User> findByUsername(String username) {
        log.debug("根据用户名查询用户: {}", username);
        return userRepository.findByUsername(username);
    }

    /**
     * 根据邮箱查找用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    public Optional<User> findByEmail(String email) {
        log.debug("根据邮箱查询用户: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * 根据ID查找用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public Optional<User> findById(Long id) {
        log.debug("根据ID查询用户: {}", id);
        return userRepository.findById(id);
    }

    /**
     * 分页查询所有用户
     *
     * @param pageable 分页参数
     * @return 用户分页数据
     */
    public Page<User> findAll(Pageable pageable) {
        log.debug("分页查询所有用户");
        return userRepository.findAll(pageable);
    }

    /**
     * 根据关键字搜索用户
     *
     * @param keyword 搜索关键字
     * @return 匹配的用户列表
     */
    public List<User> searchByKeyword(String keyword) {
        log.debug("根据关键字搜索用户: {}", keyword);
        return userRepository.findByKeyword(keyword);
    }

    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 创建后的用户信息
     */
    @Transactional
    public User createUser(User user) {
        log.info("创建用户: {}", user.getUsername());

        // 检查用户名和邮箱是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateEntityException("用户", "用户名", user.getUsername());
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateEntityException("用户", "邮箱", user.getEmail());
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * 更新用户信息
     *
     * @param id 用户ID
     * @param userDetails 用户详细信息
     * @return 更新后的用户信息
     */
    @Transactional
    public User updateUser(Long id, User userDetails) {
        log.info("更新用户信息, ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("用户", id));

        // 如果用户名被修改，检查新用户名是否已存在
        if (!user.getUsername().equals(userDetails.getUsername()) &&
            userRepository.existsByUsername(userDetails.getUsername())) {
            throw new DuplicateEntityException("用户", "用户名", userDetails.getUsername());
        }

        // 如果邮箱被修改，检查新邮箱是否已存在
        if (!user.getEmail().equals(userDetails.getEmail()) &&
            userRepository.existsByEmail(userDetails.getEmail())) {
            throw new DuplicateEntityException("用户", "邮箱", userDetails.getEmail());
        }

        // 更新用户信息
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setFullName(userDetails.getFullName());
        user.setDepartment(userDetails.getDepartment());
        user.setPosition(userDetails.getPosition());
        user.setRoles(userDetails.getRoles());
        user.setEnabled(userDetails.getEnabled());
        user.setRemark(userDetails.getRemark());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * 更新用户密码
     *
     * @param id 用户ID
     * @param newPassword 新密码
     */
    @Transactional
    public void updatePassword(Long id, String newPassword) {
        log.info("更新用户密码, ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("用户", id));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    /**
     * 记录用户最后登录时间
     *
     * @param id 用户ID
     */
    @Transactional
    public void updateLastLoginTime(Long id) {
        log.debug("更新用户最后登录时间, ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("用户", id));

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    @Transactional
    public void deleteUser(Long id) {
        log.info("删除用户, ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("用户", id);
        }

        userRepository.deleteById(id);
    }

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}