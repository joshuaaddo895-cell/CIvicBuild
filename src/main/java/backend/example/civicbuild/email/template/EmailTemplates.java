package backend.example.civicbuild.email.template;

import backend.example.civicbuild.order.entity.Order;
import backend.example.civicbuild.order.entity.OrderItem;
import java.math.BigDecimal;
import org.springframework.web.util.HtmlUtils;

/**
 * Self-contained HTML email bodies with inline CSS and table-based layouts for broad client
 * compatibility. User-provided values are HTML-escaped to prevent injection into the markup.
 */
public final class EmailTemplates {

    public static final String WELCOME_SUBJECT = "Welcome to CivicBuild";
    public static final String PASSWORD_RESET_SUBJECT = "Reset your CivicBuild password";
    public static final String PASSWORD_CHANGED_SUBJECT = "Your CivicBuild password was changed";
    public static final String ACCOUNT_DELETED_SUBJECT = "Your CivicBuild account was deleted";
    public static final String PAYMENT_CONFIRMATION_SUBJECT = "Payment received — CivicBuild order confirmed";

    private static final String FONT_STACK =
            "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif";

    private EmailTemplates() {
    }

    public static String welcome(String fullName) {
        String safeName = HtmlUtils.htmlEscape(fullName);
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to CivicBuild</title>
                </head>
                <body style="margin:0; padding:0; background-color:#F3F4F6; font-family: %s;">

                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F3F4F6; padding:32px 16px;">
                  <tr>
                    <td align="center">

                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:480px; background-color:#FFFFFF; border-radius:16px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">

                        <!-- Header -->
                        <tr>
                          <td style="background-color:#4CAF50; padding:36px 32px; text-align:center;">
                            <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto;">
                              <tr>
                                <td style="background-color:#FFFFFF; width:40px; height:40px; border-radius:10px; text-align:center; vertical-align:middle;">
                                  <span style="font-size:22px; line-height:40px; color:#4CAF50; font-weight:800;">+</span>
                                </td>
                                <td style="padding-left:12px;">
                                  <span style="font-size:22px; font-weight:700; color:#FFFFFF; letter-spacing:-0.3px;">CivicBuild</span>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>

                        <!-- Body -->
                        <tr>
                          <td style="padding:40px 32px 32px 32px;">
                            <h1 style="margin:0 0 16px 0; font-size:24px; font-weight:700; color:#1F2937; line-height:1.3;">
                              Welcome to CivicBuild, %s! 👋
                            </h1>
                            <p style="margin:0 0 24px 0; font-size:15px; line-height:1.6; color:#4B5563;">
                              Your account has been created successfully. CivicBuild connects you with trusted suppliers, contractors, and delivery providers — all in one place, so you can build with confidence.
                            </p>

                            <!-- Confirmation line with checkmark -->
                            <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 8px 0; background-color:#F0FDF4; border-radius:10px; width:100%%;">
                              <tr>
                                <td style="padding:14px 16px;">
                                  <table role="presentation" cellpadding="0" cellspacing="0">
                                    <tr>
                                      <td style="width:24px; height:24px; background-color:#4CAF50; border-radius:50%%; text-align:center; vertical-align:middle;">
                                        <span style="color:#FFFFFF; font-size:14px; font-weight:700; line-height:24px;">✓</span>
                                      </td>
                                      <td style="padding-left:10px; font-size:14px; color:#166534; font-weight:600;">
                                        Account created — you're almost ready to go
                                      </td>
                                    </tr>
                                  </table>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>

                        <!-- Divider -->
                        <tr>
                          <td style="padding:8px 32px;">
                            <div style="height:1px; background-color:#E5E7EB;"></div>
                          </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                          <td style="padding:24px 32px 32px 32px; text-align:center;">
                            <p style="margin:0 0 6px 0; font-size:13px; color:#9CA3AF;">
                              — The CivicBuild Team
                            </p>
                            <p style="margin:0; font-size:12px; color:#B0B7C3;">
                              Need help? <a href="mailto:support@civicbuild.com" style="color:#4CAF50; text-decoration:none;">support@civicbuild.com</a>
                            </p>
                          </td>
                        </tr>

                      </table>

                    </td>
                  </tr>
                </table>

                </body>
                </html>
                """
                .formatted(FONT_STACK, safeName);
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

    public static String passwordChanged(String fullName) {
        String safeName = HtmlUtils.htmlEscape(fullName);
        String body = """
                <tr>
                  <td style="padding:40px 32px 32px 32px;">
                    <h1 style="margin:0 0 16px 0; font-size:24px; font-weight:700; color:#1F2937; line-height:1.3;">
                      Your password was changed
                    </h1>
                    <p style="margin:0 0 24px 0; font-size:15px; line-height:1.6; color:#4B5563;">
                      Hi %s, your CivicBuild password was updated successfully. For your security, all active sessions have been signed out.
                    </p>
                    %s
                    %s
                  </td>
                </tr>
                """
                .formatted(
                        safeName,
                        successBanner("Password updated - all sessions signed out"),
                        warningBanner("If you did not make this change, contact support immediately."));
        return brandedEmail("Your CivicBuild password was changed", "480px", body);
    }

    public static String accountDeleted(String fullName) {
        String safeName = HtmlUtils.htmlEscape(fullName);
        String body = """
                <tr>
                  <td style="padding:40px 32px 32px 32px;">
                    <h1 style="margin:0 0 16px 0; font-size:24px; font-weight:700; color:#1F2937; line-height:1.3;">
                      Your account has been deleted
                    </h1>
                    <p style="margin:0 0 24px 0; font-size:15px; line-height:1.6; color:#4B5563;">
                      Hi %s, your CivicBuild account and associated data have been permanently deleted as requested.
                    </p>
                    %s
                    %s
                  </td>
                </tr>
                """
                .formatted(
                        safeName,
                        successBanner("Account deletion completed"),
                        warningBanner("If you did not request this, contact support immediately."));
        return brandedEmail("Your CivicBuild account was deleted", "480px", body);
    }

    public static String paymentConfirmation(String fullName, Order order) {
        String safeName = HtmlUtils.htmlEscape(fullName);
        String safeReference = HtmlUtils.htmlEscape(order.getPaystackReference());
        String safeCurrency = HtmlUtils.htmlEscape(order.getCurrency());
        String safeAddress = HtmlUtils.htmlEscape(order.getDeliveryAddress());
        String safeCity = HtmlUtils.htmlEscape(order.getDeliveryCity());
        String safeRegion = HtmlUtils.htmlEscape(order.getDeliveryRegion());
        String safePhone = HtmlUtils.htmlEscape(order.getPhoneNumber());
        String itemRows = buildItemRows(order);
        String total = formatMoney(order.getTotal());

        String body = """
                <tr>
                  <td style="padding:40px 32px 16px 32px;">
                    <h1 style="margin:0 0 12px 0; font-size:24px; font-weight:700; color:#1F2937; line-height:1.3;">
                      Payment received
                    </h1>
                    <p style="margin:0 0 24px 0; font-size:15px; line-height:1.6; color:#4B5563;">
                      Thank you, %s. Your order is confirmed and we're preparing your materials.
                    </p>
                    <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 24px 0; background-color:#F0FDF4; border-radius:10px; width:100%%;">
                      <tr>
                        <td style="padding:18px 20px;">
                          <div style="font-size:12px; color:#166534; font-weight:700; text-transform:uppercase; letter-spacing:0.06em;">Amount paid</div>
                          <div style="font-size:30px; font-weight:700; color:#166534; margin-top:6px;">%s %s</div>
                          <div style="font-size:13px; color:#4B5563; margin-top:8px;">Reference: %s</div>
                        </td>
                      </tr>
                    </table>
                    <h2 style="margin:0 0 12px 0; font-size:18px; font-weight:700; color:#1F2937;">Order summary</h2>
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse; margin-bottom:24px;">
                      <tr style="background-color:#F9FAFB;">
                        <th align="left" style="padding:10px 12px; font-size:12px; color:#6B7280; text-transform:uppercase;">Item</th>
                        <th align="right" style="padding:10px 12px; font-size:12px; color:#6B7280; text-transform:uppercase;">Qty</th>
                        <th align="right" style="padding:10px 12px; font-size:12px; color:#6B7280; text-transform:uppercase;">Total</th>
                      </tr>
                      %s
                    </table>
                    <h2 style="margin:0 0 12px 0; font-size:18px; font-weight:700; color:#1F2937;">Delivery details</h2>
                    <table role="presentation" cellpadding="0" cellspacing="0" style="background-color:#F9FAFB; border-radius:10px; width:100%%;">
                      <tr>
                        <td style="padding:16px 18px; font-size:14px; line-height:1.7; color:#4B5563;">
                          <div><strong style="color:#1F2937;">Address:</strong> %s</div>
                          <div><strong style="color:#1F2937;">City:</strong> %s</div>
                          <div><strong style="color:#1F2937;">Region:</strong> %s</div>
                          <div><strong style="color:#1F2937;">Phone:</strong> %s</div>
                        </td>
                      </tr>
                    </table>
                    <p style="margin:24px 0 0 0; font-size:14px; line-height:1.6; color:#6B7280;">
                      You can track your order anytime in the CivicBuild app.
                    </p>
                  </td>
                </tr>
                """
                .formatted(
                        safeName,
                        safeCurrency,
                        total,
                        safeReference,
                        itemRows,
                        safeAddress,
                        safeCity,
                        safeRegion,
                        safePhone);
        return brandedEmail("Payment received — CivicBuild", "560px", body);
    }

    private static String brandedEmail(String pageTitle, String maxWidth, String innerRows) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                </head>
                <body style="margin:0; padding:0; background-color:#F3F4F6; font-family: %s;">

                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F3F4F6; padding:32px 16px;">
                  <tr>
                    <td align="center">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:%s; background-color:#FFFFFF; border-radius:16px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                        %s
                        %s
                        %s
                        %s
                      </table>
                    </td>
                  </tr>
                </table>

                </body>
                </html>
                """
                .formatted(pageTitle, FONT_STACK, maxWidth, brandHeaderRow(), innerRows, dividerRow(), footerRow());
    }

