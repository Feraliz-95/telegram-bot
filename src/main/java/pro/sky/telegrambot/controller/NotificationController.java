package pro.sky.telegrambot.controller;


import org.springframework.web.bind.annotation.*;

import pro.sky.telegrambot.service.NotificationService;

@RestController
@RequestMapping("/debug")
public class NotificationController {


    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/send-notifications")
    public String triggerSend() {
        notificationService.sendScheduledNotifications();
        return "Triggered: notifications processing started";
    }
}


