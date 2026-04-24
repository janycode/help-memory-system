package com.example.mynewwork.exception;

import java.io.Serial;

/**
 * 实体未找到异常
 *
 * @author Claude Code
 * @since 1.0.0
 */
public class EntityNotFoundException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String entityName, Long id) {
        super(404, String.format("%s不存在，ID: %d", entityName, id));
    }

    public EntityNotFoundException(String message) {
        super(404, message);
    }

    public EntityNotFoundException(String entityName, String field, String value) {
        super(404, String.format("%s不存在，%s: %s", entityName, field, value));
    }
}
