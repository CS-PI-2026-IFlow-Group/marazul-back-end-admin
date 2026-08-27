package br.com.marazulturismo.marazulbackendadmin.exception;

public class FuncionarioNotFoundException extends RuntimeException {
    public FuncionarioNotFoundException(Long id) {
        super("Funcionário não encontrado: " + id + ".");
    }
}
