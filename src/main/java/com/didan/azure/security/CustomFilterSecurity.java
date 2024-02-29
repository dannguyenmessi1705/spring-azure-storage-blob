package com.didan.azure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration // Đánh dấu đây là class cấu hình
@EnableWebSecurity // Bật tính năng bảo mật trên Web, khi truy cập vào các route sẽ phải đi vào bộ
// lọc ở đây
public class CustomFilterSecurity {
    private static Logger logger = LoggerFactory.getLogger(CustomFilterSecurity.class);
    private final JwtAuthenticationFilter jwtAuthenticationFilter; // Khai báo biến customUserDetailsService để tiêm
    // vào đây
    @Autowired // Tiêm JwtAuthenticationFilter vào đây (tự động tìm kiếm và tiêm)
    public CustomFilterSecurity(JwtAuthenticationFilter jwtAuthenticationFilter) { // Khởi tạo đối tượng
        // CustomFilterSecurity
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    // Quy định các rules
    @Bean // Đánh dấu đây là Bean, ghi đè lên Bean mặc định của Spring Security
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { // Cấu hình bộ lọc
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.cors(cors -> cors.configure(http));
        http.csrf(csrf -> csrf.disable()); // Tắt csrf
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // Tắt Session
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/auth/**", "/api-docs**/**", "/swagger-ui/**", "/files/**")
                .permitAll()
                .anyRequest()
                .authenticated()
        ); // Cấu hình các route nào được bảo mật, không bảo mật
        http.exceptionHandling(except -> except
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    String jsonMessage = "{\n\t\"statusCode\": 404\n\t\"success\": \"false\"\n\t\"description\": \"UNAUTHORIZED OR THE ROUTE IS NOT FOUND\"\n}";
                    response.getWriter().write(jsonMessage);
                    logger.error("UNAUTHORIZED OR THE ROUTE IS NOT FOUND");
                })
        ); // Bắt lỗi nếu không authorized được thì trả về message
        return http.build(); // Trả về bộ lọc
    }

    // Định nghĩa lại Bean để tạo BcryptPasswordEncoder (Mã hóa mật khẩu)
    @Bean
    public PasswordEncoder passwordEncoder() { // Đánh dấu đây là Bean, ghi đè lên Bean mặc định của Spring Security
        return new BCryptPasswordEncoder(); // Trả về đối tượng BCryptPasswordEncoder
    }

}
