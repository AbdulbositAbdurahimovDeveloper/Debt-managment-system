package uz.qarzdorlar_ai.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uz.qarzdorlar_ai.payload.errors.ErrorDTO;

import java.io.Serializable;

/**
 * A generic and standardized response wrapper for all API endpoints.
 * It ensures that every response from the API has a consistent structure,
 * containing either a data payload (on success) or a detailed error object (on failure).
 *
 * @param <T> The type of the data being returned on success.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE) // Enforces use of static factory methods
@JsonInclude(JsonInclude.Include.NON_NULL) // The most important annotation here!
public class ResponseDTO<T> implements Serializable {

    /**
     * 'true' for a successful response, 'false' for an error.
     * The primary flag for the client to check.
     */
    private boolean success;

    /**
     * The actual data payload for successful responses.
     * This field will be null (and omitted from JSON) if there's an error.
     */
    private T data;

    /**
     * A detailed error object for failed responses, using your provided ErrorDTO.
     * This field will be null (and omitted from JSON) for successful responses.
     */
    private ErrorDTO error;

    // --- Static Factory Methods for clean and controlled object creation ---

    /**
     * Creates a successful response.
     *
     * @param data The data payload to be returned.
     * @return A successful ResponseDTO instance.
     */
    public static <T> ResponseDTO<T> success(T data) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.success = true;
        response.data = data;
        return response;
    }

    /**
     * Creates an error response.
     *
     * @param error The detailed ErrorDTO object.
     * @return An error ResponseDTO instance.
     */
    public static <T> ResponseDTO<T> error(ErrorDTO error) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.success = false;
        response.error = error;
        return response;
    }
}