package com.bidket.queue.presentation.api;

import com.bidket.queue.application.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QueueController {
    private final QueueService queueService;

}
