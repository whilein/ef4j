<!-- @formatter:off  -->

# Ef4j

<div align="center">
  <a href="https://github.com/lero4ka16/ef4j/blob/master/LICENSE">
    <img src="https://img.shields.io/github/license/lero4ka16/ef4j">
  </a>

  <a href="https://discord.gg/ANEHruraCc">
    <img src="https://img.shields.io/discord/819859288049844224?logo=discord">
  </a>

  <a href="https://github.com/lero4ka16/ef4j/issues">
    <img src="https://img.shields.io/github/issues/lero4ka16/ef4j">
  </a>

  <a href="https://github.com/lero4ka16/ef4j/pulls">
    <img src="https://img.shields.io/github/issues-pr/lero4ka16/ef4j">
  </a>

  <a href="https://search.maven.org/artifact/com.github.lero4ka16/ef4j">
    <img src="https://img.shields.io/maven-central/v/com.github.lero4ka16/ef4j">
  </a>

  <!-- <a href="https://s01.oss.sonatype.org/content/repositories/snapshots/com/github/lero4ka16/ef4j">
    <img src="https://img.shields.io/nexus/s/com.github.lero4ka16/ef4j?server=https%3A%2F%2Fs01.oss.sonatype.org">
  </a> -->
</div>

## About the project
Ef4j (Event Framework For Java) - Simple and lightweight event framework

## Benchmarks
TODO

## Example
```java
class MyListener { 
  @EventHandler
  public void listen(MyEvent event) {
    System.out.println(event.getMessage());
  }
}

class MyEvent extends Event {
  private final String message;
	
  public MyEvent(String message) {
    this.message = message;
  }
    
  public String getMessage() {
    return message;
  }
}

EventBus bus = new ConcurrentEventBus();

// register listener
bus.subscribe(new MyListener());

// send event to all listeners
bus.publish(new MyEvent("Hello world!"));
```

You can to specify the `EventNamespace` to `EventBus#subscribe` and then you will be able to
remove all listeners by that `EventNamespace` using `EventBus#unsubscribeAll(EventNamespace)`

Also you can set EventHandler's priority, just use `@EventHandler(EventPriority.<...>)`

There are 6 types of priority:
- `LOWEST` will be executed first
- `LOW` will be executed after `LOWEST`
- `NORMAL` will be executed after `LOW` (default)
- `HIGH` will be executed after `NORMAL`
- `HIGHEST` will be executed after `HIGH`
- `MONITOR` will be executed last
## Add as dependency
<div>
  <a href="https://search.maven.org/artifact/com.github.lero4ka16/ef4j">
    <img src="https://img.shields.io/maven-central/v/com.github.lero4ka16/ef4j">
  </a>
</div>

### Maven
```xml
<dependencies>
    <dependency>
        <groupId>com.github.lero4ka16</groupId>
        <artifactId>ef4j</artifactId>
        <version>1.1.0</version>
    </dependency>
</dependencies>
```

### Gradle
```groovy
dependencies {
    implementation 'com.github.lero4ka16:ef4j:1.1.0'
}
```

## Build the project

1. Execute `./gradlew build`
2. Output file located at `build/libs/ef4j.jar`

## Contact

[Vkontakte](https://vk.com/id623151994),
[Telegram](https://t.me/lero4ka85)

### Post Scriptum

I will be very glad if someone can help me with development.

<!-- @formatter:on  -->