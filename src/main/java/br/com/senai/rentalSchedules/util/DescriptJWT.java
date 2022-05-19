package br.com.senai.rentalSchedules.util;

import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import br.com.senai.rentalSchedules.rest.UsuarioRestController;

public class DescriptJWT {
	
	public Map<String, Claim> decodifica(String token) {
		Algorithm algoritmo = Algorithm.HMAC256(UsuarioRestController.SECRET);
		JWTVerifier verifier = JWT.require(algoritmo).withIssuer(UsuarioRestController.EMISSOR).build();
		DecodedJWT jwt = verifier.verify(token);
		Map<String, Claim> claims = jwt.getClaims();
		return claims;
	}
}
