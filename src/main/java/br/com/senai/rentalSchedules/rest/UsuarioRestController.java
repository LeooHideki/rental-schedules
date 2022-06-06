package br.com.senai.rentalSchedules.rest;

import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import br.com.senai.rentalSchedules.annotation.Privado;
import br.com.senai.rentalSchedules.model.TrocarSenha;
import br.com.senai.rentalSchedules.util.DescriptJWT;
import br.com.senai.rentalSchedules.util.EnviaEmailService;
import br.com.senai.rentalSchedules.util.FirebaseUtil;
import br.com.senai.rentalSchedules.util.HashUtil;
import com.auth0.jwt.interfaces.Claim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import br.com.senai.rentalSchedules.annotation.PrivadoAdm;
import br.com.senai.rentalSchedules.annotation.Publico;
import br.com.senai.rentalSchedules.model.Erro;
import br.com.senai.rentalSchedules.model.TokenJWT;
import br.com.senai.rentalSchedules.model.Usuario;
import br.com.senai.rentalSchedules.repository.UsuarioRepository;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioRestController {
	
	public static final String EMISSOR = "ROCA";
	public static final String SECRET = "D123DDAS@";
	@Autowired
	private UsuarioRepository repository;
	@Autowired
	private EnviaEmailService send;

	@Autowired
	private FirebaseUtil fireUtil;

	@PrivadoAdm
	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> criarUsuario(@RequestBody Usuario usuario) {
		try {
			Usuario usuarioExiste = repository.findByMatricula(usuario.getMatricula());
			if(usuarioExiste != null && !usuarioExiste.isStatus()) {
				usuario.setRole(true);
				usuario.setId(usuarioExiste.getId());
			}
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
			
			if(!usuario.isStatus()) {
				return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
			}
			
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
	
	@PrivadoAdm
	@RequestMapping(value = "/lista", method = RequestMethod.GET)
	public Iterable<Usuario> listarUsuario(){
		return repository.findAllByStatus(true);
		
	}
	
	@PrivadoAdm
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


	@Publico
	@RequestMapping(value = "/email/{matricula}", method = RequestMethod.GET)
	public ResponseEntity<Object> testeEmail (@PathVariable("matricula") String matriculaUser, HttpServletRequest headers) {

		Usuario usuario = repository.findByMatricula(matriculaUser);

		if(usuario != null) {

			Map<String, Object> payload = new HashMap<String, Object>();
			payload.put("id_user", usuario.getId());
			payload.put("role", usuario.isRole());
			Calendar expiracao = Calendar.getInstance();
			expiracao.add(Calendar.HOUR, 1);
			Algorithm algoritmo = Algorithm.HMAC256(SECRET);
			TokenJWT tokenJwt = new TokenJWT();
			tokenJwt.setToken(JWT.create()
					.withPayload(payload)
					.withIssuer(EMISSOR)
					.withExpiresAt(expiracao.getTime())
					.sign(algoritmo));


			String urlReset = headers.getHeader("origin") + "/alterar-senha/"+ tokenJwt.getToken();
			send.enviaEmail(
					usuario.getEmail(),
					"<div>" +
							"<h1>Você pediu um reset de senha, clique e recupere a senha </h1>" +
							"<a style='background:blue;padding:.5rem 1rem;color:#fff;border-radius:8px;text-decoration:none;' href='" + urlReset + "'> Alterar sua senha </a>" +
							"</div>" +
							"<p>caso não seja você que pediu o reset de senha, por favor entre em contato</p>",
					"Reset de senha"
			);

				return new ResponseEntity<Object>(HttpStatus.OK);
		}

		return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);

	}
	
	@PrivadoAdm
	@RequestMapping(value = "/alter-role/{id}/{role}", method = RequestMethod.PUT)
	public ResponseEntity<Object> alteraRole (@PathVariable("id") Long id, @PathVariable("role") Long role) {
		if(role != 1 && role != 0) {
			return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
		}

		boolean newRole = role == 1;
		try {
			Optional<Usuario> usuario = repository.findById(id);

			if(!usuario.isPresent()) {
				return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
			}


			usuario.get().setRole(newRole);
			repository.save(usuario.get());
			return ResponseEntity.ok(usuario.get());
		} catch (Exception e) {
			Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getClass().getName());
			return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}



	@Privado
	@RequestMapping(value = "/alterar-senha/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> alterarSenha (HttpServletRequest headers, @RequestBody TrocarSenha trocarSenhas,@PathVariable("id") Long idUsuarioReq) {

		DescriptJWT desc = new DescriptJWT();
		Map<String, Claim> claims = desc.decodifica(headers.getHeader("Authorization"));
		String idUsuario = "" + claims.get("id_user");
		Long idUser = Long.parseLong(idUsuario);

		Optional<Usuario> usuario = repository.findById(idUser);

		if(!usuario.isPresent()){
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		System.out.println("IDrq" + idUsuarioReq);
		if(idUsuarioReq == -1) {
			usuario.get().setSenha(trocarSenhas.getNovaSenha());
			try {
				repository.save(usuario.get());
				return new ResponseEntity<>(HttpStatus.ACCEPTED);
			} catch (Exception e) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

		}


		if(usuario.get().getSenha().equals(HashUtil.hash(trocarSenhas.getSenhaAntiga()))) {
			if(idUsuarioReq != usuario.get().getId()){
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}
			usuario.get().setSenha(trocarSenhas.getNovaSenha());
			try {
				repository.save(usuario.get());
			} catch (Exception e) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			return new ResponseEntity<>(HttpStatus.ACCEPTED);
		}

		Erro erro = new Erro(HttpStatus.BAD_REQUEST, "Senha antiga invalida", this.getClass().getName());
		return new ResponseEntity<Object>(erro, HttpStatus.BAD_REQUEST);

	}


	@Privado
	@RequestMapping(value = "/save-profile", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Object> imagemPerfil (@RequestPart MultipartFile[] images, HttpServletRequest headers) {
		DescriptJWT desc = new DescriptJWT();
		Map<String, Claim> claims = desc.decodifica(headers.getHeader("Authorization"));
		String idUsuario = "" + claims.get("id_user");
		Long idUser = Long.parseLong(idUsuario);

		Optional<Usuario> usuario = repository.findById(idUser);
		if(usuario.get() == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		String novaUrl;

		if (images[0].getOriginalFilename().isEmpty()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		try {

			if(usuario.get().getImagem() != null) {
				fireUtil.deletar(usuario.get().getImagem());
			}
			novaUrl = fireUtil.upload(images[0]);
		} catch (IOException err) {
			err.printStackTrace();
			Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, err.getMessage(), err.getClass().getName());
			return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
		}

			usuario.get().setImagem(novaUrl);


			try {
				repository.save(usuario.get());
			} catch (Exception err) {
				err.printStackTrace();
				Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, err.getMessage(), err.getClass().getName());
				return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
			}
			return ResponseEntity.ok(usuario);
	}
}
