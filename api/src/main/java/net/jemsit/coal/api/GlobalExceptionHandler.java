package net.jemsit.coal.api;

import net.jemsit.coal.shared.api.ApiException;
import net.jemsit.coal.shared.api.ErrorDto;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorDto> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.status())
                .body(new ErrorDto(Instant.now(), ex.status(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorDto> handleValidation(MethodArgumentNotValidException ex) {
        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .collect(Collectors.joining(", "));
        return badRequest("Проверьте заполнение полей: " + fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorDto> handleUnreadable(HttpMessageNotReadableException ex) {
        return badRequest("Некорректное тело запроса");
    }

    private ResponseEntity<ErrorDto> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ErrorDto(Instant.now(), 400, message));
    }
}
