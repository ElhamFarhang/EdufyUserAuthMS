@echo off
echo Stopping usermicroservice
docker stop usermicroservice
echo Deleting container usermicroservice
docker rm usermicroservice
echo Deleting image usermicroservice
docker rmi usermicroservice
echo Running mvn package (skips tests)
call mvn package -DskipTests
echo Creating image usermicroservice
docker build -t usermicroservice .
echo Creating and running container usermicroservice
docker run -d -p 9902:9902 --name usermicroservice --network edufy_network usermicroservice
echo Done!