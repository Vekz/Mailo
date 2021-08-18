package ap.mailo.auth;

import androidx.annotation.Nullable;

/**
 * Authentication result : success (user details) or error message.
 */
public class LoginResult {
    @Nullable
    private LoggedInUser success;
    @Nullable
    private Integer error;

    LoginResult(@Nullable Integer error) {
        this.error = error;
    }

    LoginResult(@Nullable LoggedInUser success) {
        this.success = success;
    }

    @Nullable
    public LoggedInUser getSuccess() {
        return success;
    }

    @Nullable
    Integer getError() {
        return error;
    }
}
