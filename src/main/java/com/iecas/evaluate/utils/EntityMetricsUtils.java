package com.iecas.evaluate.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iecas.evaluate.pojo.entity.EntityInfo;
import com.iecas.evaluate.pojo.entity.MetricsResult;
import com.iecas.evaluate.pojo.entity.SubMetricsResult;
import com.iecas.evaluate.pojo.entity.TextIdentifyEntity;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @Author: guo_x
 * @Date: 2025/5/28 15:41
 * @Description: 文本实体识别指标计算工具类
 */
@Slf4j
public class EntityMetricsUtils {


    /**
     * 计算实体级评估结果
     * @param truePath 真实实体文件路径
     * @param predPath 预测实体文件路径
     * @return 评估结果
     * lishuoke 11.81
     * lsk123456
     * 123456lsk
     */
    public static MetricsResult calculateMetrics(String truePath, String predPath){
        List<EntityInfo> truth = extraEntity(truePath);
        List<EntityInfo> pred = extraEntity(predPath);
        MetricsResult result = new MetricsResult();

        int TN = 0;
        try {
            // 计算TN
            TN = calculateTNFromFiles(predPath, truePath);
        } catch (IOException e){
            log.error("TN无法计算");
        }
        // 计算微平均指标
        SubMetricsResult microMetrics = computeMicroEntityMetrics(truth, pred);
        microMetrics.calculateAccuracy(TN);
        result.setMicro(microMetrics);

        // 计算宏平均指标
        SubMetricsResult macroMetrics = computeMacroEntityMetrics(truth, pred);
        result.setMacro(macroMetrics);

        macroMetrics.setFP(microMetrics.getFP());
        macroMetrics.setTP(microMetrics.getTP());
        macroMetrics.setFN(microMetrics.getFN());
        macroMetrics.calculateAccuracy(TN);

        // 计算每个类别的指标
        List<SubMetricsResult> preClassResult = computePerClassMetricsFast(truth, pred);
        result.setPreClassResult(preClassResult);

        return result;
    }


    /**
     * 计算轻量级评估结果
     * @param truePath
     * @param predPath
     * @return
     */
    public static SubMetricsResult calculateLightMetrics(String truePath, String predPath){
        List<EntityInfo> truth = extraEntity(truePath);
        List<EntityInfo> pred = extraEntity(predPath);

        int TN = 0;
        try {
            // 计算TN
            TN = calculateTNFromFiles(predPath, truePath);
        } catch (IOException e){
            log.error("TN无法计算");
        }
        // 计算微平均指标
        SubMetricsResult microMetrics = computeMicroEntityMetrics(truth, pred);
        microMetrics.calculateAccuracy(TN);

        // 计算宏平均指标
        SubMetricsResult macroMetrics = computeMacroEntityMetrics(truth, pred);

        macroMetrics.setFP(microMetrics.getFP());
        macroMetrics.setTP(microMetrics.getTP());
        macroMetrics.setFN(microMetrics.getFN());
        macroMetrics.calculateAccuracy(TN);

        return macroMetrics;
    }


    /**
     * 提取实体
     * @param filePath 实体文件路径
     * @return 实体集合
     */
    public static List<EntityInfo> extraEntity(String filePath) {
        List<EntityInfo> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            StringBuilder entityBuilder = new StringBuilder();
            String currentLabel = null;
            long startIdx = 0;
            long currentIdx = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    currentIdx = 0; // 新句子开始
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length != 2) continue;
                String word = parts[0];
                String label = parts[1];

                if (label.startsWith("B-")) {
                    // 如果正在处理实体，则先关闭前一个
                    if (entityBuilder.length() > 0) {
                        result.add(new EntityInfo(entityBuilder.toString(), currentLabel, startIdx, currentIdx - 1));
                        entityBuilder.setLength(0);
                    }
                    entityBuilder.append(word);
                    currentLabel = label.substring(2);
                    startIdx = currentIdx;
                } else if (label.startsWith("I-") && entityBuilder.length() > 0 && label.endsWith(currentLabel)) {
                    entityBuilder.append(word);
                } else {
                    // 当前是O，或者实体断了
                    if (entityBuilder.length() > 0) {
                        result.add(new EntityInfo(entityBuilder.toString(), currentLabel, startIdx, currentIdx - 1));
                        entityBuilder.setLength(0);
                        currentLabel = null;
                    }
                }

