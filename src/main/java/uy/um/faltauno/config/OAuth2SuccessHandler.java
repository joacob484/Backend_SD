// uy/um/faltauno/config/OAuth2SuccessHandler.java
package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import uy.um.faltauno.service.UsuarioService;
import uy.um.faltauno.entity.Usuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final UsuarioService usuarioService;
  private final JwtUtil jwtUtil;

  // Default a la URL de producción del frontend
  @Value("${FRONTEND_URL:https://faltauno-frontend-169771742214.us-central1.run.app}")
  private String frontend;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
    try {
      log.info("[OAuth2SuccessHandler] 🔐 OAuth success callback received");
      
      DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
      String email = principal.getAttribute("email");
      String name  = principal.getAttribute("name");
      
      log.info("[OAuth2SuccessHandler] 📧 Email: {}, Name: {}", email, name);

      if (email == null || email.isBlank()) {
        log.error("[OAuth2SuccessHandler] ❌ Email is null or blank from OAuth provider");
        String errorTarget = frontend + "/oauth/error?reason=no_email";
        response.sendRedirect(errorTarget);
        return;
      }

      Usuario u = usuarioService.upsertGoogleUser(email, name, principal.getAttributes());
      log.info("[OAuth2SuccessHandler] ✅ User upserted: ID={}, Email={}", u.getId(), u.getEmail());

      // Usa la firma que tengas en JwtUtil con tokenVersion (estándar industria)
      String jwt = jwtUtil.generateToken(u.getId(), email, u.getTokenVersion(), u.getRol());
      log.info("[OAuth2SuccessHandler] 🔑 JWT generated (length={})", jwt != null ? jwt.length() : 0);

      String target = frontend + "/oauth/success?token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
      log.info("[OAuth2SuccessHandler] ➡️ Redirecting to: {}", target);
      
      response.sendRedirect(target);
      
    } catch (Exception e) {
      log.error("[OAuth2SuccessHandler] ❌ Error during OAuth success handling", e);
      String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido";
      String errorTarget = frontend + "/oauth/error?reason=server_error&message=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
      response.sendRedirect(errorTarget);
    }
  }
}
