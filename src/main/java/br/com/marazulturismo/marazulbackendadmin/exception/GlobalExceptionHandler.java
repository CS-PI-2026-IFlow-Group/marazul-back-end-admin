package br.com.marazulturismo.marazulbackendadmin.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

/**
 * Padrão das respostas de erro: toda exceção tratada devolve a mesma mensagem
 * nas chaves {@code erro} e {@code message}. A duplicação é intencional —
 * {@code erro} preserva o contrato já consumido e {@code message} é a chave
 * padronizada para o front-end.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Os erros por campo continuam na raiz do corpo, como o front já lê.
        Map<String, String> corpo = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> corpo.put(error.getField(), error.getDefaultMessage()));

        corpo.putAll(corpo("Erro de validação. Verifique os campos informados."));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(corpo(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(corpo(ex.getMessage()));
    }

    @ExceptionHandler(InvalidOrExpiredTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidOrExpiredToken(InvalidOrExpiredTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo(ex.getMessage()));
    }

    @ExceptionHandler(PlacaAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handlePlacaAlreadyExists(PlacaAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(corpo(ex.getMessage()));
    }

    @ExceptionHandler(VeiculoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleVeiculoNotFound(VeiculoNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(corpo(ex.getMessage()));
    }

    @ExceptionHandler(FuncionarioNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFuncionarioNotFound(FuncionarioNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(corpo(ex.getMessage()));
    }

    @ExceptionHandler(FuncionarioValidationException.class)
    public ResponseEntity<Map<String, String>> handleFuncionarioValidation(FuncionarioValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo(ex.getMessage()));
    }

    /**
     * Corpo da requisição malformado ou valor de ENUM inválido (ex.: tipo/modelo/status
     * fora dos valores permitidos). Retorna 400 com mensagem clara e objetiva.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
        String mensagem = "Requisição inválida: verifique o formato dos dados enviados.";

        if (ex.getCause() instanceof InvalidFormatException causa) {
            String campo = causa.getPath().stream()
                    .map(Reference::getPropertyName)
                    .filter(Objects::nonNull)
                    .reduce((primeiro, ultimo) -> ultimo)
                    .orElse("desconhecido");

            mensagem = "Valor inválido para o campo '" + campo + "': " + causa.getValue()
                    + "." + valoresAceitos(causa.getTargetType());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo(mensagem));
    }

    /**
     * Parâmetro de rota ou de query com tipo incompatível, como
     * {@code /api/frota?status=QUALQUER_COISA}.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String mensagem = "Valor inválido para o parâmetro '" + ex.getName() + "': " + ex.getValue()
                + "." + valoresAceitos(ex.getRequiredType());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo(mensagem));
    }

    /**
     * Rede de segurança para violações de constraint que escapam da checagem
     * prévia da aplicação — notadamente duas requisições concorrentes tentando
     * gravar a mesma placa.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(corpo("A operação viola uma restrição de integridade dos dados. "
                        + "Verifique se a placa informada já está cadastrada."));
    }

    /** Corpo padrão de erro: a mesma mensagem em {@code erro} e em {@code message}. */
    private static Map<String, String> corpo(String mensagem) {
        return Map.of("erro", mensagem, "message", mensagem);
    }

    /** Complemento da mensagem listando os valores de um enum, quando aplicável. */
    private static String valoresAceitos(Class<?> tipo) {
        if (tipo == null || !tipo.isEnum()) {
            return "";
        }
        String valores = Arrays.stream(tipo.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        return " Valores aceitos: " + valores + ".";
    }
}
