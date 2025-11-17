# Phase 12.4: CI/CD 流水线完善指南

## 📋 概述

本指南介绍如何建立完善的CI/CD（持续集成/持续部署）流水线，实现全自动化DevOps流程，包括代码构建、测试、部署、监控等关键环节。

---

## 🏗️ CI/CD 架构

### 流水线架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                      CI/CD 流水线架构                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │  持续集成     │  │  质量检查     │  │  持续部署     │           │
│  │  (CI)        │  │  (QA)        │  │  (CD)        │           │
│  │              │  │              │  │              │           │
│  │ • 代码拉取    │  │ • 单元测试    │  │ • 自动部署    │           │
│  │ • 编译构建    │  │ • 集成测试    │  │ • 灰度发布    │           │
│  │ • 代码扫描    │  │ • 安全扫描    │  │ • 健康检查    │           │
│  │ • 镜像构建    │  │ • 性能测试    │  │ • 自动回滚    │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                     │
│  ┌──────▼────────┐  ┌─────▼──────┐  ┌──────▼──────┐           │
│  │   版本管理     │  │   监控告警   │  │   文档生成   │           │
│  │              │  │              │  │              │           │
│  │ • Git 标签    │  │ • 性能监控   │  │ • API 文档   │           │
│  │ • 变更记录     │  │ • 错误追踪   │  │ • 部署文档   │           │
│  │ • 发布说明     │  │ • 告警通知   │  │ • 变更日志   │           │
│  │ • 制品管理     │  │ • 日志聚合   │  │ • 用户手册   │           │
│  └──────────────┘  └─────────────┘  └─────────────┘           │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                     工具链层                                  │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • GitLab CI / Jenkins                                       │ │
│  │ • SonarQube (代码质量)                                      │ │
│  │ • Trivy (安全扫描)                                          │ │
│  │ • Docker / Kubernetes                                       │ │
│  │ • ArgoCD (GitOps)                                          │ │
│  │ • Prometheus + Grafana                                      │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 流水线阶段

| 阶段 | 主要任务 | 工具 | 时间 |
|------|----------|------|------|
| **Code** | 代码提交、PR检查 | GitLab / GitHub | < 1min |
| **Build** | 编译打包、镜像构建 | Maven + Docker | 5-10min |
| **Test** | 单元测试、集成测试 | JUnit / TestNG | 10-15min |
| **Quality** | 代码质量、安全扫描 | SonarQube / Trivy | 5-10min |
| **Deploy** | 部署到测试/生产 | Kubernetes / Helm | 5-20min |
| **Verify** | 健康检查、冒烟测试 | Prometheus | 2-5min |
| **Monitor** | 性能监控、告警 | Grafana / AlertManager | 实时 |

---

## 🔄 GitLab CI/CD 配置

### 1. .gitlab-ci.yml 完整配置

