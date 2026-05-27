package minecraft.milkwind.manager.server.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("server_config")
public class ServerConfigEntity {

    @TableId
    private String serverId;
    private String displayName;
    private String rootDirectory;
    private String jvmArguments;
    private String publicAddress;
    private String gameVersion;
    private Boolean chatEnabled;
    private String status;
    private String createdAt;
    private String updatedAt;
}
