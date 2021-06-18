plugins {
    `java-library`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(16))
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:"
        )
    }
}

dependencies {
    compileOnlyApi(libs.checkerframework.qual)

    implementation(platform(libs.log4j.bom))
    implementation(libs.log4j.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.truth) {
        exclude(group = "junit")
    }
}

tasks.test {
    useJUnitPlatform()
}
