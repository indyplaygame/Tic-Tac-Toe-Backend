package api.indy.auth;

import api.indy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.UUID;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;

    @Autowired
    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if(handler instanceof HandlerMethod method) {
            if(method.getMethodAnnotation(AuthRequired.class) == null && !method.getBeanType().isAnnotationPresent(AuthRequired.class)) return true;

            String token = request.getHeader("Authorization");

            if(token == null || token.isEmpty()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authorization token");
                return false;
            }

            if(!authService.verifyToken(UUID.fromString(token))) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authorization token");
                return false;
            }
        }

        return true;
    }
}
