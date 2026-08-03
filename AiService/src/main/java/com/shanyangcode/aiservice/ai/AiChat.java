package com.shanyangcode.aiservice.ai;

import com.shanyangcode.aiservice.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

@InputGuardrails({SafeInputGuardrail.class})
public interface AiChat {

    String chat(@MemoryId Long sessionId, @UserMessage String prompt);

    Flux<String> streamChat(@MemoryId Long sessionId, @UserMessage String prompt);
}
