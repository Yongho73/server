# Getting Started

# 캐쉬 관련 추가한 라이브러리
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis' // 기본 클라이언트: Lettuce
}

# 큐허용
curl -X POST http://localhost:8080/api/queue/allow/EVT123

# 마스터 상태 조회
curl -X GET http://localhost:8080/api/queue/flag/master

# 마스터 ON
curl -X POST http://localhost:8080/api/queue/flag/master/on

# 마스터 OFF
curl -X POST http://localhost:8080/api/queue/flag/master/off

# 특정 이벤트 상태 조회
curl -X GET http://localhost:8080/api/queue/flag/EVT123

# 특정 이벤트 ON
curl -X POST http://localhost:8080/api/queue/flag/EVT123/on

# 특정 이벤트 OFF
curl -X POST http://localhost:8080/api/queue/flag/EVT123/off

# 특정 이벤트 토글
curl -X POST http://localhost:8080/api/queue/flag/EVT123/toggle

# Redis 클러스터 포트 구성
노드 이름	서비스 포트 (Client Port)	클러스터 버스 포트 (Bus Port = Client+10000)	용도
redis-7001	7001	17001	클러스터 Master 1
redis-7002	7002	17002	클러스터 Master 2
redis-7003	7003	17003	클러스터 Master 3
redis-7004	7004	17004	Replica (Master 2)
redis-7005	7005	17005	Replica (Master 3)
redis-7006	7006	17006	Replica (Master 1)

# 방화벽 / 보안그룹 설정
서비스 포트 (7001~7006)
앱 서버(Spring Boot 등)에서 Redis에 붙을 때 사용하는 포트
외부에서 Redis에 접근할 필요가 있다면 이 포트들을 허용해야 함
클러스터 버스 포트 (17001~17006)
Redis 노드끼리 통신할 때만 사용
외부 애플리케이션은 사용하지 않음
따라서 Redis 노드 간에는 반드시 열려 있어야 함
외부로는 열 필요 없음 (보안상 닫아두는 게 좋음)

# 레디스 클러스터 설정 (docker compose)
services:
  redis-7001:
    image: redis:7
    container_name: redis-7001
    ports: ["7001:7001", "17001:17001"]
    volumes: ["./data/redis-7001:/data"]
    command:
      - redis-server
      - --port
      - "7001"
      - --cluster-enabled
      - "yes"
      - --cluster-config-file
      - /data/nodes.conf
      - --cluster-node-timeout
      - "5000"
      - --appendonly
      - "yes"
      - --cluster-announce-ip
      - ${HOST:-host.docker.internal}
      - --cluster-announce-port
      - "7001"
      - --cluster-announce-bus-port
      - "17001"
      # - --requirepass
      # - YOUR_PASSWORD
      # - --masterauth
      # - YOUR_PASSWORD

  redis-7002:
    image: redis:7
    container_name: redis-7002
    ports: ["7002:7002", "17002:17002"]
    volumes: ["./data/redis-7002:/data"]
    command:
      - redis-server
      - --port
      - "7002"
      - --cluster-enabled
      - "yes"
      - --cluster-config-file
      - /data/nodes.conf
      - --cluster-node-timeout
      - "5000"
      - --appendonly
      - "yes"
      - --cluster-announce-ip
      - ${HOST:-host.docker.internal}
      - --cluster-announce-port
      - "7002"
      - --cluster-announce-bus-port
      - "17002"

  redis-7003:
    image: redis:7
    container_name: redis-7003
    ports: ["7003:7003", "17003:17003"]
    volumes: ["./data/redis-7003:/data"]
    command:
      - redis-server
      - --port
      - "7003"
      - --cluster-enabled
      - "yes"
      - --cluster-config-file
      - /data/nodes.conf
      - --cluster-node-timeout
      - "5000"
      - --appendonly
      - "yes"
      - --cluster-announce-ip
      - ${HOST:-host.docker.internal}
      - --cluster-announce-port
      - "7003"
      - --cluster-announce-bus-port
      - "17003"

  redis-7004:
    image: redis:7
    container_name: redis-7004
    ports: ["7004:7004", "17004:17004"]
    volumes: ["./data/redis-7004:/data"]
    command:
      - redis-server
      - --port
      - "7004"
      - --cluster-enabled
      - "yes"
      - --cluster-config-file
      - /data/nodes.conf
      - --cluster-node-timeout
      - "5000"
      - --appendonly
      - "yes"
      - --cluster-announce-ip
      - ${HOST:-host.docker.internal}
      - --cluster-announce-port
      - "7004"
      - --cluster-announce-bus-port
      - "17004"

  redis-7005:
    image: redis:7
    container_name: redis-7005
    ports: ["7005:7005", "17005:17005"]
    volumes: ["./data/redis-7005:/data"]
    command:
      - redis-server
      - --port
      - "7005"
      - --cluster-enabled
      - "yes"
      - --cluster-config-file
      - /data/nodes.conf
      - --cluster-node-timeout
      - "5000"
      - --appendonly
      - "yes"
      - --cluster-announce-ip
      - ${HOST:-host.docker.internal}
      - --cluster-announce-port
      - "7005"
      - --cluster-announce-bus-port
      - "17005"

  redis-7006:
    image: redis:7
    container_name: redis-7006
    ports: ["7006:7006", "17006:17006"]
    volumes: ["./data/redis-7006:/data"]
    command:
      - redis-server
      - --port
      - "7006"
      - --cluster-enabled
      - "yes"
      - --cluster-config-file
      - /data/nodes.conf
      - --cluster-node-timeout
      - "5000"
      - --appendonly
      - "yes"
      - --cluster-announce-ip
      - ${HOST:-host.docker.internal}
      - --cluster-announce-port
      - "7006"
      - --cluster-announce-bus-port
      - "17006"
      
