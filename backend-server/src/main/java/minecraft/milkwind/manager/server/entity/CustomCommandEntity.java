package minecraft.milkwind.manager.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("custom_command")
public class CustomCommandEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String serverId;
    private String displayName;
    private String commandText;
    private String description;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}
