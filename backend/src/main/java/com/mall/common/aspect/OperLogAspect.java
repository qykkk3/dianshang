package com.mall.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.annotation.OperLog;
import com.mall.common.util.CommonUtils;
import com.mall.entity.SysOperLog;
import com.mall.mapper.SysOperLogMapper;
import com.mall.security.LoginUser;
import com.mall.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

/**
 * 操作日志切面：拦截标注了 {@link OperLog} 的方法并写入 sys_oper_log
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private static final int MAX_PARAM_LENGTH = 2000;

    private final SysOperLogMapper sysOperLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        Throwable error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            saveLog(joinPoint, operLog, System.currentTimeMillis() - start, error);
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, OperLog operLog, long costTime, Throwable error) {
        try {
            SysOperLog entity = new SysOperLog();
            LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
            if (loginUser != null) {
                entity.setUserId(loginUser.getUserId());
                entity.setUsername(loginUser.getUsername());
            }
            entity.setModule(operLog.module());
            entity.setOperation(operLog.value());
            entity.setMethod(joinPoint.getSignature().getDeclaringTypeName()
                    + "." + joinPoint.getSignature().getName());
            entity.setCostTime(costTime);
            entity.setStatus(error == null ? 1 : 0);
            if (error != null) {
                entity.setErrorMsg(truncate(error.getMessage(), MAX_PARAM_LENGTH));
            }
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                entity.setRequestUri(request.getRequestURI());
                entity.setIp(CommonUtils.getIp(request));
            }
            if (operLog.saveParams()) {
                entity.setParams(truncate(buildParams(joinPoint.getArgs()), MAX_PARAM_LENGTH));
            }
            sysOperLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("记录操作日志失败：{}", e.getMessage());
        }
    }

    private String buildParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            Object[] filtered = Arrays.stream(args)
                    .filter(arg -> !(arg instanceof HttpServletRequest)
                            && !(arg instanceof HttpServletResponse)
                            && !(arg instanceof MultipartFile)
                            && !(arg instanceof MultipartFile[]))
                    .toArray();
            return objectMapper.writeValueAsString(filtered);
        } catch (Exception e) {
            return "参数序列化失败";
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
