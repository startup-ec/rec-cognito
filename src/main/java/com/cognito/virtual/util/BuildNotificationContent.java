package com.cognito.virtual.util;

import org.springframework.stereotype.Component;

@Component
public class BuildNotificationContent {
    public String buildConfirmationEmailContent(String confirmationCode) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #f8f9fa; padding: 20px; text-align: center;">
                        <h1 style="color: #333; margin-bottom: 20px;">¡Bienvenido a Cognito Virtual!</h1>
                    </div>
                    <div style="padding: 30px; background-color: white;">
                        <p style="color: #666; font-size: 16px; line-height: 1.6;">
                            Gracias por registrarte en nuestra plataforma. Para completar tu registro, 
                            necesitas confirmar tu dirección de email.
                        </p>
                        <div style="background-color: #e9ecef; padding: 20px; margin: 20px 0; border-radius: 5px; text-align: center;">
                            <p style="color: #333; margin-bottom: 10px; font-weight: bold;">Tu código de confirmación es:</p>
                            <p style="font-size: 24px; color: #007bff; font-weight: bold; letter-spacing: 3px; margin: 0;">
                                %s
                            </p>
                        </div>
                        <p style="color: #666; font-size: 14px;">
                            Este código expira en 24 horas. Si no solicitaste este registro, 
                            puedes ignorar este mensaje.
                        </p>
                        <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6;">
                            <p style="color: #999; font-size: 12px;">
                                Saludos cordiales,<br>
                                Equipo de Cognito Virtual
                            </p>
                        </div>
                    </div>
                </div>
                """.formatted(confirmationCode);
    }

    public String buildPasswordResetEmailContent(String resetCode) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #fff3cd; padding: 20px; text-align: center; border-left: 4px solid #ffc107;">
                        <h1 style="color: #856404; margin-bottom: 10px;">Recuperación de Contraseña</h1>
                        <p style="color: #856404; margin: 0;">Cognito Virtual</p>
                    </div>
                    <div style="padding: 30px; background-color: white;">
                        <p style="color: #666; font-size: 16px; line-height: 1.6;">
                            Hemos recibido una solicitud para restablecer la contraseña de tu cuenta. 
                            Utiliza el siguiente código para crear una nueva contraseña.
                        </p>
                        <div style="background-color: #e9ecef; padding: 20px; margin: 20px 0; border-radius: 5px; text-align: center;">
                            <p style="color: #333; margin-bottom: 10px; font-weight: bold;">Tu código de recuperación es:</p>
                            <p style="font-size: 24px; color: #dc3545; font-weight: bold; letter-spacing: 3px; margin: 0;">
                                %s
                            </p>
                        </div>
                        <p style="color: #dc3545; font-size: 14px; font-weight: bold;">
                            ⚠️ Este código expira en 15 minutos por seguridad.
                        </p>
                        <p style="color: #666; font-size: 14px;">
                            Si no solicitaste este cambio, ignora este mensaje y tu contraseña permanecerá sin cambios.
                        </p>
                        <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6;">
                            <p style="color: #999; font-size: 12px;">
                                Saludos cordiales,<br>
                                Equipo de Cognito Virtual
                            </p>
                        </div>
                    </div>
                </div>
                """.formatted(resetCode);
    }

    public String buildPasswordChangeNotificationContent() {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #d4edda; padding: 20px; text-align: center; border-left: 4px solid #28a745;">
                        <h1 style="color: #155724; margin-bottom: 10px;">Contraseña Actualizada</h1>
                        <p style="color: #155724; margin: 0;">Cognito Virtual</p>
                    </div>
                    <div style="padding: 30px; background-color: white;">
                        <p style="color: #666; font-size: 16px; line-height: 1.6;">
                            Te informamos que la contraseña de tu cuenta ha sido cambiada exitosamente.
                        </p>
                        <div style="background-color: #e9ecef; padding: 20px; margin: 20px 0; border-radius: 5px;">
                            <p style="color: #333; margin: 0; font-weight: bold;">
                                ✅ Cambio realizado el: %s
                            </p>
                        </div>
                        <p style="color: #dc3545; font-size: 14px; font-weight: bold;">
                            Si no realizaste este cambio, contacta inmediatamente con nuestro soporte técnico.
                        </p>
                        <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6;">
                            <p style="color: #999; font-size: 12px;">
                                Saludos cordiales,<br>
                                Equipo de Cognito Virtual
                            </p>
                        </div>
                    </div>
                </div>
                """.formatted(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm")
        ));
    }
}
