package com.yupi.usercenter.utils;

import java.util.List;

/**
 * 算法工具：编辑距离
 * https://leetcode.cn/problems/edit-distance/
 *
 */
public class AlogrithmUtils {

    /**
     * 根据标签列表进行匹配
     * @param tagList1
     * @param tagList2
     * @return
     */
    public static int minDistanceTagList(List<String> tagList1, List<String> tagList2) {
        int n = tagList1.size();
        int m = tagList2.size();
        int[] f = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            f[j] = j;
        }

        for (int i = 0; i < n; i++) {
            int pre = f[0];
            f[0] = i + 1;
            for (int j = 0; j < m; j++) {
                int tmp = f[j + 1];
                if (tagList1.get(i).equals(tagList2.get(j))) {
                    f[j + 1] = pre;
                } else {
                    f[j + 1] = Math.min(Math.min(pre, tmp), f[j]) + 1;
                }
                pre = tmp;
            }
        }

        return f[m];
    }


    /**
     * 根据两个字符串进行匹配
     * @param word1
     * @param word2
     * @return
     */
    public static int minDistance(String word1, String word2) {
        int n = word1.length(), m = word2.length();
        char[] s = word1.toCharArray(), t = word2.toCharArray();
        int[] f = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            f[j] = j;
        }

        for (int i = 0; i < n; i++) {
            int pre = f[0];
            f[0] = i + 1;
            for (int j = 0; j < m; j++) {
                int tmp = f[j + 1];
                if (s[i] == t[j]) {
                    f[j + 1] = pre;
                } else {
                    f[j + 1] = Math.min(Math.min(pre, tmp), f[j]) + 1;
                }
                pre = tmp;
            }
        }

        return f[m];
    }
}
