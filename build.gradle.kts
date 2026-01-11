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
    
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut.reactor:micronaut-reactor")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    
    // WebSocket
    implementation("io.micronaut:micronaut-websocket")
    
    // Reactive Streams
    implementation("io.projectreactor:reactor-core")
    
    // Akka (Modelo de Actor)
    implementation("com.typesafe.akka:akka-actor-typed_2.13:2.8.5")
    implementation("com.typesafe.akka:akka-stream_2.13:2.8.5")
    
    // Logging
    runtimeOnly("ch.qos.logback:logback-classic")
    
    // YAML
    runtimeOnly("org.yaml:snakeyaml")
}

application {
    mainClass.set("com.beepit.server.Application")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("spock2")
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