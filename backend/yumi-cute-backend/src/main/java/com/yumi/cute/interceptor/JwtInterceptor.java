package com.yumi.cute.interceptor;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.common.UserContext;
import com.yumi.cute.security.TokenBlacklistService;
import com.yumi.cute.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    /**
     * 设置白名单
     **/
    private static final List<String> WHITE_LIST = List.of(
            "/user/login",
            "/user/register",
            "/style/list"
    );

    /**
     * 前缀白名单
     **/
    private static final List<String> WHITE_PREFIX_LIST = List.of(
            "/share/"
    );

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtInterceptor(JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        //1.判断是否存在于白名单
        if (WHITE_LIST.contains(path)) {
            return true; // 直接放行
        }

        for (String prefix : WHITE_PREFIX_LIST) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        //2. 浏览器的跨域预检请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        //3. 去请求头里取token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BizException(ResultCode.UNAUTHORIZED, "请先登录！");
        }

        //4. 去掉“Bearer”前缀，剩下真正的token
        token = token.substring(7);

        //5. 校验token，解析出用户id
        try {
            Claims claims = jwtUtil.parseToken(token);

            // 验签通过后，再查这个 token 是否已被登出
            if (tokenBlacklistService.isRevoked(claims.getId())) {
                throw new BizException(ResultCode.UNAUTHORIZED, "登录已失效，请重新登录");
            }
            Long userId = Long.valueOf(claims.getSubject());
            UserContext.setUserId(userId);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后清理
        UserContext.clear();
    }
}
