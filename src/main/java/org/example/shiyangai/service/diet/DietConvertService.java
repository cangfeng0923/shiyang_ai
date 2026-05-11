package org.example.shiyangai.service.diet;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DietConvertService {

    public Double convertToGrams(
            Double amount,
            String unit
    ) {

        if (amount == null || unit == null) {
            return null;
        }

        Map<String, Double> unitMap = Map.of(
                "g", 1.0,
                "kg", 1000.0,
                "ml", 1.0,
                "杯", 200.0,
                "碗", 250.0,
                "勺", 15.0,
                "个", 50.0,
                "片", 10.0,
                "块", 30.0,
                "盘", 200.0
        );

        return amount
                * unitMap.getOrDefault(unit, 1.0);
    }
}