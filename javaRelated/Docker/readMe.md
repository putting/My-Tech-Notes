# Docker readMe

Sample app with dockerfile and docker compose.
Sql Server is also Docker deployed.

## App: Spring Launcher
```yml
FROM eclipse-temurin:17 as builder
WORKDIR application
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:17
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
#COPY --from=builder application/resources/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
```

## Docker Compose
This should be located on the target env.
```yml
#
# Builds all carbon services
#
# Create docker volume
# $ docker volume create carbon-sqlserver-data
# Build projects and run docker-compose
# $ gradle clean build
# $ docker-compose up -d --build --force-recreate --always-recreate-deps
#
# SQServer Initialization:
# Create Carbon_DEV1 Database:
# $ docker exec carbon-sqlserver /opt/mssql-tools/bin/sqlcmd -U sa -P "PASSXXX." -H localhost -Q "CREATE DATABASE Carbon_DEV1"
#
#
version: "3.8"

networks:
  default:
    external:
      name: carbon-api-network

volumes:
  carbon-sqlserver-data:
    external: true


services:

  carbon-sqlserver:
    image: mcr.microsoft.com/mssql/server:2019-latest
    container_name: carbon-sqlserver
    restart: always
    volumes:
      - "carbon-sqlserver-data:/var/opt/mssql"


    ports:
      - "1431:1431"
      - "1433:1433"
      - "1434:1434"
    environment:
      SA_PASSWORD: "XXXXXX"
      ACCEPT_EULA: "Y"
    healthcheck:
      test: /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P "$${SA_PASSWORD}" -Q 'SELECT 1' || exit 1
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 40s

  carbon-config-service:
    image: amphora-nexus.amphora.net:8485/carbon/api/config-service:SNAPSHOT-20220225164055
    container_name: carbon-config-service
    restart: always
    ports:
      - "8999:8999"
      - "7999:7999"
    healthcheck:
      test: "curl --fail --silent localhost:7999/actuator/health | grep UP || exit 1"
      interval: 30s
      timeout: 5s
      retries: 5
      start_period: 40s
    depends_on:
      carbon-sqlserver:
        condition: service_healthy
        
  carbon-program-api:
    image: amphora-nexus.amphora.net:8485/carbon/api/program-api:SNAPSHOT-20220225164055
    container_name: carbon-program-api
    environment:
      - SPRING_PROFILES_ACTIVE=dev1,dev1-development
      - spring.cloud.config.host=carbon-config-service
#      - database.host=carbon-sqlserver
#      - spring.jpa.hibernate.ddl-auto=update
    restart: always
    ports:
      - "8388:8388"
      - "7388:7388"
    depends_on:
      carbon-config-service:
        condition: service_healthy
    healthcheck:
      test: "curl --fail --silent localhost:7388/actuator/health | grep UP || exit 1"
      interval: 30s
      timeout: 5s
      retries: 5
      start_period: 40s
```

