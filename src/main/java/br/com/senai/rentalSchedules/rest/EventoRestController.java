package br.com.senai.rentalSchedules.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import br.com.senai.rentalSchedules.annotation.PrivadoAdm;
import br.com.senai.rentalSchedules.model.Solicitacao;
import br.com.senai.rentalSchedules.util.EnviaEmailService;
import br.com.senai.rentalSchedules.util.FirebaseUtil;

import org.checkerframework.checker.units.qual.A;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.auth0.jwt.interfaces.Claim;

import br.com.senai.rentalSchedules.annotation.Privado;
import br.com.senai.rentalSchedules.annotation.Publico;
import br.com.senai.rentalSchedules.model.Erro;
import br.com.senai.rentalSchedules.model.Evento;
import br.com.senai.rentalSchedules.repository.EventoRepository;
import br.com.senai.rentalSchedules.util.DescriptJWT;
import br.com.senai.rentalSchedules.util.EnviaEmailService;
import br.com.senai.rentalSchedules.util.FirebaseUtil;

@RestController
@RequestMapping("/api/eventos")
public class EventoRestController {
	@Autowired
	private EventoRepository repository;

	@Autowired
	private EnviaEmailService send;
	
	@Autowired
	private FirebaseUtil fireUtil;
	
	@Publico
	@RequestMapping(value = "", method = RequestMethod.GET)
	public ResponseEntity<Iterable<Evento>> pegarEventos() {
		return ResponseEntity.ok(repository.findAll());
	}

