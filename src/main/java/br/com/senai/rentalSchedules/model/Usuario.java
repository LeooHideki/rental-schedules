package br.com.senai.rentalSchedules.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import br.com.senai.rentalSchedules.util.HashUtil;
import lombok.Data;

@Entity
@Data
public class Usuario {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(unique = true)
	@NotNull(message = "Email não poder ser nulo")
	private String email;
	@NotNull(message="Nome não pode ser nulo")
	private String nome;
	@JsonProperty(access = Access.WRITE_ONLY)
	private String senha;
	// usuario ativado ou desativado
	private boolean status;
	// se é admin ou não
	private boolean role;
	@Column(unique = true)
	@NotNull(message = "Matrícula não pode ser nulo")
	private String matricula;
	private String imagem;
	
	
	//aplica hash na senha
	public void setSenha(String senha) {
		this.senha = HashUtil.hash(senha);
	}
	
}
