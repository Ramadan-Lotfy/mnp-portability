package com.mnp.portability.common.config;

import com.mnp.portability.common.security.CurrentOperator;
import com.mnp.portability.common.security.OrganizationInterceptor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "organization";

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentOperator.class);
    }

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MNP Portability API")
                        .version("1.0.0-dev")
                        .description("Mobile Number Portability: submit, decide and view porting requests."))
                .components(new Components().addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(OrganizationInterceptor.ORGANIZATION_HEADER)
                        .description("Acting operator: vodafone, orange or etisalat (mocked authentication)")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}