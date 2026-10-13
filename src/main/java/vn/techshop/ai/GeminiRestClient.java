package vn.techshop.ai;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** JDK HTTP transport with a deadline covering receipt of the complete response body. */
@Component
public class GeminiRestClient implements GeminiGateway {
    private final AiSettings settings;
    private final HttpClient client;
    private final URI origin;
    private final JsonMapper json=JsonMapper.builder().build();
    @org.springframework.beans.factory.annotation.Autowired
    public GeminiRestClient(AiSettings settings){this(settings,URI.create("https://generativelanguage.googleapis.com"));}
    GeminiRestClient(AiSettings settings,URI origin){this.settings=settings;this.origin=origin;client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();}
    public Result generate(String instructions,String input,Map<String,Object> schema,Instant deadline){
        if(!settings.ready())throw new Failure("NOT_CONFIGURED",false);
        long remaining=Duration.between(Instant.now(),deadline).toMillis();if(remaining<=0)throw new Failure("TIMEOUT",true);
        var body=Map.of("systemInstruction",Map.of("parts",List.of(Map.of("text",instructions))),
            "contents",List.of(Map.of("role","user","parts",List.of(Map.of("text",input)))),
            "generationConfig",Map.of("temperature",0.2,"maxOutputTokens",settings.number("max-output-tokens",800,2000),"responseMimeType","application/json","responseJsonSchema",schema));
        var request=HttpRequest.newBuilder(origin.resolve("/v1beta/models/"+settings.model()+":generateContent"))
            .timeout(Duration.ofMillis(remaining)).header("Content-Type","application/json").header("x-goog-api-key",settings.key()).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        var future=client.sendAsync(request,HttpResponse.BodyHandlers.ofString());
        try {
            var response=future.get(remaining,TimeUnit.MILLISECONDS);int status=response.statusCode();
            if(status!=200)throw new Failure(switch(status){case 400->"BAD_REQUEST";case 401,403->"AUTH_CONFIGURATION";case 404->"MODEL_NOT_FOUND";case 429->"PROVIDER_QUOTA";default->"PROVIDER_UNAVAILABLE";},status==429||status>=500);
            if(response.body().length()>1000000)throw new Failure("INVALID_RESPONSE",false);
            var root=json.readTree(response.body());var candidates=root.path("candidates");
            if(!candidates.isArray() || candidates.isEmpty())throw new Failure("BLOCKED_OR_EMPTY",false);
            var candidate=candidates.get(0);String finish=candidate.path("finishReason").asText();
            if(!"STOP".equals(finish))throw new Failure("BLOCKED_OR_INCOMPLETE",false);
            StringBuilder text=new StringBuilder();for(var part:candidate.path("content").path("parts"))if(!part.path("thought").asBoolean())text.append(part.path("text").asText(""));
            if(text.isEmpty())throw new Failure("BLOCKED_OR_EMPTY",false);
            var usage=root.path("usageMetadata");return new Result(text.toString(),Math.max(0,usage.path("promptTokenCount").asInt()),Math.max(0,usage.path("candidatesTokenCount").asInt()));
        }catch(Failure e){throw e;}catch(TimeoutException e){throw new Failure("TIMEOUT",true);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new Failure("INTERRUPTED",true);}catch(ExecutionException e){throw new Failure("NETWORK_ERROR",true);}catch(Exception e){throw new Failure("INVALID_RESPONSE",false);}finally{if(!future.isDone())future.cancel(true);}
    }
}
