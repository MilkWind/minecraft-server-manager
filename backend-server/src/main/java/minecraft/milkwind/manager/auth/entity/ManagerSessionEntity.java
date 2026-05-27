package minecraft.milkwind.manager.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("manager_session")
public class ManagerSessionEntity {

    @TableId
    private String token;
    private String username;
    private String displayName;
    private String createdAt;
    private String lastSeenAt;
    private String expiresAt;
}
