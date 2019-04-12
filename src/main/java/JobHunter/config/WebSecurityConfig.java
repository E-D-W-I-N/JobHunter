package JobHunter.config;

import JobHunter.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	private final UserService userService;
	private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
	private final PasswordEncoder passwordEncoder;

	@Autowired
	public WebSecurityConfig(UserService userService,
							 CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
							 PasswordEncoder passwordEncoder) {
		this.userService = userService;
		this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
		this.passwordEncoder = passwordEncoder;
	}

	@Bean
	public static PasswordEncoder getPasswordEncoder() {
		return new BCryptPasswordEncoder(8);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
				.authorizeRequests()
				.antMatchers("/", "/registration", "/activate/*", "/static/**").permitAll()
				.anyRequest().authenticated()
				.and()
				.formLogin()
				.loginPage("/login")
				.successHandler(customAuthenticationSuccessHandler)
				.permitAll()
				.and()
				.logout()
				.permitAll();
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userService)
				.passwordEncoder(passwordEncoder);
	}
}