### Updating the Docker Images above by GitHub Actions (does a commit)
A deamon process running every few changes does a Git diff with local copy and fetches if there are changes.
```bash
#!/bin/bash

# Check if docker-compose.yml in git repository has been updated.
# If there is any update, pull the latest version and rebuild docker containers.

set -o errexit # Exit if any command fails
set -o nounset # Exit if script tries to use unset variable

commandName=$(basename $0)

# Use 'realpath' because 'readlink -m' does not work on MacOS.
# Install 'realpath' in MacOS via "brew install coreutils"
scriptPath=$(realpath -m "$(dirname $0)" )

# Variable for keeping diff between local and remote docker-compose.yml files.
docker_compose_diff=""

printUsageAndExit() {
  cat << USAGE >&2
  This script checks the github repository for the latest version of docker-compose.yml file for given environment.
  If the file has been updated, it will be pulled and docker-compose stack will be recreated.
  Usage: ${commandName} -e <environment> [-f] [-d]
    -e : Environment, should match one of the folders in "env". i.e. dev1, dev2, qa
    -f : Force rebuild docker-compose stack. Even if the repository has not been updated, will trigger docker-compose down / docker-compose up command sequence.
    -d : Run 'docker-compose down' before 'dpcker-compose up'
    -q : Quiet
    -v : Verbose

USAGE
  exit 1
}

validateParameters() {
  if [ -z "${environment}" ]; then
    printUsageAndExit
  fi
}


# Prints given parameter to std out only if quiet is not set.
log_info() {
  if [[ $quiet -ne 1 ]]; then  echo -e "$(date +"%Y_%m_%d %H:%M:%S") $@";  fi
}

# Prints given parameter to std out only if verbose is set.
log_verbose() {
  if [[ $verbose -eq 1 ]]; then  echo -e "$(date +"%Y_%m_%d %H:%M:%S") $@";  fi
}

# Prints given parameter to both std out/err
log_error() {
  echo -e "$(date +"%Y_%m_%d %H:%M:%S") $@" 1>&2
}


check_docker_compose_file_updates() {
  log_info "Compare local docker-compose.yml to remote master"


  log_info "Fetch origin ${gitBranch}"
  git fetch origin ${gitBranch}

  log_info "Compare local '${dockerComposeFile}' to remote version"
  docker_compose_diff=`git diff origin/${gitBranch} "${dockerComposeFile}"`


  if [ -z "${docker_compose_diff}" ]
  then
    log_info "'${dockerComposeFile}' file has not been modified on origin/${gitBranch}"
  else
    log_info "'${dockerComposeFile}' file has been modified on origin/${gitBranch}"
    log_verbose "${docker_compose_diff}"
  fi


}
update_local_repo() {
  log_info "Updating local repository."
  git pull
}

rebuild_docker_stack() {
  log_info "Rebuildinig docker stack for ${dockerComposeFile}"

  log_info "Pull docker images"
  docker-compose --file "${dockerComposeFile}" pull

  if [[ ${runComposeDown} -eq 1 ]]
  then
    log_info "docker-compose down"
    docker-compose --file "${dockerComposeFile}" down
  fi

  log_info "docker-compose up"
  docker-compose --file "${dockerComposeFile}" up -d

}

# #######################################################################################
# Main
# #######################################################################################
while getopts "e:fdvq" opt
do
  case $opt in
    e) environment="${OPTARG}";;
    v) verbose=1; quiet=0;;
    q) quiet=1; verbose=0;;
    f) forceDockerRebuild=1;;
    d) runComposeDown=1;;
  esac
done

environment=${environment:-}
quiet=${quiet:-}
verbose=${verbose:-}
forceDockerRebuild=${forceDockerRebuild:-}
runComposeDown=${runComposeDown:-}

validateParameters

dockerComposeFile="env/${environment}/docker-compose.yml"

log_info "Script Path: '${scriptPath}'"
log_info "Environment: ${environment}"
log_info "docker-compose file: '${dockerComposeFile}'"

# Script should run on ~/carbon-cd/docker-builds
pushd "${scriptPath}/.."

gitBranch=$(git branch --show-current)
log_info "Git Working Branch: '${gitBranch}'"

log_info "Working in folder $(pwd)"



if [[ ${forceDockerRebuild} -eq 1 ]]; then update_local_repo; rebuild_docker_stack; exit 0;  fi

check_docker_compose_file_updates

  if [ ! -z "${docker_compose_diff}" ]
  then
    update_local_repo
    rebuild_docker_stack
  fi

popd

```

### Removing containers and images
```bash
#!/bin/bash

# Removes all docker containers and images, but not volumes.
# Run this script to cleanup space on the host.

# Remove docker containers
docker ps -a -q | xargs -r docker rm -fv

# Remove docker images
docker images -q | xargs -r docker rmi -f
```