```yaml
# .gitlab-ci.yml
stages:
  - validate
  - build
  - test
  - quality
  - security
  - package
  - deploy-dev
  - deploy-staging
  - deploy-prod
  - verify
  - monitor

variables:
  # 全局变量
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  DOCKER_REGISTRY: "registry.gitlab.com/basebackend"
  HELM_VERSION: "3.12.0"
  KUBECTL_VERSION: "1.29.0"

  # 环境变量
  DEV_KUBECONFIG: "$DEV_KUBE_CONFIG"
  STAGING_KUBECONFIG: "$STAGING_KUBE_CONFIG"
  PROD_KUBECONFIG: "$PROD_KUBE_CONFIG"

  # 应用变量
  APP_NAME: "basebackend"
  APP_VERSION: "$CI_COMMIT_SHORT_SHA"
  NAMESPACE_DEV: "basebackend-dev"
  NAMESPACE_STAGING: "basebackend-staging"
  NAMESPACE_PROD: "basebackend-prod"

# 缓存配置
cache:
  paths:
    - .m2/repository
    - node_modules

# 代码验证
validate:code:
  stage: validate
  image: alpine:latest
  script:
    - apk add --no-cache git
    - git fetch origin $CI_DEFAULT_BRANCH
    - git diff --name-only origin/$CI_DEFAULT_BRANCH...$CI_COMMIT_SHA | grep -E '\.(java|xml|yml|yaml|properties)$' || echo "无代码变更"
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'

# Docker 构建
build:docker:
  stage: build
  image: docker:24.0
  services:
    - docker:24.0-dind
  before_script:
    - echo $CI_REGISTRY_PASSWORD | docker login -u $CI_REGISTRY_USER --password-stdin $CI_REGISTRY
  script:
    # 构建多阶段镜像
    - docker build --target builder -t $CI_REGISTRY_IMAGE:builder-$CI_COMMIT_SHA .
    - docker build --target runtime -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - docker build --target runtime -t $CI_REGISTRY_IMAGE:$CI_COMMIT_BRANCH .
    # 推送到仓库
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_BRANCH
    # 打标签
    - |
      if [ "$CI_COMMIT_BRANCH" == "$CI_DEFAULT_BRANCH" ]; then
        docker tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA $CI_REGISTRY_IMAGE:latest
        docker push $CI_REGISTRY_IMAGE:latest
      fi
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
  artifacts:
    paths:
      - target/
    expire_in: 1 week

# 单元测试
test:unit:
  stage: test
  image: maven:3.9-eclipse-temurin-17-jammy
  script:
    - mvn clean test -B
  coverage: '/Code coverage: \d+\.\d+%/'
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml
      coverage_report:
        coverage_format: cobertura
        path: target/site/jacoco/jacoco.xml
    expire_in: 1 week
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'

# 集成测试
test:integration:
  stage: test
  image: maven:3.9-eclipse-temurin-17-jammy
  services:
    - name: mysql:8.0
      alias: mysql
      variables:
        MYSQL_ROOT_PASSWORD: "root"
        MYSQL_DATABASE: "basebackend_test"
    - name: redis:7-alpine
      alias: redis
  variables:
    SPRING_PROFILES_ACTIVE: "test"
  script:
    - mvn clean verify -B -Pintegration-tests
  artifacts:
    reports:
      junit:
        - target/integration-tests/TEST-*.xml
    expire_in: 1 week
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'

# 代码质量检查
quality:sonarqube:
  stage: quality
  image: sonarqube:9.9-community
  variables:
    SONAR_HOST_URL: "$SONAR_HOST_URL"
    SONAR_TOKEN: "$SONAR_TOKEN"
  script:
    - mvn sonar:sonar -Dsonar.projectKey=$CI_PROJECT_PATH_SLUG
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
  allow_failure: true

# 安全扫描
security:trivy:
  stage: security
  image: aquasec/trivy:latest
  script:
    - trivy image --format json --output image-report.json $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    - trivy image --severity HIGH,CRITICAL --exit-code 1 $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
  artifacts:
    reports:
      container_scanning:
        image: $CI_PROJECT_PATH:$CI_COMMIT_SHA
    paths:
      - image-report.json
    expire_in: 1 week
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'

# 依赖检查
security:dependency:
  stage: security
  image: owasp/dependency-check:8.0.1
  script:
    - dependency-check.sh --project "$CI_PROJECT_NAME" --scan $(pwd) --enableRetired
  artifacts:
    paths:
      - reports/
    expire_in: 1 week
  rules:
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
  allow_failure: true

# Helm 打包
package:helm:
  stage: package
  image: alpine/helm:$HELM_VERSION
  before_script:
    - helm repo add bitnami https://charts.bitnami.com/bitnami
    - helm repo update
  script:
    - helm package ./charts/$APP_NAME
    - helm repo index --url $HELM_REPO_URL .
  artifacts:
    paths:
      - "*.tgz"
      - index.yaml
    expire_in: 1 month
  rules:
    - if: '$CI_COMMIT_TAG'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'

# 部署到开发环境
deploy:dev:
  stage: deploy-dev
  image: alpine/helm:$HELM_VERSION
  environment:
    name: development
    url: $DEV_ENV_URL
    on_stop: cleanup:dev
  before_script:
    - apk add --no-cache kubectl
    - echo "$DEV_KUBECONFIG" | base64 -d > kubeconfig
    - export KUBECONFIG=kubeconfig
  script:
    - helm upgrade --install $APP_NAME-dev ./charts/$APP_NAME \
      --namespace $NAMESPACE_DEV \
      --create-namespace \
      --set image.tag=$CI_COMMIT_SHA \
      --set image.repository=$CI_REGISTRY_IMAGE \
      --set ingress.hosts[0].host=api-dev.basebackend.com \
      --set resources.limits.cpu=500m \
      --set resources.limits.memory=512Mi \
      --wait --timeout=300s
    - kubectl rollout status deployment/$APP_NAME-dev -n $NAMESPACE_DEV --timeout=300s
  rules:
    - if: '$CI_COMMIT_BRANCH == "develop"'

# 清理开发环境
cleanup:dev:
  stage: deploy-dev
  image: alpine/helm:$HELM_VERSION
  environment:
    name: development
    action: stop
  before_script:
    - apk add --no-cache kubectl
    - echo "$DEV_KUBECONFIG" | base64 -d > kubeconfig
    - export KUBECONFIG=kubeconfig
  script:
    - helm uninstall $APP_NAME-dev -n $NAMESPACE_DEV || true
    - kubectl delete namespace $NAMESPACE_DEV --ignore-not-found=true
  when: manual

# 部署到测试环境
deploy:staging:
  stage: deploy-staging
  image: alpine/helm:$HELM_VERSION
  environment:
    name: staging
    url: $STAGING_ENV_URL
  before_script:
    - apk add --no-cache kubectl
    - echo "$STAGING_KUBECONFIG" | base64 -d > kubeconfig
    - export KUBECONFIG=kubeconfig
  script:
    - helm upgrade --install $APP_NAME-staging ./charts/$APP_NAME \
      --namespace $NAMESPACE_STAGING \
      --create-namespace \
      --set image.tag=$CI_COMMIT_SHA \
      --set image.repository=$CI_REGISTRY_IMAGE \
      --set ingress.hosts[0].host=api-staging.basebackend.com \
      --set replicaCount=3 \
      --set resources.limits.cpu=1000m \
      --set resources.limits.memory=1Gi \
      --set autoscaling.enabled=true \
      --wait --timeout=600s
    - kubectl rollout status deployment/$APP_NAME-staging -n $NAMESPACE_STAGING --timeout=600s
  rules:
    - if: '$CI_COMMIT_BRANCH == "develop"'
  when: manual

# 部署到生产环境
deploy:prod:
  stage: deploy-prod
  image: alpine/helm:$HELM_VERSION
  environment:
    name: production
    url: $PROD_ENV_URL
  before_script:
    - apk add --no-cache kubectl curl
    - echo "$PROD_KUBECONFIG" | base64 -d > kubeconfig
    - export KUBECONFIG=kubeconfig
  script:
    - helm upgrade --install $APP_NAME-prod ./charts/$APP_NAME \
      --namespace $NAMESPACE_PROD \
      --create-namespace \
      --set image.tag=$CI_COMMIT_SHA \
      --set image.repository=$CI_REGISTRY_IMAGE \
      --set ingress.hosts[0].host=api.basebackend.com \
      --set replicaCount=5 \
      --set resources.limits.cpu=2000m \
      --set resources.limits.memory=2Gi \
      --set autoscaling.enabled=true \
      --set monitoring.enabled=true \
      --wait --timeout=900s
    - kubectl rollout status deployment/$APP_NAME-prod -n $NAMESPACE_PROD --timeout=900s
  rules:
    - if: '$CI_COMMIT_TAG'
  when: manual

# 健康检查
verify:health:
  stage: verify
  image: alpine/curl:latest
  script:
    - sleep 30  # 等待应用启动
    - |
      curl -f $PROD_ENV_URL/actuator/health || \
      (echo "健康检查失败，执行回滚"; helm rollback $APP_NAME-prod -n $NAMESPACE_PROD; exit 1)
  rules:
    - if: '$CI_COMMIT_TAG'
  when: on_success

# 性能测试
verify:performance:
  stage: verify
  image: loadimpact/k6:latest
  script:
    - k6 run --out json=performance-report.json performance-test.js
  artifacts:
    paths:
      - performance-report.json
    expire_in: 1 week
  rules:
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
  when: manual

# 监控检查
monitor:status:
  stage: monitor
  image: alpine/curl:latest
  before_script:
    - apk add --no-cache promtool
  script:
    # 检查Prometheus指标
    - promtool query instant 'up{job="basebackend"}' | grep -q "1" || exit 1
    # 检查Grafana仪表盘
    - curl -f -s $GRAFANA_URL/api/health | grep -q "OK" || exit 1
  rules:
    - if: '$CI_COMMIT_TAG'
  when: on_success

# 通知
notify:slack:
  stage: monitor
  image: alpine/curl:latest
  script:
    - |
      curl -X POST -H 'Content-type: application/json' \
      --data "{\"text\":\"Pipeline $CI_PIPELINE_ID finished for $CI_PROJECT_NAME\"}" \
      $SLACK_WEBHOOK_URL
  rules:
    - if: '$CI_PIPELINE_SOURCE == "web"'
  when: always
```

