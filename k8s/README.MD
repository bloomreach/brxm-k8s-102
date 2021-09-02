# 101: BrXM up and running

## MYSQL:

`bash mysql-heml-install.sh ` 

## CONFIGMAP:

`kubectl create -f configmap-my-mysql-variables.yaml`

## SERVICES:

`kubectl create -f services.yaml`

## BRXM deployment:

`kubectl create -f deployment-brxm.yaml`

## Frontend deployments:

`kubectl create -f deployments-frontend.yaml`

## INGRESS:

`kubectl create -f ingress-brxm.yaml`
`kubectl create -f ingress-frontend.yaml`

# 102 Index Export / Import & Repository Maintenance

## INDEX EXPORTER

`kubectl create -f cronjob-index-export.yaml`

## REPOSITORY MAINTENANCE

`bash repository-maintenance/setup-serviceaccount.sh`
`kubectl create -f cronjob-repository-maintenance.yaml`


