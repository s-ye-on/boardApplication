# Docker
## Docker란?
도커는 "애플리케이션 컨테이너(container)에 넣어서 어디서든 동일하게 실행할 수 있도록 해주는 플랫폼"

"내 노트북에서 되던 코드, 서버에서도 똑같이 실행되게 해주는 기술"

개발 환경이 달라서 생기는 환경 충돌 문제(예: 실행은 되는데 서버에서 오류나는 문제)를 해결해줌

## 도커 핵심 개념/용어
| 용어                | 설명                                            |
|-------------------|-----------------------------------------------|
| Docker Image(이미지) | 실행에 필요한 모든 것(코드, 라이브러리, 환경)을 포장한 실행 템플릿       |
| Contatiner(컨테이너)  | 이미지를 실행한 실제 동작 인스턴스. 예를 들면 이미지는 클래스, 컨테이너는 객체 |
| Dockerfile        | 이미지를 생성하기 위한 레시피(요리 레시피 같은 설정 파일)             |
| Docker Hub        | 이미지 공유 저장소(이미 만들어진 이미지를 다운받아 사용할 수 있음         |
| Registry          | 이미지를 저장해두는 공간(Docker Hub가 대표적인 레지스트리)         |

## 도커가 필요한 이유(장점)
✅개발 환경이 달라도 항상 똑같이 실행됨
✅프로그램 실행을 매우 단순화 (docker run ...)
✅가벼움(VM보다 훨씬 빠르고 작은 실행 단위)
✅배포가 쉬움(이미지를 서버에 올리기만 하면 끝)
✅마이크로서비스 구조와 잘 맞음

## 🖼 이미지 → 🏃 컨테이너 과정
1. Dockerfile 작성
2. Docker build → Image 생성
3. Docker run → Container 실행

예시 흐름 :
[Dockerfile] ---> docker build ---> [Image] ---> docker run ---> [Container]

## Node.js 앱을 Docker로 실행
1. 프로젝트 파일 구조

   my-app/

   ├─ app.js

   └─ package.json
app.js 예시 : 
```js
console.log("Hello Docker!");
```
2. Dockerfile 작성
```dockerfile
FROM node:18
WORKDIR /app
COPY . .
CMD ["node", "app.js"]
```
3. 이미지 빌드
```shell
docker build -t my-node-app .
```
4. 컨테이너 실행
```shell
docker run my-node-app
```
출력 결과 : `Hello Docker!`

## 비유로 이해하기 
| 현실 비유           | Docker 개념        |
|-----------------|------------------|
| 요리 레시피          | Dockerfile       |
| 레시피로 만들어진 냉동 식품 | Docker Image     |
| 실제로 조리해서 먹는 음식  | Docker Container |

## 도커와 가상머신(VM) 차이는?
| 가상 머신     | 도커                    |
|-----------|-----------------------|
| 전체 OS를 올림 | 호스트 OS 위에서 필요한 부분만 격리 |
| 무겁고 느림    | 가볍고 빠름                |
| 수 GB 용량   | 수백 MB 이하로 가능          |

요약 : 도커는 애플리케이션을 어디서든 동일하게 실행할 수 있도록 도와주는 컨테이너 기술