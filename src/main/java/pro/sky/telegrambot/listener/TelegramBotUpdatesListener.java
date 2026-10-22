package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Service
public class TelegramBotUpdatesListener implements UpdatesListener {
    private static final Pattern TASK_PATTERN = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})(\\s+)(.+)");
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final TelegramBot telegramBot;
    private final NotificationTaskRepository notificationTaskRepository;

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskRepository notificationTaskRepository) {
        this.telegramBot = telegramBot;
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            var message = update.message();
            if (message == null || message.text() == null) {
                return;
            }

            String text = message.text();

            if ("/start".equals(text)) {
                long chatId = message.chat().id();
                var sendMessage = new SendMessage(chatId, "Привет! Я бот. Чем могу помочь?");
                telegramBot.execute(sendMessage);
                return;
            }

            Matcher matcher = TASK_PATTERN.matcher(text);
            if (matcher.matches()) {
                try {
                    String dateStr = matcher.group(1);
                    String reminderText = matcher.group(3);
                    LocalDateTime sendAt = LocalDateTime.parse(dateStr, DTF);

                    var task = new NotificationTask();
                    task.setChatId(message.chat().id());
                    task.setMessageText(reminderText);
                    task.setSendAt(sendAt);

                    notificationTaskRepository.save(task);
                    logger.info("Reminder saved for chat {}: {} at {}", message.chat().id(), reminderText, sendAt);

                    // Опционально: подтверждение пользователю
                    var confirmMessage = new SendMessage(message.chat().id(), "Напоминание сохранено: " + reminderText + " на " + dateStr);
                    telegramBot.execute(confirmMessage);
                } catch (DateTimeParseException e) {
                    logger.warn("Failed to parse date in message: {}", text, e);
                    var errorMessage = new SendMessage(message.chat().id(), "Не удалось распознать дату. Используйте формат: ДД.ММ.ГГГГ ЧЧ:ММ Текст");
                    telegramBot.execute(errorMessage);
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