    private static String brandHeaderRow() {
        return """
                <tr>
                  <td style="background-color:#4CAF50; padding:36px 32px; text-align:center;">
                    <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto;">
                      <tr>
                        <td style="background-color:#FFFFFF; width:40px; height:40px; border-radius:10px; text-align:center; vertical-align:middle;">
                          <span style="font-size:22px; line-height:40px; color:#4CAF50; font-weight:800;">+</span>
                        </td>
                        <td style="padding-left:12px;">
                          <span style="font-size:22px; font-weight:700; color:#FFFFFF; letter-spacing:-0.3px;">CivicBuild</span>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
                """;
    }

    private static String dividerRow() {
        return """
                <tr>
                  <td style="padding:8px 32px;">
                    <div style="height:1px; background-color:#E5E7EB;"></div>
                  </td>
                </tr>
                """;
    }

    private static String footerRow() {
        return """
                <tr>
                  <td style="padding:24px 32px 32px 32px; text-align:center;">
                    <p style="margin:0 0 6px 0; font-size:13px; color:#9CA3AF;">
                      — The CivicBuild Team
                    </p>
                    <p style="margin:0; font-size:12px; color:#B0B7C3;">
                      Need help? <a href="mailto:support@civicbuild.com" style="color:#4CAF50; text-decoration:none;">support@civicbuild.com</a>
                    </p>
                  </td>
                </tr>
                """;
    }