### 2. GitLab CI 变量配置

```bash
# .gitlab-ci-variables.txt

# Docker Registry
CI_REGISTRY=registry.gitlab.com/basebackend
CI_REGISTRY_USER=gitlab-ci-token
CI_REGISTRY_PASSWORD=${CI_JOB_TOKEN}

# Kubernetes 配置
DEV_KUBE_CONFIG=<base64编码的kubeconfig>
STAGING_KUBE_CONFIG=<base64编码的kubeconfig>
PROD_KUBE_CONFIG=<base64编码的kubeconfig>

# 环境URL
DEV_ENV_URL=https://api-dev.basebackend.com
STAGING_ENV_URL=https://api-staging.basebackend.com
PROD_ENV_URL=https://api.basebackend.com

# SonarQube
SONAR_HOST_URL=https://sonar.basebackend.com
SONAR_TOKEN=<token>

# Slack 通知
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...

# Helm Repository
HELM_REPO_URL=https://charts.basebackend.com
```

---

## 🔧 Jenkins Pipeline 配置

### 1. Jenkinsfile 完整配置

```groovy
// Jenkinsfile
pipeline {
    agent any

    options {
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
    }

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        DOCKER_REGISTRY = 'registry.gitlab.com/basebackend'
        APP_NAME = 'basebackend'
        SONAR_HOST_URL = credentials('sonar-host-url')
        SONAR_TOKEN = credentials('sonar-token')
        SLACK_CHANNEL = '#devops'
    }

    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['dev', 'staging', 'prod'],
            description: '选择部署环境'
        )
        booleanParam(
            name: 'RUN_INTEGRATION_TESTS',
            defaultValue: true,
            description: '是否运行集成测试'
        )
        booleanParam(
            name: 'RUN_PERFORMANCE_TESTS',
            defaultValue: false,
            description: '是否运行性能测试'
        )
    }

    tools {
        maven 'maven-3.9'
        dockerTool 'docker-latest'
        jdk 'jdk-17'
        kubectl 'kubectl-latest'
    }

    stages {
        stage('Code Validation') {
            parallel {
                stage('Check Code Changes') {
                    steps {
                        script {
                            sh 'git fetch origin develop'
                            def changedFiles = sh(
                                script: 'git diff --name-only origin/develop...HEAD',
                                returnStdout: true
                            ).trim()
                            echo "Changed files: ${changedFiles}"
                            if (changedFiles) {
                                writeFile file: 'changed-files.txt', text: changedFiles
                            }
                        }
                    }
                }

                stage('Linting') {
                    steps {
                        sh 'mvn checkstyle:check'
                    }
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    sh 'mvn clean compile -B'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                script {
                    sh 'mvn test -B -DtestFailureIgnore=false'
                }
            }
            post {
                always {
                    junit 'target/surefire-reports/**/*.xml'
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Report'
                    ])
                }
            }
        }

        stage('Integration Tests') {
            when {
                expression { params.RUN_INTEGRATION_TESTS }
            }
            steps {
                script {
                    withMaven(
                        maven: 'maven-3.9',
                        mavenSettingsConfig: 'maven-settings'
                    ) {
                        sh 'mvn clean verify -Pintegration-tests -B'
                    }
                }
            }
            post {
                always {
                    junit 'target/integration-tests/TEST-*.xml'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    dockerImage = docker.build("${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.BUILD_NUMBER}")
                    dockerImage.push()
                    dockerImage.push("develop")
                    docker.withRegistry("${env.DOCKER_REGISTRY}", 'docker-registry') {
                        dockerImage.push("latest")
                    }
                }
            }
        }

        stage('Code Quality') {
            parallel {
                stage('SonarQube Analysis') {
                    steps {
                        script {
                            withSonarQubeEnv('SonarQube') {
                                sh 'mvn sonar:sonar'
                            }
                        }
                    }
                    post {
                        always {
                            timeout(time: 5) {
                                waitForQualityGate(true)
                            }
                        }
                    }
                }

                stage('Trivy Security Scan') {
                    steps {
                        sh 'trivy image --format json --output image-report.json ${DOCKER_REGISTRY}/${APP_NAME}:${BUILD_NUMBER}'
                        sh 'trivy image --severity HIGH,CRITICAL --exit-code 1 ${DOCKER_REGISTRY}/${APP_NAME}:${BUILD_NUMBER}'
                    }
                    post {
                        always {
                            archiveArtifacts 'image-report.json'
                        }
                    }
                }

                stage('Dependency Check') {
                    steps {
                        sh '''
                            dependency-check.sh --project "${APP_NAME}" \
                                --scan $(pwd) \
                                --enableRetired \
                                --format JSON \
                                --out reports/
                        '''
                    }
                    post {
                        always {
                            publishHTML([
                                allowMissing: false,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: 'reports',
                                reportFiles: 'dependency-check-report.html',
                                reportName: 'OWASP Dependency Check'
                            ])
                        }
                    }
                }
            }
        }

        stage('Package Helm Chart') {
            steps {
                script {
                    sh 'helm package charts/${APP_NAME}'
                    sh 'helm repo index .'
                }
            }
            post {
                always {
                    archiveArtifacts '*.tgz'
                    archiveArtifacts 'index.yaml'
                }
            }
        }

        stage('Deploy') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'main'
                    tag 'v*'
                }
            }
            steps {
                script {
                    def deploymentEnv = params.DEPLOY_ENV.toLowerCase()
                    def namespace = "${APP_NAME}-${deploymentEnv}"
                    def kubeconfig = credentials("kubeconfig-${deploymentEnv}")
                    sh """
                        echo "${kubeconfig}" | base64 -d > kubeconfig
                        export KUBECONFIG=kubeconfig

                        helm upgrade --install ${APP_NAME}-${deploymentEnv} ./${APP_NAME}-*.tgz \
                            --namespace ${namespace} \
                            --create-namespace \
                            --set image.tag=${BUILD_NUMBER} \
                            --set image.repository=${DOCKER_REGISTRY}/${APP_NAME} \
                            --wait --timeout=900s

                        kubectl rollout status deployment/${APP_NAME}-${deploymentEnv} -n ${namespace} --timeout=900s
                    """
                }
            }
        }

        stage('Verify') {
            parallel {
                stage('Health Check') {
                    steps {
                        script {
                            def envUrl = getEnvUrl(params.DEPLOY_ENV)
                            sh """
                                sleep 30
                                curl -f ${envUrl}/actuator/health
                            """
                        }
                    }
                }

                stage('Performance Test') {
                    when {
                        expression { params.RUN_PERFORMANCE_TESTS }
                    }
                    steps {
                        script {
                            sh 'k6 run performance-test.js'
                        }
                    }
                    post {
                        always {
                            archiveArtifacts 'performance-results.json'
                        }
                    }
                }
            }
        }

        stage('Notify') {
            steps {
                script {
                    def status = currentBuild.result ?: 'SUCCESS'
                    def color = status == 'SUCCESS' ? 'good' : 'danger'
                    def message = "Build ${BUILD_NUMBER} - ${status}\nJob: ${JOB_NAME}\nURL: ${BUILD_URL}"

                    slackSend(
                        channel: env.SLACK_CHANNEL,
                        color: color,
                        message: message
                    )
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
            slackSend(
                channel: env.SLACK_CHANNEL,
                color: 'danger',
                message: "Build ${BUILD_NUMBER} FAILED!\nJob: ${JOB_NAME}\nURL: ${BUILD_URL}"
            )
        }
        unstable {
            echo 'Pipeline unstable!'
        }
    }
}

def getEnvUrl(String env) {
    switch(env.toLowerCase()) {
        case 'dev':
            return 'https://api-dev.basebackend.com'
        case 'staging':
            return 'https://api-staging.basebackend.com'
        case 'prod':
            return 'https://api.basebackend.com'
        default:
            return ''
    }
}
```

