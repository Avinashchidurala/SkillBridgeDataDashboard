# USE AN OFFICIAL MAVEN IMEAGE TO BUILD THE SPRING BOOT APP
FROM maven:3.8.4-openjdk-17 AS build

# SET THE WORKING DIRECTORY
WORKDIR /app

#Copy the pom.xml and install dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

#COPY THE SOURCE CODE AND BUILD THE APPLICATION
COPY src ./src
RUN mvn clean package -DskipTests

#USE an official OPENJDK IMAGE to run the application
FROM openjdk:17-jdk-slim

#set the working directory
WORKDIR /app

#Copy the built JAR FILE FROM the build stage
COPY --from=build /app/target/SkillBridgeDataDashboard-0.0.1-SNAPSHOT.jar .

#Expose port 8080
EXPOSE 8080

#Specify the command to run the application
ENTRYPOINT ["java","-jar","/app/SkillBridgeDataDashboard-0.0.1-SNAPSHOT.jar"]