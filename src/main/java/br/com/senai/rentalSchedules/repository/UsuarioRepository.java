package br.com.senai.rentalSchedules.repository;

import org.springframework.data.repository.PagingAndSortingRepository;


import br.com.senai.rentalSchedules.model.Usuario;


public interface UsuarioRepository extends PagingAndSortingRepository<Usuario, Long>{
	
	public Usuario findByEmailAndSenha(String email, String senha);
	
}
	