### 2. Jenkins Shared Library

```groovy
// vars/basebackendPipeline.groovy
def call(Map config = [:]) {
    pipeline {
        agent any

        options {
            timeout(time: config.timeout ?: 60, unit: 'MINUTES')
            buildDiscarder(logRotator(numToKeepStr: '10'))
            timestamps()
        }

        environment {
            APP_NAME = config.appName ?: 'basebackend'
            DOCKER_REGISTRY = config.dockerRegistry ?: 'registry.gitlab.com/basebackend'
        }

        stages {
            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Build') {
                steps {
                    sh "mvn clean compile -B"
                }
            }

            stage('Test') {
                steps {
                    sh "mvn test -B"
                }
                post {
                    always {
                        junit 'target/surefire-reports/**/*.xml'
                    }
                }
            }

            // 更多阶段...
        }

        post {
            always {
                cleanWs()
            }
        }
    }
}
```

---

## 🚀 ArgoCD GitOps 部署

### 1. ArgoCD Application 配置

```yaml
# argocd-application.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: basebackend
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: default

  source:
    repoURL: https://gitlab.com/basebackend/basebackend.git
    targetRevision: HEAD
    path: charts/basebackend
    helm:
      valueFiles:
        - values-production.yaml
      parameters:
        - name: image.tag
          value: $COMMIT_SHA

  destination:
    server: https://kubernetes.default.svc
    namespace: basebackend-prod

  syncPolicy:
    automated:
      prune: true
      selfHeal: true
      allowEmpty: false
    syncOptions:
      - CreateNamespace=true
      - PruneLast=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m

  # 健康检查
  health:
    status:
      degradedConditions:
        - type: Available
          status: False
        - type: Ready
          status: False

---
# ApplicationSet (多环境部署)
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: basebackend
  namespace: argocd
spec:
  generators:
  - list:
      elements:
      - env: dev
        namespace: basebackend-dev
        revision: develop
      - env: staging
        namespace: basebackend-staging
        revision: develop
      - env: prod
        namespace: basebackend-prod
        revision: main
  template:
    metadata:
      name: 'basebackend-{{env}}'
      namespace: argocd
    spec:
      project: default
      source:
        repoURL: https://gitlab.com/basebackend/basebackend.git
        targetRevision: '{{revision}}'
        path: charts/basebackend
        helm:
          valueFiles:
            - 'values-{{env}}.yaml'
      destination:
        server: https://kubernetes.default.svc
        namespace: '{{namespace}}'
      syncPolicy:
        automated:
          prune: true
          selfHeal: true
```

