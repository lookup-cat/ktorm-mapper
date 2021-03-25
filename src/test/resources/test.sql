/*
SQLyog Community v13.1.6 (64 bit)
MySQL - 10.5.8-MariaDB : Database - KtormMapper
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`KtormMapper` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `KtormMapper`;

/*Table structure for table `Permission` */

DROP TABLE IF EXISTS `Permission`;

CREATE TABLE `Permission` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

/*Data for the table `Permission` */

insert  into `Permission`(`id`,`name`) values 
(1,'用户管理'),
(2,'系统管理'),
(3,'数据总览');

/*Table structure for table `Role` */

DROP TABLE IF EXISTS `Role`;

CREATE TABLE `Role` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

/*Data for the table `Role` */

insert  into `Role`(`id`,`name`) values 
(1,'管理员'),
(2,'用户'),
(3,'运维');

/*Table structure for table `RolePermission` */

DROP TABLE IF EXISTS `RolePermission`;

CREATE TABLE `RolePermission` (
  `roleId` int(11) DEFAULT NULL,
  `permissionId` int(11) DEFAULT NULL,
  UNIQUE KEY `RolePermission_pk` (`roleId`,`permissionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `RolePermission` */

insert  into `RolePermission`(`roleId`,`permissionId`) values 
(1,1),
(1,2),
(1,3),
(2,3),
(3,1);

/*Table structure for table `User` */

DROP TABLE IF EXISTS `User`;

CREATE TABLE `User` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

/*Data for the table `User` */

insert  into `User`(`id`,`username`) values 
(1,'张三'),
(2,'李四'),
(3,'王五');

/*Table structure for table `UserRole` */

DROP TABLE IF EXISTS `UserRole`;

CREATE TABLE `UserRole` (
  `userId` int(11) DEFAULT NULL,
  `roleId` int(11) DEFAULT NULL,
  UNIQUE KEY `UserRole_pk` (`userId`,`roleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `UserRole` */

insert  into `UserRole`(`userId`,`roleId`) values 
(1,1),
(2,2),
(2,3),
(3,3);

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
