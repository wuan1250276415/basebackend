package com.basebackend.logging.statistics.predictor;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 趋势预测器
 *
 * 提供多种趋势预测算法：
 * - 线性回归预测
 * - 移动平均预测
 * - 指数平滑预测
 * - 复合预测（组合多种算法）
 * - 置信区间计算
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class TrendPredictor {

    /**
     * 使用线性回归预测未来值
     *
     * @param historicalData 历史数据 (timestamp -> value)
     * @param steps          预测步数
     * @return 预测结果
     */
    public PredictionResult predictLinearRegression(Map<Long, Double> historicalData, int steps) {
        if (historicalData == null || historicalData.size() < 2 || steps <= 0) {
            return createEmptyPrediction("线性回归", steps);
        }

        List<Map.Entry<Long, Double>> sortedData = historicalData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        // 计算回归系数
        LinearRegressionResult regression = calculateLinearRegression(sortedData);

        // 生成预测
        List<PredictedPoint> predictions = new ArrayList<>();
        long lastTimestamp = sortedData.get(sortedData.size() - 1).getKey();
        long interval = calculateAverageInterval(sortedData);

        double rSquared = calculateRSquared(sortedData, regression);

        for (int i = 1; i <= steps; i++) {
            long futureTimestamp = lastTimestamp + (interval * i);
            double predictedValue = regression.slope * (sortedData.size() + i - 1) + regression.intercept;
            double confidence = calculateConfidence(rSquared, i, sortedData.size());

            predictions.add(PredictedPoint.builder()
                    .timestamp(futureTimestamp)
                    .value(predictedValue)
                    .confidence(confidence)
                    .build());
        }

        log.debug("线性回归预测完成: steps={}, R²={}", steps, rSquared);

        return PredictionResult.builder()
                .method("线性回归")
                .predictions(predictions)
                .rSquared(rSquared)
                .slope(regression.slope)
                .intercept(regression.intercept)
                .meanAbsoluteError(regression.meanAbsoluteError)
                .build();
    }

    /**
     * 使用移动平均预测
     *
     * @param values     值序列
     * @param windowSize 窗口大小
     * @param steps      预测步数
     * @return 预测结果
     */
    public PredictionResult predictMovingAverage(List<Double> values, int windowSize, int steps) {
        if (values == null || values.size() < windowSize || steps <= 0) {
            return createEmptyPrediction("移动平均", steps);
        }

        List<PredictedPoint> predictions = new ArrayList<>();
        List<Double> smoothedValues = new ArrayList<>(values);

        for (int i = 0; i < steps; i++) {
            int startIndex = Math.max(0, smoothedValues.size() - windowSize);
            double average = smoothedValues.subList(startIndex, smoothedValues.size()).stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            predictions.add(PredictedPoint.builder()
                    .timestamp(System.currentTimeMillis() + i * 1000) // 假设1秒间隔
                    .value(average)
                    .confidence(calculateMovingAverageConfidence(values.size(), i))
                    .build());

            smoothedValues.add(average);
        }

        log.debug("移动平均预测完成: window={}, steps={}", windowSize, steps);

        return PredictionResult.builder()
                .method("移动平均")
                .predictions(predictions)
                .windowSize(windowSize)
                .build();
    }

    /**
     * 使用指数平滑预测
     *
     * @param values 值序列
     * @param alpha  平滑参数 (0-1)
     * @param steps  预测步数
     * @return 预测结果
     */
    public PredictionResult predictExponentialSmoothing(List<Double> values, double alpha, int steps) {
        if (values == null || values.isEmpty() || steps <= 0) {
            return createEmptyPrediction("指数平滑", steps);
        }

        if (alpha <= 0 || alpha > 1) {
            alpha = 0.3; // 默认值
        }

        // 计算指数平滑值
        double smoothed = values.get(0);
        List<Double> smoothedSeries = new ArrayList<>();
        smoothedSeries.add(smoothed);

        for (int i = 1; i < values.size(); i++) {
            smoothed = alpha * values.get(i) + (1 - alpha) * smoothed;
            smoothedSeries.add(smoothed);
        }

        // 生成预测
        List<PredictedPoint> predictions = new ArrayList<>();
        double lastSmoothed = smoothedSeries.get(smoothedSeries.size() - 1);

        for (int i = 1; i <= steps; i++) {
            predictions.add(PredictedPoint.builder()
                    .timestamp(System.currentTimeMillis() + i * 1000)
                    .value(lastSmoothed) // 指数平滑假设未来值保持当前平滑值
                    .confidence(calculateExponentialSmoothingConfidence(values.size(), i, alpha))
                    .build());
        }

        log.debug("指数平滑预测完成: alpha={}, steps={}", alpha, steps);

        return PredictionResult.builder()
                .method("指数平滑")
                .predictions(predictions)
                .alpha(alpha)
                .build();
    }

    /**
     * 复合预测（组合多种算法）
     *
     * @param historicalData 历史数据
     * @param steps          预测步数
     * @return 复合预测结果
     */
    public CompositePredictionResult predictComposite(Map<Long, Double> historicalData, int steps) {
        if (historicalData == null || historicalData.size() < 3 || steps <= 0) {
            return CompositePredictionResult.builder()
                    .method("复合预测")
                    .predictions(Collections.emptyList())
                    .build();
        }

        List<Double> values = historicalData.values().stream()
                .sorted()
                .collect(Collectors.toList());

        // 使用多种算法预测
        PredictionResult linearResult = predictLinearRegression(historicalData, steps);
        PredictionResult movingAvgResult = predictMovingAverage(values, Math.min(5, values.size()), steps);
        PredictionResult expSmoothingResult = predictExponentialSmoothing(values, 0.3, steps);

        // 组合预测 (简单平均)
        List<PredictedPoint> compositePredictions = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            double linearValue = i < linearResult.getPredictions().size()
                    ? linearResult.getPredictions().get(i).getValue()
                    : 0;
            double movingAvgValue = i < movingAvgResult.getPredictions().size()
                    ? movingAvgResult.getPredictions().get(i).getValue()
                    : 0;
            double expSmoothingValue = i < expSmoothingResult.getPredictions().size()
                    ? expSmoothingResult.getPredictions().get(i).getValue()
                    : 0;

            double compositeValue = (linearValue + movingAvgValue + expSmoothingValue) / 3.0;
            double compositeConfidence = (linearResult.getPredictions().get(i).getConfidence()
                    + movingAvgResult.getPredictions().get(i).getConfidence()
                    + expSmoothingResult.getPredictions().get(i).getConfidence()) / 3.0;

            compositePredictions.add(PredictedPoint.builder()
                    .timestamp(linearResult.getPredictions().get(i).getTimestamp())
                    .value(compositeValue)
                    .confidence(compositeConfidence)
                    .build());
        }

        log.debug("复合预测完成: steps={}", steps);

        return CompositePredictionResult.builder()
                .method("复合预测")
                .predictions(compositePredictions)
                .componentResults(Arrays.asList(linearResult, movingAvgResult, expSmoothingResult))
                .build();
    }

    /**
     * 计算置信区间
     *
     * @param predictions     预测点
     * @param confidenceLevel 置信水平 (0.95 表示95%)
     * @return 带置信区间的预测
     */
    public List<PredictionInterval> calculateConfidenceIntervals(
            List<PredictedPoint> predictions, double confidenceLevel) {
        if (predictions == null || predictions.isEmpty()) {
            return Collections.emptyList();
        }

        return predictions.stream()
                .map(point -> {
                    double marginOfError = calculateMarginOfError(
                            point.getConfidence(), confidenceLevel);
                    double lower = point.getValue() - marginOfError;
                    double upper = point.getValue() + marginOfError;

                    return PredictionInterval.builder()
                            .timestamp(point.getTimestamp())
                            .predictedValue(point.getValue())
                            .lowerBound(lower)
                            .upperBound(upper)
                            .confidenceLevel(confidenceLevel)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==================== 私有辅助方法 ====================

    private PredictionResult createEmptyPrediction(String method, int steps) {
        return PredictionResult.builder()
                .method(method)
                .predictions(Collections.emptyList())
                .build();
    }

    private long calculateAverageInterval(List<Map.Entry<Long, Double>> sortedData) {
        if (sortedData.size() < 2)
            return 1000; // 默认1秒

        long totalInterval = 0;
        for (int i = 1; i < sortedData.size(); i++) {
            totalInterval += sortedData.get(i).getKey() - sortedData.get(i - 1).getKey();
        }
        return totalInterval / (sortedData.size() - 1);
    }

    private LinearRegressionResult calculateLinearRegression(List<Map.Entry<Long, Double>> data) {
        int n = data.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = data.get(i).getValue();

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // 计算平均绝对误差
        double mae = 0;
        for (int i = 0; i < n; i++) {
            double predicted = slope * i + intercept;
            mae += Math.abs(data.get(i).getValue() - predicted);
        }
        mae /= n;

        return LinearRegressionResult.builder()
                .slope(slope)
                .intercept(intercept)
                .meanAbsoluteError(mae)
                .build();
    }

    private double calculateRSquared(List<Map.Entry<Long, Double>> data, LinearRegressionResult regression) {
        double sumY = data.stream().mapToDouble(Map.Entry::getValue).sum();
        double meanY = sumY / data.size();

        double ssTot = 0; // 总平方和
        double ssRes = 0; // 残差平方和

        for (int i = 0; i < data.size(); i++) {
            double actual = data.get(i).getValue();
            double predicted = regression.slope * i + regression.intercept;

            ssTot += Math.pow(actual - meanY, 2);
            ssRes += Math.pow(actual - predicted, 2);
        }

        return ssTot == 0 ? 0 : 1 - (ssRes / ssTot);
    }

    private double calculateConfidence(double rSquared, int step, int dataSize) {
        // 置信度随步数增加而降低
        double baseConfidence = Math.max(0, Math.min(1, rSquared));
        double decayFactor = Math.pow(0.9, step); // 每步衰减10%
        double sizeFactor = Math.min(1.0, dataSize / 100.0); // 数据量因子

        return baseConfidence * decayFactor * sizeFactor;
    }

    private double calculateMovingAverageConfidence(int dataSize, int step) {
        double baseConfidence = Math.min(1.0, dataSize / 50.0);
        return baseConfidence * Math.pow(0.85, step);
    }

    private double calculateExponentialSmoothingConfidence(int dataSize, int step, double alpha) {
        double baseConfidence = Math.min(1.0, dataSize / 30.0);
        double alphaFactor = Math.max(0.3, alpha); // 高alpha有更好的短期预测
        return baseConfidence * alphaFactor * Math.pow(0.8, step);
    }

    private double calculateMarginOfError(double confidence, double confidenceLevel) {
        // 简化的误差边界计算
        double zScore = confidenceLevel == 0.95 ? 1.96 : 2.58; // 95% 或 99%
        return zScore * (1 - confidence);
    }

    // ==================== 数据模型 ====================

    /**
     * 线性回归结果
     */
    public static class LinearRegressionResult {
        private double slope;
        private double intercept;
        private double meanAbsoluteError;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private LinearRegressionResult result = new LinearRegressionResult();

            public Builder slope(double slope) {
                result.slope = slope;
                return this;
            }

            public Builder intercept(double intercept) {
                result.intercept = intercept;
                return this;
            }

            public Builder meanAbsoluteError(double mae) {
                result.meanAbsoluteError = mae;
                return this;
            }

            public LinearRegressionResult build() {
                return result;
            }
        }
    }

    /**
     * 预测点
     */
    @lombok.Data
    public static class PredictedPoint {
        private long timestamp;
        private double value;
        private double confidence; // 0-1

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private PredictedPoint point = new PredictedPoint();

            public Builder timestamp(long timestamp) {
                point.timestamp = timestamp;
                return this;
            }

            public Builder value(double value) {
                point.value = value;
                return this;
            }

            public Builder confidence(double confidence) {
                point.confidence = confidence;
                return this;
            }

            public PredictedPoint build() {
                return point;
            }
        }
    }

    /**
     * 预测结果
     */
    @lombok.Data
    public static class PredictionResult {
        private String method;
        private List<PredictedPoint> predictions;
        private double rSquared;
        private double slope;
        private double intercept;
        private double meanAbsoluteError;
        private int windowSize;
        private double alpha;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private PredictionResult result = new PredictionResult();

            public Builder method(String method) {
                result.method = method;
                return this;
            }

            public Builder predictions(List<PredictedPoint> predictions) {
                result.predictions = predictions;
                return this;
            }

            public Builder rSquared(double rSquared) {
                result.rSquared = rSquared;
                return this;
            }

            public Builder slope(double slope) {
                result.slope = slope;
                return this;
            }

            public Builder intercept(double intercept) {
                result.intercept = intercept;
                return this;
            }

            public Builder meanAbsoluteError(double mae) {
                result.meanAbsoluteError = mae;
                return this;
            }

            public Builder windowSize(int size) {
                result.windowSize = size;
                return this;
            }

            public Builder alpha(double alpha) {
                result.alpha = alpha;
                return this;
            }

            public PredictionResult build() {
                return result;
            }
        }
    }

    /**
     * 复合预测结果
     */
    @lombok.Data
    public static class CompositePredictionResult {
        private String method;
        private List<PredictedPoint> predictions;
        private List<PredictionResult> componentResults;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CompositePredictionResult result = new CompositePredictionResult();

            public Builder method(String method) {
                result.method = method;
                return this;
            }

            public Builder predictions(List<PredictedPoint> predictions) {
                result.predictions = predictions;
                return this;
            }

            public Builder componentResults(List<PredictionResult> results) {
                result.componentResults = results;
                return this;
            }

            public CompositePredictionResult build() {
                return result;
            }
        }
    }

    /**
     * 预测区间
     */
    @lombok.Data
    public static class PredictionInterval {
        private long timestamp;
        private double predictedValue;
        private double lowerBound;
        private double upperBound;
        private double confidenceLevel;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private PredictionInterval interval = new PredictionInterval();

            public Builder timestamp(long timestamp) {
                interval.timestamp = timestamp;
                return this;
            }

            public Builder predictedValue(double value) {
                interval.predictedValue = value;
                return this;
            }

            public Builder lowerBound(double lower) {
                interval.lowerBound = lower;
                return this;
            }

            public Builder upperBound(double upper) {
                interval.upperBound = upper;
                return this;
            }

            public Builder confidenceLevel(double level) {
                interval.confidenceLevel = level;
                return this;
            }

            public PredictionInterval build() {
                return interval;
            }
        }
    }
}