### 2. ArgoCD Sync Waves

```yaml
# argo-sync-waves.yaml
# 第一波：基础资源
apiVersion: v1
kind: Namespace
metadata:
  name: basebackend
  annotations:
    argocd.argoproj.io/sync-wave: "1"

---
# 第二波：ConfigMap 和 Secret
apiVersion: v1
kind: ConfigMap
metadata:
  name: basebackend-config
  namespace: basebackend
  annotations:
    argocd.argoproj.io/sync-wave: "2"
data:
  application.yml: |
    server:
      port: 8080
    spring:
      datasource:
        url: jdbc:mysql://mysql:3306/basebackend
        username: ${DB_USERNAME}
        password: ${DB_PASSWORD}

---
# 第三波：Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: basebackend
  namespace: basebackend
  annotations:
    argocd.argoproj.io/sync-wave: "3"
spec:
  replicas: 3
  selector:
    matchLabels:
      app: basebackend
  template:
    metadata:
      labels:
        app: basebackend
    spec:
      containers:
      - name: basebackend
        image: basebackend:latest
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: basebackend-config
        - secretRef:
            name: basebackend-secret

---
# 第四波：Service
apiVersion: v1
kind: Service
metadata:
  name: basebackend
  namespace: basebackend
  annotations:
    argocd.argoproj.io/sync-wave: "4"
spec:
  selector:
    app: basebackend
  ports:
  - port: 80
    targetPort: 8080
```

### 3. ArgoCD Webhook 配置

```yaml
# argo-webhook.yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: argocd-gateway
  namespace: argocd
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: argocd-tls
    hosts:
    - argocd.basebackend.com

---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: argocd-vs
  namespace: argocd
spec:
  hosts:
  - argocd.basebackend.com
  gateways:
  - argocd-gateway
  http:
  - match:
    - uri:
        prefix: /
    route:
    - destination:
        host: argocd-server
        port:
          number: 443
    timeout: 300s
```

---

## 🧪 自动化测试

### 1. JMeter 性能测试

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testname="BaseBackend Performance Test">
      <elementProp name="TestPlan.arguments" elementType="Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">https://api.basebackend.com</stringProp>
          </elementProp>
          <elementProp name="THREADS" elementType="Argument">
            <stringProp name="Argument.name">THREADS</stringProp>
            <stringProp name="Argument.value">100</stringProp>
          </elementProp>
          <elementProp name="RAMP_TIME" elementType="Argument">
            <stringProp name="Argument.name">RAMP_TIME</stringProp>
            <stringProp name="Argument.value">60</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
      <stringProp name="TestPlan.user_define_classpath"></stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
    </TestPlan>

    <hashTree>
      <!-- 用户登录场景 -->
      <ThreadGroup guiclass="ThreadGroupGui" testname="User Login">
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">-1</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">${THREADS}</stringProp>
        <stringProp name="ThreadGroup.ramp_time">${RAMP_TIME}</stringProp>
        <longProp name="ThreadGroup.duration">300</longProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>

        <hashTree>
          <!-- HTTP 请求默认值 -->
          <ConfigTestElement guiclass="HttpDefaultsGui" testname="HTTP Request Defaults">
            <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
              <collectionProp name="Arguments.arguments"/>
            </elementProp>
            <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
            <stringProp name="HTTPSampler.port"></stringProp>
            <stringProp name="HTTPSampler.protocol">https</stringProp>
            <stringProp name="HTTPSampler.contentEncoding"></stringProp>
            <stringProp name="HTTPSampler.path"></stringProp>
          </ConfigTestElement>

          <hashTree>
            <!-- 登录请求 -->
            <HTTPSamplerProxy guiclass="HttpTestSampleGui" testname="Login Request">
              <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
                <collectionProp name="Arguments.arguments">
                  <elementProp name="username" elementType="Argument">
                    <stringProp name="Argument.name">username</stringProp>
                    <stringProp name="Argument.value">testuser</stringProp>
                  </elementProp>
                  <elementProp name="password" elementType="Argument">
                    <stringProp name="Argument.name">password</stringProp>
                    <stringProp name="Argument.value">password123</stringProp>
                  </elementProp>
                </collectionProp>
              </elementProp>
              <stringProp name="HTTPSampler.domain"></stringProp>
              <stringProp name="HTTPSampler.port"></stringProp>
              <stringProp name="HTTPSampler.protocol"></stringProp>
              <stringProp name="HTTPSampler.contentEncoding"></stringProp>
              <stringProp name="HTTPSampler.path">/api/auth/login</stringProp>
              <stringProp name="HTTPSampler.method">POST</stringProp>
              <boolProp name="HTTPSampler.follow_redirects">false</boolProp>
              <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
              <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
            </HTTPSamplerProxy>

            <hashTree>
              <!-- 响应断言 -->
              <ResponseAssertion guiclass="AssertionGui" testname="Response Assertion">
                <collectionProp name="Asserion.test_strings">
                  <stringProp name="49586">200</stringProp>
                </collectionProp>
                <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
                <boolProp name="Assertion.assume_success">false</boolProp>
                <intProp name="Assertion.test_type">1</intProp>
              </ResponseAssertion>

              <!-- JSON 提取器 -->
              <JSONPostProcessor guiclass="JSONPostProcessorGui" testname="JSON Extractor">
                <stringProp name="JSONPostProcessor.referenceNames">token</stringProp>
                <stringProp name="JSONPostProcessor.jsonPathExpressions">$.token</stringProp>
                <stringProp name="JSONPostProcessor.match_numbers">1</stringProp>
              </JSONPostProcessor>
            </hashTree>

            <!-- 获取用户信息 -->
            <HTTPSamplerProxy guiclass="HttpTestSampleGui" testname="Get User Profile">
              <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
                <collectionProp name="Arguments.arguments"/>
              </elementProp>
              <stringProp name="HTTPSampler.domain"></stringProp>
              <stringProp name="HTTPSampler.port"></stringProp>
              <stringProp name="HTTPSampler.protocol"></stringProp>
              <stringProp name="HTTPSampler.contentEncoding"></stringProp>
              <stringProp name="HTTPSampler.path">/api/user/profile</stringProp>
              <stringProp name="HTTPSampler.method">GET</stringProp>
              <boolProp name="HTTPSampler.follow_redirects">false</boolProp>
              <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
              <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
            </HTTPSamplerProxy>

            <hashTree>
              <!-- Header 管理器 -->
              <HeaderManager guiclass="HeaderPanel" testname="HTTP Header Manager">
                <collectionProp name="HeaderManager.headers">
                  <elementProp name="Authorization" elementType="Header">
                    <stringProp name="Header.name">Authorization</stringProp>
                    <stringProp name="Header.value">Bearer ${token}</stringProp>
                  </elementProp>
                </collectionProp>
              </HeaderManager>

              <!-- 响应断言 -->
              <ResponseAssertion guiclass="AssertionGui" testname="Response Assertion">
                <collectionProp name="Asserion.test_strings">
                  <stringProp name="49586">200</stringProp>
                </collectionProp>
                <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
                <boolProp name="Assertion.assume_success">false</boolProp>
                <intProp name="Assertion.test_type">1</intProp>
              </ResponseAssertion>
            </hashTree>
          </hashTree>
        </hashTree>
      </ThreadGroup>

      <!-- 监听器 -->
      <ResultCollector guiclass="SummaryReport" testname="Summary Report">
        <boolProp name="ResultCollector.error_logging">false</boolProp>
        <objProp>
          <name>saveConfig</name>
          <value class="SampleSaveConfiguration">
            <time>true</time>
            <latency>true</latency>
            <timestamp>true</timestamp>
            <success>true</success>
            <label>true</label>
            <code>true</code>
            <message>true</message>
            <threadName>true</threadName>
            <dataType>true</dataType>
            <encoding>false</encoding>
            <assertions>true</assertions>
            <subresults>true</subresults>
            <responseData>false</responseData>
            <samplerData>false</samplerData>
            <xml>false</xml>
            <fieldNames>true</fieldNames>
            <responseHeaders>false</responseHeaders>
            <requestHeaders>false</requestHeaders>
            <responseDataOnError>false</responseDataOnError>
            <saveAssertionResultsFailureMessage>true</saveAssertionResultsFailureMessage>
            <assertionsResultsToSave>0</assertionsResultsToSave>
            <bytes>true</bytes>
            <sentBytes>true</sentBytes>
            <url>true</url>
            <threadCounts>true</threadCounts>
            <idleTime>true</idleTime>
            <connectTime>true</connectTime>
          </value>
        </objProp>
        <stringProp name="filename">performance-test-results.jtl</stringProp>
      </ResultCollector>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

