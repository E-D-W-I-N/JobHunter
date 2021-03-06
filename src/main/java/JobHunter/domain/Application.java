package JobHunter.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table
@Getter
@Setter
public class Application {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn
	private Vacancy vacancy;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn
	private Department department;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn
	private User user;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn
	private Interview interview;

	private LocalDateTime dateTime;

	public Application() {
	}

	public Application(Vacancy vacancy, Department department, User user, LocalDateTime dateTime) {
		this.vacancy = vacancy;
		this.department = department;
		this.user = user;
		this.dateTime = dateTime;
	}
}
