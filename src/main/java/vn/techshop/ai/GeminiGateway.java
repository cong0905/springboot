package vn.techshop.ai;

import java.time.Instant;
import java.util.Map;

public interface GeminiGateway {
    record Result(String json,int inputTokens,int outputTokens) {}
    Result generate(String instructions,String input,Map<String,Object> schema,Instant deadline);
    final class Failure extends RuntimeException {
        private final String code;private final boolean retryable;
        public Failure(String code,boolean retryable){super("Gemini request failed: "+code);this.code=code;this.retryable=retryable;}
        public String code(){return code;}public boolean retryable(){return retryable;}
    }
}
