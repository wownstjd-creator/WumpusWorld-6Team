# Wumpus World 지능형 탐색 에이전트

인공지능 교과목 6조 팀 프로젝트입니다.

## 팀원

- 안재성: 팀장, 발표 및 보고서 작성
- 장지연: 도메인 및 추론 분석, 코드 분석 및 수정
- 조현준: UI 제작 및 최적화

## 주요 기능

- Knowledge Base 기반 상태 관리
- IF-THEN 규칙 기반 안전 지역 추론
- BFS 기반 안전 지역 탐색 및 귀환
- Java Swing 듀얼 GUI
- 100회 대량 검증 모드
- 단계별 테스트 모드

## 개발 환경

- Java
- Java Swing

## 실행 방법

저장소 최상위 폴더에서 다음 명령을 실행합니다.

```bash
javac -d out src/agent/*.java
java -cp out agent.Simulator