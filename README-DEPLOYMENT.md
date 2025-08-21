# Vehicle Tracker 배포 가이드

## 개요
이 문서는 Ubuntu 서버에서 Docker를 사용하여 Vehicle Tracker 애플리케이션을 배포하는 방법을 설명합니다.

## 시스템 요구사항
- Ubuntu 20.04 LTS 이상
- Docker 및 Docker Compose
- 최소 2GB RAM, 10GB 디스크 공간
- 포트 3000, 8080, 3306 사용 가능

## 배포 단계

### 1. Docker 설치
```bash
# Docker 설치
sudo apt update
sudo apt install -y apt-transport-https ca-certificates curl software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER
newgrp docker
```

### 2. 방화벽 설정
```bash
sudo ufw enable
sudo ufw allow 22/tcp          # SSH
sudo ufw allow 3000/tcp        # 프론트엔드
sudo ufw allow 8080/tcp        # 백엔드 API
sudo ufw status
```

### 3. 프로젝트 배포
```bash
# 프로젝트 디렉토리로 이동
cd /path/to/Car_Infor

# Docker 컨테이너 빌드 및 실행
docker-compose up -d --build

# 실행 상태 확인
docker-compose ps
```

### 4. 접속 확인
- 프론트엔드: http://your-server-ip:3000
- 백엔드 API: http://your-server-ip:8080
- 헬스체크: http://your-server-ip:8080/actuator/health

## 주요 명령어

### 서비스 관리
```bash
# 전체 서비스 시작
docker-compose up -d

# 전체 서비스 중지
docker-compose down

# 특정 서비스 재시작
docker-compose restart backend

# 로그 확인
docker-compose logs -f [service-name]
```

### 문제 해결
```bash
# 컨테이너 상태 확인
docker-compose ps

# 시스템 리소스 확인
docker stats

# 볼륨 포함 완전 삭제
docker-compose down -v
```

## 구성 요소

### 서비스
- **MySQL**: 데이터베이스 (포트 3306)
- **Backend**: Spring Boot API 서버 (포트 8080)
- **Frontend**: Next.js 웹 애플리케이션 (포트 3000)

### 환경 변수
- `SPRING_PROFILES_ACTIVE=docker`
- `SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/vehicle_tracker`
- `NEXT_PUBLIC_API_URL=http://localhost:8080`

## 보안 고려사항
1. MySQL 외부 접속 차단
2. 강력한 데이터베이스 비밀번호 사용
3. 정기적인 보안 업데이트
4. SSL 인증서 적용 (프로덕션 환경)

## 백업 및 복구
```bash
# 데이터베이스 백업
docker exec vehicle-tracker-mysql mysqldump -u vehicle_user -p vehicle_tracker > backup.sql

# 데이터베이스 복구
docker exec -i vehicle-tracker-mysql mysql -u vehicle_user -p vehicle_tracker < backup.sql
```
