package com.expense.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingsDTO {
    private Boolean notificationsEnabled;
    private Boolean pushNotifications;
    private Boolean budgetAlerts;
    private Boolean darkMode;
}
