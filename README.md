# Capehub

## Mysql

### Create database by executing below command
```sh
CREATE DATABASE capehub CHARACTER SET utf8 COLLATE utf8_general_ci;
 ```
 
 ### Give grant permission to capehub user
 ```sh
 GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP,INDEX ON capehub.* TO 'capehub'@'localhost' IDENTIFIED BY 'capehub';
 ```