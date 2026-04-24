package com.example.mynewwork.exception;

import java.io.Serial;

/**
 * 实体重复异常
 *
 * @author Claude Code
 * @since 1.0.0
 */
public class DuplicateEntityException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateEntityException(String entityName, String field, String value) {
        super(400, String.format("%s已存在，%s: %s", entityName, field, value));
    }

    public DuplicateEntityException(String message) {
        super(400, message);
    }
}
