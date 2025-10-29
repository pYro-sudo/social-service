build:
	docker build -t pyrodocker1/user-service:latest ./user-service
	docker build -t pyrodocker1/image-service:latest ./image-service
	docker build -t pyrodocker1/comment-like-service:latest ./comment-like-service
	docker build -t pyrodocker1/social-activity-service:latest ./activity-service
	docker build -t pyrodocker1/api-gateway:latest ./api-gateway
	docker build -t pyrodocker1/photo-sharing-frontend:latest ./photo-sharing-app

push: build
	docker push pyrodocker1/user-service:latest
	docker push pyrodocker1/image-service:latest
	docker push pyrodocker1/comment-like-service:latest
	docker push pyrodocker1/social-activity-service:latest
	docker push pyrodocker1/api-gateway:latest
	docker push pyrodocker1/photo-sharing-frontend:latest

deploy: push
	terraform init
	terraform apply -auto-approve

kind-load: build
	kind load docker-image pyrodocker1/user-service:latest
	kind load docker-image pyrodocker1/image-service:latest
	kind load docker-image pyrodocker1/comment-like-service:latest
	kind load docker-image pyrodocker1/social-activity-service:latest
	kind load docker-image pyrodocker1/api-gateway:latest
	kind load docker-image pyrodocker1/photo-sharing-frontend:latest