# 기존 컨테이너 내리기
docker compose down

# (선택) 데이터 폴더 비우기: E:\docker\redis\data\redis-7001 ~ 7006
# nodes.conf나 AOF 남아있으면 새 클러스터 만들 때 헷갈릴 수 있어요.

# 다시 올리기
docker compose up -d      

# 도커 컴포즈 실행 후 클러스터 묶기(한 번만)

Git Bash에서는 줄바꿈에 \ 사용하세요:

docker exec -it redis-7001 redis-cli --cluster create \
  host.docker.internal:7001 host.docker.internal:7002 host.docker.internal:7003 \
  host.docker.internal:7004 host.docker.internal:7005 host.docker.internal:7006 \
  --cluster-replicas 1

비밀번호 사용 시:

docker exec -it redis-7001 redis-cli -a YOUR_PASSWORD --cluster create \
  host.docker.internal:7001 host.docker.internal:7002 host.docker.internal:7003 \
  host.docker.internal:7004 host.docker.internal:7005 host.docker.internal:7006 \
  --cluster-replicas 1      

# 노드 살아있는지 체크
docker exec -it redis-7001 redis-cli -p 7001 ping
PONG 나오면 OK
비밀번호 쓰셨다면: redis-cli -a YOUR_PASSWORD -p 7001 ping

# 클러스터 상태 확인
docker exec -it redis-7001 redis-cli -p 7001 cluster info
docker exec -it redis-7001 redis-cli -p 7001 cluster nodes

# RedisInsight로 클러스터 접속하기

RedisInsight 실행
Redis 공식 홈페이지
에서 RedisInsight 다운로드 & 설치 (Windows/Mac/Linux 모두 지원)
새 데이터베이스 추가
RedisInsight 첫 화면에서 ➕ Add Redis Database 클릭
연결 정보 입력
Host: host.docker.internal
Port: 예를 들어 7001 (클러스터 노드 중 아무 마스터 하나)
이름(Name): 자유롭게 local-cluster 등
비밀번호(Password): --requirepass를 설정했다면 입력, 안 했으면 비워둡니다.
⚠️ 체크박스: “This is a Redis Cluster” 반드시 켜기
저장 후 연결
연결 성공하면 RedisInsight가 클러스터 노드(7001~7006)를 자동으로 탐지합니다.
슬롯 분포, 마스터/레플리카 상태, 키 갯수 등을 GUI로 확인 가능해요.


# 운영 배치 원칙 (요약)

구성: 3 마스터 + 3 레플리카 = 6 노드
배치: 가능하면 서로 다른 6대(안정성↑). 예산상 3대면 각 서버에 2노드(교차 배치).
네트워크: 같은 서브넷이면 편리. 필수는 아니나 모든 노드 간 아래 포트가 L3에서 상호 통신되어야 함
서비스 포트: 7001~7006
클러스터 버스 포트: 17001~17006 (= 700x + 10000)
고정 주소: 고정 IP 또는 FQDN 사용 (DHCP 변동 금지).
NAT/방화벽: NAT 넘어가면 --cluster-announce-ip에 외부에서 접근 가능한 IP를 정확히 설정. 방화벽에 상호 허용.


# 해야할거
1. 입장 시스템: 여러대 서버 사용시 사용, 미사용 redis 에 저장 (대기열 api 호출)
2. 입장 시스템: 화면 이벤트 발생시 allow 및 토큰 세션 연장 (대기열 api 호출)
3. 입장 시스템: 브라우저를 닫을때 redis queue 삭제


