package com.co.lowcode.security.gateway;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.co.lowcode.security.common.JwtAuthenticationConfig;
import com.co.lowcode.security.common.JwtTokenAuthenticationFilter;
import com.co.lowcode.service.RedisService;


@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthenticationConfig config;
    
    @Autowired
    private RedisService redisService;
    
    @Bean
    public RedisService redisServiceConfig() {
        return new RedisService();
    }
    
   
    @Bean
    public JwtAuthenticationConfig jwtConfig() {
        return new JwtAuthenticationConfig();
    }
    

	@Override
	public void configure(WebSecurity web) throws Exception {
	    web.ignoring().antMatchers(HttpMethod.OPTIONS, "/**");
	}

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf().disable()
                .logout().disable()
                .formLogin().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                    .anonymous()
                .and()
                    .exceptionHandling().authenticationEntryPoint(
                            (req, rsp, e) -> rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .and()
                    .addFilterAfter(new JwtTokenAuthenticationFilter(config, redisService),
                            UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                   .antMatchers(config.getUrl()).permitAll()
                   .antMatchers("/proxyurl").permitAll()
                   .antMatchers("/token").permitAll()
                   .antMatchers("/googleLogin").permitAll()
                   .antMatchers("/getApiToken").permitAll()
                   .antMatchers("/app").permitAll()
                   .antMatchers("/getUsers").permitAll()
                   .antMatchers("/activateAccount").permitAll()
                   .antMatchers("/forgotPassword").permitAll()
                   .antMatchers("/public-service/**").permitAll()
                   .antMatchers("/ronda-eolica/createAccount").permitAll()
                
                    .anyRequest().authenticated();
    }
}

