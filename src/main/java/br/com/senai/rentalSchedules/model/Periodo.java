package br.com.senai.rentalSchedules.model;

public enum Periodo {

	MANHA("manha"), TARDE("tarde"), NOITE("noite");

	String periodo;

	private Periodo(String periodo) {
		this.periodo = periodo;
	}

	@Override
	public String toString() {
		return this.periodo;

	}
}
