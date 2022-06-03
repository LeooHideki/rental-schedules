package br.com.senai.rentalSchedules.interceptor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import br.com.senai.rentalSchedules.annotation.PrivadoAdm;
import br.com.senai.rentalSchedules.annotation.Publico;
import br.com.senai.rentalSchedules.rest.UsuarioRestController;
import br.com.senai.rentalSchedules.util.DescriptJWT;

@Component
public class AppInterceptor implements HandlerInterceptor {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// variável para obter a URI da request
		String uri = request.getRequestURI();
		// variável para a sessão
		HttpSession session = request.getSession();

		// se for página de erro, libera
		if (uri.startsWith("/error")) {
			return true;
		}

		if (handler instanceof HandlerMethod) {
			HandlerMethod metodo = (HandlerMethod) handler;
			if (uri.startsWith("/api")) {
				String token = null;

				if (metodo.getMethodAnnotation(Publico.class) != null) {
					return true;
				}

				try {
					token = request.getHeader("Authorization");
					
					DescriptJWT desc = new DescriptJWT();
					
					Map<String, Claim> claims = desc.decodifica(token);
							
					if(metodo.getMethodAnnotation(PrivadoAdm.class) != null) {
						if(claims.get("role") != null) {
							return true;
						}
						
						
						response.sendError(HttpStatus.UNAUTHORIZED.value(), "Você não tem auth");
						return false;
					}
					return true;
				} catch (Exception e) {
					response.sendError(HttpStatus.UNAUTHORIZED.value(), e.getMessage());
					return false;
				}
			}
		}

		return true;
	}
}
