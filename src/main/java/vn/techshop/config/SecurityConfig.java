package vn.techshop.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.techshop.identity.TechPrincipal;

@Configuration
public class SecurityConfig {
    @Bean org.springframework.security.core.session.SessionRegistry sessionRegistry(){return new org.springframework.security.core.session.SessionRegistryImpl();}
    @Bean org.springframework.security.web.session.HttpSessionEventPublisher sessionEvents(){return new org.springframework.security.web.session.HttpSessionEventPublisher();}
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain security(HttpSecurity http, Clock clock,org.springframework.security.core.session.SessionRegistry sessions,ClientIpResolver clientIps,
        org.springframework.beans.factory.ObjectProvider<org.springframework.security.oauth2.client.registration.ClientRegistrationRepository> google,vn.techshop.identity.GoogleAccounts accounts,org.springframework.beans.factory.ObjectProvider<HttpServletRequest> requests,vn.techshop.cart.CartService cartService) throws Exception {
        var attempts=new LoginAttempts(clock);
        var throttle=new OncePerRequestFilter() {
            protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
                if ("POST".equals(request.getMethod()) && "/login".equals(request.getServletPath()) && attempts.blocked(clientIps.resolve(request),request.getParameter("username"))) {
                    response.setHeader("Retry-After","900"); failure(request,response,429,"TOO_MANY_ATTEMPTS"); return;
                }
                chain.doFilter(request,response);
            }
        };
        http.sessionManagement(session->session.maximumSessions(-1).sessionRegistry(sessions).expiredSessionStrategy(event->{
            var request=event.getRequest();var response=event.getResponse();
            if(request.getRequestURI().startsWith("/api/"))failure(request,response,401,"SESSION_EXPIRED");
            else {response.setStatus(303);response.setHeader("Location","/login?expired");}
        }));
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/assets/**","/css/**","/js/**","/error","/api/v1/csrf").permitAll()
            .requestMatchers("/guest/**","/api/v1/guest/**","/news","/news/**","/newsletter","/newsletter/**","/api/v1/newsletter","/api/v1/newsletter/**","/forgot-password","/reset-password","/api/v1/auth/forgot-password","/api/v1/auth/reset-password","/oauth2/authorization/google","/login/oauth2/code/google").permitAll()
            .requestMatchers("/register","/api/v1/auth/register").anonymous()
            .requestMatchers("/admin/**","/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers("/chat","/api/v1/chat/**").hasRole("CUSTOMER")
            .requestMatchers("/api/v1/reviews/**").hasAnyRole("CUSTOMER","ADMIN")
            .requestMatchers("/cart/**","/checkout/**","/orders/**","/api/v1/cart/**","/api/v1/orders/**","/api/v1/coupons/**","/vouchers").hasRole("CUSTOMER")
            .requestMatchers("/profile","/addresses","/wishlist","/api/v1/me","/api/v1/me/**","/member","/trade-in","/payment/mock-pay/**").hasRole("CUSTOMER")
            .requestMatchers("/","/products/**","/login","/order-lookup","/payment/**","/api/v1/payment/**","/api/v1/products/**","/api/v1/categories","/branches","/compare","/warranty","/api/v1/branches","/api/v1/variants/*/stores","/api/v1/trade-in/devices","/api/v1/trade-in/criteria","/api/v1/installment/**","/api/v1/warranty/lookup").permitAll()
            .anyRequest().denyAll())
            .formLogin(form -> form.loginPage("/login")
                .successHandler((request,response,auth) -> {
                    attempts.success(clientIps.resolve(request),request.getParameter("username"));
                    var principal = (TechPrincipal) auth.getPrincipal();
                    var session = request.getSession(false);
                    if (session != null) {
                        Long guestOwner = (Long) session.getAttribute("guestOwner");
                        if (guestOwner != null && "CUSTOMER".equals(principal.role())) {
                            cartService.mergeGuestCart(guestOwner, principal.id());
                            session.removeAttribute("guestOwner");
                        }
                    }
                    response.setStatus(303);
                    response.setHeader("Location", resolveSafeRedirect(request, principal));
                })
                .failureHandler((request,response,ex) -> {
                    attempts.failed(clientIps.resolve(request),request.getParameter("username"));
                    response.setStatus(303);
                    String redirect = request.getParameter("redirect");
                    String suffix = (redirect != null && !redirect.isBlank()) ? "&redirect=" + java.net.URLEncoder.encode(redirect, java.nio.charset.StandardCharsets.UTF_8) : "";
                    response.setHeader("Location","/login?error" + suffix);
                }))
            .logout(logout -> logout.logoutSuccessHandler((request,response,auth) -> { response.setStatus(303);response.setHeader("Location","/login?logout"); }))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request,response,ex) -> { if(request.getRequestURI().startsWith("/api/")) failure(request,response,401,"AUTHENTICATION_REQUIRED"); else {response.setStatus(303);response.setHeader("Location","/login");} })
                .accessDeniedHandler((request,response,ex) -> failure(request,response,403,ex instanceof org.springframework.security.web.csrf.CsrfException?"CSRF_INVALID":"ACCESS_DENIED")))
            .addFilterBefore(throttle,UsernamePasswordAuthenticationFilter.class);
        if(google.getIfAvailable()!=null)http.oauth2Login(oauth->oauth.loginPage("/login").userInfoEndpoint(info->info.oidcUserService(input->{
            var claims=new org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService().loadUser(input);
            var session=requests.getObject().getSession(false);Long owner=null;
            if(session!=null){var until=(Long)session.getAttribute("googleLinkUntil");if(until!=null&&until>=clock.millis())owner=(Long)session.getAttribute("googleLinkUser");session.removeAttribute("googleLinkUser");session.removeAttribute("googleLinkUntil");}
            return accounts.resolve(claims,owner);
        })).successHandler((request,response,authentication)->{
            var principal = (TechPrincipal) authentication.getPrincipal();
            var session = request.getSession(false);
            if (session != null) {
                Long guestOwner = (Long) session.getAttribute("guestOwner");
                if (guestOwner != null && "CUSTOMER".equals(principal.role())) {
                    cartService.mergeGuestCart(guestOwner, principal.id());
                    session.removeAttribute("guestOwner");
                }
            }
            String target = resolveSafeRedirect(request, principal);
            response.setStatus(303);
            response.setHeader("Location", target.equals("/products") ? "/profile" : target);
        })
          .failureHandler((request,response,error)->{response.setStatus(303);response.setHeader("Location","/login?googleError");}));
        return http.build();
    }
    private static String resolveSafeRedirect(HttpServletRequest request, TechPrincipal principal) {
        if ("ADMIN".equals(principal.role())) return "/admin";
        String redirect = request.getParameter("redirect");
        if (redirect != null && !redirect.isBlank()) {
            redirect = redirect.trim();
            if (redirect.startsWith("/") && !redirect.startsWith("//") && !redirect.startsWith("/\\") && !redirect.contains("://")) {
                if (redirect.equals("/guest/cart") || redirect.startsWith("/guest/cart")) return "/cart";
                if (redirect.equals("/guest/checkout") || redirect.startsWith("/guest/checkout")) return "/checkout";
                return redirect;
            }
        }
        var savedRequest = new org.springframework.security.web.savedrequest.HttpSessionRequestCache().getRequest(request, null);
        if (savedRequest != null) {
            String url = savedRequest.getRedirectUrl();
            if (url != null) {
                try {
                    var uri = java.net.URI.create(url);
                    String path = uri.getPath();
                    if (path != null && path.startsWith("/") && !path.startsWith("//") && !path.startsWith("/\\")) {
                        if (path.equals("/guest/cart") || path.startsWith("/guest/cart")) return "/cart";
                        if (path.equals("/guest/checkout") || path.startsWith("/guest/checkout")) return "/checkout";
                        String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";
                        return path + query;
                    }
                } catch (Exception ignored) {}
            }
        }
        return "/products";
    }
    private static void failure(HttpServletRequest request,HttpServletResponse response,int status,String code) throws IOException {
        response.setStatus(status); response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\""+code+"\",\"message\":\"Yêu cầu không được chấp nhận.\",\"requestId\":\""+request.getAttribute("requestId")+"\",\"fieldErrors\":[]}");
    }
}
