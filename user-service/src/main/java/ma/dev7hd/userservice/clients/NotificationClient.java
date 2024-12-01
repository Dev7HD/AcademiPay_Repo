package ma.dev7hd.userservice.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import ma.dev7hd.userservice.entities.registrations.PendingStudent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/push-student-registration")
    @CircuitBreaker(name = "NotificationsService", fallbackMethod = "defaultNotifications")
    void pushStudentRegistration(@RequestBody PendingStudent pendingStudent);

    @PostMapping("/push-students-registrations")
    @CircuitBreaker(name = "NotificationsService", fallbackMethod = "defaultNotifications")
    void pushStudentsRegistrations(@RequestBody List<PendingStudent> pendingStudents);

    @PostMapping("/notification-seen")
    @CircuitBreaker(name = "NotificationsService", fallbackMethod = "defaultNotifications")
    void adminNotificationSeen(String studentEmail);

    @PostMapping("/notifications-seen")
    @CircuitBreaker(name = "NotificationsService", fallbackMethod = "defaultNotifications")
    void adminNotificationsSeen(@RequestBody List<String> studentEmails);

    default void defaultNotifications(PendingStudent pendingStudent, Exception e) {
        System.out.println("Notification service not available.");
        System.err.println(e.getMessage());
    }
}
