package uz.qarzdorlar_ai.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Set;

/**
 * An extended version of the Token DTO that includes additional user information.
 * This is useful for front-end applications that need immediate access to user
 * details like roles or name upon login, reducing the need for an extra API call.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenDTO {

    @Builder.Default
    private String tokenType = "Bearer";

    @JsonProperty("access_token") // Using snake_case for JSON, a common convention for APIs.
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * The duration in seconds for which the access token is valid.
     * This allows the client to proactively manage token refresh cycles.
     */
    @JsonProperty("expires_in")
    private Long expiresIn; // Example: 3600 (for 1 hour)

    /**
     * A set of user roles or authorities.
     * This allows the front-end to show/hide UI elements based on user permissions
     * without making a separate request.
     */
    private Set<String> authorities;

    /**
     * User's display name or username, for immediate use in the UI.
     */
    private String username;
}