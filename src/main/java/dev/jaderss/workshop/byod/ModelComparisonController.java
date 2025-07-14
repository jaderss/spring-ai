package dev.jaderss.workshop.byod;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
class ModelComparisonController {

    private final ChatClient chatClient;

    private static final String CONTEXT_WINDOW = """
            If you are asked about the context window of a model, here is an up to date list of popular LLMs and their current context window:
            {
              "llm_models_2025": [
                {
                  "model_name": "Llama 4 Scout",
                  "developer": "Meta",
                  "context_window_tokens": 10000000
                },
                {
                  "model_name": "Gemini 2.5 Pro",
                  "developer": "Google",
                  "context_window_tokens": 1000000
                },
                {
                  "model_name": "Llama 4 Maverick",
                  "developer": "Meta",
                  "context_window_tokens": 1000000
                },
                {
                  "model_name": "Grok 3",
                  "developer": "xAI",
                  "context_window_tokens": 1000000
                },
                {
                  "model_name": "Claude 4 Opus",
                  "developer": "Anthropic",
                  "context_window_tokens": 200000
                },
                {
                  "model_name": "Claude 4 Sonnet",
                  "developer": "Anthropic",
                  "context_window_tokens": 200000
                },
                {
                  "model_name": "OpenAI o3",
                  "developer": "OpenAI",
                  "context_window_tokens": 200000
                },
                {
                  "model_name": "OpenAI o4-mini",
                  "developer": "OpenAI",
                  "context_window_tokens": 200000
                },
                {
                  "model_name": "GPT-4.5",
                  "developer": "OpenAI",
                  "context_window_tokens": 128000
                },
                {
                  "model_name": "DeepSeek R1",
                  "developer": "DeepSeek",
                  "context_window_tokens": 128000
                },
                {
                  "model_name": "Mistral Large 2",
                  "developer": "Mistral AI",
                  "context_window_tokens": 128000
                },
                {
                  "model_name": "Command R+",
                  "developer": "Cohere",
                  "context_window_tokens": 128000
                },
                {
                  "model_name": "Qwen 3",
                  "developer": "Alibaba",
                  "context_window_tokens": 32000
                },
                {
                  "model_name": "Llama 3.1 (405B)",
                  "developer": "Meta",
                  "context_window_tokens": 128000
                },
                {
                  "model_name": "Mixtral 8x22B",
                  "developer": "Mistral AI",
                  "context_window_tokens": 64000
                },
                {
                  "model_name": "Falcon 180B",
                  "developer": "Technology Innovation Institute",
                  "context_window_tokens": 32000
                },
                {
                  "model_name": "Gemma 2 (27B)",
                  "developer": "Google",
                  "context_window_tokens": 8000
                },
                {
                  "model_name": "Phi-3 (Medium)",
                  "developer": "Microsoft",
                  "context_window_tokens": 128000
                },
                {
                  "model_name": "DBRX Instruct",
                  "developer": "Databricks",
                  "context_window_tokens": 32000
                },
                {
                  "model_name": "Jamba",
                  "developer": "AI21 Labs",
                  "context_window_tokens": 256000
                },
                {
                  "model_name": "Stable LM 2 12B",
                  "developer": "Stability AI",
                  "context_window_tokens": 32000
                },
                {
                    "model_name": "Nemotron-4 340B",
                    "developer": "NVIDIA",
                    "context_window_tokens": 4096
                }
              ]
            }
            """;

    @GetMapping("/models")
    public String models() {
        return chatClient.prompt()
                .user("Can you give me an up to date list of popular LLMs and their current context window?")
                .call()
                .content();
    }

    @GetMapping("/models/stuff-the-prompt")
    public String modelsStuffThePrompt() {
        return chatClient.prompt()
                .system(CONTEXT_WINDOW)
                .user("Can you give me an up to date list of popular LLMs and their current context window?")
                .call()
                .content();
    }

}
