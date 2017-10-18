package gov.usgs.wma.mlrgateway.config;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Configuration
@EnableResourceServer
public class OAuth2ResourceServerConfig extends ResourceServerConfigurerAdapter {
//    @Override
//    public void configure(ResourceServerSecurityConfigurer config) {
//        config.tokenServices(tokenServices());
//        config.resourceId(null);
//    }
// 
//    @Bean
//    public TokenStore tokenStore() {
//        return new JwtTokenStore(accessTokenConverter());
//    }
// 
//    @Bean
//    public JwtAccessTokenConverter accessTokenConverter() {
//    	final JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
//        Resource resource = new ClassPathResource("public.txt");
//        String publicKey = null;
//        try {
//            publicKey = IOUtils.toString(resource.getInputStream());
//        } catch (final IOException e) {
//            throw new RuntimeException(e);
//        }
//        converter.setVerifierKey(publicKey);
//        return converter;
//    }
// 
//    @Bean
//    @Primary
//    public DefaultTokenServices tokenServices() {
//        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
//        defaultTokenServices.setTokenStore(tokenStore());
//        return defaultTokenServices;
//    }

}
