package br.com.senai.rentalSchedules.rest;

import java.net.URI;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import br.com.senai.rentalSchedules.annotation.PrivadoAdm;
import br.com.senai.rentalSchedules.annotation.Publico;
import br.com.senai.rentalSchedules.model.Erro;
import br.com.senai.rentalSchedules.model.TokenJWT;
import br.com.senai.rentalSchedules.model.Usuario;
import br.com.senai.rentalSchedules.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioRestController {
	
	public static final String EMISSOR = "ROCA";
	public static final String SECRET = "D123DDAS@";
	@Autowired
	private UsuarioRepository repository;
	
	@Publico
	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> criarUsuario(@RequestBody Usuario usuario) {
		try {
			repository.save(usuario);
			return ResponseEntity.created(URI.create("/api/usuario/" + usuario.getId())).body(usuario);
		} catch (DataIntegrityViolationException e) {
			e.printStackTrace();
			Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, "Registtro Duplicado", e.getClass().getName());
			return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getClass().getName());
			return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PrivadoAdm
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ResponseEntity<Usuario> getUsuario(@PathVariable("id") Long idUser) {
// tenta buscar o usuario no repository
		Optional<Usuario> optional = repository.findById(idUser);
// se usuario existir
		if (optional.isPresent()) {
			return ResponseEntity.ok(optional.get());
		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	@PrivadoAdm
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> atualizarUsuario(@RequestBody Usuario usuario, @PathVariable("id") Long id) {
//validação do ID
		if (id != usuario.getId()) {
			throw new RuntimeException("ID inválido");
		}
		repository.save(usuario);
		return ResponseEntity.ok().build();
	}

	
	@Publico
	@RequestMapping(value="/login", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> logar(@RequestBody Usuario usuario){
		//buscar o usuário no banco de dados
		usuario = repository.findByEmailAndSenha(usuario.getEmail(), usuario.getSenha());
		//verifica se o usuário não é nulo
		System.out.println("TESTETESTE");
		if(usuario != null) {
			System.out.println("User is " + usuario.getNome());
			//variável para inserir dados no payload
			Map<String, Object> payload = new HashMap<String, Object>();
			payload.put("id_user", usuario.getId());
			payload.put("role", usuario.isRole());
			//variável para a data de expiração					
			Calendar expiracao = Calendar.getInstance();
			//adiciona
			expiracao.add(Calendar.HOUR, 1);
			//algoritmo para assinar o token
			Algorithm algoritmo = Algorithm.HMAC256(SECRET);
			//cria o objeto token
			TokenJWT tokenJwt = new TokenJWT();
			//gera o token
			tokenJwt.setToken(JWT.create()
					.withPayload(payload)
					.withIssuer(EMISSOR)
					.withExpiresAt(expiracao.getTime())
					.sign(algoritmo));
			
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("user", usuario);
			result.put("access_token", tokenJwt.getToken());
			
			return  ResponseEntity.ok(result);
		}else {
			System.out.println("User is NOK");

			return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	@Publico
	@RequestMapping(value = "/lista", method = RequestMethod.GET)
	public Iterable<Usuario> listarUsuario(){
		return repository.findAll();
		
	}
	
	@Publico
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<Object> inativarUsuario(@PathVariable("id") Long idUsuario) {
		Optional<Usuario> usuario = repository.findById(idUsuario);
		try {
			if(usuario.get() != null) {
				boolean statusUser = !usuario.get().isStatus();
				usuario.get().setStatus(statusUser);
				repository.save(usuario.get());
				return ResponseEntity.ok(usuario.get());
			}
		} catch (Exception e) {
			Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, "Usuario não encontrado", "UsuarioControllerRest");
			return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return null;
	}


	
}
