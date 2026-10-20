package dev.jaderss.workshop.multimodal.audio;

import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
class AudioGenerationController {

    private final OpenAiAudioSpeechModel speechModel;

    AudioGenerationController(OpenAiAudioSpeechModel speechModel) {
        this.speechModel = speechModel;
    }

    @GetMapping("/speak")
    public ResponseEntity<byte[]> generateSpeech(
            @RequestParam(defaultValue = "It's a great time to be a Java Developer") String prompt) {
        var options = OpenAiAudioSpeechOptions.builder()
                .model("tts-1-hd")
                .voice(OpenAiAudioSpeechOptions.Voice.ALLOY)
                .responseFormat(OpenAiAudioSpeechOptions.AudioResponseFormat.MP3)
                .speed(1.0)
                .build();

        TextToSpeechPrompt speechPrompt = new TextToSpeechPrompt(prompt, options);
        TextToSpeechResponse speechResponse = speechModel.call(speechPrompt);

        byte[] output = speechResponse.getResult().getOutput();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"speech.mp3\"")
                .body(output);
    }
}
