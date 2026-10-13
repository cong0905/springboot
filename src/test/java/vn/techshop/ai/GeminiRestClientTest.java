package vn.techshop.ai;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

/** Local HTTP stub only. Does not contact Google or use a real key. */
class GeminiRestClientTest {
 HttpServer server;GeminiRestClient client;MockEnvironment env;int status;String body;int delay;AtomicReference<String> key=new AtomicReference<>(),url=new AtomicReference<>(),payload=new AtomicReference<>();
 @BeforeEach void start() throws Exception {
  status=200;delay=0;body="{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"{\\\"answer\\\":\\\"[Giả lập] Xin chào\\\"}\"}]}}],\"usageMetadata\":{\"promptTokenCount\":7,\"candidatesTokenCount\":3}}";
  server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/",exchange->{try{url.set(exchange.getRequestURI().toString());key.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));payload.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));if(delay>0)Thread.sleep(delay);byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);}catch(Exception ignored){}finally{exchange.close();}});server.start();
  env=new MockEnvironment().withProperty("app.ai.enabled","true").withProperty("app.ai.api-key","fake-local-test-key").withProperty("app.ai.model","test-model");client=new GeminiRestClient(new AiSettings(env),URI.create("http://127.0.0.1:"+server.getAddress().getPort()));
 }
 @AfterEach void stop(){server.stop(0);}
 GeminiGateway.Result call(){return client.generate("System","User input",Map.of("type","object"),Instant.now().plusSeconds(3));}
 @Test void correctEndpointHeaderStructuredSchemaAndTokenUsage(){var result=call();assertThat(url.get()).isEqualTo("/v1beta/models/test-model:generateContent");assertThat(key.get()).isEqualTo("fake-local-test-key");var parsed=JsonMapper.builder().build().readTree(payload.get());assertThat(parsed.path("generationConfig").path("responseMimeType").asText()).isEqualTo("application/json");assertThat(parsed.path("generationConfig").path("responseJsonSchema").path("type").asText()).isEqualTo("object");assertThat(result.inputTokens()).isEqualTo(7);assertThat(result.outputTokens()).isEqualTo(3);assertThat(result.json()).contains("[Giả lập]");}
 @Test void missingConfigurationDoesNotCallHttp(){env.setProperty("app.ai.api-key","");assertThatThrownBy(this::call).isInstanceOfSatisfying(GeminiGateway.Failure.class,e->assertThat(e.code()).isEqualTo("NOT_CONFIGURED"));assertThat(payload.get()).isNull();}
 @Test void providerErrorsAreSanitizedAndQuotaIsRetryable(){status=429;body="SECRET_RAW_PROVIDER_ERROR";assertThatThrownBy(this::call).isInstanceOfSatisfying(GeminiGateway.Failure.class,e->{assertThat(e.code()).isEqualTo("PROVIDER_QUOTA");assertThat(e.retryable()).isTrue();assertThat(e.toString()).doesNotContain("SECRET","fake-local-test-key");});status=403;assertThatThrownBy(this::call).isInstanceOfSatisfying(GeminiGateway.Failure.class,e->{assertThat(e.code()).isEqualTo("AUTH_CONFIGURATION");assertThat(e.retryable()).isFalse();});}
 @Test void blockedAndMalformedResponsesAreRejected(){body="{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}";assertThatThrownBy(this::call).isInstanceOfSatisfying(GeminiGateway.Failure.class,e->assertThat(e.code()).isEqualTo("BLOCKED_OR_INCOMPLETE"));body="broken";assertThatThrownBy(this::call).isInstanceOfSatisfying(GeminiGateway.Failure.class,e->assertThat(e.code()).isEqualTo("INVALID_RESPONSE"));}
 @Test void deadlineBoundsCompleteResponse(){delay=1200;var begin=Instant.now();assertThatThrownBy(()->client.generate("System","User",Map.of("type","object"),Instant.now().plusMillis(180))).isInstanceOf(GeminiGateway.Failure.class);assertThat(Duration.between(begin,Instant.now()).toMillis()).isLessThan(900);}
 @Test void masksEmailAndVietnamesePhone(){assertThat(AiAdvisor.mask("abc@example.com 0912345678 +84912345678")).isEqualTo("[email] [phone] [phone]");}
}
