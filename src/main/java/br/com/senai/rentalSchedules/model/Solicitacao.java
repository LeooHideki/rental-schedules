package br.com.senai.rentalSchedules.model;

import lombok.Data;

@Data
public class Solicitacao {
    private String nomeDe;
    private String de;
    private String para;
    private String motivo;
    private Long idEvento;
}
