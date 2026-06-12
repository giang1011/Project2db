package com.library.model;

import java.time.LocalDateTime;

public record ActivityLogDTO(
    long logId,
    String userName,
    String action,
    String oldValue,
    String newValue,
    LocalDateTime createdAt
) {}
