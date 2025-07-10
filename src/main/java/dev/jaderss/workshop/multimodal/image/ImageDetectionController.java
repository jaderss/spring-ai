package dev.jaderss.workshop.multimodal.image;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class ImageDetectionController {

    @Value("classpath:images/34312498-1caf-41f3-a9c6-4f80e9a3f53a-1400.jpg")
    private Resource image;

    private final ChatClient chatClient;

    @GetMapping("/image-to-text")
    public String image() {
        return chatClient.prompt()
                .user(u -> u.text("Describe the image you see in this sample.")
                        .media(MimeTypeUtils.IMAGE_JPEG, image))
                .call()
                .content();
    }

}
