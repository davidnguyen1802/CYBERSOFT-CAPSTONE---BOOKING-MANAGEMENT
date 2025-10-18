package com.Cybersoft.Final_Capstone.util;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

public class UpdateHelper {

    /**
     * Copy các field != null từ src sang target NẾU giá trị khác hiện tại.
     */
    public static void copyNonNullChangedFields(Object src, Object target, String... excludeFields) {
        if (src == null || target == null) return;

        Set<String> excludes = new HashSet<>(Arrays.asList(excludeFields));
        int changed = 0;

        for (Field srcField : src.getClass().getDeclaredFields()) {
            String name = srcField.getName();
            if (excludes.contains(name)) continue;

            srcField.setAccessible(true);
            try {
                Object newVal = srcField.get(src);
                if (newVal == null) continue; // chỉ update khi có giá trị mới

                Field tgtField;
                try {
                    tgtField = target.getClass().getDeclaredField(name);
                } catch (NoSuchFieldException ignore) {
                    continue; // target không có field này -> bỏ qua
                }
                tgtField.setAccessible(true);
                Object oldVal = tgtField.get(target);

                if (isDifferent(newVal, oldVal, tgtField.getType())) {
                    tgtField.set(target, newVal);
                    changed++;
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** So sánh khác biệt với xử lý đặc biệt cho BigDecimal, mảng; còn lại dùng deepEquals */
    private static boolean isDifferent(Object newVal, Object oldVal, Class<?> targetType) {
        if (oldVal == null) return true; // cũ null, mới có giá trị -> khác

        // BigDecimal: dùng compareTo để bỏ qua scale (10.0 vs 10.00)
        if (newVal instanceof BigDecimal nv && oldVal instanceof BigDecimal ov) {
            return nv.compareTo(ov) != 0;
        }

        // Enum: dùng ==/equals là đủ
        if (targetType.isEnum()) {
            return !Objects.equals(newVal, oldVal);
        }

        // Mảng: deepEquals xử lý cả primitive/obj arrays
        if (newVal.getClass().isArray() || oldVal.getClass().isArray()) {
            return !Objects.deepEquals(wrapArray(newVal), wrapArray(oldVal));
        }

        // Collection/Map/String/LocalDateTime...: equals là đủ
        return !Objects.deepEquals(newVal, oldVal);
    }

    /** Gói phần tử mảng vào Object[] để Objects.deepEquals so sánh chuẩn */
    private static Object wrapArray(Object arr) {
        if (!arr.getClass().isArray()) return arr;
        int len = java.lang.reflect.Array.getLength(arr);
        Object[] boxed = new Object[len];
        for (int i = 0; i < len; i++) {
            boxed[i] = java.lang.reflect.Array.get(arr, i);
        }
        return boxed;
    }
}
