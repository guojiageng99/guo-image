package com.gjg.guopicturebackend.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.gjg.guopicturebackend.common.BaseResponse;
import com.gjg.guopicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginException(NotLoginException e) {
        log.error("NotLoginException", e);
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e) {
        log.error("NotPermissionException: {}", e.getMessage(), e);
        // 如果错误信息为空，返回默认的无权限提示
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = "无权限";
        }
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, message);
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MyBatisSystemException.class)
    public BaseResponse<?> myBatisSystemExceptionHandler(MyBatisSystemException e) {
        log.error("MyBatisSystemException: MyBatis系统异常", e);
        Throwable cause = e.getCause();
        String message = "数据库操作失败";
        
        // 检查是否是数据库连接问题
        if (cause instanceof DataAccessException) {
            DataAccessException dataAccessException = (DataAccessException) cause;
            Throwable nestedCause = dataAccessException.getCause();
            
            if (nestedCause instanceof CannotGetJdbcConnectionException) {
                CannotGetJdbcConnectionException connException = (CannotGetJdbcConnectionException) nestedCause;
                Throwable sqlCause = connException.getCause();
                
                if (sqlCause instanceof SQLException) {
                    SQLException sqlException = (SQLException) sqlCause;
                    String sqlMessage = sqlException.getMessage();
                    
                    // 识别常见的数据库连接错误
                    if (sqlMessage != null) {
                        if (sqlMessage.contains("Access denied")) {
                            message = "数据库访问被拒绝，请检查用户名和密码是否正确";
                        } else if (sqlMessage.contains("Communications link failure")) {
                            message = "数据库连接失败，请检查数据库服务是否启动";
                        } else if (sqlMessage.contains("Unknown database")) {
                            message = "数据库不存在，请检查数据库名称是否正确";
                        } else {
                            message = "数据库连接失败: " + sqlMessage;
                        }
                    }
                } else {
                    message = "无法获取数据库连接: " + connException.getMessage();
                }
            } else if (nestedCause instanceof SQLException) {
                SQLException sqlException = (SQLException) nestedCause;
                message = "数据库操作失败: " + sqlException.getMessage();
            }
        }
        
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, message);
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public BaseResponse<?> cannotGetJdbcConnectionExceptionHandler(CannotGetJdbcConnectionException e) {
        log.error("CannotGetJdbcConnectionException: 无法获取数据库连接", e);
        Throwable cause = e.getCause();
        String message = "无法获取数据库连接";
        
        if (cause instanceof SQLException) {
            SQLException sqlException = (SQLException) cause;
            String sqlMessage = sqlException.getMessage();
            
            if (sqlMessage != null) {
                if (sqlMessage.contains("Access denied")) {
                    message = "数据库访问被拒绝，请检查用户名和密码是否正确";
                } else if (sqlMessage.contains("Communications link failure")) {
                    message = "数据库连接失败，请检查数据库服务是否启动";
                } else if (sqlMessage.contains("Unknown database")) {
                    message = "数据库不存在，请检查数据库名称是否正确";
                } else {
                    message = "数据库连接失败: " + sqlMessage;
                }
            }
        }
        
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, message);
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public BaseResponse<?> redisConnectionFailureExceptionHandler(RedisConnectionFailureException e) {
        log.error("RedisConnectionFailureException: Redis连接失败", e);
        String message = "Redis服务不可用，请检查Redis服务是否启动";
        
        Throwable cause = e.getCause();
        if (cause != null && cause.getMessage() != null) {
            if (cause.getMessage().contains("Connection refused")) {
                message = "Redis服务未启动，请启动Redis服务（默认端口：6379）";
            } else if (cause.getMessage().contains("Unable to connect")) {
                message = "无法连接到Redis服务器，请检查Redis服务是否正常运行";
            }
        }
        
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, message);
    }

    @ExceptionHandler(DataAccessException.class)
    public BaseResponse<?> dataAccessExceptionHandler(DataAccessException e) {
        log.error("DataAccessException: 数据库访问异常", e);
        
        // 检查是否是 Redis 连接异常
        if (e instanceof RedisConnectionFailureException) {
            return redisConnectionFailureExceptionHandler((RedisConnectionFailureException) e);
        }
        
        Throwable cause = e.getCause();
        String message = "数据库操作失败";
        
        if (cause instanceof SQLException) {
            SQLException sqlException = (SQLException) cause;
            message = "数据库操作失败: " + sqlException.getMessage();
        }
        
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, message);
    }

    @ExceptionHandler(SQLException.class)
    public BaseResponse<?> sqlExceptionHandler(SQLException e) {
        log.error("SQLException: SQL执行异常", e);
        String message = "数据库操作失败: " + e.getMessage();
        
        if (e.getMessage() != null) {
            if (e.getMessage().contains("Access denied")) {
                message = "数据库访问被拒绝，请检查用户名和密码是否正确";
            }
        }
        
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.error("HttpMessageNotReadableException: JSON解析异常", e);
        String message = "请求参数格式错误";
        
        Throwable cause = e.getCause();
        if (cause instanceof MismatchedInputException) {
            MismatchedInputException mismatchedInputException = (MismatchedInputException) cause;
            String inputValue = mismatchedInputException.getPathReference();
            
            // 检查是否是 JSON 格式错误（如末尾有逗号）
            if (inputValue != null && inputValue.contains(",")) {
                // 检查是否是末尾逗号
                String trimmed = inputValue.trim();
                if (trimmed.endsWith(",}") || trimmed.endsWith(",]")) {
                    message = "JSON格式错误：请移除末尾的逗号。正确的格式应该是：{\"current\": 1, \"pageSize\": 20}";
                } else {
                    message = "JSON格式错误，请检查请求体格式是否正确";
                }
            } else {
                message = "请求参数格式错误，请确保 Content-Type 为 application/json，且 JSON 格式正确";
            }
        } else if (e.getMessage() != null) {
            if (e.getMessage().contains("JSON parse error")) {
                message = "JSON解析失败，请检查请求体格式是否正确。错误信息：" + e.getMessage();
            }
        }
        
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException: 系统运行时异常", e);
        String message = "系统错误: " + e.getMessage();
        
        // 如果是数据库连接异常，给出更明确的提示
        if (e.getMessage() != null) {
            if (e.getMessage().contains("Communications link failure")) {
                message = "数据库连接失败，请检查数据库服务是否启动";
            } else if (e.getMessage().contains("Access denied")) {
                message = "数据库访问被拒绝，请检查用户名和密码是否正确";
            }
        }
        
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, message);
    }
}
