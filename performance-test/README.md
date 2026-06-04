# Performance Test

## 사전 조건
- 모든 서비스가 실행 중이어야 합니다
- Gateway: http://localhost:8080

## 실행 방법

### 기본 부하 테스트 (curl 기반, ab 도구 필요)
```bash
./performance-test/load-test.sh
```

커스텀 옵션:
```bash
./performance-test/load-test.sh [base_url] [concurrent] [requests]
# 예시:
./performance-test/load-test.sh http://localhost:8080 20 200
```

### k6 부하 테스트 (k6 설치 필요)
```bash
k6 run performance-test/k6-load-test.js
```

커스텀 Base URL:
```bash
BASE_URL=http://localhost:8080 k6 run performance-test/k6-load-test.js
```

## k6 설치

macOS:
```bash
brew install k6
```

Linux:
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6
```

## 테스트 목표 (SLA)
- 95th percentile 응답 시간: 500ms 이하
- 에러율: 10% 미만
- 최대 동시 사용자: 50명
