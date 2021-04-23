TERRAFORM := cd terraform && terraform

.PHONY:\
	plan \
	run \
	deploy \
	destroy

run:
	sbt client/run

plan:
	$(TERRAFORM) plan

deploy:
	rm -rf dist/
	sbt function/assembly
	$(TERRAFORM) apply

destroy:
	$(TERRAFORM) destroy
