package minecraft.milkwind.manager.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("manager_user")
public class ManagerUserEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String username;
    private String displayName;
    private String passwordHash;
    private String totpCode;
    private Boolean active;
    private String createdAt;
    private String updatedAt;
}
