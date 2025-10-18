package com.basebackend.security.filter;

import com.alibaba.fastjson2.JSON;
import com.basebackend.common.constant.CommonConstants;
import com.basebackend.common.model.Result;
import com.basebackend.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 获取Token
            String token = getTokenFromRequest(request);

            // 验证Token
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                String subject = jwtUtil.getSubjectFromToken(token);

                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("设置用户认证信息到SecurityContext: {}", subject);
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("认证失败: {}", e.getMessage());
            handleAuthenticationError(response, "认证失败");
        }
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(CommonConstants.TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 处理认证错误
     */
    private void handleAuthenticationError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(CommonConstants.CONTENT_TYPE_JSON);
        response.setCharacterEncoding(CommonConstants.UTF8);
        response.getWriter().write(JSON.toJSONString(Result.error(401, message)));
    }
}
