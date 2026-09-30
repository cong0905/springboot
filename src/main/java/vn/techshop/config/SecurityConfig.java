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
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain security(HttpSecurity http, Clock clock) throws Exception {
        var attempts=new LoginAttempts(clock);
        var throttle=new OncePerRequestFilter() {
            protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
                if ("POST".equals(request.getMethod()) && "/login".equals(request.getServletPath()) && attempts.blocked(attempts.key(request.getRemoteAddr(),request.getParameter("username")))) {
                    response.setHeader("Retry-After","900"); failure(request,response,429,"TOO_MANY_ATTEMPTS"); return;
                }
                chain.doFilter(request,response);
            }
        };
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/assets/**","/css/**","/js/**","/error","/api/v1/csrf").permitAll()
            .requestMatchers("/register","/api/v1/auth/register").anonymous()
            .requestMatchers("/admin/**","/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers("/cart/**","/checkout/**","/orders/**","/api/v1/cart/**","/api/v1/orders/**").hasRole("CUSTOMER")
            .requestMatchers("/api/v1/me").authenticated()
            .requestMatchers("/","/products/**","/login","/api/v1/products/**","/api/v1/categories").permitAll()
            .anyRequest().denyAll())
            .formLogin(form -> form.loginPage("/login")
                .successHandler((request,response,auth) -> { attempts.success(attempts.key(request.getRemoteAddr(),request.getParameter("username"))); response.setStatus(303); response.setHeader("Location", "ADMIN".equals(((TechPrincipal)auth.getPrincipal()).role())?"/admin":"/products"); })
                .failureHandler((request,response,ex) -> { attempts.failed(attempts.key(request.getRemoteAddr(),request.getParameter("username"))); response.setStatus(303); response.setHeader("Location","/login?error"); }))
            .logout(logout -> logout.logoutSuccessHandler((request,response,auth) -> { response.setStatus(303);response.setHeader("Location","/login?logout"); }))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request,response,ex) -> { if(request.getRequestURI().startsWith("/api/")) failure(request,response,401,"AUTHENTICATION_REQUIRED"); else {response.setStatus(303);response.setHeader("Location","/login");} })
                .accessDeniedHandler((request,response,ex) -> failure(request,response,403,ex instanceof org.springframework.security.web.csrf.CsrfException?"CSRF_INVALID":"ACCESS_DENIED")))
            .addFilterBefore(throttle,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    private static void failure(HttpServletRequest request,HttpServletResponse response,int status,String code) throws IOException {
        response.setStatus(status); response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\""+code+"\",\"message\":\"Yêu cầu không được chấp nhận.\",\"requestId\":\""+request.getAttribute("requestId")+"\",\"fieldErrors\":[]}");
    }
}
