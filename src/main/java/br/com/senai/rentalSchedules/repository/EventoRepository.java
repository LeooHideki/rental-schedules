package br.com.senai.rentalSchedules.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;

import br.com.senai.rentalSchedules.model.Evento;
import br.com.senai.rentalSchedules.model.Periodo;

public interface EventoRepository extends PagingAndSortingRepository<Evento, Long>{
	
	public Evento findByDataReservadaAndPeriodo(String dataReservada, Periodo periodo);
	
	public Evento findByUsuarioIdAndId(Long idUsuario, Long id);
	
	public List<Evento> findByUsuarioId(Long idUsuario);
}
