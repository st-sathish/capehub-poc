
## RUN

### Export environment variables
```sh
echo "export M2_REPO=/home/$USER/.m2/repository" >> ~/.bashrc
echo "export FELIX_HOME=/opt/capehub" >> ~/.bashrc
echo "export JAVA_OPTS='-Xms1024m -Xmx1024m -XX:MaxPermSize=256m'" >> ~/.bashrc
source ~/.bashrc
```