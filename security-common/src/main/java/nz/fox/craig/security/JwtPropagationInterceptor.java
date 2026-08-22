package nz.fox.craig.security;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class JwtPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        System.out.println(
            "JWT propagation authentication: "
                    + (authentication == null
                            ? "null"
                            : authentication.getClass().getSimpleName()));

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            Object credentials = authentication.getCredentials();
            System.out.println(
                "JWT propagation credentials are String: "
                        + (credentials instanceof String));
    

            if (credentials instanceof String jwt) {
                request.getHeaders().setBearerAuth(jwt);
            }
        }

        return execution.execute(request, body);
    }
}
