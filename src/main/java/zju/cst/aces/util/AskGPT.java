package zju.cst.aces.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.api.config.ModelConfig;
import zju.cst.aces.dto.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@code AskGPT} 类提供与ChatGPT模型交互的方法。
 */
public class AskGPT {
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public Config config;

    /**
     * 使用给定的配置初始化 {@code AskGPT} 对象。
     * @param config 配置对象
     */
    public AskGPT(Config config) {
        this.config = config;
    }

    /**
     * 向ChatGPT发送消息并获取响应。
     * @param messages 消息列表
     * @return 响应对象，若请求失败则返回 {@code null}
     */
    public Response askChatGPT(List<Message> messages) {
        String apiKey = config.getRandomKey();
        int maxTry = 5;
        while (maxTry > 0) {
            Response response = null;
            try {
                Map<String, Object> payload = new HashMap<>();

                // 设置模型配置参数
                ModelConfig modelConfig = config.getModel().getDefaultConfig();

                payload.put("messages", messages);
                payload.put("model", modelConfig.getModelName());
                payload.put("temperature", modelConfig.getTemperature());
                payload.put("frequency_penalty", modelConfig.getFrequencyPenalty());
                payload.put("presence_penalty", modelConfig.getPresencePenalty());
                payload.put("max_tokens", config.getMaxResponseTokens());
                String jsonPayload = GSON.toJson(payload);

                RequestBody body = RequestBody.create(MEDIA_TYPE, jsonPayload);
                Request request = new Request.Builder()
                        .url(modelConfig.getUrl())
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .build();

                response = config.getClient().newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                try {
                    Thread.sleep(config.sleepTime);
                } catch (InterruptedException ie) {
                    throw new RuntimeException("In AskGPT.askChatGPT: " + ie);
                }

                return response;
            } catch (IOException e) {
                if (response != null) {
                    response.close();
                }
                config.getLog().error("In AskGPT.askChatGPT: " + e);
                maxTry--;
            }
        }
        config.getLog().debug("AskGPT: Failed to get response\n");
        return null;
    }
}
