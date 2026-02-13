package Portfolio.Checkout_api_sandbox.dto.response;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de error estandarizadas.
 * Se usa en el GlobalExceptionHandler para devolver errores consistentes.
 */
public class ErrorResponse {

    private String message;
    private int status;
    private LocalDateTime timestamp;
    private String path;

    // Constructores
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String message, int status, String path) {
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }

    // Getters y Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

