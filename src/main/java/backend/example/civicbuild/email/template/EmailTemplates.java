package backend.example.civicbuild.email.template;

import org.springframework.web.util.HtmlUtils;

/**
 * Simple, self-contained HTML email bodies. User-provided values are HTML-escaped to prevent
 * injection into the message markup. Kept intentionally minimal — richer templating can come later.
 */
public final class EmailTemplates {

    public static final String WELCOME_SUBJECT = "Welcome to CivicBuild";
    public static final String PASSWORD_RESET_SUBJECT = "Reset your CivicBuild password";

    private EmailTemplates() {
    }

    public static String welcome(String fullName) {
        String safeName = HtmlUtils.htmlEscape(fullName);
        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.5;">
                  <h2>Welcome to CivicBuild, %s!</h2>
                  <p>Your account has been created successfully.</p>
                  <p>You can now sign in and complete your onboarding.</p>
                  <p>— The CivicBuild Team</p>
                </div>
                """.formatted(safeName);
    }

    public static String passwordReset(String fullName, String resetLink) {
        String safeName = HtmlUtils.htmlEscape(fullName);
        String safeLink = HtmlUtils.htmlEscape(resetLink);
        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.5;">
                  <h2>Password reset requested</h2>
                  <p>Hi %s,</p>
                  <p>We received a request to reset your CivicBuild password. This link expires shortly:</p>
                  <p><a href="%s">Reset your password</a></p>
                  <p>If you did not request this, you can safely ignore this email.</p>
                  <p>— The CivicBuild Team</p>
                </div>
                """.formatted(safeName, safeLink);
    }
}
