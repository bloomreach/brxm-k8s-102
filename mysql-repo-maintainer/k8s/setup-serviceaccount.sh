
#The following creates a serviceaccount object called "brxm-repo-maintainer"
# with privilege to read pod names from kube-apiserver
kubectl create -f brxm-repo-maintainer-serviceaccount.yaml
kubectl create -f brxm-repo-maintainer-role.yaml
kubectl create -f brxm-repo-maintainer-rolebinding.yaml
