package br.com.senai.rentalSchedules.rest;

import java.net.URI;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.rentalSchedules.annotation.Privado;
import br.com.senai.rentalSchedules.annotation.Publico;
import br.com.senai.rentalSchedules.model.Evento;
import br.com.senai.rentalSchedules.repository.EventoRepository;

@RestController
@RequestMapping("/api/eventos")
public class EventoRestController {
	@Autowired
	private EventoRepository repository;

	@Privado
	@RequestMapping(value = "", method = RequestMethod.POST)
	public ResponseEntity<Evento> criarEvento(@RequestBody Evento evento) {
		repository.save(evento);
		return ResponseEntity.created(URI.create("/api/eventos" + evento.getId())).body(evento);
	}

	@Publico
	@RequestMapping(value = "", method = RequestMethod.GET)
	public ResponseEntity<Iterable<Evento>> pegarEventos() {
		return ResponseEntity.ok(repository.findAll());
	}
	
	@Publico
	@RequestMapping(value = "", method = RequestMethod.POST)
	public void criarEventoMultiplo(ArrayList<Evento> evento) {
		for (Evento e : evento) {
			if(e.getDataReservada() != null) {
				break;
			}else {
				repository.save(e);
			}
		}

	}

}