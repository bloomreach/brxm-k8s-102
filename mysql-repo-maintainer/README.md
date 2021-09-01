# brXM Repo Maintainer Application
### A standalone application that takes care of running [repository maintenance queries](https://documentation.bloomreach.com/library/enterprise/installation-and-configuration/repository-maintenance.html) in a kubernetes cluster
#### Run it locally in minikube:

(Tested minikube version: v1.5.2)

Install virtualbox https://www.virtualbox.org/wiki/Downloads

Install minikube https://github.com/kubernetes/minikube
```bash
brew cask install minikube
```
Start minikube with some additional resources

```bash
minikube --memory 8192 --cpus 2 start
```

Setup helm (tested with v3.0.0) (kubernetes package manager) https://github.com/helm/helm
```bash
brew install kubernetes-helm
```

Switch to kubernetes folder
```bash
cd kubernetes
```

Setup a postgresql db for brxm

```bash
./setup_db.sh
```

After db is up, create a brxm deployment (from kubernetes directory)

```bash
./deploy-brxm.sh
```

Create serviceaccount for brxm-repo-maintainer application so that it can read pod names from kube-apiserver

```bash
./setup-serviceaccount.sh
```

To be able to work with the docker daemon on your mac/linux host use the docker-env command in your shell
```bash
eval $(minikube docker-env)
```
* More info on the above command is at: https://kubernetes.io/docs/setup/minikube#reusing-the-docker-daemon

Now that you have run the eval command above, build the brxm-repo-maintainer image: (you have to keep using the same shell!)

```bash
cd .. # switch to pom.xml directory
mvn clean compile jib:dockerBuild
``` 

Create either a cronjob (brxm-repo-maintainer-cronjob.yaml), job (brxm-repo-maintainer-job.yaml), or a regular deployment (brxm-repo-maintainer.yaml)

```bash
kubectl create -f kubernetes/brxm-repo-maintainer-job.yaml
```

Caveats:
* This application is built for postgresql. For other relational dbs the jdbc connection and the actual queries run should be tweaked.
* The k8s manifest files (deployment/job/cronjob) rely on a serviceaccount named "brxm-repo-maintainer". 
You may or may not have the rights to create such a k8s object in your own organization.
* Ideally you want to run brxm-repo-maintainer as a cronjob and once a day. (Right after a daily db backup is done for example)
* BRXM_SELECTOR envrionment variable is used to select the brxm pods. This value should match the labels on brxm pod manfiest. 
(See "app: brxm" in kubernetes/brxm.yaml)
* If you are running brXM as stateful sets, you also have to change how journal_id's are constructed. 
Then, they have the form: pod_name.service_name.namespace_name.svc.cluster.local