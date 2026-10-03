package lt.prifkodas.knygnesys.goal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendInactivityReminder(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Knygnesys: laikas grįžti prie knygų!");
        message.setText(buildEmailText(username));
        mailSender.send(message);
    }

    private String buildEmailText(String username) {
        return """
                Sveiki, %s!

                Pastebėjome, kad jau 7 dienas neskaitėte knygų Knygnesys platformoje.

                Jūsų metinis tikslas laukia – grįžkite ir tęskite skaitymo kelionę!

                Prisijunkite: http://localhost:4200

                Sėkmingo skaitymo,
                Knygnesys komanda
                """.formatted(username);
    }
}