### 2. K6 性能测试

```javascript
// performance-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 自定义指标
const errors = new Counter('errors');
const successRate = new Rate('success_rate');
const responseTime = new Trend('response_time');

export const options = {
    stages: [
        { duration: '2m', target: 100 },  // 预热
        { duration: '5m', target: 100 },  // 稳定负载
        { duration: '2m', target: 200 },  // 负载增加
        { duration: '5m', target: 200 },  // 高负载
        { duration: '2m', target: 0 },    // 负载减少
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95%的请求小于500ms
        http_req_failed: ['rate<0.05'],   // 错误率小于5%
    },
};

const BASE_URL = __ENV.BASE_URL || 'https://api.basebackend.com';
const USERS = {
    username: 'testuser',
    password: 'password123',
};

export function setup() {
    // 登录获取 token
    const loginResponse = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(USERS), {
        headers: { 'Content-Type': 'application/json' },
    });

    check(loginResponse, {
        'login successful': (r) => r.status === 200,
    });

    const token = JSON.parse(loginResponse.body).token;
    return { token };
}

export default function(data) {
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`,
        },
    };

    // 模拟用户操作
    const scenarios = [
        getUserProfile,
        getUserList,
        getUserById,
        updateUserSettings,
    ];

    const scenario = scenarios[Math.floor(Math.random() * scenarios.length)];
    scenario(BASE_URL, params);
}

function getUserProfile(baseUrl, params) {
    const response = http.get(`${baseUrl}/api/user/profile`, params);

    const success = check(response, {
        'profile status is 200': (r) => r.status === 200,
        'profile has data': (r) => JSON.parse(r.body).id !== undefined,
    });

    successRate.add(success);
    if (!success) errors.add(1);
    responseTime.add(response.timings.duration);

    sleep(randomIntBetween(1, 3));
}

function getUserList(baseUrl, params) {
    const response = http.get(`${baseUrl}/api/user/list?page=1&size=10`, params);

    const success = check(response, {
        'user list status is 200': (r) => r.status === 200,
        'user list has items': (r) => JSON.parse(r.body).items.length > 0,
    });

    successRate.add(success);
    if (!success) errors.add(1);
    responseTime.add(response.timings.duration);

    sleep(randomIntBetween(1, 3));
}

function getUserById(baseUrl, params) {
    const response = http.get(`${baseUrl}/api/user/123`, params);

    const success = check(response, {
        'user by id status is 200': (r) => r.status === 200,
    });

    successRate.add(success);
    if (!success) errors.add(1);
    responseTime.add(response.timings.duration);

    sleep(randomIntBetween(1, 3));
}

function updateUserSettings(baseUrl, params) {
    const payload = {
        settings: {
            theme: 'dark',
            language: 'zh-CN',
        },
    };

    const response = http.put(`${baseUrl}/api/user/settings`, JSON.stringify(payload), params);

    const success = check(response, {
        'update settings status is 200': (r) => r.status === 200,
    });

    successRate.add(success);
    if (!success) errors.add(1);
    responseTime.add(response.timings.duration);

    sleep(randomIntBetween(2, 5));
}

