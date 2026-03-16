package com.fortnite.pronos.service.alert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Sends email alerts when the player identity pipeline has stale UNRESOLVED entries. Only active
 * when {@code alert.email.enabled=true} is set in configuration.
 */
@Service
@ConditionalOnProperty(name = "alert.email.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class EmailAlertService {

  static final String SUBJECT_PREFIX = "[Fortnite Pronos] ALERTE pipeline — ";

  private final JavaMailSender mailSender;
  private final String to;
  private final String from;

  @Autowired
  public EmailAlertService(
      JavaMailSender mailSender,
      @Value("${alert.email.to}") String to,
      @Value("${alert.email.from}") String from) {
    this.mailSender = mailSender;
    this.to = to;
    this.from = from;
  }

  /**
   * Sends a consolidated alert email listing the number of stale UNRESOLVED pipeline entries.
   *
   * @param count number of entries exceeding the threshold
   * @param thresholdHours the configured age threshold in hours
   */
  public void sendUnresolvedAlert(long count, long thresholdHours) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(to);
    msg.setFrom(from);
    msg.setSubject(SUBJECT_PREFIX + count + " joueur(s) UNRESOLVED > " + thresholdHours + "h");
    msg.setText(buildBody(count, thresholdHours));
    mailSender.send(msg);
    log.info("Email alert sent to {} — {} UNRESOLVED entries > {}h", to, count, thresholdHours);
  }

  String buildBody(long count, long thresholdHours) {
    return count
        + " entrée(s) PlayerIdentityEntry sont restées UNRESOLVED depuis plus de "
        + thresholdHours
        + " heures.\n\n"
        + "Veuillez vous connecter au panneau d'administration pour les traiter :\n"
        + "http://localhost:8080/admin/pipeline\n\n"
        + "-- Fortnite Pronos (alerte automatique)";
  }
}
