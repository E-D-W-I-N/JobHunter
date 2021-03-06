package JobHunter.controller;

import JobHunter.domain.*;
import JobHunter.service.ApplicationService;
import JobHunter.service.DepartmentService;
import JobHunter.service.InterviewService;
import JobHunter.util.PDFGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/interviews")
public class InterviewController {

	private final InterviewService interviewService;
	private final ApplicationService applicationService;
	private final DepartmentService departmentService;

	@Autowired
	public InterviewController(InterviewService interviewService, ApplicationService applicationService, DepartmentService departmentService) {
		this.interviewService = interviewService;
		this.applicationService = applicationService;
		this.departmentService = departmentService;
	}

	@GetMapping
	public String interviews(@AuthenticationPrincipal User user, Model model) {

		if (user.getRoles().contains(Role.ADMIN)) {
			List<Interview> interviews = interviewService.findAllInterviews();
			model.addAttribute("interviews", interviews);
			return "interviews";
		} else if (user.getRoles().contains(Role.HEADHUNTER)) {
			Department department = departmentService.findDepartmentByUser(user);

			List<Interview> interviews = interviewService.findInterviewsByDepartment(department);
			model.addAttribute("interviews", interviews);
			return "interviews";
		}

		Iterable<Interview> interviews = interviewService.findInterviewsByCandidate(user);
		model.addAttribute("interviews", interviews);
		return "interviews";
	}

	@GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<InputStreamResource> interviewsReport(@AuthenticationPrincipal User user) {
		List<Interview> interviews = new ArrayList<>();
		if (user.getRoles().contains(Role.ADMIN)) {
			interviews = interviewService.findAllInterviews();
		} else if (user.getRoles().contains(Role.HEADHUNTER)) {
			Department department = departmentService.findDepartmentByUser(user);
			interviews = interviewService.findInterviewsByDepartment(department);
		} else
			interviews = interviewService.findInterviewsByCandidate(user);

		String title = "Собеседования";
		List<String> tableHeader = Arrays.asList("ID", "Вакансия", "Отдел", "Кандидат", "Работник", "Дата", "Время");
		List content = new ArrayList();

		for (Interview interview : interviews) {
			content.add(Arrays.asList(interview.getId().toString(), interview.getApplication().getVacancy().getVacancyName(),
					interview.getDepartment().getDepartmentName(), interview.getApplication().getUser().getUsername(),
					interview.getHeadHunter().getUsername(), interview.getDateTime().toLocalDate().toString(),
					interview.getDateTime().toLocalTime().toString().substring(0, 8)));
		}


		ByteArrayInputStream bis = PDFGenerator.customerPDFReport(title, tableHeader, content);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition", "inline; filename=interviews.pdf");

		return ResponseEntity
				.ok()
				.headers(headers)
				.contentType(MediaType.APPLICATION_PDF)
				.body(new InputStreamResource(bis));
	}

	@PostMapping("/add")
	@PreAuthorize("hasAnyAuthority('ADMIN', 'HEADHUNTER')")
	public String addInterview(@AuthenticationPrincipal User user,
							   @RequestParam("applicationId") Application application,
							   @RequestParam String stringDateTime,
							   Model model) {

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		LocalDateTime dateTime = LocalDateTime.parse(stringDateTime, dateTimeFormatter);
		if (!interviewService.addInterview(application, user, dateTime)) {
			model.addAttribute("messageType", "danger");
			model.addAttribute("message", "Вы уже назначали собеседование по этому заявлению");

			List<Application> applicationList = applicationService.findAllApplications();
			model.addAttribute("applications", applicationList);

			return "applications";
		}

		return "redirect:/interviews";
	}

	@PostMapping("/result/add")
	@PreAuthorize("hasAnyAuthority('ADMIN', 'HEADHUNTER')")
	public String addResult(@RequestParam("interviewId") Interview interview,
							@RequestParam(defaultValue = "false") Boolean isSuccess,
							@RequestParam String result,
							Model model) {

		if (!interviewService.addResult(interview, isSuccess, result)) {
			model.addAttribute("message", "Результат собеседования не может быть пустым");
			return "interviews";
		}

		return "redirect:/interviews";
	}
}
