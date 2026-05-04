# ============================================
# Smart Bug Tracker — Docker Build
# ============================================

FROM eclipse-temurin:23-jre-noble
WORKDIR /app
COPY target/smart-bug-tracker-1.0.0.jar app.jar

LABEL maintainer="bugtracker-team"
LABEL description="Smart Bug and Issue Tracking System"
LABEL version="1.0.0"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD java -cp /dev/null -Djava.net.preferIPv4Stack=true -XX:+TieredCompilation -XX:TieredStopAtLevel=1 \
  -e "try{var c=(java.net.HttpURLConnection)java.net.URI.create(\"http://localhost:8080/api/health\").toURL().openConnection();c.setConnectTimeout(2000);c.setReadTimeout(2000);System.exit(c.getResponseCode()==200?0:1);}catch(Exception e){System.exit(1);}" || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