export function teardown(data) {
    console.log('Test completed');
}
```

---

## 📊 监控与告警

### 1. Prometheus 告警规则

```yaml
# alerts.yml
groups:
- name: pipeline.rules
  rules:
  # Pipeline 失败告警
  - alert: PipelineFailed
    expr: increase(ci_pipeline_total{failed="true"}[5m]) > 0
    for: 0m
    labels:
      severity: critical
      team: devops
    annotations:
      summary: "CI/CD Pipeline 失败"
      description: "Pipeline {{ $labels.pipeline }} 连续失败"
      runbook: "https://docs.basebackend.com/runbooks/pipeline-failure"

  # 部署失败告警
  - alert: DeploymentFailed
    expr: increase(ci_deployment_total{failed="true"}[5m]) > 0
    for: 0m
    labels:
      severity: critical
      team: devops
    annotations:
      summary: "部署失败"
      description: "环境 {{ $labels.environment }} 的部署失败"
      runbook: "https://docs.basebackend.com/runbooks/deployment-failure"

  # 测试失败率告警
  - alert: HighTestFailureRate
    expr: rate(ci_tests_failed_total[5m]) / rate(ci_tests_total[5m]) > 0.1
    for: 5m
    labels:
      severity: warning
      team: devops
    annotations:
      summary: "测试失败率过高"
      description: "测试失败率: {{ $value | humanizePercentage }}"

  # 构建时间过长告警
  - alert: BuildTimeTooLong
    expr: ci_build_duration_seconds > 1800
    for: 5m
    labels:
      severity: warning
      team: devops
    annotations:
      summary: "构建时间过长"
      description: "构建时间超过 30 分钟"

  # 代码质量检查失败告警
  - alert: CodeQualityFailed
    expr: ci_quality_gate_status == "failed"
    for: 0m
    labels:
      severity: warning
      team: developers
    annotations:
      summary: "代码质量检查失败"
      description: "项目 {{ $labels.project }} 代码质量检查失败"

  # 安全扫描发现漏洞告警
  - alert: SecurityVulnerabilities
    expr: increase(ci_security_vulnerabilities_total[5m]) > 0
    for: 0m
    labels:
      severity: critical
      team: security
    annotations:
      summary: "发现安全漏洞"
      description: "检测到 {{ $value }} 个高危漏洞"

  # 部署回滚告警
  - alert: RollbackTriggered
    expr: increase(ci_rollback_total[5m]) > 0
    for: 0m
    labels:
      severity: critical
      team: devops
    annotations:
      summary: "触发自动回滚"
      description: "环境 {{ $labels.environment }} 触发自动回滚"
```

### 2. Grafana 仪表盘

```json
{
  "dashboard": {
    "title": "CI/CD Pipeline Dashboard",
    "tags": ["ci", "cd", "pipeline"],
    "panels": [
      {
        "title": "Pipeline Status",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(ci_pipeline_total) by (status)",
            "legendFormat": "{{ status }}"
          }
        ]
      },
      {
        "title": "Deployment Frequency",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(ci_deployment_total[24h])",
            "legendFormat": "{{ environment }}"
          }
        ]
      },
      {
        "title": "Build Duration",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(ci_build_duration_seconds_bucket[5m]))",
            "legendFormat": "P95"
          },
          {
            "expr": "histogram_quantile(0.50, rate(ci_build_duration_seconds_bucket[5m]))",
            "legendFormat": "P50"
          }
        ]
      },
      {
        "title": "Test Results",
        "type": "piechart",
        "targets": [
          {
            "expr": "sum(ci_tests_total) by (result)",
            "legendFormat": "{{ result }}"
          }
        ]
      },
      {
        "title": "Code Quality Gate",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(ci_quality_gate_status == \"passed\") / sum(ci_quality_gate_status)",
            "legendFormat": "Pass Rate"
          }
        ]
      },
      {
        "title": "Security Vulnerabilities",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(ci_security_vulnerabilities_total) by (severity)",
            "legendFormat": "{{ severity }}"
          }
        ]
      },
      {
        "title": "Deployment Success Rate",
        "type": "singlestat",
        "targets": [
          {
            "expr": "sum(rate(ci_deployment_total{status=\"success\"}[5m])) / sum(rate(ci_deployment_total[5m]))",
            "legendFormat": "Success Rate"
          }
        ]
      },
      {
        "title": "Mean Time To Recovery (MTTR)",
        "type": "stat",
        "targets": [
          {
            "expr": "avg(ci_mttr_minutes)",
            "legendFormat": "MTTR (minutes)"
          }
        ]
      }
    ],
    "time": {
      "from": "now-24h",
      "to": "now"
    },
    "refresh": "5s"
  }
}
```

---

## 🔄 自动回滚机制

### 1. 回滚脚本

```bash
#!/bin/bash
# rollback.sh

set -e

NAMESPACE=$1
RELEASE_NAME=$2
REVISION=$3

if [ -z "$NAMESPACE" ] || [ -z "$RELEASE_NAME" ]; then
    echo "用法: $0 <namespace> <release_name> [revision]"
    echo "示例: $0 basebackend-prod basebackend-prod 100"
    exit 1
fi

echo "开始回滚..."
echo "命名空间: $NAMESPACE"
echo "发布名称: $RELEASE_NAME"

# 执行 Helm 回滚
if [ -n "$REVISION" ]; then
    helm rollback $RELEASE_NAME $REVISION -n $NAMESPACE
else
    helm rollback $RELEASE_NAME -n $NAMESPACE
fi

# 等待部署完成
echo "等待部署完成..."
kubectl rollout status deployment/$RELEASE_NAME -n $NAMESPACE --timeout=600s

