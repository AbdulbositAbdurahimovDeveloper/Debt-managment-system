package uz.qarzdorlar_ai;

public interface Utils {

    interface Sheets {
        String paper1 = "Jurnal";
        String paper2 = "Dashboard";
        String paper3 = "database";
        String paper4 = "Users";
        String paper5 = "data";
    }

    interface Prompts {

        String userPrompt = """
                ACT AS A HIGH-PRECISION HARDWARE ANALYST. 
                Parse the input list into a JSON array of 'ProductParseDTO' objects.
                
                CRITICAL RULE: 'brandName' and 'categoryName' MUST NEVER BE NULL. 
                If they are not explicitly mentioned in the text, you MUST infer them based on your knowledge of hardware models and specifications.
                
                FIELD-SPECIFIC EXTRACTION RULES:
                1. 'brandName': MANDATORY. Identify the manufacturer (e.g., "HP", "Lenovo", "Asus", "Avtech", "HyperX"). If unknown, use the first word of the model name.
                2. 'categoryName': MANDATORY. Identify the device type. 
                   - Infer "All-in-One" for: HP OmniStudio, HP EliteOne, HP Pavilion 32, or any desktop-spec CPU with a screen > 20 inches.
                   - Infer "Laptop" for: portable computers with 13-17 inch screens.
                   - Infer "Monitor" for: display-only units (e.g., Avtech DW/NW series).
                   - Infer "Headset" for: audio devices (e.g., HyperX Cloud).
                   - Infer "Desktop" for: system units without integrated screens.
                3. 'name': Full commercial model name.
                4. 'price': Numeric value from 'PriceSource' as a string (e.g., "2400").
                5. 'cpu': Processor model (e.g., "Intel Core i7-13700T").
                6. 'ram': ALWAYS format as "CAPACITY TYPE" (e.g., "16GB DDR4", "32GB DDR5").
                7. 'storage': ALWAYS format as "CAPACITY TYPE" (e.g., "1TB SSD", "512GB SSD").
                8. 'display': Full screen details (e.g., "31.5 inch IPS").
                9. 'resolution': Extract: 4K, UHD, FHD, QHD, 2K, WUXGA. (e.g., "4K UHD").
                10. 'os': Operating system (e.g., "Windows 11 Home").
                11. 'color': Color name if mentioned.
                12. 'modelCode': Extract ONLY the code inside parentheses (e.g., from '(6J6L2AV)' extract '6J6L2AV'). 
                13. 'touchScreen': Boolean true/false. Set true if "Touch" or "TS" present.
                14. 'backlit': Boolean true/false. Set true if "Backlit" present.
                15. 'description': Extra details like stand type or special features.
                
                STRICT 'rawData' VERBATIM RULE:
                - 'rawData' MUST be a 100% exact copy of the text provided in 'ProductSource'.
                - DO NOT include 'PriceSource' or its value.
                - DO NOT include the '|' separator or any trailing characters/spaces.
                - Result must be exactly what was in the source item.
                
                FORMATTING REQUIREMENTS:
                - Return ONLY a valid JSON ARRAY. 
                - No markdown formatting.
                - For other missing string fields, use null.
                
                {format}
                
                INPUT DATA:
                {items}
                """;

    }
}
