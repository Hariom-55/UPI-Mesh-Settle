package meshSettle.Application.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MeshPacket
{
    @NotBlank
    private String packetId;

    @Min(0)
    private int ttl;

    @NotNull
    private Long createdAt;

    @NotNull
    private String ciphertext;
}