# 健康检查
echo "执行健康检查..."
HEALTH_CHECK_URL=$(kubectl get service $RELEASE_NAME -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
if [ -z "$HEALTH_CHECK_URL" ]; then
    # 使用 ClusterIP
    CLUSTER_IP=$(kubectl get service $RELEASE_NAME -n $NAMESPACE -o jsonpath='{.spec.clusterIP}')
    HEALTH_CHECK_URL="http://$CLUSTER_IP:8080"
fi

sleep 30  # 等待应用启动

# 执行健康检查
curl -f "$HEALTH_CHECK_URL/actuator/health" || {
    echo "健康检查失败，尝试回滚到上一个版本..."
    helm rollback $RELEASE_NAME -n $NAMESPACE
    exit 1
}

echo "回滚成功!"

# 通知
curl -X POST -H 'Content-type: application/json' \
    --data "{\"text\":\"回滚完成: $RELEASE_NAME-$NAMESPACE\"}" \
    $SLACK_WEBHOOK_URL
```

### 2. Kubernetes 回滚控制器

```yaml
# rollback-controller.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: deployment-rollback-controller
  namespace: kube-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: deployment-rollback-controller
  template:
    metadata:
      labels:
        app: deployment-rollback-controller
    spec:
      serviceAccountName: deployment-rollback-controller
      containers:
      - name: controller
        image: basebackend/deployment-rollback-controller:latest
        imagePullPolicy: Always
        env:
        - name: NAMESPACE
          value: "basebackend-prod"
        - name: HEALTH_CHECK_URL
          value: "https://api.basebackend.com/actuator/health"
        - name: SLACK_WEBHOOK_URL
          valueFrom:
            secretKeyRef:
              name: rollback-notifications
              key: slack-webhook-url

---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: deployment-rollback-controller
  namespace: kube-system

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: deployment-rollback-controller
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments"]
  verbs: ["get", "list", "watch", "patch"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: deployment-rollback-controller
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: deployment-rollback-controller
subjects:
- kind: ServiceAccount
  name: deployment-rollback-controller
  namespace: kube-system
```

---

## 📝 文档生成

### 1. API 文档自动化

```yaml
# api-docs-pipeline.yml
stage: documentation
script:
  # 使用 SpringDoc 生成 OpenAPI 文档
  - mvn springdoc-openapi:api-docs
  # 生成 HTML 文档
  - mvn asciidoctor:process-asciidoc
  # 上传到文档服务器
  - aws s3 sync target/site/asciidoc s3://docs.basebackend.com/api
  # 更新索引
  - aws s3 cp s3://docs.basebackend.com/api/index.html s3://docs.basebackend.com/
```

### 2. 变更日志生成

```bash
#!/bin/bash
# generate-changelog.sh

LAST_TAG=$1
CURRENT_TAG=$2

if [ -z "$LAST_TAG" ]; then
    LAST_TAG=$(git describe --tags --abbrev=0 HEAD~1 2>/dev/null || echo "")
fi

# 生成变更日志
git-chglog --next-tag $CURRENT_TAG --output CHANGELOG.md

# 格式化为发布说明
cat > RELEASE_NOTES.md << EOF
# Release $CURRENT_TAG

## 🚀 新功能
<!-- 新功能列表 -->

## 🐛 Bug 修复
<!-- Bug 修复列表 -->

## 📚 文档更新
<!-- 文档更新列表 -->

## 🔧 技术改进
<!-- 技术改进列表 -->

## 完整变更日志
[查看完整变更日志](https://gitlab.com/basebackend/basebackend/-/tags/$CURRENT_TAG)
EOF

# 上传到 GitLab
git tag $CURRENT_TAG
git push origin $CURRENT_TAG
```

---

## 🧪 测试执行脚本

### 1. 综合测试脚本

```bash
#!/bin/bash
# run-all-tests.sh

set -e

echo "========================================"
echo "    BaseBackend 全量测试套件"
echo "========================================"

# 单元测试
echo "1. 运行单元测试..."
mvn test -B -DtestFailureIgnore=false
if [ $? -eq 0 ]; then
    echo "✅ 单元测试通过"
else
    echo "❌ 单元测试失败"
    exit 1
fi

# 集成测试
echo "2. 运行集成测试..."
mvn verify -B -Pintegration-tests
if [ $? -eq 0 ]; then
    echo "✅ 集成测试通过"
else
    echo "❌ 集成测试失败"
    exit 1
fi

# API 测试
echo "3. 运行 API 测试..."
mvn test -B -Dtest.groups=api
if [ $? -eq 0 ]; then
    echo "✅ API 测试通过"
else
    echo "❌ API 测试失败"
    exit 1
fi

# 性能测试
echo "4. 运行性能测试..."
k6 run performance-test.js
if [ $? -eq 0 ]; then
    echo "✅ 性能测试通过"
else
    echo "❌ 性能测试失败"
    exit 1
fi

# 安全扫描
echo "5. 运行安全扫描..."
trivy fs --format json --output security-report.json .
if [ $? -eq 0 ]; then
    echo "✅ 安全扫描完成"
else
    echo "⚠️ 安全扫描发现问题"
fi

# 代码质量检查
echo "6. 代码质量检查..."
mvn sonar:sonar
if [ $? -eq 0 ]; then
    echo "✅ 代码质量检查通过"
else
    echo "❌ 代码质量检查失败"
    exit 1
fi

echo ""
echo "========================================"
echo "🎉 所有测试通过!"
echo "========================================"
```

---

## 📚 参考资料

1. [GitLab CI/CD 官方文档](https://docs.gitlab.com/ee/ci/)
2. [Jenkins Pipeline 文档](https://www.jenkins.io/doc/book/pipeline/)
3. [ArgoCD 用户指南](https://argo-cd.readthedocs.io/)
4. [Kubernetes 部署最佳实践](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-14
**状态：** 📋 指南完成，准备实施

**加油喵～ CI/CD流水线即将完成！** ฅ'ω'ฅ
