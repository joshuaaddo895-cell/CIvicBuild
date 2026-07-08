package backend.example.civicbuild.email.template;

import backend.example.civicbuild.order.entity.Order;
import backend.example.civicbuild.order.entity.OrderItem;
import java.math.BigDecimal;
import org.springframework.web.util.HtmlUtils;

/**
 * Simple, self-contained HTML email bodies. User-provided values are HTML-escaped to prevent
 * injection into the message markup. Kept intentionally minimal — richer templating can come later.
 */
public final class EmailTemplates {

    public static final String WELCOME_SUBJECT = "Welcome to CivicBuild";
    public static final String PASSWORD_RESET_SUBJECT = "Reset your CivicBuild password";
    public static final String PAYMENT_CONFIRMATION_SUBJECT = "Payment received — CivicBuild order confirmed";

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

        return """
                <div style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f6f8;padding:24px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 8px 24px rgba(15,23,42,0.08);">
                          <tr>
                            <td style="background:linear-gradient(135deg,#0f766e,#14b8a6);padding:28px 32px;color:#ffffff;">
                              <div style="font-size:13px;letter-spacing:0.08em;text-transform:uppercase;opacity:0.9;">CivicBuild</div>
                              <h1 style="margin:12px 0 0;font-size:28px;font-weight:700;">Payment Received</h1>
                              <p style="margin:8px 0 0;font-size:15px;opacity:0.95;">Thank you, %s. Your order is confirmed.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px 32px 8px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#ecfdf5;border:1px solid #a7f3d0;border-radius:10px;">
                                <tr>
                                  <td style="padding:18px 20px;">
                                    <div style="font-size:13px;color:#047857;font-weight:700;text-transform:uppercase;letter-spacing:0.06em;">Amount Paid</div>
                                    <div style="font-size:32px;font-weight:700;color:#065f46;margin-top:6px;">%s %s</div>
                                    <div style="font-size:13px;color:#047857;margin-top:8px;">Reference: %s</div>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 32px 8px;">
                              <h2 style="margin:0 0 12px;font-size:18px;color:#0f172a;">Order Summary</h2>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;">
                                <tr style="background:#f8fafc;">
                                  <th align="left" style="padding:10px 12px;font-size:12px;color:#64748b;text-transform:uppercase;">Item</th>
                                  <th align="right" style="padding:10px 12px;font-size:12px;color:#64748b;text-transform:uppercase;">Qty</th>
                                  <th align="right" style="padding:10px 12px;font-size:12px;color:#64748b;text-transform:uppercase;">Total</th>
                                </tr>
                                %s
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:8px 32px 24px;">
                              <h2 style="margin:0 0 12px;font-size:18px;color:#0f172a;">Delivery Details</h2>
                              <div style="background:#f8fafc;border-radius:10px;padding:16px 18px;color:#334155;font-size:14px;line-height:1.6;">
                                <div><strong>Address:</strong> %s</div>
                                <div><strong>City:</strong> %s</div>
                                <div><strong>Region:</strong> %s</div>
                                <div><strong>Phone:</strong> %s</div>
                              </div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 32px 28px;color:#64748b;font-size:13px;line-height:1.6;">
                              We are preparing your materials order. You can track your order anytime in the CivicBuild app.
                              <br><br>
                              — The CivicBuild Team
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </div>
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
    }

    private static String buildItemRows(Order order) {
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            rows.append("""
                    <tr>
                      <td style="padding:12px;border-bottom:1px solid #e2e8f0;color:#0f172a;">%s<br><span style="font-size:12px;color:#64748b;">%s</span></td>
                      <td align="right" style="padding:12px;border-bottom:1px solid #e2e8f0;color:#0f172a;">%s %s</td>
                      <td align="right" style="padding:12px;border-bottom:1px solid #e2e8f0;color:#0f172a;">%s</td>
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
