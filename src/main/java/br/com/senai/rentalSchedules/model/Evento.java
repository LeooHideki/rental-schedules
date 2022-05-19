package br.com.senai.rentalSchedules.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;

@Entity
@Data
public class Evento {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String titulo;
	private String dataCriacao;
	private String dataReservada;
	private Periodo periodo;
	private String imagens;
	private String descricao;
	@ManyToOne
	private Usuario usuario;
	private String tipo;
	private String mencao;
	
	//retorna as fotos na forma vetor de string
		public String[] verFotos() {
		return imagens.split(";");
		}
}