	@Privado
	@RequestMapping(value = "", method = RequestMethod.POST)
	public ResponseEntity<Object> criarEventoMultiplo(@RequestBody ArrayList<Evento> evento) {
		boolean hasError = false;
		String dataOccuped = "";

		for (Evento e : evento) {

			Evento event = repository.findByDataReservadaAndPeriodo(e.getDataReservada(), e.getPeriodo());
			if (event != null) {
				hasError = true;
				dataOccuped = e.getDataReservada();
				break;
			}

		}

		if (hasError) {
			Erro erro = new Erro(HttpStatus.BAD_REQUEST, "Data já está sendo utilizada : " + dataOccuped,
					"EventControllerRest");
			return new ResponseEntity<Object>(erro, HttpStatus.BAD_REQUEST);
		}

		List<Evento> createdEvents = new ArrayList<Evento>();

		for (Evento e : evento) {
			try {
				Evento event = repository.save(e);
				createdEvents.add(event);
			} catch (Exception err) {
				err.printStackTrace();
				Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, err.getMessage(), err.getClass().getName());
				return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return ResponseEntity.ok(createdEvents);
	}

	@Privado
	@RequestMapping(value = "/delete", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> excluirEvento(HttpServletRequest headers, @RequestBody Iterable<Long> ids) {
		
		System.out.println("PASAPASPASSAP");
		Iterable<Evento> eventos = repository.findAllById(ids);
		
		for (Evento evento : eventos) {
			if (evento == null) {
				return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
			}
		}

		DescriptJWT desc = new DescriptJWT();
		Map<String, Claim> claims = desc.decodifica(headers.getHeader("Authorization"));
		Boolean roleUser = Boolean.parseBoolean("" + claims.get("role"));
		String idUsuario = "" + claims.get("id_user");
		Long idUser = Long.parseLong(idUsuario);

		
		List<Long> idsDelete = new ArrayList<Long>();
		for (Evento evento : eventos) {
			if (roleUser || idUser == evento.getUsuario().getId()) {
				idsDelete.add(evento.getId());
				for (String foto: evento.verFotos()) {
					if(foto.isEmpty()){
						continue;
					}
					fireUtil.deletar(foto);
				}
				repository.delete(evento);
			} else {
				return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
			}
		}
		

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("idsDelete", idsDelete);
		
		return  ResponseEntity.ok(result);
	}

	@Privado
	@RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
	public ResponseEntity<Object> eventoPorUsuario(@PathVariable("id") Long idUsuario) {
		try {
			List<Evento> eventos = repository.findByUsuarioId(idUsuario);
			return ResponseEntity.ok(eventos);
		} catch (Exception err) {
			err.printStackTrace();
			Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, err.getMessage(), err.getClass().getName());
			return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@Privado
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> atualizarEvento(HttpServletRequest headers, @PathVariable("id") Long idEvento,
			@RequestBody Evento eventoAtualizado) {

		Optional<Evento> event = repository.findById(idEvento);
		if (!event.isPresent()) {
			return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
		}

		DescriptJWT desc = new DescriptJWT();
		Map<String, Claim> claims = desc.decodifica(headers.getHeader("Authorization"));
		Boolean roleUser = Boolean.parseBoolean("" + claims.get("role"));
		String idUsuario = "" + claims.get("id_user");
		Long idUser = Long.parseLong(idUsuario);
		System.out.println(claims.get("role"));
		if (roleUser || idUser == event.get().getUsuario().getId()) {
			repository.save(eventoAtualizado);
			return new ResponseEntity<Object>(HttpStatus.ACCEPTED);
		}

		return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
	}

	@Publico
	@RequestMapping(value = "/email", method = RequestMethod.GET)
	public ResponseEntity<Object> testeEmail() {
		return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
	}
	

	@Publico
	@RequestMapping(value = "/send-images/{id}", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Object> salvarImagens (@RequestPart MultipartFile[] images, @PathVariable("id") Long idEvento) {
		Optional<Evento> evento = repository.findById(idEvento);

		if(evento.get() == null){
			return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
		}

		String urlSalvas = evento.get().getImagens() == null ? "" : evento.get().getImagens();


		for (MultipartFile arquivo : images) {
			if (arquivo.getOriginalFilename().isEmpty()) {continue;}
			try {
				urlSalvas += fireUtil.upload(arquivo) + ";";
			} catch (IOException err) {
				err.printStackTrace();
				Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, err.getMessage(), err.getClass().getName());
				return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		evento.get().setImagens(urlSalvas);
		try {
			repository.save(evento.get());
		} catch (Exception err) {
			Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, err.getMessage(), err.getClass().getName());
			return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
		}

			return ResponseEntity.ok(evento.get());
		}

	@RequestMapping(value = "/send-images/{id}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> inserirFotos (@RequestBody MultipartFile[] fileFotos, @PathVariable("id") Long idEvent) {
		Optional<Evento> evento = repository.findById(idEvent);
		
		if(evento.get() == null) {
			return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
		}
		
		String fotos = evento.get().getImagens();
		// percorre cada arquivo no vetor
		for (MultipartFile arquivo : fileFotos) {
			// verifica se o arquivo existe
			if (arquivo.getOriginalFilename().isEmpty()) {
				// vai para o proximo arquivo
				continue;
			}
			try {
				// faz o upload e guarda a URL na string fotos
				fotos += fireUtil.upload(arquivo) + ";";
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		// guarda na variavel hospital as fotos
		evento.get().setImagens(fotos);
		
		try {
			repository.save(evento.get());	
		} catch (Exception e) {
			return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return ResponseEntity.ok(fotos);
	}


	@PrivadoAdm
	@RequestMapping(value = "imagens/{nfoto}/{evento}", method = RequestMethod.DELETE)
	public ResponseEntity<Object> excluirFotos(HttpServletRequest headers, @PathVariable("nfoto") Integer nfoto, @PathVariable("evento") Long idEvento) {

		DescriptJWT desc = new DescriptJWT();
		Map<String, Claim> claims = desc.decodifica(headers.getHeader("Authorization"));
		String idUsuario = "" + claims.get("id_user");
		boolean hasRole = Boolean.parseBoolean(claims.get("role") + "");
		Long idUser = Long.parseLong(idUsuario);

		Evento evento = repository.findById(idEvento).get();

		if(evento == null) {
			return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
		}


		if(idUser == evento.getUsuario().getId() || hasRole) {
			try {
				String urlFoto = evento.verFotos()[nfoto];
				System.out.println(urlFoto);
				fireUtil.deletar(urlFoto);

				evento.setImagens(evento.getImagens().replace(urlFoto + ";", ""));
				repository.save(evento);


				return new ResponseEntity<Object>(HttpStatus.OK);
			} catch (Exception e) {
				e.printStackTrace();
				Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getClass().getName());
				return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}


		return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
	}

	@PrivadoAdm
	@RequestMapping(value = "/solicitacao", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> solicitacaoDeAssunto(@RequestBody Solicitacao solicitacao) {
		Evento evento = repository.findById(solicitacao.getIdEvento()).get();
		if(evento == null) {
			return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
		}

		String SOLICITE_TITULO = "Reinvidicação de evento - De : " + solicitacao.getNomeDe() + " - evento do dia " + evento.getDataReservada() + " - " + evento.getTitulo();
		String HTML_MOTIVO = "<div> " +
				"<h1>Este é um email automatico para reivindicacão de email, seu evento não será cancelado, mas por favor entre em contato com o solicitante!</h1>" +
				"<h1>"+ solicitacao.getDe()  +" solicita o evento " + evento.getTitulo() + " </h1>" +
				"<p>" + solicitacao.getMotivo() + "</p>" +
				"<p>Email para contato - "+ solicitacao.getDe()  +"</p>" +
				"</div>";
		try {
			send.enviaEmail(
					solicitacao.getPara(),
					HTML_MOTIVO,
					SOLICITE_TITULO
			);

			return new ResponseEntity<Object>(HttpStatus.ACCEPTED);
		} catch (Exception e) {
			e.printStackTrace();
			Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getClass().getName());
			return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
		}


	}

}