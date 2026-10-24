package meshSettle.Application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void unknownDeviceReturnsCleanBadRequest() throws Exception {
        String body = """
                {"senderVpa":"alice@demo","receiverVpa":"bob@demo","amount":10,
                 "pin":"1234","startDevice":"phone-does-not-exist"}
                """;

        mockMvc.perform(post("/api/demo/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Unknown device: phone-does-not-exist"))
                // Nothing beyond our own clean message — no stack trace, no exception class name.
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Exception"))));
    }

    @Test
    void invalidMeshPacketFailsValidationNotA500() throws Exception {

        String body = """
                {"packetId":"","ttl":-1,"createdAt":123}
                """;

        mockMvc.perform(post("/api/bridge/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isArray());
    }
}
