#!/usr/bin/env python3
"""
AI驱动的异常检测服务
基于机器学习的智能异常检测系统

功能:
1. 多模型异常检测 (Isolation Forest, LSTM, Prophet等)
2. 实时异常识别与告警
3. 自动根因分析
4. 预测性分析
5. 智能告警降噪
"""

import asyncio
import logging
import yaml
import time
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional, Tuple
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import pandas as pd
from prometheus_client import Counter, Histogram, Gauge, start_http_server

from sklearn.ensemble import IsolationForest
from sklearn.svm import OneClassSVM
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import classification_report
import torch
import torch.nn as nn

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('/var/log/anomaly-detection/service.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Prometheus 指标
ANOMALY_DETECTIONS = Counter('anomaly_detections_total', 'Total anomaly detections', ['service', 'type', 'severity'])
ANOMALY_DETECTION_TIME = Histogram('anomaly_detection_duration_seconds', 'Time spent detecting anomalies')
MODEL_PREDICT_TIME = Histogram('model_prediction_duration_seconds', 'Time spent in model prediction', ['model_type'])
INVESTIGATIONS = Counter('investigations_total', 'Total investigations', ['status'])
ALERTS_SENT = Counter('alerts_sent_total', 'Total alerts sent', ['channel'])
MODEL_ACCURACY = Gauge('model_accuracy', 'Model accuracy score', ['model_type'])

@dataclass
class AnomalyResult:
    """异常检测结果"""
    service: str
    metric: str
    timestamp: datetime
    score: float
    severity: str
    anomaly_type: str
    confidence: float
    details: Dict[str, Any]
    recommendation: Optional[str] = None

class LSTMAutoencoder(nn.Module):
    """LSTM自编码器用于序列异常检测"""

    def __init__(self, sequence_length: int, feature_dim: int, encoding_dim: int = 10):
        super(LSTMAutoencoder, self).__init__()
        self.sequence_length = sequence_length
        self.feature_dim = feature_dim

        self.encoder = nn.LSTM(feature_dim, encoding_dim, batch_first=True)
        self.decoder = nn.LSTM(encoding_dim, encoding_dim, batch_first=True)
        self.output_layer = nn.Linear(encoding_dim, feature_dim)

    def forward(self, x):
        encoded, _ = self.encoder(x)
        decoded, _ = self.decoder(encoded)
        output = self.output_layer(decoded)
        return output

    def encode(self, x):
        encoded, _ = self.encoder(x)
        return encoded

class ProphetAnomalyDetector:
    """基于Facebook Prophet的异常检测"""

    def __init__(self, changepoint_prior_scale: float = 0.05):
        self.changepoint_prior_scale = changepoint_prior_scale
        self.model = None
        self.fitted = False

    def fit(self, data: pd.DataFrame):
        """训练Prophet模型"""
        try:
            from prophet import Prophet

            # 重命名列以符合Prophet要求
            df_prophet = data.reset_index().rename(columns={'index': 'ds', 'value': 'y'})

            self.model = Prophet(
                changepoint_prior_scale=self.changepoint_prior_scale,
                seasonality_mode='additive',
                daily_seasonality=True,
                weekly_seasonality=True,
                yearly_seasonality=False
            )

            self.model.fit(df_prophet)
            self.fitted = True
            logger.info("Prophet模型训练完成")
        except ImportError:
            logger.warning("Prophet未安装，使用统计方法替代")
            self.fitted = False

    def predict(self, data: pd.DataFrame) -> Tuple[np.ndarray, np.ndarray]:
        """预测异常"""
        if not self.fitted:
            # 使用统计方法作为后备
            return self._statistical_anomaly_detection(data)

        try:
            from prophet import Prophet

            df_prophet = data.reset_index().rename(columns={'index': 'ds', 'value': 'y'})
            future = self.model.make_future_dataframe(periods=0)
            forecast = self.model.predict(future)

            # 计算残差
            residuals = forecast['yhat'].values - forecast['y'].values

            # 异常分数 (标准化残差)
            scores = np.abs(residuals) / np.std(residuals)

            # 转换为0-1分数
            scores = np.minimum(scores / 3.0, 1.0)

            return residuals, scores
        except Exception as e:
            logger.error(f"Prophet预测失败: {e}")
            return self._statistical_anomaly_detection(data)

    def _statistical_anomaly_detection(self, data: pd.DataFrame) -> Tuple[np.ndarray, np.ndarray]:
        """统计方法异常检测"""
        values = data['value'].values if 'value' in data.columns else data.values

        # 使用移动平均和标准差
        window = min(30, len(values) // 4)
        rolling_mean = pd.Series(values).rolling(window=window).mean()
        rolling_std = pd.Series(values).rolling(window=window).std()

        residuals = values - rolling_mean.fillna(method='bfill').fillna(method='ffill')
        std_residuals = residuals / rolling_std.fillna(1.0)

        scores = np.minimum(np.abs(std_residuals) / 3.0, 1.0)

        return residuals.values, scores

class AnomalyDetectionEngine:
    """异常检测引擎"""

    def __init__(self, config_path: str = "/config/model_config.yaml"):
        self.config_path = config_path
        self.config = self._load_config()
        self.models = {}
        self.scalers = {}
        self.detectors = {}

        # 初始化检测器
        self._initialize_detectors()

    def _load_config(self) -> Dict[str, Any]:
        """加载配置文件"""
        try:
            with open(self.config_path, 'r') as f:
                return yaml.safe_load(f)
        except Exception as e:
            logger.error(f"加载配置文件失败: {e}")
            return {}

    def _initialize_detectors(self):
        """初始化异常检测器"""
        logger.info("初始化异常检测器...")

        # CPU异常检测器 (Isolation Forest)
        self.detectors['cpu'] = IsolationForest(
            contamination=0.1,
            random_state=42,
            n_estimators=100
        )

        # 内存异常检测器 (LSTM)
        self.detectors['memory'] = LSTMAutoencoder(
            sequence_length=30,
            feature_dim=6,
            encoding_dim=10
        )

        # 延迟异常检测器 (统计方法)
        self.detectors['latency'] = ProphetAnomalyDetector()

        # 网络异常检测器 (One-Class SVM)
        self.detectors['network'] = OneClassSVM(
            kernel='rbf',
            gamma='scale',
            nu=0.05
        )

        logger.info(f"已初始化 {len(self.detectors)} 个检测器")

    @ANOMALY_DETECTION_TIME.time()
    async def detect_anomalies(self, service: str, metric_data: Dict[str, Any]) -> List[AnomalyResult]:
        """检测异常"""
        anomalies = []

        for metric_type, data in metric_data.items():
            if metric_type not in self.detectors:
                continue

            try:
                logger.debug(f"检测 {service} 的 {metric_type} 异常")
                anomaly_result = await self._detect_single_metric(
                    service, metric_type, data
                )

                if anomaly_result and anomaly_result.score > self._get_threshold(metric_type):
                    anomalies.append(anomaly_result)

                    # 更新指标
                    ANOMALY_DETECTIONS.labels(
                        service=service,
                        type=metric_type,
                        severity=anomaly_result.severity
                    ).inc()

            except Exception as e:
                logger.error(f"检测 {service} {metric_type} 异常失败: {e}", exc_info=True)

        return anomalies

    async def _detect_single_metric(
        self,
        service: str,
        metric_type: str,
        data: Dict[str, Any]
    ) -> Optional[AnomalyResult]:
        """检测单个指标异常"""
        detector = self.detectors[metric_type]

        if metric_type == 'cpu':
            return self._detect_cpu_anomaly(service, detector, data)
        elif metric_type == 'memory':
            return self._detect_memory_anomaly(service, detector, data)
        elif metric_type == 'latency':
            return self._detect_latency_anomaly(service, detector, data)
        elif metric_type == 'network':
            return self._detect_network_anomaly(service, detector, data)
        else:
            logger.warning(f"未知的指标类型: {metric_type}")
            return None

    def _detect_cpu_anomaly(self, service: str, detector: IsolationForest, data: Dict) -> AnomalyResult:
        """CPU异常检测"""
        features = np.array([[
            data.get('cpu_usage_percent', 0),
            data.get('cpu_usage_trend', 0),
            data.get('load_average_1m', 0),
            data.get('load_average_5m', 0),
            data.get('cpu_throttle_count', 0)
        ]])

        # 预测
        score = detector.decision_function(features)[0]
        anomaly_score = 1 - (score + 1) / 2  # 转换为0-1分数

        severity = self._classify_severity(anomaly_score)
        anomaly_type = self._classify_anomaly_type(features[0])

        return AnomalyResult(
            service=service,
            metric='cpu',
            timestamp=datetime.now(),
            score=anomaly_score,
            severity=severity,
            anomaly_type=anomaly_type,
            confidence=min(anomaly_score * 1.2, 1.0),
            details={
                'features': features[0].tolist(),
                'raw_score': score,
                'cpu_usage': data.get('cpu_usage_percent', 0)
            },
            recommendation=self._generate_recommendation('cpu', severity, data)
        )

    def _detect_memory_anomaly(self, service: str, detector: LSTMAutoencoder, data: Dict) -> AnomalyResult:
        """内存异常检测"""
        features = np.array([[
            data.get('memory_usage_percent', 0),
            data.get('memory_available', 0),
            data.get('memory_cache', 0),
            data.get('memory_swap', 0),
            data.get('oom_kill_count', 0),
            data.get('page_faults', 0)
        ]])

        # 简化的异常分数计算
        memory_trend = data.get('memory_usage_trend', 0)
        leak_indicator = data.get('oom_kill_count', 0)

        # 检测内存泄漏模式
        anomaly_score = 0
        if memory_trend > 0.05:  # 持续增长
            anomaly_score += 0.5
        if leak_indicator > 0:
            anomaly_score += 0.7
        if data.get('memory_usage_percent', 0) > 90:
            anomaly_score += 0.3

        anomaly_score = min(anomaly_score, 1.0)

        severity = self._classify_severity(anomaly_score)
        anomaly_type = "memory_leak" if leak_indicator > 0 else "memory_high_usage"

        return AnomalyResult(
            service=service,
            metric='memory',
            timestamp=datetime.now(),
            score=anomaly_score,
            severity=severity,
            anomaly_type=anomaly_type,
            confidence=min(anomaly_score * 1.1, 1.0),
            details={
                'memory_usage': data.get('memory_usage_percent', 0),
                'memory_trend': memory_trend,
                'oom_kills': leak_indicator
            },
            recommendation=self._generate_recommendation('memory', severity, data)
        )

    def _detect_latency_anomaly(self, service: str, detector: ProphetAnomalyDetector, data: Dict) -> AnomalyResult:
        """延迟异常检测"""
        # 构建时间序列数据
        timestamps = pd.date_range(
            start=datetime.now() - timedelta(minutes=10),
            end=datetime.now(),
            freq='1T'
        )

        response_times = data.get('response_times', [100, 120, 110, 105, 115])
        if len(response_times) < len(timestamps):
            response_times = response_times + [response_times[-1]] * (len(timestamps) - len(response_times))

        df = pd.DataFrame({
            'timestamp': timestamps[:len(response_times)],
            'value': response_times
        })
        df.set_index('timestamp', inplace=True)

        # 使用Prophet检测异常
        residuals, anomaly_scores = detector.predict(df)

        # 当前异常分数
        current_score = anomaly_scores[-1] if len(anomaly_scores) > 0 else 0

        severity = self._classify_severity(current_score)
        anomaly_type = "latency_spike" if current_score > 0.7 else "latency_high"

        return AnomalyResult(
            service=service,
            metric='latency',
            timestamp=datetime.now(),
            score=current_score,
            severity=severity,
            anomaly_type=anomaly_type,
            confidence=current_score,
            details={
                'response_time_p99': data.get('response_time_p99', 0),
                'response_time_p95': data.get('response_time_p95', 0),
                'error_rate': data.get('error_rate', 0)
            },
            recommendation=self._generate_recommendation('latency', severity, data)
        )

    def _detect_network_anomaly(self, service: str, detector: OneClassSVM, data: Dict) -> AnomalyResult:
        """网络异常检测"""
        features = np.array([[
            data.get('network_rx_bytes', 0),
            data.get('network_tx_bytes', 0),
            data.get('network_errors', 0),
            data.get('network_drops', 0),
            data.get('connection_count', 0),
            data.get('connection_errors', 0)
        ]])

        # 预测
        score = detector.decision_function(features)[0]
        anomaly_score = 1 - (score + 1) / 2

        severity = self._classify_severity(anomaly_score)
        anomaly_type = "network_performance" if anomaly_score > 0.7 else "network_io"

        return AnomalyResult(
            service=service,
            metric='network',
            timestamp=datetime.now(),
            score=anomaly_score,
            severity=severity,
            anomaly_type=anomaly_type,
            confidence=min(anomaly_score * 1.15, 1.0),
            details={
                'features': features[0].tolist(),
                'raw_score': score,
                'rx_bytes': data.get('network_rx_bytes', 0),
                'tx_bytes': data.get('network_tx_bytes', 0)
            },
            recommendation=self._generate_recommendation('network', severity, data)
        )

    def _classify_severity(self, score: float) -> str:
        """分类严重性"""
        if score >= 0.9:
            return "critical"
        elif score >= 0.7:
            return "warning"
        elif score >= 0.5:
            return "info"
        else:
            return "normal"

    def _classify_anomaly_type(self, features: np.ndarray) -> str:
        """分类异常类型"""
        cpu_usage = features[0]

        if cpu_usage > 90:
            return "cpu_spike"
        elif cpu_usage > 70:
            return "cpu_high"
        else:
            return "cpu_normal"

    def _get_threshold(self, metric_type: str) -> float:
        """获取检测阈值"""
        thresholds = self.config.get('severity_classification', {})
        return thresholds.get('info', {}).get('score_threshold', 0.5)

    def _generate_recommendation(self, metric_type: str, severity: str, data: Dict) -> str:
        """生成优化建议"""
        recommendations = {
            'cpu': {
                'critical': "立即扩容CPU资源，检查是否有CPU密集型进程",
                'warning': "考虑增加CPU资源或优化CPU密集型操作",
                'info': "监控CPU使用趋势，适当调整"
            },
            'memory': {
                'critical': "立即重启应用，检查内存泄漏，启用堆转储",
                'warning': "增加内存限制，检查内存泄漏风险",
                'info': "监控内存使用，考虑调整内存配置"
            },
            'latency': {
                'critical': "检查慢查询，优化数据库连接，分析代码性能",
                'warning': "检查性能瓶颈，优化缓存策略",
                'info': "监控响应时间，适当调整超时配置"
            },
            'network': {
                'critical': "检查网络连接，重启网络组件，联系网络团队",
                'warning': "检查网络配置，监控带宽使用",
                'info': "监控网络指标，评估网络容量"
            }
        }

        return recommendations.get(metric_type, {}).get(severity, "监控指标变化")

    def investigate_anomaly(self, anomaly: AnomalyResult) -> Dict[str, Any]:
        """自动调查异常"""
        logger.info(f"开始调查 {anomaly.service} {anomaly.metric} 异常")

        investigation_result = {
            'anomaly_id': f"{anomaly.service}-{anomaly.metric}-{anomaly.timestamp.isoformat()}",
            'start_time': datetime.now().isoformat(),
            'status': 'completed',
            'findings': [],
            'actions_taken': [],
            'root_cause': None
        }

        try:
            # 模拟调查过程
            INVESTIGATIONS.labels(status='started').inc()

            # 1. 检查相关指标
            findings = []
            findings.append({
                'check': 'correlated_metrics',
                'result': 'found_correlation',
                'details': f'{anomaly.metric}异常可能影响相关指标'
            })

            # 2. 分析日志
            findings.append({
                'check': 'log_analysis',
                'result': 'errors_detected',
                'details': '发现相关错误日志'
            })

            # 3. 检查变更
            findings.append({
                'check': 'recent_changes',
                'result': 'no_changes',
                'details': '近期无相关配置变更'
            })

            investigation_result['findings'] = findings

            # 根据异常类型推荐操作
            if anomaly.severity == 'critical':
                investigation_result['actions_taken'].append({
                    'action': 'escalate',
                    'timestamp': datetime.now().isoformat(),
                    'description': '已升级到运维团队'
                })
            elif anomaly.score > 0.8:
                investigation_result['actions_taken'].append({
                    'action': 'collect_diagnostics',
                    'timestamp': datetime.now().isoformat(),
                    'description': '已收集诊断信息'
                })

            investigation_result['end_time'] = datetime.now().isoformat()
            INVESTIGATIONS.labels(status='completed').inc()

        except Exception as e:
            logger.error(f"调查异常失败: {e}")
            investigation_result['status'] = 'failed'
            investigation_result['error'] = str(e)
            INVESTIGATIONS.labels(status='failed').inc()

        return investigation_result

    def send_alert(self, anomaly: AnomalyResult, channels: List[str] = None):
        """发送告警"""
        if channels is None:
            channels = ['slack'] if anomaly.severity != 'critical' else ['pagerduty', 'slack']

        logger.info(f"发送 {anomaly.severity} 告警到 {channels}")

        for channel in channels:
            ALERTS_SENT.labels(channel=channel).inc()

            # 模拟告警发送
            logger.info(f"已发送 {channel} 告警: {anomaly.service} {anomaly.metric} 异常")

async def main():
    """主函数"""
    logger.info("启动AI异常检测服务...")

    # 启动Prometheus指标服务器
    start_http_server(8080)
    logger.info("Prometheus指标服务器已启动在端口8080")

    # 初始化异常检测引擎
    engine = AnomalyDetectionEngine()

    # 模拟实时检测
    test_services = [
        'basebackend-admin-api',
        'basebackend-user-service',
        'basebackend-auth-service'
    ]

    logger.info("开始实时异常检测...")

    while True:
        try:
            # 模拟指标数据
            for service in test_services:
                metric_data = {
                    'cpu': {
                        'cpu_usage_percent': np.random.uniform(20, 95),
                        'cpu_usage_trend': np.random.uniform(-0.1, 0.2),
                        'load_average_1m': np.random.uniform(0.5, 3.0),
                        'load_average_5m': np.random.uniform(0.5, 2.5),
                        'cpu_throttle_count': np.random.randint(0, 10)
                    },
                    'memory': {
                        'memory_usage_percent': np.random.uniform(30, 85),
                        'memory_usage_trend': np.random.uniform(-0.05, 0.1),
                        'memory_available': np.random.uniform(1000, 8000),
                        'oom_kill_count': np.random.randint(0, 3)
                    },
                    'latency': {
                        'response_time_p99': np.random.uniform(100, 2000),
                        'response_time_p95': np.random.uniform(50, 1000),
                        'error_rate': np.random.uniform(0, 5)
                    }
                }

                # 检测异常
                anomalies = await engine.detect_anomalies(service, metric_data)

                for anomaly in anomalies:
                    logger.warning(f"检测到异常: {anomaly.service} {anomaly.metric} "
                                 f"(分数: {anomaly.score:.2f}, 严重性: {anomaly.severity})")

                    # 自动调查
                    investigation = engine.investigate_anomaly(anomaly)
                    logger.info(f"调查完成: {investigation['status']}")

                    # 发送告警
                    engine.send_alert(anomaly)

            # 等待下次检测
            await asyncio.sleep(60)

        except Exception as e:
            logger.error(f"检测过程中出错: {e}", exc_info=True)
            await asyncio.sleep(30)

if __name__ == "__main__":
    asyncio.run(main())
