--
-- History Monitor Schema
--

USE [master];
GO

-- Create database
CREATE DATABASE [historymonitordb] COLLATE SQL_Latin1_General_CP1_CS_AS;
GO

-- Configure DB settings
ALTER DATABASE [historymonitordb] SET READ_COMMITTED_SNAPSHOT ON;
ALTER DATABASE [historymonitordb] SET AUTO_CLOSE OFF ; 
ALTER DATABASE [historymonitordb] SET AUTO_CREATE_STATISTICS ON; 
ALTER DATABASE [historymonitordb] SET AUTO_UPDATE_STATISTICS ON; 
ALTER DATABASE [historymonitordb] SET AUTO_UPDATE_STATISTICS_ASYNC ON;
GO

-- Create logins
CREATE LOGIN [historymonitoruser] WITH PASSWORD = 'DbHi$tory';
GO

-- Create users, schema and grants
USE [historymonitordb];
GO

CREATE USER [historymonitoruser] FOR LOGIN [historymonitoruser] WITH DEFAULT_SCHEMA = [historymonitordb];
GO

-- Create schema
CREATE SCHEMA [historymonitordb] AUTHORIZATION [historymonitoruser];
GO

GRANT CREATE TABLE, ALTER, SELECT, INSERT, UPDATE, DELETE ON DATABASE::[historymonitordb] TO [historymonitoruser];
GO

-- Create table
USE[historymonitordb];
GO

CREATE TABLE [historymonitordb].[historymonitordb].[history_monitor_table] (
    [id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [Event] [nvarchar](max) NULL,
    [Site] [nvarchar](max) NULL,
    [UserName] [nvarchar](max) NULL,
    [ObjectID] [nvarchar](max) NULL,
	[ObjectType] [nvarchar](max) NULL,
	[ObjectDisplayName] [nvarchar](max) NULL,
	[Timestamp] [datetime],
);
GO
