package minecraft.milkwind.manager.server.dto;

public record PlayerDto(
        String name,
        boolean operator,
        int latencyMs
) {
}
