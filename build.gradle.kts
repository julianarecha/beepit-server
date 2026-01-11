plugins {
    id("groovy") 
    id("io.micronaut.application") version "4.6.1"
    id("com.gradleup.shadow") version "8.3.9"
    id("io.micronaut.aot") version "4.6.1"
}

version = "1.0.0"
group = "com.beepit.server"

repositories {
    mavenCentral()
}

dependencies {
    // Micronaut
    annotationProcessor("io.micronaut:micronaut-http-validation")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
    
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut.reactor:micronaut-reactor")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.validation:micronaut-validation")
    
    // Views - Thymeleaf
    implementation("io.micronaut.views:micronaut-views-thymeleaf")
    
    // WebSocket
    implementation("io.micronaut:micronaut-websocket")
    
    // Reactive Streams
    implementation("io.projectreactor:reactor-core")
    
    // Akka (Modelo de Actor) - v2.10.9 with Scala 3 for Java 25 compatibility
    implementation("com.typesafe.akka:akka-actor-typed_3:2.8.8")
    
    // Resilience4j - Rate Limiting
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.1.0")
    implementation("io.github.resilience4j:resilience4j-reactor:2.1.0")
    
    // Logging
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.4")
    
    // YAML
    runtimeOnly("org.yaml:snakeyaml")
    
    // Testing
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("com.typesafe.akka:akka-actor-testkit-typed_3:2.8.8")
}

application {
    mainClass.set("com.beepit.server.Application")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<Test> {
    useJUnitPlatform()
}

graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.example.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
    }
}