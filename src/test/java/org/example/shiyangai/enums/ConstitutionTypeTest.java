package org.example.shiyangai.enums;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstitutionTypeTest {

    @Test
    void testEvaluateGoodFood() {
        ConstitutionType type = ConstitutionType.QI_DEFICIENCY;
        ConstitutionType.SuitabilityResult result = type.evaluate("山药");

        assertEquals("✅ 适宜食用", result.getSuitability());
        // 修改：检查是否包含核心文本（忽略前缀）
        assertTrue(result.getReason().contains("符合气虚质调养原则"));
        System.out.println(result);
    }

    @Test
    void testEvaluateBadFood() {
        ConstitutionType type = ConstitutionType.YANG_DEFICIENCY;
        ConstitutionType.SuitabilityResult result = type.evaluate("西瓜");

        assertEquals("❌ 不宜食用", result.getSuitability());
        // 修改：检查是否包含核心文本
        assertTrue(result.getReason().contains("可能加重体质偏颇"));
        System.out.println(result);
    }
    @Test
    void testEvaluateNeutralFood() {
        ConstitutionType type = ConstitutionType.PEACE;
        ConstitutionType.SuitabilityResult result = type.evaluate("米饭");

        assertEquals("⚖️ 中性", result.getSuitability());
        System.out.println(result);
    }

    @Test
    void testFromName() {
        assertEquals(ConstitutionType.PEACE, ConstitutionType.fromName("平和质"));
        assertEquals(ConstitutionType.QI_DEFICIENCY, ConstitutionType.fromName("气虚质"));
        assertEquals(ConstitutionType.PEACE, ConstitutionType.fromName("不存在的体质"));
    }

    @Test
    void testAllConstitutions() {
        for (ConstitutionType type : ConstitutionType.values()) {
            System.out.println("=== " + type.getName() + " ===");
            System.out.println("描述: " + type.getDescription());
            System.out.println("宜吃: " + String.join(", ", type.getGoodFoods()));
            System.out.println("忌吃: " + String.join(", ", type.getBadFoods()));
            System.out.println();

            assertNotNull(type.getGoodFoods());
            assertNotNull(type.getBadFoods());
            assertTrue(type.getGoodFoods().length > 0);
        }
    }
}
