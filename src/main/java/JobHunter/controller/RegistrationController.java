package JobHunter.controller;

import JobHunter.domain.User;
import JobHunter.domain.dto.CaptchaResponseDto;
import JobHunter.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

@Controller
public class RegistrationController {

	private final static String CAPTCHA_URL = "https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s";
	private final UserService userService;
	private final RestTemplate restTemplate;

	@Value("${recaptcha.secret}")
	private String secret;

	@Autowired
	public RegistrationController(UserService userService, RestTemplate restTemplate) {
		this.userService = userService;
		this.restTemplate = restTemplate;
	}

	@GetMapping("/registration")
	public String registration() {
		return "registration";
	}

	@PostMapping("/registration")
	@Transactional
	public String addUser(
			@RequestParam("password2") String passwordConfirm,
			@RequestParam("g-recaptcha-response") String captchaResponse,
			@Valid User user,
			BindingResult bindingResult,
			Model model
	) {
		String url = String.format(CAPTCHA_URL, secret, captchaResponse);
		CaptchaResponseDto response = restTemplate.postForObject(url, Collections.emptyList(), CaptchaResponseDto.class);

		if (!response.isSuccess()) {
			model.addAttribute("captchaError", "Fill captcha");
		}

		boolean isConfirmEmpty = StringUtils.isEmpty(passwordConfirm);
		boolean isPasswordDifferent = user.getPassword() != null && !user.getPassword().equals(passwordConfirm);

		if (isConfirmEmpty)
			model.addAttribute("password2Error", "Password confirmation can't be empty");

		if (isPasswordDifferent) {
			model.addAttribute("passwordError", "Passwords are different!");
		}

		if (isConfirmEmpty || isPasswordDifferent || bindingResult.hasErrors() || !response.isSuccess()) {
			Map<String, String> errors = ControllerUtils.getErrors(bindingResult);

			model.mergeAttributes(errors);

			return "registration";
		}
		if (!userService.addUser(user)) {
			model.addAttribute("usernameError", "This username is already taken!");
			return "registration";
		}

		return "redirect:/login";
	}

	@GetMapping("/activate/{code}")
	public String activate(Model model, @PathVariable String code) {
		boolean isActivated = userService.activateUser(code);

		if (isActivated) {
			model.addAttribute("messageType", "success");
			model.addAttribute("message", "User successfully activated");
		} else {
			model.addAttribute("messageType", "danger");
			model.addAttribute("message", "Activation code is not found!");
		}

		return "login";
	}
}