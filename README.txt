Esto es un proyecto para la universidad en el que se desea construir un simulador de clientes peer to peer que se conectan mediante una autenticación segura entre ellos. Para ello hacen uso de una base de datos con los diferentes verificadores de contraseñas. 

// Primera parte, obtener las clases de los ficheros fuente
javac [options] <destination> <source_files>
-----------------------------------------------------------
// Do this from dist folder
javac -d . ../src/app/*.java
javac -d . ../src/verifier/*.java
javac -d . ../src/simulator/*.java

// Segunda parte, obtener los diversos jars
jar [options] <destination> <class_files>
-----------------------------------------------------------
// Do this from dist folder
jar cvf ../application.jar app/*.class verifier/*.class simulator/AddClient.class simulator/CheckClient.class simulator/GetClientVerifier.class
jar cvf ../simulator.jar simulator/AppServiceSimulator.class

// Tercera parte, ejecución del simulador sin policy
java [options] <jar_files>
-----------------------------------------------------------
// Do this from UpnaSignal folder
//Para Windows
java -cp simulator.jar;application.jar;lib/derbyclient.jar -Djava.security.auth.login.config=./etc/login.conf simulator.AppServiceSimulator
//Para Linux
java -cp simulator.jar:application.jar;lib/derbyclient.jar -Djava.security.auth.login.config=./etc/login.conf simulator.AppServiceSimulator

// Cuarta parte, ejecución del simulador con policy
java [options] <jar_files>
-----------------------------------------------------------
// Do this from UpnaSignal folder
// Para Windows
java -cp simulator.jar;application.jar;lib/derbyclient.jar -Djava.security.auth.login.config=./etc/login.conf -Djava.security.manager -Djava.security.policy=./etc/security.policy simulator.AppServiceSimulator 
// Para Linux
java -cp simulator.jar:application.jar:lib/derbyclient.jar -Djava.security.auth.login.config=./etc/login.conf -Djava.security.manager -Djava.security.policy=./etc/security.policy simulator.AppServiceSimulator

// Información sobre el simulador
La primera ejecución es distinta de la segunda, puesto que en la primera se añaden un par de contactos y en la segunda no se vuelven a añadir. Es por ello que recomiendo borrar las entradas de la base de datos después de una segunda ejecución, de esa forma se pueden ver diferentes escenarios. Al no esperarse los hilos unos a otros se puede observar que mientras un usuario añade a alguien y otro lee, a veces consigue leer después de que ese usuario se añada y otras no lo encuentra, puesto que se añade después de la lectura.