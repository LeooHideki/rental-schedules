package br.com.senai.rentalSchedules.repository;

import org.springframework.data.repository.PagingAndSortingRepository;

import br.com.senai.rentalSchedules.model.Evento;

public interface EventoRepository extends PagingAndSortingRepository<Evento, Long>{

}
