package br.com.senai.rentalSchedules.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;


import br.com.senai.rentalSchedules.model.Usuario;


public interface UsuarioRepository extends PagingAndSortingRepository<Usuario, Long>{
	public Usuario findByEmailAndSenha(String email, String senha);

	public Usuario findByEmail(String email);

	public Usuario findByMatricula(String matricula);
	
	public List<Usuario> findAllByStatus(boolean status);
	
}
	