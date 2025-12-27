package uz.qarzdorlar_ai.payload;

import java.util.List;

// Yangi klass yaratamiz
public record ProductInsertEvent(List<ProductParseDTO> products) {}