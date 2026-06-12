package com.library.model;

import java.util.List;
import java.util.Map;

public record AdminStatisticsDTO(
    int totalBorrow,
    int totalReturned,
    double totalFines,
    Map<String, Integer> borrowedBooksByDate,
    Map<String, Integer> booksByCategory,
    List<TopItem> topBooks,
    List<TopItem> topMembers
) {
    public record TopItem(String name, int count) {}
}
