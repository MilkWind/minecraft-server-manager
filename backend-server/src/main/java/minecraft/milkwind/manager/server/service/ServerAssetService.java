package minecraft.milkwind.manager.server.service;

import minecraft.milkwind.manager.common.exception.ApiException;
import minecraft.milkwind.manager.server.dto.AssetActionResultDto;
import minecraft.milkwind.manager.server.dto.BatchAssetActionResultDto;
import minecraft.milkwind.manager.server.dto.ManagedAssetDto;
import minecraft.milkwind.manager.server.entity.ServerConfigEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ServerAssetService {

    private static final String DISABLED_FOLDER_NAME = "_manager_disabled";

    public List<ManagedAssetDto> listMods(ServerConfigEntity config) {
        return scanAssets(Path.of(config.getRootDirectory(), "mods"), "MOD");
    }

    public List<ManagedAssetDto> listDatapacks(ServerConfigEntity config) {
        return scanAssets(Path.of(config.getRootDirectory(), "world", "datapacks"), "DATAPACK");
    }

    public AssetActionResultDto suspendAsset(ServerConfigEntity config, String assetId) {
        return moveAsset(config, assetId, false);
    }

    public AssetActionResultDto resumeAsset(ServerConfigEntity config, String assetId) {
        return moveAsset(config, assetId, true);
    }

    public BatchAssetActionResultDto suspendAssets(ServerConfigEntity config, List<String> assetIds) {
        return moveAssets(config, assetIds, false);
    }

    public BatchAssetActionResultDto resumeAssets(ServerConfigEntity config, List<String> assetIds) {
        return moveAssets(config, assetIds, true);
    }

    private List<ManagedAssetDto> scanAssets(Path baseDirectory, String type) {
        List<ManagedAssetDto> assets = new ArrayList<>();
        Path disabledDirectory = baseDirectory.resolve(DISABLED_FOLDER_NAME);

        scanDirectoryInto(assets, baseDirectory, type, true);
        scanDirectoryInto(assets, disabledDirectory, type, false);

        assets.sort(Comparator.comparing(ManagedAssetDto::name, String.CASE_INSENSITIVE_ORDER));
        return assets;
    }

    private void scanDirectoryInto(List<ManagedAssetDto> target, Path directory, String type, boolean enabled) {
        if (!Files.isDirectory(directory)) {
            return;
        }

        try (var stream = Files.list(directory)) {
            stream.filter(path -> !path.getFileName().toString().equalsIgnoreCase(DISABLED_FOLDER_NAME))
                    .filter(this::isAssetFile)
                    .forEach(path -> target.add(new ManagedAssetDto(
                            buildAssetId(type, path.getFileName().toString()),
                            path.getFileName().toString(),
                            type,
                            enabled
                    )));
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "asset_scan_failed", "扫描服务器资源失败");
        }
    }

    private AssetActionResultDto moveAsset(ServerConfigEntity config, String assetId, boolean enable) {
        AssetLocation location = resolveAssetLocation(config, assetId);
        Path source = enable ? location.disabledPath() : location.enabledPath();
        Path target = enable ? location.enabledPath() : location.disabledPath();

        if (!Files.exists(source)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "asset_not_found", "目标资源不存在");
        }

        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "asset_move_failed", "更新资源状态失败");
        }

        return new AssetActionResultDto(
                config.getServerId(),
                buildAssetId(location.type(), location.fileName()),
                location.type(),
                enable ? "resume" : "suspend",
                "UPDATED",
                enable ? "资源已恢复，建议重启服务器" : "资源已停用，建议重启服务器"
        );
    }

    private AssetLocation resolveAssetLocation(ServerConfigEntity config, String assetId) {
        String normalized = assetId == null ? "" : assetId.trim();
        if (normalized.isBlank() || !normalized.contains(":")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_asset_id", "资源 ID 无效");
        }

        String[] segments = normalized.split(":", 2);
        String type = segments[0].toUpperCase(Locale.ROOT);
        String fileName = segments[1];

        Path baseDirectory = switch (type) {
            case "MOD" -> Path.of(config.getRootDirectory(), "mods");
            case "DATAPACK" -> Path.of(config.getRootDirectory(), "world", "datapacks");
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_asset_type", "不支持的资源类型");
        };

        return new AssetLocation(
                type,
                fileName,
                baseDirectory.resolve(fileName),
                baseDirectory.resolve(DISABLED_FOLDER_NAME).resolve(fileName)
        );
    }

    private BatchAssetActionResultDto moveAssets(ServerConfigEntity config, List<String> assetIds, boolean enable) {
        List<AssetActionResultDto> results = assetIds.stream()
                .map(assetId -> moveAsset(config, assetId, enable))
                .toList();

        return new BatchAssetActionResultDto(
                config.getServerId(),
                enable ? "resume" : "suspend",
                results.size(),
                results,
                "UPDATED",
                enable ? "Batch asset resume completed" : "Batch asset suspend completed"
        );
    }

    private boolean isAssetFile(Path path) {
        return Files.isRegularFile(path) || Files.isDirectory(path);
    }

    private String buildAssetId(String type, String fileName) {
        return type + ":" + fileName;
    }

    private record AssetLocation(
            String type,
            String fileName,
            Path enabledPath,
            Path disabledPath
    ) {
    }
}
