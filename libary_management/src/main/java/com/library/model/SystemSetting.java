package com.library.model;

import java.time.LocalDateTime;

public class SystemSetting {
    private Long settingId;
    private String settingKey;
    private String settingValue;
    private String dataType;
    private String description;
    private LocalDateTime updatedAt;

    public SystemSetting() {}

    public Long getSettingId() { return settingId; }
    public void setSettingId(Long settingId) { this.settingId = settingId; }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
