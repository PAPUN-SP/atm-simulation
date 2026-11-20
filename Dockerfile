# ---- build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY src ./src
RUN mkdir out && javac -d out $(find src -name "*.java")

# ---- runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 atm
COPY --from=build /app/out ./out
COPY web ./web
USER atm
ENV PORT=8080 WEB_DIR=/app/web
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1
CMD ["java", "-cp", "out", "com.atm.AtmServer"]
