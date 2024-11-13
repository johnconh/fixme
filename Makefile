all:
	docker compose up -d

run:
	@./launch.sh

down:
	docker compose down

postgres:
	docker exec -it postgres psql -U jdasilva -d fixme -h localhost -W

clean: down
	yes | docker system prune -a

.PHONY: all run down postgres clean