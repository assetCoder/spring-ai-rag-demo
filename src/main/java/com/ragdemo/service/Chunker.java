package com.ragdemo.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档分块工具
 * 将长文档按段落/句子分割成小块，每块约500字符，重叠50字符
 */
public class Chunker {

    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 50;

    /**
     * 将文本分成适合嵌入的块
     */
    public static List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();

        // 先按双换行分段落
        String[] paragraphs = text.split("\n\n+");
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            // 如果段落本身超长，按句子切
            if (para.length() > CHUNK_SIZE) {
                // 先把当前累积的存起来
                if (!current.isEmpty()) {
                    chunks.add(current.toString().trim());
                    current = new StringBuilder();
                }
                // 长段落按句子切
                for (String sentence : splitSentences(para)) {
                    if (current.length() + sentence.length() > CHUNK_SIZE) {
                        chunks.add(current.toString().trim());
                        // 保留尾部重叠
                        String overlap = current.length() > OVERLAP
                                ? current.substring(current.length() - OVERLAP)
                                : "";
                        current = new StringBuilder(overlap);
                    }
                    current.append(sentence).append(" ");
                }
            } else {
                // 短段落，看累积是否超长
                if (current.length() + para.length() > CHUNK_SIZE) {
                    chunks.add(current.toString().trim());
                    // 保留尾部重叠
                    String overlap = current.length() > OVERLAP
                            ? current.substring(current.length() - OVERLAP)
                            : "";
                    current = new StringBuilder(overlap);
                }
                current.append(para).append("\n");
            }
        }

        // 最后一段
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }

    /**
     * 按句子分割（句号、问号、感叹号、换行）
     */
    private static List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        // 中文句号、英文句号、问号、感叹号
        String[] parts = text.split("(?<=[。！？.!?\\n])");
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                sentences.add(part);
            }
        }
        return sentences;
    }
}
