package com.mnp.portability.common.security;

import com.mnp.portability.operator.Operator;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentOperatorResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentOperator.class)
                && Operator.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Object operator = webRequest.getAttribute(
                OrganizationInterceptor.OPERATOR_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (operator == null) {
            throw new IllegalStateException(
                    "No operator on the request; is the path covered by OrganizationInterceptor?");
        }
        return operator;
    }
}