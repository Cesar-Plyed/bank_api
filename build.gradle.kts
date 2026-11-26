plugins {
    id("io.micronaut.application") version "4.6.2"
    id("com.gradleup.shadow") version "8.3.9"
    id("io.micronaut.aot") version "4.6.2"
}

version = "0.1"
group = "io.onstructive.micronaut"



repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("io.micronaut.data:micronaut-data-processor")
    annotationProcessor("io.micronaut:micronaut-http-validation")
    annotationProcessor("io.micronaut.openapi:micronaut-openapi")
    annotationProcessor("io.micronaut.security:micronaut-security-annotations")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    // spec-05: sin el processor los @Valid son no-op; sin el runtime no hay ConstraintViolationException.
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.flyway:micronaut-flyway")
    // Flyway 10 externalizó el soporte de PostgreSQL a este módulo.
    runtimeOnly("org.flywaydb:flyway-database-postgresql:10.22.0")
    implementation("io.micronaut.security:micronaut-security")
    // spec-02: JWT firmado + BCrypt para el hash de contraseñas.
    implementation("io.micronaut.security:micronaut-security-jwt")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    // spec-06: management endpoints (/actuator/health) + health indicators.
    implementation("io.micronaut:micronaut-management")
    compileOnly("io.micronaut:micronaut-http-client")
    compileOnly("io.micronaut.openapi:micronaut-openapi-annotations")
    runtimeOnly("ch.qos.logback:logback-classic")
    // spec-06: encoder JSON de Logstash para logs estructurados por línea.
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:8.1")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("io.micronaut:micronaut-http-client")
    // spec-06: el test del encoder necesita logback y Logstash en compile classpath.
    testImplementation("ch.qos.logback:logback-classic")
    testImplementation("net.logstash.logback:logstash-logback-encoder:8.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}



application {
    mainClass = "io.onstructive.micronaut.Application"
}

java {
    sourceCompatibility = JavaVersion.toVersion("21")
    targetCompatibility = JavaVersion.toVersion("21")
}




graalvmNative.toolchainDetection = false





micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("io.onstructive.micronaut.*")
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


tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}







