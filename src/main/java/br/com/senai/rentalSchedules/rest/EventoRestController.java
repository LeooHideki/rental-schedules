package br.com.senai.rentalSchedules.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


import br.com.senai.rentalSchedules.annotation.Publico;
import br.com.senai.rentalSchedules.model.Erro;
import br.com.senai.rentalSchedules.model.Evento;
import br.com.senai.rentalSchedules.repository.EventoRepository;

@RestController
@RequestMapping("/api/eventos")
public class EventoRestController {
	@Autowired
	private EventoRepository repository;

	@Publico
	@RequestMapping(value = "", method = RequestMethod.GET)
	public ResponseEntity<Iterable<Evento>> pegarEventos() {
		return ResponseEntity.ok(repository.findAll());
	}

	@Publico
	@RequestMapping(value = "", method = RequestMethod.POST)
	public ResponseEntity<Object> criarEventoMultiplo(@RequestBody ArrayList<Evento> evento) {
		boolean hasError = false;
		String dataOccuped = "";
		
		for (Evento e : evento) {
			System.out.println(e);
			Evento event = repository.findByDataReservadaAndPeriodo(e.getDataReservada(), e.getPeriodo());
			if(event != null) {
				hasError = true;
				dataOccuped = e.getDataReservada();
				break;
			}
			
			
		}
		
		if(hasError) {
			Erro erro = new Erro(HttpStatus.BAD_REQUEST, "Data já está sendo utilizada : " + dataOccuped, "EventControllerRest");
			return new ResponseEntity<Object>(erro, HttpStatus.BAD_REQUEST);
		} 
		
		
		List<Evento> createdEvents = new ArrayList<Evento>();
		
		for (Evento e: evento) {
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

}