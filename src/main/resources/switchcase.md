# Switch-Case 문 대신 어떻게 해야할까? 

## 왜 switch-case를 지양하라고 할까? 
### 1. OCP 위반
switch-case는 새로운 조건이 생길 때마다 코드를 열어서 수정해야 함
```java
switch (type) {
    case A: ...
    case B: ...
    // 새로운 타입 C 추가 → 기존 코드 수정
    case C: ...
}
```
- 기존 코드를 계속 고쳐야한다 = 유지보수 악몽
- 확장에는 닫혀 있고 변경에 열려 있어서 OCP 위반

### 2. 조건 분기가 많아지면 난잡해짐
10개만 넘어도 읽기 힘들다
```java
switch (paymentType) {
    case CARD:
    case CASH:
    case POINT:
    case COUPON:
    case QR:
    ...
}
```
- 복잡성 폭발
- 책임이 한 클래스에 몰림 -> SRP 위반

### 3. 타입에 따라 로직을 선택하는 구조는 객체지향에서 안좋은 냄새
이걸 "Type Code"라고 부르고 </br>
이 Type Code가 많아질수록 switch-case가 생기고 </br>
switch-case가 많아질수록 객체지향 구조가 깨지는 경향이 있음

### ❌결론
switch-case는 "나쁜코드가 아니라" </br>
객체지향을 방해하는 상황에서 자주 발견되는 코드냄새 라 지양하라고 하는 것임

## switch-case 대신 뭘 쓰면 좋을까?
### ⭐️ 최강 대안1 - "전략 패턴(Strategy Pattern)"
switch-case가 하는 일을 객체로 분리해서 해결하는 방법 </br>

#### **Before**
```java
switch (shape) {
    case CIRCLE: drawCircle();
    case SQUARE: drawSquare();
    case TRIANGLE: drawTriangle();
}
```
#### **After (전략 패턴)**
Strategy 인터페이스 : 
```java
public interface ShapeDrawer {
    void draw();
}
```
구현체들 : 
```java
public class CircleDrawer implements ShapeDrawer { public void draw() {...}}
public class SquareDrawer implements ShapeDrawer { public void draw() {...}}
public class TriangleDrawer implements ShapeDrawer { public void draw() {...}}
```
팩토리 or Map : 
```java
Map<ShapeType, ShapeDrawer> drawerMap;

drawerMap.get(type).draw();
```
- switch case 완전 제거
- 타입 추가해도 기존 코드 수정 없음
- 더 객체지향스러움

### 대안2 - "Enum 내부에 로직 넣기"
enum 값마다 실행할 로직이 다르면 enum이 직접 처리
예:
```java
public enum Operation {
    ADD {
        @Override
        public int apply(int x, int y) { return x + y; }
    },
    SUBTRACT {
        @Override
        public int apply(int x, int y) { return x - y; }
    };

    public abstract int apply(int x, int y);
}
```
사용 : 
```java
int result = Operation.ADD.apply(1, 2);
```
- switch 필요 없음
- enum이 타입별 행동을 직접 알고 있음

### 대안3 - Map + 람다식(함수형 방식)
switch를 단순 함수 매핑으로 치환
#### **Before**
```java
switch(command) {
    case "start": start();
    case "stop": stop();
}
```
#### **After (Map)**
```java
Map<String, Runnable> commandMap = Map.of(
    "start", this::start,
    "stop", this::stop
);

commandMap.get(command).run();
```
- 조건 분기 zero
- 깔끔하고 확장성 뛰어남

### 대안 4 - 다형성(Polymorphism) 사용
switch-case 때문에 타입을 구분하는 구조는 </br>
"다형성"으로 해결하는게 가장 정석적임

#### **Before**
```java
switch(animal.type) {
    case DOG: bark();
    case CAT: meow();
}
```
#### **After**
```java
abstract class Animal { abstract void sound(); }

class Dog extends Animal { void sound() { bark(); } }
class Cat extends Animal { void sound() { meow(); } }

animal.sound();
```
- switch 없어짐
- 새 동물 타입 추가 시 기존 코드 수정 없음

## switch-case는 언제 써도 괜찮을까?
지양한다고 했지, 금지는 아님 </br>
아래는 써도 좋은 경우들 : </br>
✅ 비즈니스 로직이 아니라 단순한 조건 값 선택
```java
switch (dayOfWeek) {
    ...
}
```
✅Enum 값 처리인데 분기가 3개 이하 </br>
✅파싱/매핑 같은 로우 레벨 유틸 코드 </br>
✅복잡한 전략 패턴은 오버킬일 때 </br>
➡️규모가 작으면 switch-case가 더 간단하고 오히려 가독성이 좋음 </br>

