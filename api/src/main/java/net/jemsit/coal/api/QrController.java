package net.jemsit.coal.api;

import net.jemsit.coal.api.service.QrService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/qr")
public class QrController {

    private final QrService service;

    public QrController(QrService service) {
        this.service = service;
    }

    @GetMapping(value = "/{type}/{id}.png", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] png(@PathVariable String type, @PathVariable UUID id) {
        return service.png(type, id);
    }
}
