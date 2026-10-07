package com.mnp.portability.common.security;

import com.mnp.portability.common.exception.UnauthorizedException;
import com.mnp.portability.operator.Operator;
import com.mnp.portability.operator.OperatorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
@RequiredArgsConstructor
public class OrganizationInterceptor implements HandlerInterceptor {

    public static final String ORGANIZATION_HEADER = "organization";
    static final String OPERATOR_ATTRIBUTE = OrganizationInterceptor.class.getName() + ".OPERATOR";

    private final OperatorService operatorService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        String organization = request.getHeader(ORGANIZATION_HEADER);
        if (!StringUtils.hasText(organization)) {
            throw new UnauthorizedException("Missing '%s' header".formatted(ORGANIZATION_HEADER));
        }

        Operator operator = operatorService
                .findByCode(organization.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UnauthorizedException(
                        "Unknown organization '%s'".formatted(organization)));

        request.setAttribute(OPERATOR_ATTRIBUTE, operator);
        return true;
    }
}