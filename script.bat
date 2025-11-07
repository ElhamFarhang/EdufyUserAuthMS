@echo off
echo Stopping edufyuserauthms
docker stop edufyuserauthms
echo Deleting container edufyuserauthms
docker rm edufyuserauthms
echo Deleting image edufyuserauthms
docker rmi edufyuserauthms
echo Running mvn package (skips tests)
call mvn package -DskipTests
echo Creating image edufyuserauthms
docker build -t edufyuserauthms .
echo Creating and running container edufyuserauthms
docker run -d -p 9903:9903 --name edufyuserauthms --network edufy_network edufyuserauthms
echo Done!