                currentIdx++;
            }

            // 文件结尾处理遗留实体
            if (entityBuilder.length() > 0) {
                result.add(new EntityInfo(entityBuilder.toString(), currentLabel, startIdx, currentIdx - 1));
            }

        } catch (IOException e) {
            log.error("读取文件错误", e);
            throw new RuntimeException(e.getMessage());
        }

        return result;
    }


    /**
     * 从两个BIO标签文件中读取数据并计算TN数量
     * @param predFilePath 预测标签文件路径
     * @param trueFilePath 真实标签文件路径
     * @return TN数量
     * @throws IOException 文件读取异常
     */
    public static int calculateTNFromFiles(String predFilePath, String trueFilePath) throws IOException {
        List<String> predLabels = Files.readAllLines(Paths.get(predFilePath));
        List<String> trueLabels = Files.readAllLines(Paths.get(trueFilePath));

        if (predLabels.size() != trueLabels.size()) {
            // 如果长度不一致，则说明有问题，则随便返回一个值
            log.error("文本token长度不一致，无法计算TN, TN按0计算");
            return 0;
        }

        int tn = 0;
        for (int i = 0; i < predLabels.size(); i++) {
            String pred = predLabels.get(i).trim();
            String truth = trueLabels.get(i).trim();

            // 跳过空行（句子分隔）
            if (pred.isEmpty() && truth.isEmpty()) {
                continue;
            }

            if ("O".equals(pred) && "O".equals(truth)) {
                tn++;
            }
        }
        return tn;
    }


    /**
     * 判断两个实体是否严格匹配
     * @param a 实体a
     * @param b 实体b
     * @return 是否匹配
     */
    private static boolean isExactMatch(EntityInfo a, EntityInfo b) {
        return a.getStart() == b.getStart()
                && a.getEnd() == b.getEnd()
                && a.getEntityClazz().equals(b.getEntityClazz());
    }


    /**
     * 计算实体级评估结果 -- 微平均
     * @param trueEntities 真实实体
     * @param predEntities 预测实体
     */
    public static SubMetricsResult computeMicroEntityMetrics(List<EntityInfo> trueEntities, List<EntityInfo> predEntities) {
        SubMetricsResult result = new SubMetricsResult();
        int TP = 0;
        int FP = 0;
        int FN = 0;

        // 用于记录哪些真实实体已经被匹配过，避免重复匹配
        boolean[] matched = new boolean[trueEntities.size()];

        // 遍历每一个预测实体，尝试匹配真实实体
        for (EntityInfo pred : predEntities) {
            boolean foundMatch = false;
            for (int i = 0; i < trueEntities.size(); i++) {
                if (!matched[i] && isExactMatch(pred, trueEntities.get(i))) {
                    TP++;
                    matched[i] = true;
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                FP++; // 没匹配到任何真实实体
            }
        }

        // 所有未匹配到的真实实体即为 FN
        for (boolean m : matched) {
            if (!m) FN++;
        }

        double precision = TP + FP == 0 ? 0.0 : (double) TP / (TP + FP);
        double recall = TP + FN == 0 ? 0.0 : (double) TP / (TP + FN);
        double f1 = precision + recall == 0 ? 0.0 : 2 * precision * recall / (precision + recall);

        result.setFN(FN);
        result.setTP(TP);
        result.setFP(FP);
        result.setPrecision(precision);
        result.setRecall(recall);
        result.setF1(f1);

        return result;
    }


    /**
     * 计算实体级评估结果 -- 宏平均
     * @param trueEntities 真实实体
     * @param predEntities 预测实体
     */
    public static SubMetricsResult computeMacroEntityMetrics(List<EntityInfo> trueEntities, List<EntityInfo> predEntities) {

        SubMetricsResult result = new SubMetricsResult();

        Map<String, Integer> TP = new HashMap<>();
        Map<String, Integer> FP = new HashMap<>();
        Map<String, Integer> FN = new HashMap<>();

        // 标记哪些真实实体已匹配，按类别分组
        Map<String, List<Boolean>> matched = new HashMap<>();
        Map<String, List<EntityInfo>> trueByClass = new HashMap<>();
        for (EntityInfo ent : trueEntities) {
            trueByClass.computeIfAbsent(ent.getEntityClazz(), k -> new ArrayList<>()).add(ent);
        }
        for (Map.Entry<String, List<EntityInfo>> entry : trueByClass.entrySet()) {
            List<Boolean> flags = new ArrayList<>(Collections.nCopies(entry.getValue().size(), false));
            matched.put(entry.getKey(), flags);
        }

        // 处理预测实体
        for (EntityInfo pred : predEntities) {
            String clazz = pred.getEntityClazz();
            boolean found = false;
            if (trueByClass.containsKey(clazz)) {
                List<EntityInfo> goldList = trueByClass.get(clazz);
                List<Boolean> flagList = matched.get(clazz);
                for (int i = 0; i < goldList.size(); i++) {
                    if (!flagList.get(i) && isExactMatch(pred, goldList.get(i))) {
                        TP.put(clazz, TP.getOrDefault(clazz, 0) + 1);
                        flagList.set(i, true);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                FP.put(clazz, FP.getOrDefault(clazz, 0) + 1);
            }
        }

        // 统计 FN（漏检）
        for (String clazz : trueByClass.keySet()) {
            List<Boolean> flags = matched.get(clazz);
            int fnCount = 0;
            for (boolean m : flags) {
                if (!m) fnCount++;
            }
            FN.put(clazz, fnCount);
        }

        // 计算每类 Precision, Recall, F1
        double sumPrecision = 0, sumRecall = 0, sumF1 = 0;
        int classCount = 0;

        for (String clazz : trueByClass.keySet()) {
            int tp = TP.getOrDefault(clazz, 0);
            int fp = FP.getOrDefault(clazz, 0);
            int fn = FN.getOrDefault(clazz, 0);

            double precision = tp + fp == 0 ? 0.0 : (double) tp / (tp + fp);
            double recall = tp + fn == 0 ? 0.0 : (double) tp / (tp + fn);
            double f1 = precision + recall == 0 ? 0.0 : 2 * precision * recall / (precision + recall);

            sumPrecision += precision;
            sumRecall += recall;
            sumF1 += f1;
            classCount++;
        }
        result.setPrecision(sumPrecision / classCount);
        result.setRecall(sumRecall / classCount);
        result.setF1(sumF1 / classCount);
        return result;
    }



    /**
     * 计算每个类别的评估结果
     * @param trueEntities 真实实体
     * @param predEntities 预测实体
     */
    public static List<SubMetricsResult> computePerClassMetrics(List<EntityInfo> trueEntities, List<EntityInfo> predEntities) {
        List<SubMetricsResult> resultList = new ArrayList<>();

        Set<String> allClasses = new HashSet<>();

        for (EntityInfo e : trueEntities) allClasses.add(e.getEntityClazz());
        for (EntityInfo e : predEntities) allClasses.add(e.getEntityClazz());

        Map<String, Integer> TP = new HashMap<>();
        Map<String, Integer> FP = new HashMap<>();
        Map<String, Integer> FN = new HashMap<>();

        // 初始化计数
        for (String clazz : allClasses) {
            TP.put(clazz, 0);
            FP.put(clazz, 0);
            FN.put(clazz, 0);
        }

        boolean[] matched = new boolean[trueEntities.size()];

        // 判断预测中哪些匹配了真实值
        for (EntityInfo pred : predEntities) {
            boolean found = false;
            for (int i = 0; i < trueEntities.size(); i++) {
                EntityInfo truth = trueEntities.get(i);
                if (!matched[i] && isExactMatch(pred, truth)) {
                    if (pred.getEntityClazz().equals(truth.getEntityClazz())) {
                        TP.put(pred.getEntityClazz(), TP.get(pred.getEntityClazz()) + 1);
                        matched[i] = true;
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                // 预测错了或预测了根本不存在的实体
                FP.put(pred.getEntityClazz(), FP.get(pred.getEntityClazz()) + 1);
            }
        }

        // 统计 FN
        for (int i = 0; i < trueEntities.size(); i++) {
            if (!matched[i]) {
                String clazz = trueEntities.get(i).getEntityClazz();
                FN.put(clazz, FN.get(clazz) + 1);
            }
        }

        // 输出各类别指标
        for (String clazz : allClasses) {
            SubMetricsResult subResult = new SubMetricsResult();
            int tp = TP.get(clazz);
            int fp = FP.get(clazz);
            int fn = FN.get(clazz);

            double precision = tp + fp == 0 ? 0.0 : tp * 1.0 / (tp + fp);
            double recall = tp + fn == 0 ? 0.0 : tp * 1.0 / (tp + fn);
            double f1 = precision + recall == 0 ? 0.0 : 2 * precision * recall / (precision + recall);

            subResult.setTP(tp);
            subResult.setFP(fp);
            subResult.setFN(fn);
            subResult.setClazz(clazz);
            subResult.setPrecision(precision);
            subResult.setRecall(recall);
            subResult.setF1(f1);

            resultList.add(subResult);
        }
        return resultList;
    }


    /**
     * 快速计算每个类级别的评估结果
     * @param trueEntities 真实实体
     * @param predEntities 预测实体
     */
    public static List<SubMetricsResult> computePerClassMetricsFast(List<EntityInfo> trueEntities, List<EntityInfo> predEntities) {
        List<SubMetricsResult> resultList = new ArrayList<>();

        Set<String> allClasses = new HashSet<>();
        for (EntityInfo e : trueEntities) allClasses.add(e.getEntityClazz());
        for (EntityInfo e : predEntities) allClasses.add(e.getEntityClazz());

        Map<String, Integer> TP = new HashMap<>();
        Map<String, Integer> FP = new HashMap<>();
        Map<String, Integer> FN = new HashMap<>();

        for (String clazz : allClasses) {
            TP.put(clazz, 0);
            FP.put(clazz, 0);
            FN.put(clazz, 0);
        }

        // 用于快速查找真实实体是否存在
        Map<String, EntityInfo> truthMap = new HashMap<>();
        Set<String> matchedKeys = new HashSet<>();

        for (EntityInfo truth : trueEntities) {
            String key = truth.getStart() + "-" + truth.getEnd() + "-" + truth.getEntityClazz();
            truthMap.put(key, truth);
        }

        for (EntityInfo pred : predEntities) {
            String key = pred.getStart() + "-" + pred.getEnd() + "-" + pred.getEntityClazz();
            if (truthMap.containsKey(key) && !matchedKeys.contains(key)) {
                // 真值中存在此实体，且尚未匹配
                TP.put(pred.getEntityClazz(), TP.get(pred.getEntityClazz()) + 1);
                matchedKeys.add(key);
            } else {
                // 错误预测
                FP.put(pred.getEntityClazz(), FP.get(pred.getEntityClazz()) + 1);
            }
        }

        // 统计 FN
        for (String key : truthMap.keySet()) {
            if (!matchedKeys.contains(key)) {
                String clazz = truthMap.get(key).getEntityClazz();
                FN.put(clazz, FN.get(clazz) + 1);
            }
        }

        // 输出各类别指标
        for (String clazz : allClasses) {
            SubMetricsResult subResult = new SubMetricsResult();
            int tp = TP.get(clazz);
            int fp = FP.get(clazz);
            int fn = FN.get(clazz);

            double precision = tp + fp == 0 ? 0.0 : tp * 1.0 / (tp + fp);
            double recall = tp + fn == 0 ? 0.0 : tp * 1.0 / (tp + fn);
            double f1 = precision + recall == 0 ? 0.0 : 2 * precision * recall / (precision + recall);

            subResult.setTP(tp);
            subResult.setFN(fn);
            subResult.setFP(fp);

            subResult.setClazz(clazz);
            subResult.setPrecision(precision);
            subResult.setRecall(recall);
            subResult.setF1(f1);

            resultList.add(subResult);
        }
        return resultList;
    }


    /**
     *  @author: getao
     *  @Date: 2025/6/11 14:43
     *  @Description: 获取空天模型文本单一维度评估结果
     */
    public static SubMetricsResult calculateIecasTextMetrics(String prePath, String gtPath) {
        List<TextIdentifyEntity> predictedEntities = fileToEntity(prePath);
        List<TextIdentifyEntity> actualEntities = fileToEntity(gtPath);

        if (predictedEntities == null || actualEntities == null) {
            return null;
        }

        // 使用 Set 来存储实际和预测的实体，方便进行去重和对比
        Set<TextIdentifyEntity> predictedSet = new HashSet<>(predictedEntities);
        Set<TextIdentifyEntity> actualSet = new HashSet<>(actualEntities);

        // 计算 TP, FP, FN
        int TP = 0; // 真正例
        int FP = 0; // 假正例
        int FN = 0; // 假负例
        int TN = 0; // 真负例

        // 计算 TP 和 FP
        for (TextIdentifyEntity predicted : predictedSet) {
            if (actualSet.contains(predicted)) {
                TP++; // 如果预测和实际都包含这个实体，是 TP
            } else {
                FP++; // 如果预测有该实体，但实际没有，是 FP
            }
        }

        // 计算 FN
        for (TextIdentifyEntity actual : actualSet) {
            if (!predictedSet.contains(actual)) {
                FN++; // 如果实际有该实体，但预测没有，是 FN
            }
        }

        // 计算 TN (总的实体数量减去 TP, FP, FN)
        // 假设 TN 可以通过排除 TP, FP, FN 来推算
        TN = (predictedSet.size() + actualSet.size()) - TP - FP - FN;

        // 计算结果
        double precision = TP + FP == 0 ? 0.0 : (double) TP / (TP + FP);
        double recall = TP + FN == 0 ? 0.0 : (double) TP / (TP + FN);
        double f1 = precision + recall == 0 ? 0.0 : 2 * precision * recall / (precision + recall);

        SubMetricsResult result = new SubMetricsResult();
        result.setTN(TN);
        result.setFN(FN);
        result.setTP(TP);
        result.setFP(FP);
        result.setPrecision(precision);
        result.setRecall(recall);
        result.setF1(f1);
        result.calculateAccuracy(TN);

        return result;
    }


    public static List<TextIdentifyEntity> fileToEntity(String filePath) {
        List<TextIdentifyEntity> entities = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            entities = objectMapper.readValue(new File(filePath), objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, TextIdentifyEntity.class));
        } catch (IOException e) {
            e.printStackTrace();
            log.error("文件 {} 转换内容为Entity时出现错误...", filePath);
        }
        return entities;
    }
}
