package com.co.lowcode.security.auth.config;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.co.lowcode.lineabase.oauth.service.RoleService;
import com.co.lowcode.lineabase.oauth.service.UserService;
import com.co.lowcode.lineabase.oauth.service.UserServiceI;
import com.co.lowcode.security.auth.CustomAuthenticationProvider;
import com.co.lowcode.security.common.JwtAuthenticationConfig;
import com.co.lowcode.security.common.JwtUsernamePasswordAuthenticationFilter;
import com.co.lowcode.service.RedisService;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired JwtAuthenticationConfig config;
    
    @Autowired RoleService roleService;
    
    @Autowired  RedisService redisService;
    

    @Autowired UserServiceI userService;

    @Bean
    public JwtAuthenticationConfig jwtConfig() {
        return new JwtAuthenticationConfig();
    }
    

    @Bean
    public RoleService roleServiceConfig() {
        return new RoleService();
    }
    
    @Bean
    public RedisService redisServiceConfig() {
        return new RedisService();
    }
    

    @Bean
    public UserService userServiceConfig() {
        return new UserService();
    }
    

	@Override
	public void configure(WebSecurity web) throws Exception {
	    web.ignoring().antMatchers(HttpMethod.OPTIONS, "/**");
	}
    
    
	@Autowired
	private CustomAuthenticationProvider customAuthenticationProvider;


	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		customAuthenticationProvider.setUserService(userService);
		auth.authenticationProvider(customAuthenticationProvider);
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
                    .addFilterAfter(new JwtUsernamePasswordAuthenticationFilter(config, authenticationManager(), roleService, userService, redisService),
                            UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests()
                	.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .antMatchers(config.getUrl()).permitAll()
                    .antMatchers("/getLdapUsers").permitAll()
                    .antMatchers("/getToken").permitAll()
                    .antMatchers("/getApiToken").permitAll()
                    .antMatchers("/googleLogin").permitAll()
                    .antMatchers("/getInfoUser").permitAll()
                    .antMatchers("/getUsersSSO").permitAll()
                    .anyRequest().authenticated();
    }
}

