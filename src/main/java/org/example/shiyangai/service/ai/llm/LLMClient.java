// LLMClient.java
package org.example.shiyangai.service.ai.llm;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface LLMClient {

    String chatSync(List<Map<String, String>> messages, double temperature, int maxTokens) throws Exception;

    void chatStream(List<Map<String, String>> messages, double temperature, int maxTokens,
                    Consumer<String> onChunk, Runnable onComplete) throws Exception;

    String getModelName();
}