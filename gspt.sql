-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- 主机： localhost
-- 生成日期： 2026-03-18 18:39:05
-- 服务器版本： 8.0.35
-- PHP 版本： 8.2.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- 数据库： `gspt`
--

-- --------------------------------------------------------

--
-- 表的结构 `sys_article`
--

CREATE TABLE `sys_article` (
  `title` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文章标题',
  `content` longtext COLLATE utf8mb4_general_ci COMMENT '文章正文内容',
  `preview_content` text COLLATE utf8mb4_general_ci COMMENT '文章预览内容',
  `cover_file_id` bigint DEFAULT NULL COMMENT '封面图片文件ID(关联sys_file表ID)',
  `cover_image_url` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '封面图片完整访问地址',
  `preview_image_id` bigint DEFAULT NULL COMMENT '文章预览图文件ID(关联sys_file表ID)',
  `preview_image_url` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文章预览图/缩略图地址',
  `author_id` bigint DEFAULT NULL COMMENT '作者ID(关联sys_user表ID)',
  `author_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作者name(关联sys_user表用户名)',
  `status` varchar(255) COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '状态：0-草稿, 1-已发布',
  `category` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文章分类：1-需求, 2-企业, 3-公司',
  `id` bigint NOT NULL COMMENT '主键ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文章信息表';

-- --------------------------------------------------------

--
-- 表的结构 `sys_file`
--

CREATE TABLE `sys_file` (
  `original_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原始文件名',
  `storage_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '在磁盘上存储的唯一文件名',
  `file_suffix` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件后缀名',
  `file_size` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件大小(字节)',
  `local_path` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务器本地磁盘绝对路径',
  `url_path` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '前端可访问的相对或绝对URL路径',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传者ID(关联sys_user表ID)',
  `uploader_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '上传者用户名',
  `id` bigint NOT NULL COMMENT '主键ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件上传记录表';

-- --------------------------------------------------------

--
-- 表的结构 `sys_user`
--

CREATE TABLE `sys_user` (
  `username` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名/登录账号',
  `password` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码',
  `nickname` varchar(255) COLLATE utf8mb4_general_ci DEFAULT '默认用户昵称' COMMENT '用户昵称',
  `email` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱地址(企业用户必填,用于接收审核通知)',
  `company_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '公司名称(企业用户必填)',
  `license_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '营业执照名称(企业用户必填)',
  `license_url` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '营业执照图片访问路径',
  `license_file_id` bigint DEFAULT NULL COMMENT '营业执照图片文件ID(关联sys_file表ID)',
  `audit_status` varchar(255) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '审核状态：0-无需审核, 1-待审核, 2-审核通过, 3-审核拒绝',
  `audit_remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核备注/拒绝原因',
  `role` varchar(255) COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '角色标识：1-普通用户, 2-企业用户, 3-总后台',
  `id` bigint NOT NULL COMMENT '主键ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';

--
-- 转存表中的数据 `sys_user`
--

INSERT INTO `sys_user` (`username`, `password`, `nickname`, `email`, `company_name`, `license_name`, `license_url`, `license_file_id`, `audit_status`, `audit_remark`, `role`, `id`, `create_time`, `update_time`) VALUES
('admin', 'admin', 'admin', NULL, NULL, NULL, NULL, NULL, '0', NULL, '3', 1, '2026-03-18 17:51:51', '2026-03-18 17:51:51');

--
-- 转储表的索引
--

--
-- 表的索引 `sys_article`
--
ALTER TABLE `sys_article`
  ADD PRIMARY KEY (`id`);

--
-- 表的索引 `sys_file`
--
ALTER TABLE `sys_file`
  ADD PRIMARY KEY (`id`);

--
-- 表的索引 `sys_user`
--
ALTER TABLE `sys_user`
  ADD PRIMARY KEY (`id`);

--
-- 在导出的表使用AUTO_INCREMENT
--

--
-- 使用表AUTO_INCREMENT `sys_article`
--
ALTER TABLE `sys_article`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID';

--
-- 使用表AUTO_INCREMENT `sys_file`
--
ALTER TABLE `sys_file`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID';

--
-- 使用表AUTO_INCREMENT `sys_user`
--
ALTER TABLE `sys_user`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID', AUTO_INCREMENT=2;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
