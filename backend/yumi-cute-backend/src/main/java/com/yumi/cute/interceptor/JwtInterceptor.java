package com.yumi.cute.interceptor;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.common.UserContext;
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
    /** 设置白名单 **/
    private static final List<String> WHITE_LIST = List.of(
            "/user/login",
            "/user/register",
            "/style/list"
    );

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        //1.判断是否存在于白名单
        if(WHITE_LIST.contains(path)){
            return true; // 直接放行
        }

        //2. 浏览器的跨域预检请求直接放行
        if("OPTIONS".equals(request.getMethod())){
            return true;
        }

        //3. 去请求头里取token
        String token = request.getHeader("Authorization");
        if(token == null || !token.startsWith("Bearer ")){
            throw new BizException(ResultCode.UNAUTHORIZED,"请先登录！");
        }

        //4. 去掉“Bearer”前缀，剩下真正的token
        token = token.substring(7);

        //5. 校验token，解析出用户id
        try{
            Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.valueOf(claims.getSubject());
            UserContext.setUserId(userId);
        }catch (Exception e){
            throw new BizException(ResultCode.UNAUTHORIZED,"登录已过期，请重新登录");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后清理
        UserContext.clear();
    }
}
