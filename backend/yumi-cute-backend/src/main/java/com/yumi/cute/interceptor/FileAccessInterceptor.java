package com.yumi.cute.interceptor;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.util.FileSigner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 静态文件访问控制
 */
@Component
@RequiredArgsConstructor
public class FileAccessInterceptor implements HandlerInterceptor {
    private final FileSigner fileSigner;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        if(!fileSigner.verify(path,request.getParameter("exp"),request.getParameter("sig"))) {
            throw new BizException(ResultCode.FORBIDDEN,"图片链接无效或已过期");
        }

        return true;
    }
}
