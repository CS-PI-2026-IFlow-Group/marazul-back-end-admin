package br.com.marazulturismo.marazulbackendadmin.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException.Reference;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        errors.putAll(body("Erro de validação. Verifique os campos informados."));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body(ex.getMessage()));
    }

    @ExceptionHandler(InvalidOrExpiredTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidOrExpiredToken(InvalidOrExpiredTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(ex.getMessage()));
    }

    @ExceptionHandler(LicensePlateAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleLicensePlateAlreadyExists(LicensePlateAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(ex.getMessage()));
    }

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleVehicleNotFound(VehicleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(ex.getMessage()));
    }

    @ExceptionHandler(FuncionarioNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFuncionarioNotFound(FuncionarioNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(ex.getMessage()));
    }

    @ExceptionHandler(FuncionarioValidationException.class)
    public ResponseEntity<Map<String, String>> handleFuncionarioValidation(FuncionarioValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
        String message = "Requisição inválida: verifique o formato dos dados enviados.";

        if (ex.getCause() instanceof InvalidFormatException cause) {
            String field = cause.getPath().stream()
                    .map(Reference::getPropertyName)
                    .filter(Objects::nonNull)
                    .reduce((first, last) -> last)
                    .orElse("desconhecido");

            message = "Valor inválido para o campo '" + field + "': " + cause.getValue()
                    + "." + acceptedValues(cause.getTargetType());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Valor inválido para o parâmetro '" + ex.getName() + "': " + ex.getValue()
                + "." + acceptedValues(ex.getRequiredType());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body("A operação viola uma restrição de integridade dos dados. "
                        + "Verifique se a placa informada já está cadastrada."));
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<Map<String, String>> handleDatabaseFailure(Exception ex) {
        log.error("Falha de acesso ao banco de dados", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body("Não foi possível processar a requisição no momento. Tente novamente mais tarde."));
    }

    private static Map<String, String> body(String message) {
        return Map.of("erro", message, "message", message);
    }

    private static String acceptedValues(Class<?> type) {
        if (type == null || !type.isEnum()) {
            return "";
        }
        String values = Arrays.stream(type.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        return " Valores aceitos: " + values + ".";
    }
}
