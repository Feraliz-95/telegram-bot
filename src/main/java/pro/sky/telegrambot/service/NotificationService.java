package pro.sky.telegrambot.service;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationTaskRepository notificationTaskRepository;
    private final TelegramBot telegramBot;

    public NotificationService(NotificationTaskRepository notificationTaskRepository,
                               TelegramBot telegramBot) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.telegramBot = telegramBot;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void sendScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = start.plusMinutes(1);

        logger.info("Checking notifications for time range: {} - {}", start, end);
        List<NotificationTask> tasks = notificationTaskRepository.findBySendAtBetween(start, end);

        if (tasks.isEmpty()) {
            logger.debug("No notifications to send for time range {} - {}", start, end);
            return;
        }

        for (NotificationTask task : tasks) {
            try {
                var message = new SendMessage(task.getChatId(), task.getMessageText());
                telegramBot.execute(message);
                logger.info("Notification sent for chat {}: {}", task.getChatId(), task.getMessageText());
            } catch (Exception e) {
                logger.error("Failed to send notification for task id={}, chat={}", task.getId(), task.getChatId(), e);
            }
        }
    }


}



