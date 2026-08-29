package br.com.marazulturismo.marazulbackendadmin.exception;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(Long id) {
        super("Funcionário não encontrado: " + id + ".");
    }
}
