SHELL := bash# we want bash behaviour in all shell invocations

.DEFAULT_GOAL = help

### VARIABLES ###
#
export PATH := $(CURDIR):$(CURDIR)/scripts:$(PATH)

### TARGETS ###
#

.PHONY: binary
binary: clean ## Build the binary distribution
	@mvnw package -Dmaven.test.skip

.PHONY: docker-image
docker-image: binary ## Build Ubuntu-based Docker image
	@docker build \
	  --file Dockerfile \
	  --tag pivotalrabbitmq/super-stream-app:latest \
	  .

.PHONY: test-docker-image
test-docker-image: ## Test the Ubuntu-based Docker image
	@docker run -it --rm pivotalrabbitmq/super-stream-app:latest --help

.PHONY: push-docker-image
push-docker-image: ## Push docker image to Docker Hub
	@docker push pivotalrabbitmq/super-stream-app:latest

.PHONY: delete-docker-image
delete-docker-image: ## Delete the created Docker image from the local machine
	@docker rmi pivotalrabbitmq/super-stream-app:latest

.PHONY: clean
clean: 	## Clean all build artefacts
	@mvnw clean

.PHONY: compile
compile: ## Compile the source code
	@mvnw compile

.PHONY: install
install: clean ## Create and copy the binaries into the local Maven repository
	@mvnw install -Dmaven.test.skip

.PHONY: jar
jar: clean ## Build the JAR file
	@mvnw package -Dmaven.test.skip

.PHONY: run
run: compile ## Run super stream app, pass exec arguments via ARGS, e.g. ARGS="-x 1 -y 1 -r 1"
	@mvnw exec:java -Dexec.mainClass="com.rabbitmq.stream.SuperStreamApp" -Dexec.args="$(ARGS)"