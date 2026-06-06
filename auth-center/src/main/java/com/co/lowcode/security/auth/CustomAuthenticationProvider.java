package com.co.lowcode.security.auth;


import java.util.Collection;

import org.minminas.Ldap;
import org.springframework.ldap.AuthenticationException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.co.lowcode.lineabase.model.User;
import com.co.lowcode.lineabase.oauth.model.UserAuth;
import com.co.lowcode.lineabase.oauth.service.UserServiceI;



@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

	private UserServiceI userService;
	

	public void setUserService(UserServiceI userService) {
		this.userService = userService;
	}
	
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException  {
		UserDetails user = null;
		String username =   ((UserAuth)authentication.getPrincipal()).getUsername();
		String password = (String) authentication.getCredentials();

		User usuario = userService.getUserByUserNameLocal(username);
		
	
		
		if(password.equals("")){
			throw new BadCredentialsException("Password Vacio");
		}
		BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();
		
		if(usuario != null && usuario.isLdap()  ){
			authentication.isAuthenticated();
			String estado = Ldap.getInstance().getAuthentication(username, password).toString();
			//String estado ="VALID";
			
			if(estado.equals("PASSWORD_FAULT")){
				throw new BadCredentialsException("Usuario o Contraseña Invalida"); 	
			} else if(estado.equals("FAULT")){
				throw new AuthenticationServiceException("Ocurrio un error en el servicio de autenticacion, comuniquese con el administrador");
			}else if(estado.equals("NO_EXIST")){
				throw new BadCredentialsException("Usuario o Contraseña Invalida"); 
			}else if(estado.equals("VALID")){
				user = userService.loadUserByUsername(usuario);
				if (user == null) {
					throw new BadCredentialsException("Usuario o Contraseña Invalida");
				}
				if (!user.isEnabled()) {
					throw new BadCredentialsException("Cuenta Deshabilitada");
				}
			}
		}else if(usuario!=null && usuario.getUsername().equals(username) && usuario.getPassword() !=null && bcryptEncoder.matches(password, usuario.getPassword())){
			user = userService.loadUserByUsername(usuario);
			if (user == null) {
				throw new BadCredentialsException("Usuario o Contraseña Invalida");
			}
			if (!user.isEnabled()) {
				throw new BadCredentialsException("Cuenta Deshabilitada");
			}
		}else{
			throw new BadCredentialsException("Usuario o Contraseña Invalida");
		}
		
		Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

		return new UsernamePasswordAuthenticationToken(user, password, authorities);
	}

	@Override
	public boolean supports(Class<?> arg0) {
		return true;
	}

	/* @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(
          UsernamePasswordAuthenticationToken.class);
    }*/
}