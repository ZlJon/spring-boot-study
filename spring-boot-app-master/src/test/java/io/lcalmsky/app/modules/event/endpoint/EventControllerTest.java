package io.lcalmsky.app.modules.event.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.infra.IntegrationTest;
import io.lcalmsky.app.modules.account.AccountFactory;
import io.lcalmsky.app.modules.account.WithAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.event.EventFactory;
import io.lcalmsky.app.modules.event.application.EventService;
import io.lcalmsky.app.modules.event.domain.entity.Enrollment;
import io.lcalmsky.app.modules.event.domain.entity.Event;
import io.lcalmsky.app.modules.event.domain.entity.EventType;
import io.lcalmsky.app.modules.event.infra.repository.EnrollmentRepository;
import io.lcalmsky.app.modules.event.infra.repository.EventRepository;
import io.lcalmsky.app.modules.study.application.StudyService;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
class EventControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired StudyService studyService;
    @Autowired EventService eventService;
    @Autowired AccountRepository accountRepository;
    @Autowired StudyRepository studyRepository;
    @Autowired EventRepository eventRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired ObjectMapper objectMapper;
    @Autowired AccountFactory accountFactory;
    @Autowired EventFactory eventFactory;
    private final String studyPath = "study-path";
    private Study study;

    @BeforeEach
    void beforeEach() {
        Account account = accountRepository.findByNickname("jaime");
        this.study = studyService.createNewStudy(StudyForm.builder()
                .path(studyPath)
                .shortDescription("short-description")
                .fullDescription("full-description")
                .title("title")
                .build(), account);
    }

    @AfterEach
    void afterEach() {
        studyRepository.deleteAll();
    }

    @Test
    @DisplayName("이벤트 폼")
    @WithAccount("jaime")
    void eventForm() throws Exception {
        mockMvc.perform(get("/study/" + studyPath + "/new-event"))
                .andExpect(status().isOk())
                .andExpect(view().name("event/form"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(model().attributeExists("eventForm"));

    }

    @Test
    @DisplayName("모임 생성 성공")
    @WithAccount("jaime")
    void createEvent() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        ResultActions resultActions = mockMvc.perform(post("/study/" + studyPath + "/new-event")
                .param("description", "description")
                .param("eventType", EventType.FCFS.name())
                .param("endDateTime", now.plusWeeks(3).toString())
                .param("endEnrollmentDateTime", now.plusWeeks(1).toString())
                .param("limitOfEnrollments", "2")
                .param("startDateTime", now.plusWeeks(2).toString())
                .param("title", "title")
                .with(csrf()));
        Event event = eventRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("등록된 모임이 없습니다."));
        resultActions.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/events/" + event.getId()));
    }

    @Test
    @DisplayName("모임 생성 실패")
    @WithAccount("jaime")
    void createEventWithErrors() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        mockMvc.perform(post("/study/" + studyPath + "/new-event")
                        .param("description", "description")
                        .param("eventType", EventType.FCFS.name())
                        .param("endDateTime", now.plusWeeks(3).toString())
                        .param("endEnrollmentDateTime", now.plusWeeks(1).toString())
                        .param("limitOfEnrollments", "2")
                        .param("startDateTime", now.plusWeeks(2).toString())
                        .param("title", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("event/form"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"));
    }

    @Test
    @DisplayName("모임 뷰")
    @WithAccount("jaime")
    void eventView() throws Exception {
        Event event = eventFactory.createEvent(EventType.FCFS, accountFactory.createAccount("manager"), studyPath);
        mockMvc.perform(get("/study/" + studyPath + "/events/" + event.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(view().name("event/view"));
    }

    @Test
    @DisplayName("모임 리스트 뷰")
    @WithAccount("jaime")
    void eventListView() throws Exception {
        eventFactory.createEvent(EventType.FCFS, accountFactory.createAccount("manager"), studyPath);
        mockMvc.perform(get("/study/" + studyPath + "/events"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(model().attributeExists("newEvents"))
                .andExpect(model().attributeExists("oldEvents"))
                .andExpect(view().name("study/events"));
    }

    @Test
    @DisplayName("모임 수정 뷰")
    @WithAccount("jaime")
    void eventEditView() throws Exception {
        Event event = eventFactory.createEvent(EventType.FCFS, accountFactory.createAccount("manager"), studyPath);
        mockMvc.perform(get("/study/" + studyPath + "/events/" + event.getId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(model().attributeExists("event"))
                .andExpect(model().attributeExists("eventForm"))
                .andExpect(view().name("event/update-form"));
    }

    @Test
    @DisplayName("모임 수정")
    @WithAccount("jaime")
    void editEvent() throws Exception {
        Event event = eventFactory.createEvent(EventType.FCFS, accountFactory.createAccount("manager"), studyPath);
        LocalDateTime now = LocalDateTime.now();
        mockMvc.perform(post("/study/" + studyPath + "/events/" + event.getId() + "/edit")
                        .param("description", "description")
                        .param("eventType", EventType.FCFS.name())
                        .param("endDateTime", now.plusWeeks(3).toString())
                        .param("endEnrollmentDateTime", now.plusWeeks(1).toString())
                        .param("limitOfEnrollments", "2")
                        .param("startDateTime", now.plusWeeks(2).toString())
                        .param("title", "anotherTitle")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/events/" + event.getId()));
    }

    @Test
    @DisplayName("모임 삭제")
    @WithAccount("jaime")
    void deleteEvent() throws Exception {
        Event event = eventFactory.createEvent(EventType.FCFS, accountFactory.createAccount("manager"), studyPath);
        mockMvc.perform(delete("/study/" + studyPath + "/events/" + event.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/events"));
        Optional<Event> byId = eventRepository.findById(event.getId());
        assertEquals(Optional.empty(), byId);
    }

    @Test
    @DisplayName("선착순 모임에 참가 신청 - 자동 수락")
    @WithAccount("jaime")
    void enroll() throws Exception {
        Event event = eventFactory.createEvent(EventType.FCFS, accountFactory.createAccount("manager"), studyPath);
        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));
        Account account = accountRepository.findByNickname("jaime");
        isAccepted(account, event);
    }

    @Test
    @DisplayName("선착순 모임에 참가 신청 - 대기중")
    @WithAccount("jaime")
    void enroll_with_waiting() throws Exception {
        Event event = eventFactory.createEvent(EventType.FCFS, accountFactory.createAccount("manager"), studyPath);
        Account tester1 = accountFactory.createAccount("tester1");
        Account tester2 = accountFactory.createAccount("tester2");
        eventService.enroll(event, tester1);
        eventService.enroll(event, tester2);
        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));
        Account jaime = accountRepository.findByNickname("jaime");
        isNotAccepted(jaime, event);
    }

    @Test
    @DisplayName("참가신청 확정자가 취소하는 경우: 다음 대기자 자동 신청")
    @WithAccount("jaime")
    void leave_auto_enroll() throws Exception {
        Account jaime = accountRepository.findByNickname("jaime");
        Account tester1 = accountFactory.createAccount("tester1");
        Account tester2 = accountFactory.createAccount("tester2");
        Event event = eventFactory.createEvent(EventType.FCFS, accountFactory.createAccount("manager"), studyPath);
        eventService.enroll(event, tester1);
        eventService.enroll(event, jaime);
        eventService.enroll(event, tester2);
        isAccepted(tester1, event);
        isAccepted(jaime, event);
        isNotAccepted(tester2, event);
        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/leave")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));
        isAccepted(tester1, event);
        isAccepted(tester2, event);
        assertNull(enrollmentRepository.findByEventAndAccount(event, jaime));
    }

    @Test
    @DisplayName("참가신청 비확정자가 참가 신청을 취소하는 경우: 변화 없음")
    @WithAccount("jaime")
    void leave() throws Exception {
        Account jaime = accountRepository.findByNickname("jaime");
        Account tester1 = accountFactory.createAccount("tester1");
        Account tester2 = accountFactory.createAccount("tester2");
        Event event = eventFactory.createEvent(EventType.FCFS, accountFactory.createAccount("manager"), studyPath);
        eventService.enroll(event, tester2);
        eventService.enroll(event, tester1);
        eventService.enroll(event, jaime);
        isAccepted(tester1, event);
        isAccepted(tester2, event);
        isNotAccepted(jaime, event);
        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/leave")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));
        isAccepted(tester1, event);
        isAccepted(tester2, event);
        assertNull(enrollmentRepository.findByEventAndAccount(event, jaime));
    }

    @Test
    @DisplayName("참가 신청 수락")
    @WithAccount("jaime")
    void accept() throws Exception {
        Account manager = accountRepository.findByNickname("jaime");
        Account account = accountFactory.createAccount("member");
        Event event = eventFactory.createEvent(EventType.CONFIRMATIVE, manager, studyPath);
        eventService.enroll(event, account);
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);

        mockMvc.perform(get("/study/" + study.getPath() + "/events/" + event.getId() + "/enrollments/" + enrollment.getId() + "/accept"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getEncodedPath() + "/events/" + event.getId()));

        assertTrue(enrollment.isAccepted());
    }

    @Test
    @DisplayName("참가 신청 거절")
    @WithAccount("jaime")
    void reject() throws Exception {
        Account manager = accountRepository.findByNickname("jaime");
        Account account = accountFactory.createAccount("member");
        Event event = eventFactory.createEvent(EventType.CONFIRMATIVE, manager, studyPath);
        eventService.enroll(event, account);
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);

        mockMvc.perform(get("/study/" + study.getPath() + "/events/" + event.getId() + "/enrollments/" + enrollment.getId() + "/reject"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getEncodedPath() + "/events/" + event.getId()));

        assertFalse(enrollment.isAccepted());
    }

    @Test
    @DisplayName("출석 체크")
    @WithAccount("jaime")
    void checkin() throws Exception {
        Account manager = accountRepository.findByNickname("jaime");
        Account account = accountFactory.createAccount("member");
        Event event = eventFactory.createEvent(EventType.CONFIRMATIVE, manager, studyPath);
        eventService.enroll(event, account);
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);
        eventService.acceptEnrollment(event, enrollment);

        mockMvc.perform(get("/study/" + study.getPath() + "/events/" + event.getId() + "/enrollments/" + enrollment.getId() + "/checkin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getEncodedPath() + "/events/" + event.getId()));

        assertTrue(enrollment.isAttended());
    }

    @Test
    @DisplayName("출석 체크 취소")
    @WithAccount("jaime")
    void cancelCheckin() throws Exception {
        Account manager = accountRepository.findByNickname("jaime");
        Account account = accountFactory.createAccount("member");
        Event event = eventFactory.createEvent(EventType.CONFIRMATIVE, manager, studyPath);
        eventService.enroll(event, account);
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);
        eventService.acceptEnrollment(event, enrollment);

        mockMvc.perform(get("/study/" + study.getPath() + "/events/" + event.getId() + "/enrollments/" + enrollment.getId() + "/cancel-checkin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getEncodedPath() + "/events/" + event.getId()));

        assertFalse(enrollment.isAttended());
    }

    private void isNotAccepted(Account account, Event event) {
        assertFalse(enrollmentRepository.findByEventAndAccount(event, account).isAccepted());
    }

    private void isAccepted(Account account, Event event) {
        assertTrue(enrollmentRepository.findByEventAndAccount(event, account).isAccepted());
    }
}