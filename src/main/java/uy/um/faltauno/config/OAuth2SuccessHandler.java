// uy/um/faltauno/config/OAuth2SuccessHandler.java
package uy.um.faltauno.config;

import lombok.RequiredArgsConstructor;
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
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final UsuarioService usuarioService;
  private final JwtUtil jwtUtil;

  // Default a la URL de producción del frontend
  @Value("${FRONTEND_URL:https://faltauno-frontend-169771742214.us-central1.run.app}")
  private String frontend;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
    DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
    String email = principal.getAttribute("email");
    String name  = principal.getAttribute("name");

    Usuario u = usuarioService.upsertGoogleUser(email, name, principal.getAttributes());

    // Usa la firma que tengas en JwtUtil con tokenVersion (estándar industria)
    String jwt = jwtUtil.generateToken(u.getId(), email, u.getTokenVersion());

    String target = frontend + "/oauth/success?token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
    response.sendRedirect(target);
  }
}
