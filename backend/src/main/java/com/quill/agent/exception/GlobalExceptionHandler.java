package com.quill.agent.exception;

import com.quill.agent.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * 全局异常处理：所有异常翻译为友好错误码 + message。
 * 原始 Java 异常堆栈只写后端日志，绝不返回前端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        String requestId = newRequestId();
        log.warn("[{}] 业务异常 code={} message={}", requestId, errorCode.code(), ex.getMessage());
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiResponse.error(errorCode.code(), ex.getMessage(), requestId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String requestId = newRequestId();
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.ERR_PARAM_001.message());
        log.warn("[{}] 参数校验失败：{}", requestId, detail);
        return ResponseEntity.status(ErrorCode.ERR_PARAM_001.httpStatus())
                .body(ApiResponse.error(ErrorCode.ERR_PARAM_001.code(), detail, requestId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        String requestId = newRequestId();
        // 后端保留完整堆栈便于排查；前端只拿到友好提示
        log.error("[{}] 未预期异常", requestId, ex);
        return ResponseEntity.status(ErrorCode.ERR_INTERNAL_001.httpStatus())
                .body(ApiResponse.error(ErrorCode.ERR_INTERNAL_001.code(),
                        ErrorCode.ERR_INTERNAL_001.message(), requestId));
    }

    private String newRequestId() {
        return UUID.randomUUID().toString();
    }
}
