FROM quay.io/keycloak/keycloak:26.4.2 AS distribution
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build
COPY --from=distribution /opt/keycloak/lib/lib/main /classpath
COPY src ./src
COPY services ./classes/META-INF/services
RUN javac -cp '/classpath/*' -d classes src/com/meridian/identity/*.java && jar --create --file meridian-theme-selector.jar -C classes .
FROM distribution
COPY --from=build --chown=keycloak:keycloak /build/meridian-theme-selector.jar /opt/keycloak/providers/meridian-theme-selector.jar
COPY --chown=keycloak:keycloak themes /opt/keycloak/themes