    private static String successBanner(String message) {
        String safeMessage = HtmlUtils.htmlEscape(message);
        return """
                <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 16px 0; background-color:#F0FDF4; border-radius:10px; width:100%%;">
                  <tr>
                    <td style="padding:14px 16px;">
                      <table role="presentation" cellpadding="0" cellspacing="0">
                        <tr>
                          <td style="width:24px; height:24px; background-color:#4CAF50; border-radius:50%%; text-align:center; vertical-align:middle;">
                            <span style="color:#FFFFFF; font-size:14px; font-weight:700; line-height:24px;">✓</span>
                          </td>
                          <td style="padding-left:10px; font-size:14px; color:#166534; font-weight:600;">
                            %s
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
                """
                .formatted(safeMessage);
    }

    private static String warningBanner(String message) {
        String safeMessage = HtmlUtils.htmlEscape(message);
        return """
                <table role="presentation" cellpadding="0" cellspacing="0" style="background-color:#FFFBEB; border-radius:10px; width:100%%;">
                  <tr>
                    <td style="padding:14px 16px;">
                      <table role="presentation" cellpadding="0" cellspacing="0">
                        <tr>
                          <td style="width:24px; height:24px; background-color:#F59E0B; border-radius:50%%; text-align:center; vertical-align:middle;">
                            <span style="color:#FFFFFF; font-size:14px; font-weight:700; line-height:24px;">!</span>
                          </td>
                          <td style="padding-left:10px; font-size:14px; color:#92400E; font-weight:600;">
                            %s
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
                """
                .formatted(safeMessage);
    }

    private static String buildItemRows(Order order) {
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            rows.append("""
                    <tr>
                      <td style="padding:12px; border-bottom:1px solid #E5E7EB; color:#1F2937;">%s<br><span style="font-size:12px; color:#6B7280;">%s</span></td>
                      <td align="right" style="padding:12px; border-bottom:1px solid #E5E7EB; color:#1F2937;">%s %s</td>
                      <td align="right" style="padding:12px; border-bottom:1px solid #E5E7EB; color:#1F2937;">%s</td>
                    </tr>
                    """
                    .formatted(
                            HtmlUtils.htmlEscape(item.getProductName()),
                            HtmlUtils.htmlEscape(item.getSupplierName()),
                            formatQuantity(item.getQuantity()),
                            HtmlUtils.htmlEscape(item.getUnit()),
                            formatMoney(item.getLineTotal())));
        }
        return rows.toString();
    }

    private static String formatMoney(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatQuantity(BigDecimal quantity) {
        return quantity == null ? "0" : quantity.stripTrailingZeros().toPlainString();
    }
}
