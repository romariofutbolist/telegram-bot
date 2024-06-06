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
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private static final Pattern PATTERN = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

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

            if (message != null) {
                var text = update.message().text();
                var chatId = update.message().chat().id();
                if (text != null) {
                    if("/start".equals(text)) {
                        telegramBot.execute(new SendMessage(chatId, "Добро пожаловать!"));
                    } else {
                        // 01.01.2022 20:00 Сделать домашнюю работу
                        var matcher = PATTERN.matcher(text);
                        if(matcher.matches()) {
                            var date = parseDate(matcher.group(1));
                            if(date == null) {
                                telegramBot.execute(new SendMessage(chatId, "Неправильный формат даты"));
                                return;
                            }
                            var task = new NotificationTask();
                            task.setText(matcher.group(3));
                            task.setChatId(chatId);
                            task.setTaskDate(date);
                            notificationTaskRepository.save(task);
                            logger.info("Task has been saved: {}", task);
                        }
                    }
                }

            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private LocalDateTime parseDate(String date) {
        try {
            return LocalDateTime.parse(date, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.error("Incorrect date format: {}", date);
        }
        return null;
    }

}
