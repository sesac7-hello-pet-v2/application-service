package hello.pet.applicationservice.exception;

import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNotFound(EntityNotFoundException e) {
        return generateExceptionResponse(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleBadRequest(IllegalArgumentException e) {
        return generateExceptionResponse(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> methodArgumentNotValidExceptionHandler(
            MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getFieldErrors().forEach((error) -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(DuplicateApplicationException.class)
    public ResponseEntity<ExceptionResponse> handleDuplicateApplication(DuplicateApplicationException e) {
        return generateExceptionResponse(e, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ApplicationAlreadyApprovedException.class)
    public ResponseEntity<ExceptionResponse> handleApplicationAlreadyApproved(ApplicationAlreadyApprovedException e) {
        return generateExceptionResponse(e, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AlreadyProcessedApplicationException.class)
    public ResponseEntity<ExceptionResponse> handleAlreadyProcessedApplication(AlreadyProcessedApplicationException e) {
        return generateExceptionResponse(e, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AnnouncementAlreadyCompletedException.class)
    public ResponseEntity<ExceptionResponse> handleAnnouncementAlreadyCompleted(
            AnnouncementAlreadyCompletedException e) {
        return generateExceptionResponse(e, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ExceptionResponse> handleForbidden(ForbiddenOperationException e) {
        return generateExceptionResponse(e, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGeneralException(Exception e) {
        return generateExceptionResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ExceptionResponse> generateExceptionResponse(Exception e, HttpStatus status) {
        ExceptionResponse response = ExceptionResponse.of(e, status.value(), status.name());
        return ResponseEntity.status(status).body(response);
    }
}
