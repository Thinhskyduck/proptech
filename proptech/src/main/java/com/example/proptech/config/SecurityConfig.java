package com.example.proptech.config;

import com.example.proptech.security.jwt.AuthEntryPointJwt;
import com.example.proptech.security.jwt.AuthTokenFilter;
import com.example.proptech.security.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Giữ lại nếu bạn muốn dùng @PreAuthorize trên các phương thức service/controller
public class SecurityConfig {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Vô hiệu hóa CSRF cho REST API
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler)) // Xử lý lỗi xác thực
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Không tạo session
                .authorizeHttpRequests(auth ->
                        auth
                                // Trong SecurityConfig.java, trong .authorizeHttpRequests(...)
                                .requestMatchers("/api/payment/vnpay/return").permitAll() // Cho phép VNPAY gọi về
                                .requestMatchers("/api/payment/vnpay/create-payment").hasRole("REALTOR") // Chỉ Realtor được tạo yêu cầu nạp tiền
                                // Các API xác thực luôn public
                                .requestMatchers("/api/auth/**").permitAll()

                                // Các API public để xem tin đăng (ví dụ)
                                .requestMatchers("/api/listings/**").permitAll() // Cho phép GET listings, chi tiết listing
                                .requestMatchers("/api/property-types/**").permitAll() // Nếu có API này

                                // Các đường dẫn public cho trang web Thymeleaf và tài nguyên tĩnh (nếu bạn vẫn dùng Thymeleaf)
                                .requestMatchers("/", "/error", "/favicon.ico").permitAll() // Trang chủ, trang lỗi, favicon
                                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll() // Tài nguyên tĩnh
                                .requestMatchers("/listings", "/listings/{id}").permitAll() // Ví dụ trang web xem tin đăng
                                .requestMatchers("/api/favorites/**").authenticated()
                                // API cho Realtor (cần token và vai trò REALTOR)
                                .requestMatchers(HttpMethod.POST, "/api/listings").hasRole("REALTOR")
                                .requestMatchers(HttpMethod.GET, "/api/listings/my-listings").hasRole("REALTOR")
                                .requestMatchers(HttpMethod.PUT, "/api/listings/{listingId}").hasRole("REALTOR")
                                .requestMatchers(HttpMethod.DELETE, "/api/listings/{listingId}").hasRole("REALTOR")
                                .requestMatchers(HttpMethod.POST, "/api/listings/{listingId}/images").hasRole("REALTOR")
                                .requestMatchers(HttpMethod.DELETE, "/api/listings/{listingId}/images/{imageId}").hasRole("REALTOR")
                                .requestMatchers(HttpMethod.PUT, "/api/listings/{listingId}/images/{imageId}/set-primary").hasRole("REALTOR")
                                .requestMatchers(HttpMethod.POST, "/api/appointments").hasAnyRole("CUSTOMER", "REALTOR")
                                .requestMatchers(HttpMethod.GET, "/api/appointments/my-appointments").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/api/appointments/{appointmentId}/confirm").hasRole("REALTOR")
                                .requestMatchers(HttpMethod.PUT, "/api/appointments/{appointmentId}/cancel").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/api/appointments/{appointmentId}/complete").hasRole("REALTOR")
                                // Tất cả các request khác đến /api/** (trừ những cái đã permitAll ở trên) cần xác thực
                                // Bạn có thể làm chặt hơn ở đây nếu cần, ví dụ: /api/admin/** yêu cầu ROLE_ADMIN
                                .requestMatchers("/api/**").authenticated() // Bất kỳ API nào khác đều cần token
                                // API cho Admin (cần token và vai trò ADMIN)
                                .requestMatchers("/api/admin/**").hasRole("ADMIN") // Rule chung cho tất cả API admin
                                // Bất kỳ request nào không phải API (ví dụ: các trang web khác của Thymeleaf) có thể có rule riêng
                                // Hoặc nếu không có, mặc định là authenticated theo dòng trên.
                                // Ví dụ:
                                // .requestMatchers("/user/profile").authenticated() // Trang profile của user trên web
                                // .requestMatchers("/admin/**").hasRole("ADMIN") // Các trang admin trên web

                                // Mặc định, nếu không khớp các rule trên, yêu cầu xác thực
                                .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        // Thêm JWT filter để xử lý token
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}